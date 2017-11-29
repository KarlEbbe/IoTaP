package com.project.iotap.iotap.Activities;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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

    private Button btnTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupBtns();

        //WekaClassifier classifier = new WekaClassifier(getApplicationContext());
    }

    private void setupBtns() {

        btnTest = (Button) findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //just some testing code for the bt.
                BluetoothConnect btc = new BluetoothConnect(getApplicationContext());
            }
        });
    }
}
