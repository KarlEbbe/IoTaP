package com.project.iotap.iotap.Mqtt;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MqttMessageService extends Service {

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    private String deviceId, identifyAddress, commandAddress;

    @Override
    public void onCreate() {
        super.onCreate();
        setDeviceId();
        setupIntentReceivers();
        setupMqtt();
    }

    /**
     * Creates and sets callback for mqttClient.
     */
    private void setupMqtt() {
        pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), MqttConstants.MQTT_BROKER_URL, MqttConstants.CLIENT_ID);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.d(TAG, "connectComplete.");
                try {
                    pahoMqttClient.subscribe(mqttAndroidClient, MqttConstants.GREETING_TOPIC, 1);
                } catch (MqttException e) {
                    Log.d(TAG, "Error when subscribing: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "connectionLost.");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                Log.d(TAG, "messageArrived: " + s + ", " + mqttMessage.toString());
                String message = mqttMessage.toString();
                if (message.startsWith("ID:")) {
                    identifyAddress = message.substring(3);
                    sendIntentToMain("greet", identifyAddress);
                } else if (message.startsWith("DISCONNECT:")) {
                    sendIntentToMain("disconnect", "");
                    commandAddress = null;
                    pahoMqttClient.disconnect(mqttAndroidClient);
                } else if (message.contains(deviceId)) {
                    commandAddress = message;
                    sendIntentToMain("commandAddress", message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.d(TAG, "deliveryComplete.");
            }
        });
    }

    /**
     * Registers receivers for intens.
     */
    private void setupIntentReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                intentMessageReceiver, new IntentFilter("publishGreet"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                intentMessageReceiver, new IntentFilter("publishGesture"));
    }


    /**
     * Retrieves and sets a device name or later identification.
     * @return
     */
    public void setDeviceId() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        deviceId = (manufacturer + model);
    }


    @Override //TODO not used but needs to be implemented.
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Sends an intent with a name and some extra string data.
     *
     * @param intentName
     * @param extra
     */
    private void sendIntentToMain(String intentName, String extra) {
        Intent intent = new Intent(intentName);
        intent.putExtra("extra", extra);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Receives intents
     */
    private final BroadcastReceiver intentMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentName = intent.getAction();
            Log.d(TAG, "Intent received: " + intentName);
            assert intentName != null;
            if (intentName.equals("publishGreet")) {
                publishMessage(deviceId, identifyAddress);
            } else if (intentName.equals("publishGesture")) {
                publishMessage(intent.getStringExtra("extra"), commandAddress);
            }
        }
    };

    /**
     * Publishes a message to a topic.
     *
     * @param message
     * @param topic
     */
    private void publishMessage(String message, String topic) {
        try {
            pahoMqttClient.publishMessage(mqttAndroidClient, message, MqttConstants.QOS, topic);
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}