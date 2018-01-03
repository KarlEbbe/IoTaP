package com.project.iotap.iotap.Mqtt;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.project.iotap.iotap.Shared.Constants;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttMessageService extends Service {

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    private String macAddress;
    private String commandTopic;

    public MqttMessageService() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert manager != null;
        WifiInfo info = manager.getConnectionInfo();
        macAddress = info.getMacAddress();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
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