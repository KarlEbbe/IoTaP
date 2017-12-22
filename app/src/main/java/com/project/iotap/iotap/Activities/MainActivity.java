package com.project.iotap.iotap.Activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.project.iotap.iotap.Bluetooth.BTCallback;
import com.project.iotap.iotap.Bluetooth.BluetoothHandler;
import com.project.iotap.iotap.MachineLearning.DataNormalizer;
import com.project.iotap.iotap.MachineLearning.WekaClassifier;
import com.project.iotap.iotap.Mqtt.Constants;
import com.project.iotap.iotap.Mqtt.MqttMessageService;
import com.project.iotap.iotap.Mqtt.PahoMqttClient;
import com.project.iotap.iotap.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private Button btnTest;
    private Button btnGreet;
    private Button btnSend;
    private EditText text;

    private BluetoothHandler bluetoothHandler;

    private MqttAndroidClient client;
    private PahoMqttClient pahoMqttClient;

    private WekaClassifier wekaClassifier;
    private DataNormalizer dataNormalizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (EditText) findViewById(R.id.textMessage);
        setupBtButton();
        setupMqtt();
        dataNormalizer = new DataNormalizer();
        //wekaClassifier = new WekaClassifier(getApplicationContext());



        //test();
    }

    private void test() {
        int[][] x = testRawGestureData();
        dataNormalizer.processData(x);
        wekaClassifier.classifyTuple(x);
    }

    /**
     * Setups MQTT
     */
    private void setupMqtt() {
        this.pahoMqttClient = new PahoMqttClient();
        this.client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        btnGreet = (Button) findViewById(R.id.Greet);
        btnGreet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Is null?:" + pahoMqttClient.equals(null));
                System.out.println("Is null?:" + client.equals(null));

                pahoMqttClient.startListenForGreet(client);
            }
        });

        btnSend = (Button) findViewById(R.id.send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = text.getText().toString().trim();
                if (!msg.isEmpty()) {
                    try {
                        pahoMqttClient.publishMessage(client, msg, 1, Constants.GREET_SUBSCRIBE_TOPIC);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Intent intent = new Intent(MainActivity.this, MqttMessageService.class);
        startService(intent);
    }

    /**
     * Setups the bluetooth button with listeners.
     */
    private void setupBtButton() {
        btnTest = (Button) findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothHandler == null) {
                    Toast.makeText(getApplicationContext(), "Try connecting...", Toast.LENGTH_LONG).show();
                    bluetoothHandler = new BluetoothHandler(new BTCallback() {
                        @Override
                        public void rawGestureDataCB(int[][] rawGestureData) {
                            Log.d(TAG, "Callback for gesture data fired!");
                            Toast.makeText(getApplicationContext(), "GESTURE DETECTED!!!", Toast.LENGTH_LONG).show();

                            dataNormalizer.processData(rawGestureData);
//                            wekaClassifier.classifyTuple(rawGestureData);

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


    private int[][] testRawGestureData() {
        Random rand = new Random();

        int[][] rawGestureData = new int[20][6];
        for (int i = 2; i < rawGestureData.length; i++) {
            for (int j = 0; j < rawGestureData[i].length; j++) {
                int randomNbr = rand.nextInt(6000) - 2000;
                rawGestureData[i][j] = randomNbr;
            }
        }

        return rawGestureData;
    }
}
