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
    private String deviceId, idTopic, commandTopic;

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
                try {
                    pahoMqttClient.subscribe(mqttAndroidClient, MqttConstants.GREETING_TOPIC, 1); //When we are connected to mqtt, we subscribe to greeting.
                } catch (MqttException e) {
                    Log.e(TAG, "Error when subscribing: " + Log.getStackTraceString(e));
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "Connection to MQTT service lost.");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                String message = mqttMessage.toString();
                if (message.startsWith(MqttConstants.GREETING)) { // We are in proximity of sensor and have received idTopic.
                    idTopic = message.substring(2);
                    sendIntentToMain(MqttConstants.GREETING, idTopic);
                } else if (message.startsWith(MqttConstants.COMMAND)) { // Sensor received deviceId, and they sent it back to confirm it.
                    commandTopic = message.substring(2);
                    if (deviceId.equals(commandTopic)) {
                        mqttAndroidClient.unsubscribe(idTopic);
                        sendIntentToMain(MqttConstants.COMMAND, message); // We now have the topic on which we can send commands.
                    } else {
                        Log.e(TAG, "Device ID from sensor didn't match!");
                    }
                } else if (message.startsWith(MqttConstants.DISCONNECT)) {
                    sendIntentToMain(MqttConstants.DISCONNECT, "");
                    commandTopic = null;
                    mqttAndroidClient.unsubscribe(commandTopic);
                    mqttAndroidClient.unsubscribe(idTopic);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.d(TAG, "Delivery to MQTT service complete.");
            }
        });
    }

    /**
     * Registers receivers for intens.
     */
    private void setupIntentReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                intentMessageReceiver, new IntentFilter(MqttConstants.GREETING));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                intentMessageReceiver, new IntentFilter(MqttConstants.COMMAND));
    }

    /**
     * Retrieves the manufacturer and model of the unit, to be used as identification.
     */
    private void setDeviceId() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        deviceId = (manufacturer + model);
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Sends an intent with a name and some extra string data.
     * @param intentName name of the intent ("1:", "2:" or "3:")
     * @param extra extra string data
     */
    private void sendIntentToMain(String intentName, String extra) {
        Intent intent = new Intent(intentName);
        intent.putExtra("extra", extra);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Receives intents.
     */
    private final BroadcastReceiver intentMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentName = intent.getAction();
            assert intentName != null;
            if (intentName.equals(MqttConstants.GREETING)) { // User pressed the handshake button, so send deviceId.
                publishMessage(MqttConstants.GREETING + deviceId, idTopic);
            } else if (intentName.equals(MqttConstants.COMMAND)) {
                publishMessage(MqttConstants.COMMAND + intent.getStringExtra("extra"), commandTopic);
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