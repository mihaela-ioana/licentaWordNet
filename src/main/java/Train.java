import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.util.MLUtils;
import scala.Tuple2;


public class Train {

    private JavaSparkContext jsc;

    Train(JavaSparkContext jsc) {
        this.jsc = jsc;
    }

    public void train() {
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\seeds.txt";
        JavaRDD<LabeledPoint> inputData = MLUtils.loadLibSVMFile(jsc.sc(), path).toJavaRDD();
        //pentru 06 si 0.4 o dat 0.20 si pentru 0.75 si 0.25 ->0.15, apoi 0.14 ... E normal sa scada accuracy?
        JavaRDD<LabeledPoint>[] tmp = inputData.randomSplit(new double[]{0.75, 0.25});
        System.out.println("Temp0");
        tmp[0].foreach(x -> System.out.print(x));
        JavaRDD<LabeledPoint> training = tmp[0]; // training set
        JavaRDD<LabeledPoint> test = tmp[1]; // test set
        NaiveBayesModel model = NaiveBayes.train(training.rdd(), 1.0);
        JavaPairRDD<Double, Double> predictionAndLabel = test.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
        double accuracy = predictionAndLabel.filter(pl -> pl._1().equals(pl._2())).count() / (double) test.count();

        //prima rulare: 56%
        System.out.println(accuracy);
        jsc.stop();
    }
}
