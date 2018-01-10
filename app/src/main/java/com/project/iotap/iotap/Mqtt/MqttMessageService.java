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
                    pahoMqttClient.subscribe(mqttAndroidClient, MqttConstants.GREETING_TOPIC, 1); //When we are connected to mqtt, we subscribe to greeting.
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
                String message = mqttMessage.toString();
                Log.d(TAG, "package arrived from mqtt: " + s + " : " + message);

                if (message.startsWith(MqttConstants.GREETING)) {
                    identifyAddress = message.substring(3);
                    sendIntentToMain(MqttConstants.GREETING, identifyAddress);
                } else if (message.startsWith(MqttConstants.COMMAND)) {
                    commandAddress = message.substring(3);
                    if (deviceId.equals(commandAddress)) {//------------------< Step 6 in protocol.  Not sure if this is correct!
                        sendIntentToMain(MqttConstants.COMMAND, message);
                    } else {
                        Log.d(TAG, "deviceId didn't match!");
                    }

                } else if (message.startsWith(MqttConstants.DISCONNECT)) {
                    sendIntentToMain(MqttConstants.DISCONNECT, "");
                    commandAddress = null;
                    pahoMqttClient.disconnect(mqttAndroidClient);
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
                intentMessageReceiver, new IntentFilter(MqttConstants.GREETING));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                intentMessageReceiver, new IntentFilter(MqttConstants.COMMAND));
    }

    /**
     * Retrieves and sets a device name or later identification.
     *
     * @return
     */
    private void setDeviceId() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        deviceId = (manufacturer + model);
    }


    @Override
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
            if (intentName.equals(MqttConstants.GREETING)) {
                publishMessage("1:" + deviceId, identifyAddress);
            } else if (intentName.equals(MqttConstants.COMMAND)) {
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