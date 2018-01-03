package com.project.iotap.iotap.MachineLearning;

/**
 * Created by Anton on 2017-11-29.
 * Example from https://www.dropbox.com/s/hgx1y9ciqzo6525/wekaClassifier.java?dl=0
 * for using Weka with java.
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.project.iotap.iotap.Shared.Direction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class WekaClassifier {

    private static final String TAG = "Weka";
    private static final String MODEL_NAME_FILEPATH = "preComputedClassifier.model";
    private final Context appContext;

    private ArrayList<Attribute> attributeList = null;
    private List<String> classLabels = null;
    private Classifier classifier = null;

    public WekaClassifier(Context appContext) {
        this.appContext = appContext;
        setupClassifier();
    }

    /**
     * Method that starts using Weka to classify the gesture data into a gesture.
     * https://geekoverdose.wordpress.com/2016/11/27/weka-on-android-load-precomputed-model-and-predict-new-samples/
     *
     * @param rawGestureData smoothed gesture data.
     */
    public Direction classifyTuple(int[][] rawGestureData) {
        Log.d(TAG, "Start classifying");
        // unpredicted data sets (reference to sample structure for new instances)
        Instances dataUnpredicted = new Instances("Instances",
                attributeList, 1);
        // last feature is target variable
        dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1);

        // create new instance which the classifier should classify.
        int counter = 0;
        DenseInstance newInstance = new DenseInstance(dataUnpredicted.numAttributes());
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 6; col++) {
                newInstance.setValue(attributeList.get(counter++), rawGestureData[row][col]);
            }
        }

        // reference to dataset
        newInstance.setDataset(dataUnpredicted);
        // predict new sample
        String predictedClass = "unknown";
        try {
            double result = classifier.classifyInstance(newInstance);
            predictedClass = classLabels.get(Double.valueOf(result).intValue());
            Log.d(TAG, "predicted: " + predictedClass);

        } catch (Exception e) {
            Log.d(TAG, "Error when trying to classfy: " + e.getMessage());
            e.printStackTrace();
        }
        return convertStringToEnum(predictedClass);
    }


    private void setupClassifier() {
        Log.d(TAG, "Setting up classifier...");
        loadPrecomputedClassifier();
        setupAttributes();
    }

    private void setupAttributes() {
        //Classes to predicts to. TODO Decide names
        classLabels = new ArrayList<String>() {
            {
                add("left");
                add("right");
                add("up");
                add("down");
            }
        };

        attributeList = new ArrayList<>(120);

        //Iterates 120 times adds all the attributes.
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 6; col++) {
                StringBuilder name = new StringBuilder(6);
                switch (col) {
                    case 0:
                        name.append("AccX");
                        break;
                    case 1:
                        name.append("AccY");
                        break;
                    case 2:
                        name.append("AccZ");
                        break;
                    case 3:
                        name.append("GyrX");
                        break;
                    case 4:
                        name.append("GyrY");
                        break;
                    case 5:
                        name.append("GyrZ");
                        break;
                }
                name.append(row);
                attributeList.add(new Attribute(name.toString()));
            }
        }
        Attribute attributeClass = new Attribute("@@class@@", classLabels);
        attributeList.add(attributeClass);
    }

    private void loadPrecomputedClassifier() {
        AssetManager assetManager = appContext.getAssets();
        try {
            classifier = (Classifier) weka.core.SerializationHelper.read(assetManager.open(MODEL_NAME_FILEPATH));
            Log.d(TAG, "Classifier loaded...");
        } catch (IOException e) {
            Log.d(TAG, "IO Exception when loading classifier: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, "Exception when loading classifier: " + e.getMessage());
        }
    }

    private Direction convertStringToEnum(String predictedClass) {
        switch (predictedClass) {
            case "up":
                return Direction.UP;

            case "right":
                return Direction.RIGHT;

            case "down":
                return Direction.DOWN;

            case "left":
                return Direction.LEFT;

            default:
                return Direction.UNKNOWN;
        }
    }
}