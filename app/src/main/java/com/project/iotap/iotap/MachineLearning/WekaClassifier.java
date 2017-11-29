package com.project.iotap.iotap.MachineLearning;

/**
 * Created by Anton on 2017-11-29.
 * Example from https://www.dropbox.com/s/hgx1y9ciqzo6525/wekaClassifier.java?dl=0
 * for using Weka with java.
 */

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import weka.classifiers.trees.J48;
import weka.core.Instances;

public class WekaClassifier {

    private Context appContext;

    public WekaClassifier(Context appContext) {
        this.appContext = appContext;
    }

    public void createClassifier() throws Exception{

        AssetManager assetManager = appContext.getAssets();

        // Load training data.

        InputStream is = assetManager.open("training.arff");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        Instances train = new Instances(br);
        train.setClassIndex(train.numAttributes() - 1);

        // Load test data.

        is = assetManager.open("testing.arff");
        br = new BufferedReader(new InputStreamReader(is));

        Instances test = new Instances(br);
        test.setClassIndex((test.numAttributes() - 1));

        br.close();

        // Create classifier.

        J48 tree = new J48();
        tree.buildClassifier(train);

        // Label test data.

        int classIndex = train.numAttributes() - 1;
        Instances labeled = new Instances(test);

        for (int i = 0; i < test.numInstances(); i++) {
            double clsLabel = tree.classifyInstance(test.instance(i));
            labeled.instance(i).setClassValue(clsLabel);
            System.out.println(labeled.instance(i).attribute(classIndex).value((int) clsLabel));
        }

        // Save labeled data.

        String fileName = ""; // <-- Change this to write to file.
        BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));

        bw.write(labeled.toString());
        bw.close();
    }
}