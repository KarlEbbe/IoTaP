package com.project.iotap.iotap.Mqtt;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttMessageService extends Service {

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    private String macAddress;
    private String commandTopic;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert manager != null;
        WifiInfo info = manager.getConnectionInfo();
        macAddress = info.getMacAddress();

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
                Log.d(TAG, "messageArrived.");
                String message = mqttMessage.toString();
                if (message.startsWith("ID:")) { // Send back mac address.
                    pahoMqttClient.publishMessage(mqttAndroidClient, macAddress, 1, message.substring(3));
                } else if (message.startsWith("CMD:")) { // Remember topic.
                    commandTopic = message.substring(4);
                    pahoMqttClient.subscribe(mqttAndroidClient, commandTopic, 1);
                } else if (message.startsWith("END")) { // Disconnect.
                    pahoMqttClient.disconnect(mqttAndroidClient);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.d(TAG, "deliveryComplete.");
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}