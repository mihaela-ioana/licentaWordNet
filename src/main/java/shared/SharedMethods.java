package shared;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public interface SharedMethods {

    /**
     * Method that takes the words and their frequencies from xml file
     *  and build a Map<key, value>, where key = word, value = frequency
     *  of the word in the paraphrases.
     */
    static Map<String, Long> loadCorpusFromXMLFile() {
        Map<String, Long> corpusWithFrequency = new HashMap<>();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
            File file = new File("E:\\licenta\\wordNet\\src\\main\\java\\words.xml");

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            WordMap wordMap = (WordMap) jaxbUnmarshaller.unmarshal(file);

            corpusWithFrequency = wordMap.getWordMap();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return corpusWithFrequency;
    }
}
