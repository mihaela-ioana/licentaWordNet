import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MyApplication {

    public static void main (String[] args) {
        try {
            JWNL.initialize(new FileInputStream("E:\\licenta\\wordNet\\properties.xml"));
            final Dictionary dictionary = Dictionary.getInstance();

            IndexWord indexWord = dictionary.getIndexWord(POS.ADJECTIVE, "bad");

            Synset[] senses = indexWord.getSenses();
            for (Synset set : senses) {
                System.out.println(indexWord + ": " + set.getGloss());
            }
            System.out.println("Senses gloss: " + indexWord.getSenses()[0].getGloss());
            Synset[] relatedSynsets = indexWord.getSenses();
            Synset relatedSynset = relatedSynsets[0];
            List<Pointer> hypernyms = Arrays.asList(relatedSynset.getPointers(PointerType.HYPERNYM));
            /*System.out.println(relatedSynset.getWords());
            System.out.println(relatedSynset);
            System.out.println("Key: " + indexWord.getKey());
            System.out.println("Lemma: " + indexWord.getLemma());
            System.out.println("Lemma: " + indexWord.getSenseCount());
            System.out.println("POS: " + indexWord.getPOS());
            System.out.println("Synset offset: " + indexWord.getSynsetOffsets());
            System.out.println("Type: " + indexWord.getType());
*/
            SeedsWN seedsWN = new SeedsWN();
            /*SparkConf sparkConf = new SparkConf().setAppName("MyApplication").setMaster("local[2]").set("spark.executor.memory","1g");
            JavaSparkContext jsc = new JavaSparkContext(sparkConf);
            Train train = new Train(jsc);
            train.train();*/
        } catch (JWNLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
