import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class SeedsWN {

    private Dictionary dictionary;
    private List<String> corpus;
    private List<String> corpusWithDuplicates;
    private Map<String, Long> wordsWithFrequency;
    private WordMap wordMap;
    private List<String> stopWords;

    public SeedsWN() {
        try {
            JWNL.initialize(new FileInputStream("E:\\licenta\\wordNet\\properties.xml"));
            this.dictionary = Dictionary.getInstance();

            // computeCorpus();
            // computeFrequencyForWords();
            // wordMap = new WordMap();
            // wordMap.setWordMap(this.wordsWithFrequency);
            // writeArrayToXMLFile();

            this.stopWords = Arrays.asList("the", "a", "of", "or", "in", "and", "to", "an", "with", "that",
                    "for", "by", "is", "as", "on", "from", "who", "he", "used", "was", "his");
        } catch (JWNLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void writeArrayToXMLFile() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(this.wordMap, System.out);
        jaxbMarshaller.marshal(this.wordMap, new File("E:\\licenta\\wordNet\\src\\main\\java\\words.xml"));
    }

    /**
     * Count number of appearances for each word => wordsWithFrequency.
     * ! not corpus, corpus contains distinct words
     */
    public void computeFrequencyForWords(){
        this.wordsWithFrequency = new HashMap<>();
        for (String word : this.corpus){
            Long counter = this.corpusWithDuplicates.stream().filter(w->w.equals(word)).count();
            this.wordsWithFrequency.put(word, counter);
        }

        this.wordsWithFrequency = sortByValue(this.wordsWithFrequency);
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
