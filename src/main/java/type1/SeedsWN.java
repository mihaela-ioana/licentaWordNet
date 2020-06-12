package type1;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.dictionary.Dictionary;
import shared.SharedMethods;
import shared.WordMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SeedsWN {

    private Dictionary dictionary;
    private List<String> corpus;
    private List<String> corpusWithDuplicates;
    private Map<String, Long> corpusWordsWithFrequency;
    private WordMap wordMap;
    private List<String> stopWords;
    private List<Map<Integer, Integer>> X;
    private List<List<Integer>> Y;
    private List<String> posAdj = Arrays.asList("good", "pleasent", "excellent", "tasty", "super", "brilliant", "smart",
            "strong", "valid", "meaningful", "helpful", "useful");
    private List<String> posSubst = Arrays.asList("luck", "righteousness", "advantage", "morality");
    private List<String> negAdj = Arrays.asList("bad", "nasty", "awful", "unwelcome", "unlucky", "difficult", "rude",
            "sick", "false", "guilty");
    private List<String> negSubst = Arrays.asList("stress", "damage", "harm", "dred", "sin");

    public SeedsWN() {
        try {
            JWNL.initialize(new FileInputStream("E:\\licenta\\wordNet\\properties.xml"));
            this.dictionary = Dictionary.getInstance();

            //stop words = words with freq > 8000
            this.stopWords = Arrays.asList("the", "a", "of", "or", "in", "and", "to", "an", "with", "that",
                    "for", "by", "is", "as", "on", "from", "who", "he", "used", "was", "his", "one", "s",
                    "united", "states", "at", "be", "not", "are", "genus", "it", "into");

//            computeCorpus();
//            computeFrequencyForWords();,
//            wordMap = new type2.type1.WordMap();
//            wordMap.setWordMap(this.corpusWordsWithFrequency);
//            writeArrayToXMLFile();

            //get map with words and word frequency from xml file
            this.corpusWordsWithFrequency = SharedMethods.loadCorpusFromXMLFile();
            //build corpus
            putWordsFromMapInList();
            //eliminate stop words from corpus
            computeWords() ;

            //with one single pos
            Map<Long, Integer> result = seedWithWord("bad", 1, POS.ADJECTIVE);
            seedsToVectors(result, false, POS.ADJECTIVE);
            String data = this.createDataForLIBSVMFile(this.X, false);

            Map<Long, Integer> result2 = seedWithWord("good", 1, POS.ADJECTIVE);
            seedsToVectors(result2, true, POS.ADJECTIVE);
            data+= this.createDataForLIBSVMFile(this.X, true);

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
     * For the synset id, the feature vector is returned.
     * @param synsetId
     * @return
     */
    public Map<Integer, Integer> synsetDefToVector(POS pos, Long synsetId) throws JWNLException {
        Map<Integer, Integer> result = new HashMap<>();
        Synset synset = this.dictionary.getSynsetAt(pos, synsetId);

        if (synset.getGloss() != "") {
            return phraseToVector(synset.getGloss());
        }

        return result;
    }

    /**
     * Method that builds for every reached synset the seed: label && feature vector.
     * @param reachedSynsets
     * @param positive
     * @return
     */
    public void seedsToVectors(Map<Long, Integer> reachedSynsets, boolean positive, POS pos) throws JWNLException {
        this.X = new ArrayList<>();
        this.Y = new ArrayList<>();

        for (Long key: reachedSynsets.keySet()) {
            Map<Integer, Integer> defToVector = synsetDefToVector(pos, key);
            if (!defToVector.isEmpty()) {
                this.X.add(defToVector);
            }
        }

        this.Y = Collections.nCopies(X.size(), positive ? Arrays.asList(0) : Arrays.asList(1));
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
    public Map<Long, Integer> seedWithId(Long synsetOffset, Integer steps, POS categ) {
        Map<Long, Integer> result = new HashMap<>();
        System.out.println("Travel breadth-first through wordnet starting with synset id " + synsetOffset);

        try{
            Map<Long, Integer> visited = bfwalk_with_depth(synsetOffset, steps, categ, new HashMap<>());
            for (Map.Entry<Long, Integer> pair: visited.entrySet()) {
                result.put(pair.getKey(), steps-(pair.getValue()));
            }
        } catch(Exception ex){
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
     * @throws Exception
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
     * Count number of appearances for each word => wordsWithFrequency.
     * ! not corpus, corpus contains distinct words
     */
    public void computeFrequencyForWords(){
        this.corpusWordsWithFrequency = new HashMap<>();
        for (String word : this.corpus){
            Long counter = this.corpusWithDuplicates.stream().filter(w->w.equals(word)).count();
            this.corpusWordsWithFrequency.put(word, counter);
        }

        this.corpusWordsWithFrequency = sortByValue(this.corpusWordsWithFrequency);
    }

    /**
     * Methods that adds to the corpus the words from all synsets definition for all parts of speech.
     */
    public void computeCorpus() {
        this.corpusWithDuplicates = new ArrayList<String>();
        computeWithGivenPOS(POS.ADJECTIVE);
        computeWithGivenPOS(POS.NOUN);
        computeWithGivenPOS(POS.ADVERB);
        computeWithGivenPOS(POS.VERB);

        corpusProcessing();

        System.out.println("Size of corpus with duplicates: " + this.corpusWithDuplicates.size());
        System.out.println("Size of corpus: " + this.corpus.size());
    }

    /**
     * Methods that adds to the corpus the words from all synsets definition depending on the part of speech.
     * @param pos
     */
    public void computeWithGivenPOS(POS pos) {
        try {
            Iterator<IndexWord> iterator = this.dictionary.getIndexWordIterator(pos);
            while(iterator.hasNext()) {
                IndexWord indexWord = iterator.next();
                Synset[] synsets = indexWord.getSenses();
                for (Synset synset : synsets) {
                    addGlossToCorpus(synset.getGloss());
                }
            }
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Splits the synste definition in words and adds them to the corpus.
     * @param gloss
     */
    public void addGlossToCorpus(String gloss) {
        String[] words = gloss.split("[\\s+,/;(){}\\]\\[\"<>?!$%^&*!~`'\\-]");
        for (int index = 0; index < words.length; index ++){
            String word = words[index].toLowerCase();
            if(!word.isEmpty()){
                this.corpusWithDuplicates.add(word);
            }
        }
    }

    /**
     * Method for editing the corpus list.
     */
    private void corpusProcessing() {
        this.corpus = this.corpusWithDuplicates;
        //eliminate duplicates
        this.corpus = this.corpus.stream().distinct().collect(Collectors.toList());
        //eliminate null
        this.corpus = this.corpus.stream().filter(x -> x != null || !x.matches("\\d+[.-]\\d*")).collect(Collectors.toList());
    }

    /**
     * Method that transforms a seed vector to data that can be written in svm file.
     * In order to use spark's naiveBayes we need JavaRDD<LabelPoint> => svm file converted in javaRDD
     * A SVM File row format:  label  index:value index2:value2 ... , where value, value2, ... != 0.
     * @param X
     * @param positive
     * @return
     */
    public String createDataForLIBSVMFile(List<Map<Integer, Integer>> X, boolean positive) {
        String data = "";
        for ( Map<Integer, Integer> row : X) {
            Map<Integer, Integer> treeMap = new TreeMap<>(row);
            data += positive ? (1 + " ") : (0 + " ");
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
            File statText = new File("E:\\licenta\\wordNet\\src\\main\\java\\type1\\seedsFiles\\seeds.txt");
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            w.write(data);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file statsTest.txt");
        }
    }

    /**
     * Method for sorting Map.
     * @param unsortMap
     * @param <K>
     * @param <V>
     * @return sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> unsortMap) {

        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Method that writes the words with their frequency to a xml file.
     * JAXB uses a class containing the map<word, wordFreq> called type1.WordMap.
     * @throws JAXBException
     */
    public void writeArrayToXMLFile() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(this.wordMap, System.out);
        jaxbMarshaller.marshal(this.wordMap, new File("E:\\licenta\\wordNet\\src\\main\\java\\words.xml"));
    }
}
