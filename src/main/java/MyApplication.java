import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MyApplication {

    public static void main (String[] args) {
        try {
            JWNL.initialize(new FileInputStream("E:\\licenta\\wordNet\\properties.xml"));
            final Dictionary dictionary = Dictionary.getInstance();

            IndexWord indexWord = dictionary.getIndexWord(POS.NOUN, "bad");

            Synset[] senses = indexWord.getSenses();
            for (Synset set : senses) {
                System.out.println(indexWord + ": " + set.getGloss());
            }
            System.out.println("Senses gloss: " + indexWord.getSenses()[0].getGloss());
            System.out.println("Key: " + indexWord.getKey());
            System.out.println("Lemma: " + indexWord.getLemma());
            System.out.println("POS: " + indexWord.getPOS());
            System.out.println("Senses count: " + indexWord.getSenseCount());
            System.out.println("Synset offset: " + indexWord.getSynsetOffsets());
            System.out.println("Type: " + indexWord.getType());

            SeedsWN seedsWN = new SeedsWN();
        } catch (JWNLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
