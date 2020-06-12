package type2;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.classification.*;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.classification.SVMWithSGD;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.linalg.Matrix;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.*;
import java.util.*;

public class Train {

    private JavaSparkContext jsc;
    private SparkSession sparkSession;

    public Train(JavaSparkContext jsc) {

        this.jsc = jsc;
        this.sparkSession = SparkSession.builder()
                .master("local[*]")
                .config("spark.sql.warehouse.dir", "file:///c:/tmp/spark-warehouse")
                .config("spark.executor.memory", "70g")
                .config("spark.driver.memory", "50g")
                .config("spark.memory.offHeap.enabled",true)
                .config("spark.memory.offHeap.size","64g")
                .getOrCreate();
    }

    public void train() {
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\type2\\seedsFiles\\3labels\\multipleInitSeedsVerbsDepth2";
        List<Double> splitBy = Arrays.asList(0.1, 0.325, 0.55, 0.775, 1.0);
        List<String> obtainedValues = new ArrayList<>();

        for (Double split : splitBy){
            //load file
            JavaRDD<LabeledPoint> inputData = MLUtils.loadLibSVMFile(jsc.sc(), path).toJavaRDD();

            //split whole input data
            JavaRDD<LabeledPoint>[] splittedInputData = inputData.randomSplit(new double[]{split, (1.0-split)});
            inputData = splittedInputData[0];

            //split into training and test data
            JavaRDD<LabeledPoint>[] tmp = inputData.randomSplit(new double[]{0.75, 0.25});
            JavaRDD<LabeledPoint> training = tmp[0];
            JavaRDD<LabeledPoint> test = tmp[1];

            org.apache.spark.mllib.classification.NaiveBayesModel model = NaiveBayes.train(training.rdd(), 1.0);
            // Compute raw scores on the test set.
            JavaPairRDD<Object, Object> predictionAndLabels = test.mapToPair(p ->
                    new Tuple2<>(model.predict(p.features()), p.label()));
            JavaPairRDD<Object, Object> predictionAndLabelsTrain = training.mapToPair(p ->
                    new Tuple2<>(model.predict(p.features()), p.label()));

            // Get evaluation metrics.
            MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels.rdd());
            MulticlassMetrics metricsTrain = new MulticlassMetrics(predictionAndLabelsTrain.rdd());

            Matrix confusion = metrics.confusionMatrix();
            System.out.println("Confusion matrix: \n" + confusion);

            // Stats by labels
            for (int i = 0; i < metrics.labels().length; i++) {
                System.out.format("Class %f precision = %f\n", metrics.labels()[i],metrics.precision(
                        metrics.labels()[i]));
                System.out.format("Class %f recall = %f\n", metrics.labels()[i], metrics.recall(
                        metrics.labels()[i]));
                System.out.format("Class %f F1 score = %f\n", metrics.labels()[i], metrics.fMeasure(
                        metrics.labels()[i]));
            }

            //Weighted stats
            System.out.format("Weighted precision = %f\n", metrics.weightedPrecision());
            System.out.format("Weighted recall = %f\n", metrics.weightedRecall());
            System.out.format("Weighted F1 score = %f\n", metrics.weightedFMeasure());
            System.out.format("Weighted false positive rate = %f\n", metrics.weightedFalsePositiveRate());


            double accuracy = metrics.accuracy();
            double accuracyTraining = metricsTrain.accuracy();
            obtainedValues.add("accuracy: " + accuracy + " for " + split);
            obtainedValues.add(" training acc " + accuracyTraining + " for " + split);
        }

        obtainedValues.forEach(x -> System.out.println(x));

        jsc.stop();
    }

    public void trainSVC() {
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\type2\\seedsFiles\\3labels\\multipleInitSeedsVerbsDepth2";
        List<Double> splitBy = Arrays.asList(0.1, 0.325, 0.55, 0.775, 1.0);
        List<String> obtainedValues = new ArrayList<>();

        for (Double split : splitBy) {
            Dataset<Row> inputData = this.sparkSession.read().format("libsvm").load(path);

            //split the input data
            Dataset<Row>[] splittedInputData = inputData.randomSplit(new double[]{split, (1.0-split)});
            inputData = splittedInputData[0];

            //split the data into training and test data
            Dataset<Row>[] splits = inputData.randomSplit(new double[]{0.75, 0.25}, 1234L);
            Dataset<Row> train = splits[0];
            Dataset<Row> test = splits[1];

            LinearSVC classifier = new LinearSVC()
                    .setMaxIter(10)
                    .setRegParam(0.1);

            OneVsRest ovr = new OneVsRest().setClassifier(classifier);

            OneVsRestModel ovrModel = ovr.fit(train);

            Dataset<Row> predictions = ovrModel.transform(test).select("prediction", "label");
            Dataset<Row> predictionsTrain = ovrModel.transform(train).select("prediction", "label");

            // Get evaluation metrics.
            MulticlassMetrics metrics = new MulticlassMetrics(predictions);
            MulticlassMetrics metricsTrain = new MulticlassMetrics(predictionsTrain);

            Matrix confusion = metrics.confusionMatrix();
            System.out.println("Confusion matrix: \n" + confusion);

            // Stats by labels
            for (int i = 0; i < metrics.labels().length; i++) {
                System.out.format("Class %f precision = %f\n", metrics.labels()[i],metrics.precision(
                        metrics.labels()[i]));
                System.out.format("Class %f recall = %f\n", metrics.labels()[i], metrics.recall(
                        metrics.labels()[i]));
                System.out.format("Class %f F1 score = %f\n", metrics.labels()[i], metrics.fMeasure(
                        metrics.labels()[i]));
            }

            //Weighted stats
            System.out.format("Weighted precision = %f\n", metrics.weightedPrecision());
            System.out.format("Weighted recall = %f\n", metrics.weightedRecall());
            System.out.format("Weighted F1 score = %f\n", metrics.weightedFMeasure());
            System.out.format("Weighted false positive rate = %f\n", metrics.weightedFalsePositiveRate());


            double accuracy = metrics.accuracy();
            double accuracyTraining = metricsTrain.accuracy();
            obtainedValues.add("accuracy: " + accuracy + " for " + split);
            obtainedValues.add(" training acc " + accuracyTraining + " for " + split);
        }

        obtainedValues.forEach(x -> System.out.println(x));

        jsc.stop();
    }

    public void trainDT() {
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\type2\\seedsFiles\\3labels\\withoutNegLabels\\multipleVerbsDepth2";

        List<Double> splitBy = Arrays.asList(0.1, 0.325, 0.55, 0.775, 1.0);
        List<String> obtainedValues = new ArrayList<>();

        for (Double split : splitBy) {
            JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(jsc.sc(), path).toJavaRDD();

            //split the input data
            JavaRDD<LabeledPoint>[] splittedInputData = data.randomSplit(new double[]{split, (1.0-split)});
            data = splittedInputData[0];

            //split input data in training and test data
            JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{0.75, 0.25});
            JavaRDD<LabeledPoint> trainingData = splits[0];
            JavaRDD<LabeledPoint> testData = splits[1];
            trainingData.cache();
            testData.cache();

            //number of labels
            int numClasses = 3;
            Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
            String impurity = "gini";
            int maxDepth = 4;
            int maxBins = 16;

            DecisionTreeModel model = DecisionTree.trainClassifier(trainingData, numClasses,
                    categoricalFeaturesInfo, impurity, maxDepth, maxBins);


            JavaPairRDD<Object, Object> predictionAndLabels = testData.mapToPair(p ->
                    new Tuple2<>(model.predict(p.features()), p.label()));
            JavaPairRDD<Object, Object> predictionAndLabelsTrain = trainingData.mapToPair(p ->
                    new Tuple2<>(model.predict(p.features()), p.label()));

            // Get evaluation metrics.
            MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels.rdd());
            MulticlassMetrics metricsTrain = new MulticlassMetrics(predictionAndLabelsTrain.rdd());

            Matrix confusion = metrics.confusionMatrix();
            System.out.println("Confusion matrix: \n" + confusion);

            // Stats by labels
            for (int i = 0; i < metrics.labels().length; i++) {
                System.out.format("Class %f precision = %f\n", metrics.labels()[i],metrics.precision(
                        metrics.labels()[i]));
                System.out.format("Class %f recall = %f\n", metrics.labels()[i], metrics.recall(
                        metrics.labels()[i]));
                System.out.format("Class %f F1 score = %f\n", metrics.labels()[i], metrics.fMeasure(
                        metrics.labels()[i]));
            }

            //Weighted stats
            System.out.format("Weighted precision = %f\n", metrics.weightedPrecision());
            System.out.format("Weighted recall = %f\n", metrics.weightedRecall());
            System.out.format("Weighted F1 score = %f\n", metrics.weightedFMeasure());
            System.out.format("Weighted false positive rate = %f\n", metrics.weightedFalsePositiveRate());


            double accuracy = metrics.accuracy();
            double accuracyTraining = metricsTrain.accuracy();
            obtainedValues.add("accuracy: " + accuracy + " for " + split);
            obtainedValues.add(" training acc " + accuracyTraining + " for " + split);
        }

        obtainedValues.forEach(x -> System.out.println(x));

        jsc.stop();
    }

    public void trainMPC() {

        // $example on$
        // Load training data
         transformLabelToOnlyPositive("E:\\licenta\\wordNet\\src\\main\\java\\type2\\seedsFiles\\3labels\\multipleInitSeedsVerbsDepth2");
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\type2\\seedsFiles\\3labels\\withoutNegLabels\\multipleVerbsDepth2";

        List<Double> splitBy = Arrays.asList(0.1, 0.325, 0.55, 0.775, 1.0);
        List<String> obtainedValues = new ArrayList<>();

        for (Double split : splitBy) {
            Dataset<Row> dataFrame = sparkSession.read().format("libsvm").load(path);
            // Split the data into train and test
            Dataset<Row>[] splittedInputData = dataFrame.randomSplit(new double[]{split, (1.0-split)}, 1234L);
            dataFrame = splittedInputData[0];
            Dataset<Row>[] splits = dataFrame.randomSplit(new double[]{0.75, 0.25}, 1234L);
            Dataset<Row> train = splits[0];
            Dataset<Row> test = splits[1];

            // specify layers for the neural network:
            // input layer of size 4 (features), two intermediate of size 5 and 4
            // and output of size 3 (classes)
            int[] layers = new int[] {53426, 100, 3};

            // create the trainer and set its parameters
            MultilayerPerceptronClassifier trainer = new MultilayerPerceptronClassifier()
                    .setLayers(layers)
                    .setBlockSize(128)
                    .setSeed(1234L)
                    .setMaxIter(10);

            // train the model
            MultilayerPerceptronClassificationModel model = trainer.fit(train);

            // compute accuracy on the test set
            Dataset<Row> predictions = model.transform(test).select("prediction", "label");
            // Get evaluation metrics.
            MulticlassMetrics metrics = new MulticlassMetrics(predictions);

            Matrix confusion = metrics.confusionMatrix();
            System.out.println("Confusion matrix: \n" + confusion);

            System.out.println("Accuracy = " + metrics.accuracy());

            // Stats by labels
            for (int i = 0; i < metrics.labels().length; i++) {
                System.out.format("Class %f precision = %f\n", metrics.labels()[i],metrics.precision(
                        metrics.labels()[i]));
                System.out.format("Class %f recall = %f\n", metrics.labels()[i], metrics.recall(
                        metrics.labels()[i]));
                System.out.format("Class %f F1 score = %f\n", metrics.labels()[i], metrics.fMeasure(
                        metrics.labels()[i]));
            }

            //Weighted stats
            System.out.format("Weighted precision = %f\n", metrics.weightedPrecision());
            System.out.format("Weighted recall = %f\n", metrics.weightedRecall());
            System.out.format("Weighted F1 score = %f\n", metrics.weightedFMeasure());
            System.out.format("Weighted false positive rate = %f\n", metrics.weightedFalsePositiveRate());
        }

        obtainedValues.forEach(x -> System.out.println(x));



//        System.out.println("Test set accuracy = " + evaluator.evaluate(predictionAndLabels));
//        System.out.println("Training set accuracy = " + evaluatorTr.evaluate(predictionAndLabelsTraining));

        sparkSession.stop();
    }

    public void transformLabelToOnlyPositive(String path) {
        String data = "";
        try {
            File file = new File(path);

            BufferedReader br = new BufferedReader(new FileReader(file));

            String st;
            while ((st = br.readLine()) != null){
                String[] splitted = st.split(" ");
                Double label = Double.parseDouble(splitted[0]) + 1;
                data += label + " ";
                for (Integer index=1; index<splitted.length; index++) {
                    data += splitted[index] + " ";
                }
                data += "\n";
            }
        } catch (IOException ex) {
            System.err.println("Problem reading the given file.");
        }

        try {
            File statText = new File("E:\\licenta\\wordNet\\src\\main\\java\\type2\\seedsFiles\\3labels\\withoutNegLabels\\multipleVerbsDepth2");
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
