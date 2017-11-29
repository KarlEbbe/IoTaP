package com.project.iotap.iotap.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.project.iotap.iotap.Bluetooth.BluetoothConnect;
import com.project.iotap.iotap.MachineLearning.WekaClassifier;
import com.project.iotap.iotap.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import weka.classifiers.functions.SimpleLogistic;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //BluetoothConnect btc = new BluetoothConnect(getApplicationContext());
        WekaClassifier classifier = new WekaClassifier();

        try{

            classifier.createClassifier();

        }catch(Exception e){
            System.out.println("ERROR:");
            System.out.println(e);
        }

    }
}
