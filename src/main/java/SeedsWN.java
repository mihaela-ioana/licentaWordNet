import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.dictionary.Dictionary;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
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
    private List<List<Integer>> X;
    private List<List<Integer>> Y;

    public SeedsWN() {
        try {
            JWNL.initialize(new FileInputStream("E:\\licenta\\wordNet\\properties.xml"));
            this.dictionary = Dictionary.getInstance();

            //stop words = words with freq > 8000
            this.stopWords = Arrays.asList("the", "a", "of", "or", "in", "and", "to", "an", "with", "that",
                    "for", "by", "is", "as", "on", "from", "who", "he", "used", "was", "his");

            // computeCorpus();
            // computeFrequencyForWords();
            // wordMap = new WordMap();
            // wordMap.setWordMap(this.wordsWithFrequency);
            // writeArrayToXMLFile();

            //get map with words and word frequency from xml file
            loadCorpusFromXMLFile();
            //build corpus
            putWordsFromMapInList();
            //eliminate stop words from corpus
            computeWords() ;

            List<POS> allPos = POS.getAllPOS();
            String data = "";

            for (POS pos: allPos) {
                Map<Long, Integer> result = seedWithWord("bad", 10, pos);
                seedsToVectors(result, false, pos);
                data += this.createDataForLIBSVMFile(this.X, false);
                Map<Long, Integer> result2 = seedWithWord("good", 10, pos);
                seedsToVectors(result2, true, pos);
                data+= this.createDataForLIBSVMFile(this.X, true);
            }

//            Map<Long, Integer> result = seedWithWord("bad", 100, POS.ADJECTIVE);
//            seedsToVectors(result, false, POS.ADJECTIVE);
//
//            /*for(Map.Entry<Long, Integer> pair: result.entrySet()) {
//                System.out.println(this.dictionary.getSynsetAt(POS.ADJECTIVE, pair.getKey()).getGloss() + ": " + pair.getValue());
//            }*/
//            String data = this.createDataForLIBSVMFile(this.X, false);

//            Map<Long, Integer> result2 = seedWithWord("good", 100, POS.ADJECTIVE);
//            seedsToVectors(result2, true, POS.ADJECTIVE);
//
//            data+= this.createDataForLIBSVMFile(this.X, true);
            this.writeSeedsToSVMFile(data);
        } catch (JWNLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Integer> phraseToVector(String paraphrase) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < this.corpus.size(); index++){
            result.add(0);
        }
        String[] splittedParaphrase = paraphrase.split("[\\s+,/]");

        for (String word: splittedParaphrase) {
            word = word.replace("(", "");
            word = word.replace(")", "");
            word = word.replace(";", "");

            Integer index = this.corpus.indexOf(word);
            if (index <= result.size() && index >= 0){
                result.add(index, result.get(index) + 1);
            }
        }

        return result;
    }

    public List<Integer> synsetDefToVector(POS pos, Long synsetId) throws JWNLException {
        List<Integer> result = Collections.nCopies(this.corpus.size(), 0);
        Synset synset = this.dictionary.getSynsetAt(pos, synsetId);

        if (synset.getGloss() != "") {
            return phraseToVector(synset.getGloss());
        }

        return result;
    }

    public void seedsToVectors(Map<Long, Integer> reachedSynsets, boolean positive, POS pos) throws JWNLException {
        this.X = new ArrayList<>();
        this.Y = new ArrayList<>();

        for (Long key: reachedSynsets.keySet()) {
            this.X.add(synsetDefToVector(pos, key));
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
        System.out.println(synset.getGloss());
        if (synset == null) {
            throw new Exception("Synset with offset '" + synsetOffset + "' is not in the wordnet");
        }

        visited.put(synsetOffset, steps);

        List<PointerType> type = PointerType.getAllPointerTypesForPOS(categ);
        List<Pointer> relations = new ArrayList<>();
        for (PointerType pt: type) {
            if (!pt.equals(PointerType.ANTONYM)){
                relations.addAll(Arrays.asList(synset.getPointers(pt)));
            }
        }
//        List<Pointer> hypernyms = Arrays.asList(synset.getPointers(PointerType.SIMILAR_TO));
//        List<Pointer> hyponyms = Arrays.asList(synset.getPointers(PointerType.INSTANCES_HYPONYM));
//        List<Pointer> relations = new ArrayList<>();
//        relations.addAll(hypernyms);
//        relations.addAll(hyponyms);

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
     * @throws Exception
     */
    public Map<Long, Integer> seedWithId(Long synsetOffset, Integer steps, POS categ) throws Exception {
        Map<Long, Integer> result = new HashMap<>();
        System.out.println("Travel breadth-first through wordnet starting with synset id " + synsetOffset);

        Map<Long, Integer> visited = bfwalk_with_depth(synsetOffset, steps, categ, new HashMap<>());
        for (Map.Entry<Long, Integer> pair: visited.entrySet()) {
            result.put(pair.getKey(), steps-(pair.getValue()));
        }

        return result;
    }

    /**
     * Method that receives a list with synsets of the starting word which as given.
     * For every synset in list, the program crosses through all related synsets with given depth (steps).
     * @return a list with all synsets and the min step where they occured
     * @throws Exception
     */
    public Map<Long, Integer> seedWithIds(List<Synset> synsets, Integer steps, POS categ) throws Exception {
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
        List<Synset> synsets = Arrays.asList(indexWord.getSenses());
        if (synsets.size() > 0){
            result = seedWithIds(synsets, steps, categ);
        }

        return result;
    }

    //eliminate stop words from corpus
    public void computeWords(){
        this.corpus.stream().filter(x -> !this.stopWords.contains(x)).collect(Collectors.toList());
    }

    /**
     * Mehod that takes all read data from xml and build a corpus
     *      with all the found words.
     */
    public void putWordsFromMapInList() {
        this.corpus = new ArrayList<>();
        this.corpus.addAll(this.corpusWordsWithFrequency.keySet());
    }

    public void writeArrayToXMLFile() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(this.wordMap, System.out);
        jaxbMarshaller.marshal(this.wordMap, new File("E:\\licenta\\wordNet\\src\\main\\java\\words.xml"));
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

    public void addGlossToCorpus(String gloss) {
        String[] words = gloss.split("[\\s+,/]");        //nur Komma und Space erscheint
        for (int index = 0; index < words.length; index ++){
            String word = words[index].toLowerCase().replaceAll("[,.\"*?();':!`]", "");
            this.corpusWithDuplicates.add(word);
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

        //!!! to lower is neccesary here??
    }

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
     * Method that transforms a seed vector to data that can be written in svm file.
     * In order to use spark's naiveBayes we need JavaRDD<LabelPoint> => svm file converted in javaRDD
     * A SVM File row format:  label  index:value index2:value2 ... , where value, value2, ... != 0.
     * @param X
     * @param positive
     * @return
     */
    public String createDataForLIBSVMFile(List<List<Integer>> X, boolean positive) {
        String data = "";
        for (List<Integer> row : X) {
            //variable which shows if the row is not empty (not only 0)
            boolean flag = false;
            for (int index = 0; index < row.size(); index ++){
                if (row.get(index) != 0) {
                    if (!flag){
                        data += positive ? (1 + " ") : (0 + " ");
                        flag = true;
                    }
                    data += index + ":" + row.get(index) + " ";
                }
            }
            if (flag) {
                data += "\n";
            }
        }

        return data;
    }

    public void writeSeedsToSVMFile(String data) {
        try {
            File statText = new File("E:\\licenta\\wordNet\\src\\main\\java\\seeds.txt");
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


//needed class for writing into xml file
@XmlRootElement(name="words")
@XmlAccessorType(XmlAccessType.FIELD)
class WordMap{
    private Map<String, Long> wordMap = new HashMap<String, Long>();

    public Map<String, Long> getWordMap() {
        return  wordMap;
    }

    public void setWordMap (Map<String, Long> wordMap){
        this.wordMap = wordMap;
    }
}
