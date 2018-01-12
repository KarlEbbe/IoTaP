package com.project.iotap.iotap.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.project.iotap.iotap.Bluetooth.BTCallback;
import com.project.iotap.iotap.Bluetooth.BluetoothHandler;
import com.project.iotap.iotap.MachineLearning.WekaClassifier;
import com.project.iotap.iotap.Mqtt.MqttConstants;
import com.project.iotap.iotap.Mqtt.MqttMessageService;
import com.project.iotap.iotap.R;
import com.project.iotap.iotap.Shared.Direction;

/**
 * Main Activity.
 * @author Anton Gustafsson, Christoffer Nilsson, Christoffer Strandberg, Karl-Ebbe Jönsson
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private Button btnHandshake;
    private TextView twProxId, twGesture;
    private BluetoothHandler bluetoothHandler;
    private WekaClassifier wekaClassifier;
    private String commandTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupHandshakeButton();
        twProxId = (TextView) findViewById(R.id.twProxId);
        twGesture = (TextView) findViewById(R.id.twGesture);
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.colorBCGray));
        wekaClassifier = new WekaClassifier(getApplicationContext());
        setupIntentReceivers();
        setupBluetooth();
        restartMqttService();
    }

    /**
     * Makes this Activity listen for Intents.
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
     * Sets up the handshake button.
     */
    private void setupHandshakeButton() {
        btnHandshake = (Button) findViewById(R.id.btnHandshake);
        btnHandshake.setEnabled(false);
        btnHandshake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIntentToService(MqttConstants.GREETING, ""); // We should now send the deviceId.
            }
        });
    }

    /**
     * Restarts the message service.
     */
    private void restartMqttService() {
        stopService(new Intent(MainActivity.this, MqttMessageService.class));
        startService(new Intent(MainActivity.this, MqttMessageService.class));
    }

    /**
     * Setups the bluetooth button with listeners.
     */
    private void setupBluetooth() {
        final Button btnConnectSensor = (Button) findViewById(R.id.btnConnectSensor);
        btnConnectSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothHandler == null) {
                    Toast.makeText(getApplicationContext(), "Connecting to motion sensor...", Toast.LENGTH_LONG).show();
                    btnConnectSensor.setText(getResources().getString(R.string.btnDisconnectSensor));
                    bluetoothHandler = new BluetoothHandler(new BTCallback() {
                        @Override
                        public void rawGestureDataCB(int[][] rawGestureData) {
                            Direction direction = wekaClassifier.classifyTuple(rawGestureData);
                            vibrate();
                            twGesture.setText(String.valueOf(direction));
                            publishGestureToArdunio(direction);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "Disconnected from motion sensor.", Toast.LENGTH_LONG).show();
                    btnConnectSensor.setText(getResources().getString(R.string.btnConnectSensor));
                    bluetoothHandler.cancel();
                    bluetoothHandler = null;
                }
            }
        });
    }

    /**
     * Publishes the given direction to the proximity sensor.
     * @param direction the direction
     */
    private void publishGestureToArdunio(Direction direction) {
        if (commandTopic != null) {
            sendIntentToService(MqttConstants.COMMAND, direction.name());
        } else {
            Log.e(TAG, "Command topic is null.");
            Toast.makeText(getApplicationContext(), "Couldn't publish gesture to sensor.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Vibrates the phone.
     */
    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(350);
    }

    /**
     * Receives intents.
     */
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentName = intent.getAction();
            assert intentName != null;
            switch (intentName) {
                case MqttConstants.GREETING: // We have idTopic, so enable handshake button. This button sends deviceId to sensor.
                    btnHandshake.setEnabled(true);
                    twProxId.setText(intent.getStringExtra("extra"));
                    break;
                case MqttConstants.COMMAND:
                    commandTopic = intent.getStringExtra("extra");
                    break;
                case MqttConstants.DISCONNECT:
                    commandTopic = null;
                    break;
            }
        }
    };

    /**
     * Sends an intent to MqttService with a name and a String.
     */
    private void sendIntentToService(String intentName, String extra) {
        Intent intent = new Intent(intentName);
        intent.putExtra("extra", extra);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}