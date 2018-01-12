package com.project.iotap.iotap.MachineLearning;


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

/**
 * Created by Anton on 2017-11-29.
 * Example from https://www.dropbox.com/s/hgx1y9ciqzo6525/wekaClassifier.java?dl=0
 * for using Weka with java.
 */

public class WekaClassifier {

    private static final String TAG = "Weka";
    private static final String MODEL_NAME_FILEPATH = "j48xval_raw.model";
    private final Context appContext;

    private ArrayList<Attribute> attributeList = null;
    private List<String> classLabels = null;
    private Classifier classifier = null;

    public WekaClassifier(Context appContext) {
        this.appContext = appContext;
        Log.d(TAG, "Setting up classifier...");
        loadPrecomputedClassifier();
        setupAttributes();
    }

    /**
     * Method that uses a precomputed classifier model to classify a dataset into a gesture.
     *
     * @param rawGestureData smoothed gesture data.
     */
    public Direction classifyTuple(int[][] rawGestureData) {
        Log.d(TAG, "Start classifying...");
        // unpredicted data sets (reference to sample structure for new instances)
        Instances dataUnpredicted = new Instances("Instances", attributeList, 1);
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
            Log.d(TAG, "Error when trying to classify: " + e.getMessage());
            e.printStackTrace();
        }
        return convertStringToEnum(predictedClass);
    }


    /**
     * Creates a list of the different class labels and attributes that the classifier will use.
     */
    private void setupAttributes() {
        classLabels = new ArrayList<String>() {
            {
                add("up");
                add("down");
                add("left");
                add("right");
            }
        };

        attributeList = new ArrayList<>(120);

        for (int i = 1; i < 21; i++) {
            attributeList.add(new Attribute("AccX" + i));
            attributeList.add(new Attribute("AccY" + i));
            attributeList.add(new Attribute("AccZ" + i));
            attributeList.add(new Attribute("GyrX" + i));
            attributeList.add(new Attribute("GyrY" + i));
            attributeList.add(new Attribute("GyrZ" + i));
        }
        attributeList.add(new Attribute("gesture", classLabels));
    }

    /**
     * Loads the classifier model from asset folder.
     */
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

    /**
     * Converts the predicted label which is a string to the corresponding enum and returns it.
     *
     * @param predictedClass
     * @return
     */
    private Direction convertStringToEnum(String predictedClass) {
        switch (predictedClass) {
            case "up":
                return Direction.UP;

            case "down":
                return Direction.DOWN;

            case "left":
                return Direction.LEFT;

            case "right":
                return Direction.RIGHT;

            default:
                return Direction.UNKNOWN;
        }
    }
}