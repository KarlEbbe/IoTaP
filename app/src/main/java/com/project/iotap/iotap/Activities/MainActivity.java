package com.project.iotap.iotap.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.project.iotap.iotap.Bluetooth.BluetoothConnect;
import com.project.iotap.iotap.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothConnect btc = new BluetoothConnect(getApplicationContext());
    }
}
