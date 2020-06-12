package type1;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.classification.SVMWithSGD;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.util.*;


public class Train {

    private JavaSparkContext jsc;

    public Train(JavaSparkContext jsc) {
        this.jsc = jsc;
    }

    public void train() {
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\oneInitSeedWithAllRelNounsD4";
        List<Double> splitBy = Arrays.asList(0.1, 0.325, 0.55, 0.775, 1.0);
        List<String> obtainedValues = new ArrayList<>();

        for (Double split : splitBy){
            JavaRDD<LabeledPoint> inputData = MLUtils.loadLibSVMFile(jsc.sc(), path).toJavaRDD();
            JavaRDD<LabeledPoint>[] splittedInputData = inputData.randomSplit(new double[]{split, (1.0-split)});
            inputData = splittedInputData[0];
            JavaRDD<LabeledPoint>[] tmp = inputData.randomSplit(new double[]{0.75, 0.25});
            JavaRDD<LabeledPoint> training = tmp[0]; // training set
            JavaRDD<LabeledPoint> test = tmp[1]; // test set
            NaiveBayesModel model = NaiveBayes.train(training.rdd(), 1.0);
            JavaPairRDD<Double, Double> predictionAndLabelTest = test.mapToPair(p ->
                    new Tuple2<>(model.predict(p.features()), p.label()));
            JavaPairRDD<Double, Double> predictionAndLabelTraining = training.mapToPair(p ->
                    new Tuple2<>(model.predict(p.features()), p.label()));
            double accuracy = predictionAndLabelTest.filter(pl -> pl._1().equals(pl._2())).count() / (double) test.count();
            double accuracyTraining = predictionAndLabelTraining.filter(pl -> pl._1().equals(pl._2())).count() / (double) training.count();
            obtainedValues.add("accuracy: " + accuracy + " for " + split);
            obtainedValues.add(" training acc " + accuracyTraining + " for " + split);
            System.out.println(split);
        }

        obtainedValues.forEach(x -> System.out.println(x));

//        System.out.println("Test:" + accuracy);
//        System.out.println("Trining:" + accuracyTraining);
        jsc.stop();
    }

    public void trainDT() {
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\type1\\seedsFiles\\oneInitSeedWithAllRelAdj";

        List<Double> splitBy = Arrays.asList(1.0);
        List<String> obtainedValues = new ArrayList<>();

        for (Double split : splitBy) {
            JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(jsc.sc(), path).toJavaRDD();
            JavaRDD<LabeledPoint>[] splittedInputData = data.randomSplit(new double[]{split, (1.0-split)});
            data = splittedInputData[0];
            JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{0.75, 0.25});
            JavaRDD<LabeledPoint> trainingData = splits[0];
            JavaRDD<LabeledPoint> testData = splits[1];
            trainingData.cache();
            testData.cache();

            int numClasses = 2;
            Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
            String impurity = "gini";
            int maxDepth = 2;
            int maxBins = 16;

            DecisionTreeModel model = DecisionTree.trainClassifier(trainingData, numClasses,
                    categoricalFeaturesInfo, impurity, maxDepth, maxBins);

            JavaPairRDD<Double, Double> predictionAndLabel =
                    testData.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
            double testAcc =
                    predictionAndLabel.filter(pl -> pl._1().equals(pl._2())).count() / (double) testData.count();

            JavaPairRDD<Double, Double> predictionAndLabelTr =
                    trainingData.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
            double trainAcc =
                    predictionAndLabelTr.filter(pl -> pl._1().equals(pl._2())).count() / (double) trainingData.count();
            System.out.println("Learned classification tree model:\n" + model.toDebugString());

            obtainedValues.add("accuracy: " + testAcc + " for " + split);
            obtainedValues.add(" training acc " + trainAcc + " for " + split);
            System.out.println(split);
        }

        obtainedValues.forEach(x -> System.out.println(x));

//        System.out.println("type1.Train Accuracy: " + trainAcc);
//        System.out.println("Test Accuracy: " + testAcc);
//        System.out.println("Learned classification tree model:\n" + model.toDebugString());

        jsc.stop();
    }

    public void trainSVM() {
        String path = "E:\\licenta\\wordNet\\src\\main\\java\\oneInitSeedWithAllRelNounsD4";
        List<Double> splitBy = Arrays.asList(0.1, 0.325, 0.55, 0.775, 1.0);
        List<String> obtainedValues = new ArrayList<>();

        for (Double split : splitBy) {
            JavaRDD<LabeledPoint> inputData = MLUtils.loadLibSVMFile(jsc.sc(), path).toJavaRDD();
            JavaRDD<LabeledPoint>[] splittedInputData = inputData.randomSplit(new double[]{split, (1.0-split)});
            inputData = splittedInputData[0];
            JavaRDD<LabeledPoint> training = inputData.sample(false, 0.75, 11L);
            training.cache();
            JavaRDD<LabeledPoint> test = inputData.subtract(training);

            training.foreach(x -> System.out.print(x));

            int numIterations = 100;

            SVMModel model = SVMWithSGD.train(training.rdd(), numIterations);

            model.clearThreshold();

            JavaRDD<Tuple2<Object, Object>> scoreAndLabels = test.map(p ->
                    new Tuple2<>(model.predict(p.features()), p.label()));

            JavaPairRDD<Double, Double> predictionAndLabel = test.mapToPair(p ->
                    new Tuple2<>( model.predict(p.features()) <= 0 ? 0.0 : 1.0, p.label()));
            JavaPairRDD<Double, Double> predictionAndLabelTraining = training.mapToPair(p ->
                    new Tuple2<>( model.predict(p.features()) <= 0 ? 0.0 : 1.0, p.label()));
            double accuracy =
                    predictionAndLabel.filter(pl -> pl._1().equals(pl._2())).count() / (double) test.count();
            double accuracyTraining =
                    predictionAndLabelTraining.filter(pl -> pl._1().equals(pl._2())).count() / (double) training.count();

            obtainedValues.add("accuracy: " + accuracy + " for " + split);
            obtainedValues.add(" training acc " + accuracyTraining + " for " + split);
            System.out.println(split);
        }

        obtainedValues.forEach(x -> System.out.println(x));

//        System.out.println("Test Error: " + accuracy);
//        System.out.println("Training Error: " + accuracyTraining);
//
//        BinaryClassificationMetrics metrics =
//                new BinaryClassificationMetrics(JavaRDD.toRDD(scoreAndLabels));
//        double auROC = metrics.areaUnderROC();
//
//        System.out.println("Area under ROC = " + auROC);

        jsc.stop();
    }

    public void trainMPC() {
        SparkSession sparkSession = SparkSession.builder()
                .master("local[*]")
                .config("spark.sql.warehouse.dir", "file:///c:/tmp/spark-warehouse")
                .config("spark.executor.memory", "70g")
                .config("spark.driver.memory", "50g")
                .config("spark.memory.offHeap.enabled",true)
                .config("spark.memory.offHeap.size","64g")
                .getOrCreate();
        // $example on$
        // Load training data
        String path = "";

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
            int[] layers = new int[] {53425, 100, 2};

            // create the trainer and set its parameters
            MultilayerPerceptronClassifier trainer = new MultilayerPerceptronClassifier()
                    .setLayers(layers)
                    .setBlockSize(128)
                    .setSeed(1234L)
                    .setMaxIter(100);

            // train the model
            MultilayerPerceptronClassificationModel model = trainer.fit(train);

            // compute accuracy on the test set
            Dataset<Row> result = model.transform(test);
            Dataset<Row> predictionAndLabels = result.select("prediction", "label");
            MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                    .setMetricName("accuracy");


            // compute accuracy on the training set
            Dataset<Row> resultTr = model.transform(train);
            Dataset<Row> predictionAndLabelsTraining = resultTr.select("prediction", "label");
            MulticlassClassificationEvaluator evaluatorTr = new MulticlassClassificationEvaluator()
                    .setMetricName("accuracy");
            obtainedValues.add("accuracy: " +  evaluator.evaluate(predictionAndLabels) + " for " + split);
            obtainedValues.add(" training acc " + evaluatorTr.evaluate(predictionAndLabelsTraining) + " for " + split);
            System.out.println(split);
        }

        obtainedValues.forEach(x -> System.out.println(x));



//        System.out.println("Test set accuracy = " + evaluator.evaluate(predictionAndLabels));
//        System.out.println("Training set accuracy = " + evaluatorTr.evaluate(predictionAndLabelsTraining));

        sparkSession.stop();
    }
}
