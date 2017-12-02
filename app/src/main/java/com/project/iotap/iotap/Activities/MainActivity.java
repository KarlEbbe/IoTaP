package com.project.iotap.iotap.Activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.project.iotap.iotap.Bluetooth.BluetoothHandler;
import com.project.iotap.iotap.R;

public class MainActivity extends AppCompatActivity {

    private Button btnTest;
    private BluetoothHandler btc = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupBtns();
        //WekaClassifier classifier = new WekaClassifier(getApplicationContext());
    }

    //Adds click listeners to the buttons.
    private void setupBtns() {
        btnTest = (Button) findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btc == null){
                    Toast.makeText(getApplicationContext(), "New BluetoothHandler", Toast.LENGTH_LONG).show();
                    btc = new BluetoothHandler(MainActivity.this);
                }else{
                    Toast.makeText(getApplicationContext(), "Cancel BluetoothHandler", Toast.LENGTH_LONG).show();
                    btc = null;
                }
            }
        });
    }

    //Executes when user has turned bt on or off. Probably not needed.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String msg = "";
        if (resultCode == RESULT_OK) {
            msg = "BT turned on!";
        }
        if (resultCode == RESULT_CANCELED) {
            msg = "BT turned off!";
        }
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
}
