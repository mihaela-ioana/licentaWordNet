package shared;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement(name="words")
@XmlAccessorType(XmlAccessType.FIELD)
public class WordMap{
    private Map<String, Long> wordMap = new HashMap<String, Long>();

    public Map<String, Long> getWordMap() {
        return  wordMap;
    }

    public void setWordMap (Map<String, Long> wordMap){
        this.wordMap = wordMap;
    }
}
