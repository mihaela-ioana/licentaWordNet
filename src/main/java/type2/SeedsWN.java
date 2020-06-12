package type2;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.dictionary.Dictionary;
import shared.MultipleInitSeeds;
import shared.SharedMethods;
import shared.StopWords;
import shared.WordMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public class SeedsWN {

    private Dictionary dictionary;
    private List<String> corpus;
    private List<String> corpusWithDuplicates;
    private Map<String, Long> corpusWordsWithFrequency;
    private List<String> stopWords;
    private List<String> posAdj = MultipleInitSeeds.posAdj;
    private List<String> posSubst = MultipleInitSeeds.posSubst;
    private List<String> posVerbs = MultipleInitSeeds.posVerbs;
    private List<String> negAdj = MultipleInitSeeds.negAdj;
    private List<String> negSubst = MultipleInitSeeds.negSubst;
    private List<String> negVerbs = MultipleInitSeeds.negVerbs;
    private List<Seed> negSeeds = new ArrayList<>();
    private List<Seed> posSeeds = new ArrayList<>();

    public SeedsWN() {
        try {
            JWNL.initialize(new FileInputStream("E:\\licenta\\wordNet\\properties.xml"));
            this.dictionary = Dictionary.getInstance();

            //stop words = words with freq > 8000
            this.stopWords = StopWords.stopWords;

            //get map with words and word frequency from xml file
            this.corpusWordsWithFrequency = SharedMethods.loadCorpusFromXMLFile();
            //build corpus
            putWordsFromMapInList();
            //eliminate stop words from corpus
            computeWords() ;

            //with one single pos
//            Map<Long, Integer> negResult = seedWithWord("bad", 4, POS.ADJECTIVE);
//            this.negSeeds = seedsToVectors(negResult, -1.0, POS.ADJECTIVE);
//
//
//            Map<Long, Integer> posResult = seedWithWord("good", 4, POS.ADJECTIVE);
//            this.posSeeds = seedsToVectors(posResult, 1.0, POS.ADJECTIVE);

            Map<Long, Integer> negResult = seedWithWordList(this.negVerbs, 4, POS.VERB);
            this.negSeeds = seedsToVectors(negResult, -1.0, POS.VERB);

            Map<Long, Integer> posResult = seedWithWordList(this.posVerbs, 4, POS.VERB);
            this.posSeeds = seedsToVectors(posResult, 1.0, POS.VERB);

//            eliminateAndEditDuplicates();
            this.posSeeds = roundLabels(this.posSeeds);
            this.negSeeds = roundLabels(this.negSeeds);

            String data = this.createDataForLIBSVMFile(this.negSeeds);
            data+= this.createDataForLIBSVMFile(this.posSeeds);

            this.writeSeedsToSVMFile(data);

        } catch (JWNLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * In order to make the features vector, we have to get for every word from the synset definition the corpus index.
     * We also get the frequency for every word from the synset definition (paraphrase).
     * @param paraphrase
     * @return
     */
    public Map<Integer, Integer> phraseToVector(String paraphrase) {
        Map<Integer, Integer> result = new HashMap<>();
        String[] splittedParaphrase = paraphrase.split("[\\s+,/]");

        for (String word: splittedParaphrase) {
            word = word.replace("(", "");
            word = word.replace(")", "");
            word = word.replace(";", "");
            word = word.replace("\"", "");

            Integer index = this.corpus.indexOf(word);
            if (index >= 0){
                result.put(index, result.containsKey(index) ? result.get(index) + 1 : 1);
            }
        }

        return result;
    }

    /**
     * For a given synset id, the label is calculated:
     *      sign = -1 (for negative seeds) / 1 (for positive seeds)
     *      foundAt = the depth where the synset was found.
     * @param pos
     * @param synsetId
     * @param foundAt
     * @param sign
     * @return
     * @throws JWNLException
     */
    public Seed synsetDefToVector(POS pos, Long synsetId, Integer foundAt, Double sign) throws JWNLException {
        Synset synset = this.dictionary.getSynsetAt(pos, synsetId);

        if (synset.getGloss() != "") {
            return new Seed( Math.floor((sign/(foundAt+1))*100)/100, phraseToVector(synset.getGloss()));
        }

        return null;
    }

    /**
     * Method that builds for every reached synset the seed: label && feature vector.
     * @param reachedSynsets
     * @param sign
     * @param pos
     * @return
     * @throws JWNLException
     */
    public List<Seed> seedsToVectors(Map<Long, Integer> reachedSynsets, Double sign, POS pos) throws JWNLException {
        List<Seed> reachedSeeds = new ArrayList<>();

        for (Long key: reachedSynsets.keySet()) {
            Seed defToVector = synsetDefToVector(pos, key, reachedSynsets.get(key), sign);
            if (defToVector!=null && defToVector.getFeatures().size() > 0) {
                reachedSeeds.add(defToVector);
            }
        }

        return reachedSeeds;
    }

    /**
     * For every seed that was reached in both positive and negative class,
     *      we make an average from their labels and eliminate one of the two seeds.
     */
    public void eliminateAndEditDuplicates() {
        for (Seed seed : posSeeds) {
            Integer similarSeedIndex = searchSeedInList(seed, this.negSeeds);
            if (similarSeedIndex >= 0) {
                Integer posIndex = posSeeds.indexOf(seed);
                seed.setLabel( Math.floor(((seed.getLabel() + this.negSeeds.get(similarSeedIndex).getLabel()) / 2)*100)/100);
                posSeeds.set(posIndex, seed);
                negSeeds.remove(negSeeds.get(similarSeedIndex));
            }
        }

    }

    /**
     * Method that check whether there are duplicates in the seeds list:
     *      duplicates are considered two seeds hat have the same feature vectors.
     * @param searchedSeed
     * @return
     */
    public Integer searchSeedInList(Seed searchedSeed, List<Seed> seeds) {
        for (Integer index=0; index<seeds.size(); index++ ) {
            if (seeds.get(index).getFeatures().equals(searchedSeed.getFeatures())){
                return index;
            }
        }

        return -1;
    }

    /**
     * The labels for our seeds are in R. In order to have a classification problem, we only need 3 label.
     * This is why we round the labels. We will only hae three labels:
     *      -1.0 for negative
     *      0.0 neutral
     *      1.0 positive
     * @param seeds
     * @return
     */
    public List<Seed> roundLabels (List<Seed> seeds) {
        for (Seed seed: seeds) {
            double label = 0.0;
            if (seed.getLabel() >= 0.2){
                label = 1.0;
            }
            else if (seed.getLabel() <= -0.2) {
                label = -1.0;
            }

            Integer seedIndex = searchSeedInList(seed, seeds);
            seed.setLabel(label);
            seeds.set(seedIndex, seed);
        }

        return seeds;
    }

    /**
     * Recursive method that crosses through the related synsets of a given synset.
     *
     * @return
     * @throws Exception
     */
    public Map<Long, Integer> bfwalk_with_depth(Long synsetOffset, Integer steps, POS categ, Map<Long, Integer> visited) throws Exception {
        Synset synset = this.dictionary.getSynsetAt(categ, synsetOffset);
        System.out.println(synset.getOffset());
        if (synset == null) {
            throw new Exception("Synset with offset '" + synsetOffset + "' is not in the wordnet");
        }

        visited.put(synsetOffset, steps);

        //visit all relations, except antonymy
        List<PointerType> type = PointerType.getAllPointerTypesForPOS(categ);
        List<Pointer> relations = new ArrayList<>();
        for (PointerType pt: type) {
            if (!pt.equals(PointerType.ANTONYM)){
                relations.addAll(Arrays.asList(synset.getPointers(pt)));
            }
        }

        for (Pointer pointer : relations) {
            Synset syn = pointer.getTargetSynset();
            if (!visited.keySet().contains(syn.getOffset()) && ( categ == null || syn.getPOS().equals(categ))) {
                if (steps > 1) {
                    bfwalk_with_depth(syn.getOffset(), steps - 1, categ, visited);
                } else {
                    visited.put(syn.getOffset(), 0);
                }
            }
        }

        return visited;
    }

    /**
     * For the given synset id, a function is called (bfwalk) which returns a list with all
     *  the synsets that were reached with "steps" steps.
     * @return
     */
    public Map<Long, Integer> seedWithId(Long synsetOffset, Integer steps, POS categ){
        Map<Long, Integer> result = new HashMap<>();
        System.out.println("Travel breadth-first through wordnet starting with synset id " + synsetOffset);

        try {
            Map<Long, Integer> visited = bfwalk_with_depth(synsetOffset, steps, categ, new HashMap<>());
            for (Map.Entry<Long, Integer> pair: visited.entrySet()) {
                result.put(pair.getKey(), steps-(pair.getValue()));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return result;
    }

    /**
     * Method that receives a list with synsets of the starting word which as given.
     * For every synset in list, the program crosses through all related synsets with given depth (steps).
     * @return a list with all synsets and the min step where they occured
     */
    public Map<Long, Integer> seedWithIds(List<Synset> synsets, Integer steps, POS categ) {
        Map<Long, Integer> result = new HashMap<>();
        Map<Long, Integer> tmp;
        for ( Synset syn : synsets) {
            tmp = seedWithId(syn.getOffset(), steps, categ);
            for (Long synsetId: tmp.keySet()) {
                //take the lowest value
                if (result.get(synsetId) != null) {
                    result.put(synsetId, result.get(synsetId) > tmp.get(synsetId) ? tmp.get(synsetId) : result.get(synsetId));
                } else {
                    result.put(synsetId, tmp.get(synsetId));
                }
            }
        }

        return result;
    }

    /**
     * Method that receives a starting word, a category for the next to be traveled words and a depth
     *  (how far from the given synset should the program go.
     * @param word -> given start word
     * @param steps -> the iteration depth (how far is the found synset from the given one
     * @param categ -> word category, can be : adj, verb, nomen
     * @return a list of synset id's and the minimum position where they were found
     * @throws Exception
     */
    public Map<Long, Integer> seedWithWord(String word, Integer steps, POS categ) throws Exception {
        if (steps < 0){
            steps = 100;
        }
        Map<Long, Integer> result = new HashMap<>();
        //get all synsets
        IndexWord indexWord = this.dictionary.getIndexWord(categ, word);
        if (indexWord != null) {
            List<Synset> synsets = Arrays.asList(indexWord.getSenses());
            if (synsets.size() > 0){
                result = seedWithIds(synsets, steps, categ);
            }
        }

        return result;
    }

    /**
     * Start the seeding process from multiple initial seeds.
     * This method takes a list with words from which the trasersing the relational graph begins.
     * @param list
     * @param steps
     * @param pos
     * @return
     */
    public Map<Long, Integer> seedWithWordList(List<String> list, Integer steps, POS pos) throws Exception {

        Map<Long, Integer> result = new HashMap<>();
        for (String word : list) {
            Map<Long, Integer> resultForWord = seedWithWord(word, steps, pos);
            for (Long synsetId: resultForWord.keySet()) {
                //take the lowest depth if the synset already exists in map
                if (result.get(synsetId) != null) {
                    result.put(synsetId, result.get(synsetId) > resultForWord.get(synsetId) ? resultForWord.get(synsetId) : result.get(synsetId));
                } else {
                    result.put(synsetId, resultForWord.get(synsetId));
                }
            }
        }

        return result;
    }

    //eliminate stop words, numbers and words having len < 3 from corpus
    public void computeWords(){
        System.out.println("Corpus size: " + this.corpus.size());
        this.corpus = this.corpus.stream()
                .filter(x -> (!this.stopWords.contains(x) && x.matches("[a-zA-Z\\-]{3}[a-zA-Z\\-]*") && !x.isEmpty()))
                .collect(Collectors.toList());
        System.out.println("Corpus size after removing stop words: " + this.corpus.size());
    }

    /**
     * Mehod that takes all read data from xml and build a corpus
     *      with all the found words.
     */
    public void putWordsFromMapInList() {
        this.corpus = new ArrayList<>();
        this.corpus.addAll(this.corpusWordsWithFrequency.keySet());
    }

    /**
     * Method that takes the words and their frequencies from xml file
     *  and build a Map<key, value>, where key = word, value = frequency
     *  of the word in the paraphrases.
     */
    public void loadCorpusFromXMLFile() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
            File file = new File("E:\\licenta\\wordNet\\src\\main\\java\\words.xml");

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            WordMap wordMap = (WordMap) jaxbUnmarshaller.unmarshal(file);

            this.corpusWordsWithFrequency = wordMap.getWordMap();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that transforms a seed vector to data that can be written in svm file.
     * In order to use spark's naiveBayes we need JavaRDD<LabelPoint> => svm file converted in javaRDD
     * A SVM File row format:  label  index:value index2:value2 ... , where value, value2, ... != 0.
     * @param X
     * @return
     */
    public String createDataForLIBSVMFile(List<Seed> X) {
        String data = "";
        for ( Seed row : X) {
            Map<Integer, Integer> treeMap = new TreeMap<>(row.getFeatures());
            data += row.getLabel() + " ";
            for (Map.Entry<Integer, Integer> pair : treeMap.entrySet()) {
                data += pair.getKey() + ":" + pair.getValue() + " ";
            }
            data += "\n";
        }

        return data;
    }

    /**
     * Writes reached seeds to SVM file.
     * @param data
     */
    public void writeSeedsToSVMFile(String data) {
        try {
            File statText = new File("E:\\licenta\\wordNet\\src\\main\\java\\type2\\seedsFiles\\seeds.txt");
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            w.write(data);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file statsTest.txt");
        }
    }
}
