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
import android.widget.TextView;
import android.widget.Toast;

import com.project.iotap.iotap.Bluetooth.BTCallback;
import com.project.iotap.iotap.Bluetooth.BluetoothHandler;
import com.project.iotap.iotap.MachineLearning.DataPreProcesser;
import com.project.iotap.iotap.MachineLearning.WekaClassifier;
import com.project.iotap.iotap.Mqtt.MqttConstants;
import com.project.iotap.iotap.Mqtt.MqttMessageService;
import com.project.iotap.iotap.R;
import com.project.iotap.iotap.Shared.Direction;
import com.project.iotap.iotap.Shared.ExcellToArray;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private Button btnGreet;
    private TextView twProxId;

    private BluetoothHandler bluetoothHandler;
    private WekaClassifier wekaClassifier;
    private DataPreProcesser dataPreProcesser;

    private String commandAddress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupGreetButton();
        twProxId = (TextView) findViewById(R.id.twProxId);
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.colorBCGray));

        dataPreProcesser = new DataPreProcesser();
        wekaClassifier = new WekaClassifier(getApplicationContext());
        setupIntentReceivers();
        setupBluetooth();
        restartMqttService();
    }

    /**
     * Makes this activity listen for intents.
     */
    private void setupIntentReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(MqttConstants.GREETING));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(MqttConstants.COMMAND));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(MqttConstants.DISCONNECT));
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
                sendIntentToService(MqttConstants.GREETING, "");
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
        final ExcellToArray lol =  new ExcellToArray();
        final Button btnConnectSensor = (Button) findViewById(R.id.btnConnectSensor);
        btnConnectSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothHandler == null) {
                    Toast.makeText(getApplicationContext(), "Try connecting...", Toast.LENGTH_LONG).show();
                    btnConnectSensor.setText("Disconnect Motion Sensor");
                    bluetoothHandler = new BluetoothHandler(new BTCallback() {
                        @Override
                        public void rawGestureDataCB(int[][] rawGestureData) {

                            rawGestureData = lol.getArray(1); //----------------------------------------------------------DEBUG! To be removed
                            dataPreProcesser.processData(rawGestureData);
                            Direction direction = wekaClassifier.classifyTuple(rawGestureData);

                            Log.d(TAG, "Gesture: " + String.valueOf(direction));
                            Toast.makeText(getApplicationContext(), "Gesture: " + String.valueOf(direction), Toast.LENGTH_LONG).show();

                            publishGestureToArdunio(direction);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "Disconnected BT.", Toast.LENGTH_LONG).show();
                    btnConnectSensor.setText("Connect Motion Sensor");
                    bluetoothHandler.cancel();
                    bluetoothHandler = null;
                }
            }
        });
    }

    private void publishGestureToArdunio(Direction direction) {
        if (commandAddress != null) {
            sendIntentToService(MqttConstants.COMMAND, direction.name().toLowerCase());
        } else {
            Log.d(TAG, "No commandAddress");
            Toast.makeText(getApplicationContext(), "Couldn't publish gesture to sensor!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Receives intents
     */
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentName = intent.getAction();
            assert intentName != null;
            Log.d(TAG, "Intent received: " + intentName);
            switch (intentName) {
                case MqttConstants.GREETING:
                    btnGreet.setEnabled(true); //Enable the handshake button. Maybe we should have a timeout or something here?
                    twProxId.setText(intent.getStringExtra("extra"));
                    break;
                case MqttConstants.COMMAND:
                    commandAddress = intent.getStringExtra("extra");
                    break;
                case MqttConstants.DISCONNECT:
                    commandAddress = null;
                    break;
            }
        }
    };

    /**
     * Sends an intent to mqttService with a name and some string data.
     */
    private void sendIntentToService(String intentName, String extra) {
        Intent intent = new Intent(intentName);
        intent.putExtra("extra", extra);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
