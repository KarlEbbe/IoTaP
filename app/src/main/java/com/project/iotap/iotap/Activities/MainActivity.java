package com.project.iotap.iotap.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.project.iotap.iotap.Bluetooth.BTCallback;
import com.project.iotap.iotap.Bluetooth.BluetoothHandler;
import com.project.iotap.iotap.MachineLearning.DataNormalizer;
import com.project.iotap.iotap.MachineLearning.WekaClassifier;
import com.project.iotap.iotap.Mqtt.MqttMessageService;
import com.project.iotap.iotap.R;
import com.project.iotap.iotap.Shared.Direction;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private Button btnGreet;

    private BluetoothHandler bluetoothHandler;
    private WekaClassifier wekaClassifier;
    private DataNormalizer dataNormalizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupGreetButton();
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.colorBCGray));

        dataNormalizer = new DataNormalizer();
        wekaClassifier = new WekaClassifier(getApplicationContext());
        setupIntent();
        setupBluetooth();
        restartMqttService();
    }

    /**
     * Makes this activity listen for intents.
     */
    private void setupIntent() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("greet"));
    }

    /**
     * Setups the greet button
     */
    private void setupGreetButton() {
        btnGreet = (Button) findViewById(R.id.btnGreet);
        btnGreet.setEnabled(false);
        btnGreet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIntentToService();
            }
        });
    }


    /**
     * Restarts the mqtt service.
     */
    private void restartMqttService() {
        stopService(new Intent(MainActivity.this, MqttMessageService.class));
        startService(new Intent(MainActivity.this, MqttMessageService.class));
    }

    /**
     * Setups the bluetooth button with listeners.
     */
    private void setupBluetooth() {
        Button btnTest = (Button) findViewById(R.id.btnConnectSensor);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothHandler == null) {
                    Toast.makeText(getApplicationContext(), "Try connecting...", Toast.LENGTH_LONG).show();
                    bluetoothHandler = new BluetoothHandler(new BTCallback() {
                        @Override
                        public void rawGestureDataCB(int[][] rawGestureData) {

                            dataNormalizer.processData(rawGestureData);
                            Direction direction = wekaClassifier.classifyTuple(rawGestureData);

                            Log.d(TAG, "Gesture: " + String.valueOf(direction));
                            Toast.makeText(getApplicationContext(), "Gesture: " + String.valueOf(direction), Toast.LENGTH_LONG).show();

                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "Cancel connecting...", Toast.LENGTH_LONG).show();
                    bluetoothHandler.cancel();
                    bluetoothHandler = null;
                }
            }
        });
    }


    /**
     * Receives intents
     */
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentName = intent.getAction();
            Log.d(TAG, "Intent received: " + intentName);
            if(intentName.equals("greet")){
                btnGreet.setEnabled(true); //Enable the handshake button. Maybe we should have a timeout or something here?
            }
        }
    };

    /**
     * Sends an intent to be received by the mqtt service to send a greet message.
     */
    private void sendIntentToService() {
        Intent intent = new Intent("publishGreet");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
