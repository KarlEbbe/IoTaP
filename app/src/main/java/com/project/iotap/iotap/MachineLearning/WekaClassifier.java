package com.project.iotap.iotap.MachineLearning;

/**
 * Created by Anton on 2017-11-29.
 * Example from https://www.dropbox.com/s/hgx1y9ciqzo6525/wekaClassifier.java?dl=0
 * for using Weka with java.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import weka.classifiers.trees.J48;
import weka.core.Instances;

public class WekaClassifier {
    public static void main(String[] args) throws Exception {
        // load training data
        BufferedReader breader = null;
        breader = new BufferedReader(new FileReader("/train.arff"));
        Instances train = new Instances(breader);
        train.setClassIndex(train.numAttributes() - 1);

        // load test data
        breader = new BufferedReader(new FileReader("/test1.arff"));
        Instances test = new Instances(breader);
        test.setClassIndex(test.numAttributes() - 1);


        breader.close();

        // build classifier
//  String[] options = new String[1];
        J48 tree = new J48();         // new instance of tree
        tree.buildClassifier(train);

        //label the test data
        int classIndex = train.numAttributes() - 1;
        Instances labeled = new Instances(test);
        for (int i = 0; i < test.numInstances(); i++) {
            double clsLabel = tree.classifyInstance(test.instance(i));
            labeled.instance(i).setClassValue(clsLabel);
            System.out.println(labeled.instance(i).attribute(classIndex).value((int) clsLabel));
        }
//  save labeled data
        BufferedWriter writer = new BufferedWriter(
                new FileWriter("/Users/ag6031/Dropbox/IOTAP/Teaching/IoTaP Course/Labs/Lab 3/labeled.arff"));
        writer.write(labeled.toString());
        writer.close();

    }
}