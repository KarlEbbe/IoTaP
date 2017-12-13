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
import com.project.iotap.iotap.Mqtt.Constants;
import com.project.iotap.iotap.Mqtt.MqttMessageService;
import com.project.iotap.iotap.Mqtt.PahoMqttClient;
import com.project.iotap.iotap.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private Button btnTest;
    private BluetoothHandler testBtc = null;

    private Button btnGreet;
    private Button btnSend;

    private EditText text;

    private MqttAndroidClient client;
    private final String TAG = "MainActivity";
    private PahoMqttClient pahoMqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (EditText) findViewById(R.id.textMessage);

        this.pahoMqttClient = new PahoMqttClient();


        //setupMqtt();
        setupBtns();
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
        //WekaClassifier classifier = new WekaClassifier(getApplicationContext());
    }

    private void setupMqtt(){


    }

    //Adds click listeners to the buttons.
    private void setupBtns() {
        btnTest = (Button) findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testBtc == null){
                    Toast.makeText(getApplicationContext(), "Try connecting...", Toast.LENGTH_LONG).show();

                    testBtc = new BluetoothHandler(MainActivity.this, new BTCallback() {
                        @Override
                        public void rawGestureDataCB(int[][] rawGestureData) {
                            Log.d(TAG, "Callback for gesture data fired!");
                        }
                    });
                }else{
                    Toast.makeText(getApplicationContext(), "Cancel connecting...", Toast.LENGTH_LONG).show();
                    //Perhaps some cancel method in the BT Handler for stopping the threads.
                    testBtc = null;
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
