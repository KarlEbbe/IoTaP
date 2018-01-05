package com.project.iotap.iotap.Mqtt;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
    private String macAddress;
    private String commandTopic;
    private String identifyAddress;
    private String commandAddress;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert manager != null;
        WifiInfo info = manager.getConnectionInfo();
        macAddress = info.getMacAddress();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("publishGreet"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("gesture"));




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
                    identifyAddress = message.substring(3); //Save the identifyAddress

                    sendIntentToMain("greet", identifyAddress);
                    //pahoMqttClient.publishMessage(mqttAndroidClient, macAddress, MqttConstants.QOS, message.substring(3));// Send back mac address.
                } else if (message.startsWith("CMD:")) { // Remember topic.
                    commandTopic = message.substring(4);
                    pahoMqttClient.subscribe(mqttAndroidClient, commandTopic, MqttConstants.QOS);
                } else if (message.startsWith("END")) { // Disconnect.
                    sendIntentToMain("disconnect", "NULL");
                    commandAddress = null;
                    pahoMqttClient.disconnect(mqttAndroidClient);
                }else if(message.contains(macAddress)){
                    commandAddress = message; //Send this to Main Activity.
                    sendIntentToMain("commandAddress",message);
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

    private void sendIntentToMain(String intentName, String extra) {
        Intent intent = new Intent(intentName);
        intent.putExtra("extra", extra);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Receives intents
     */
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentName = intent.getAction();
            Log.d(TAG, "Intent received: " + intentName);
            if(intentName.equals("publishGreet")){
                try {
                    pahoMqttClient.publishMessage(mqttAndroidClient, macAddress, MqttConstants.QOS, identifyAddress);// Send back mac address.
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }else if(intentName.equals("publishGesture")){
                try {
                    pahoMqttClient.publishMessage(mqttAndroidClient, intent.getStringExtra("extra"), MqttConstants.QOS, commandAddress);// Send gesture to arduino.
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}