import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MyApplication {

    public static void main (String[] args) {
        try {
            JWNL.initialize(new FileInputStream("E:\\licenta\\wordNet\\properties.xml"));

            type1.SeedsWN seedsWN = new type1.SeedsWN();
//            SparkConf sparkConf = new SparkConf().setAppName("MyApplication").setMaster("local[2]").set("spark.executor.memory","6g");
//            JavaSparkContext jsc = new JavaSparkContext(sparkConf);
//            Train train = new Train(jsc);
//            train.trainDT();

        } catch (JWNLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
