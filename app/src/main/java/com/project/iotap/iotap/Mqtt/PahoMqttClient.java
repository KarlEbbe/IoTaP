package com.project.iotap.iotap.Mqtt;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class PahoMqttClient {

    private static final String TAG = "PahoMqttClient";
    private MqttAndroidClient mqttAndroidClient;

    public MqttAndroidClient getMqttClient(Context context, String brokerUrl, String clientId) {

        mqttAndroidClient = new MqttAndroidClient(context, brokerUrl, clientId);
        try {
            IMqttToken token = mqttAndroidClient.connect(getMqttConnectionOption());
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqttAndroidClient.setBufferOpts(getDisconnectedBufferOptions());
                    Log.d(TAG, "Success");
                    startListenForGreet(mqttAndroidClient);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failure " + exception.toString());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        return mqttAndroidClient;
    }

    public void disconnect(@NonNull MqttAndroidClient client) throws MqttException {
        IMqttToken mqttToken = client.disconnect();
        mqttToken.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "Successfully disconnected");
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.d(TAG, "Failed to disconnected " + throwable.toString());
            }
        });
    }

    @NonNull
    private DisconnectedBufferOptions getDisconnectedBufferOptions() {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(100);
        disconnectedBufferOptions.setPersistBuffer(false);
        disconnectedBufferOptions.setDeleteOldestMessages(false);
        return disconnectedBufferOptions;
    }

    @NonNull
    private MqttConnectOptions getMqttConnectionOption() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setUserName(MqttConstants.CLIENT_USER);
        mqttConnectOptions.setPassword(MqttConstants.CLIENT_PASSWORD.toCharArray());
        return mqttConnectOptions;
    }


    public void publishMessage(@NonNull MqttAndroidClient client, @NonNull String msg, int qos, @NonNull String topic)
            throws MqttException, UnsupportedEncodingException {
        byte[] encodedPayload;
        encodedPayload = msg.getBytes("UTF-8");
        MqttMessage message = new MqttMessage(encodedPayload);
        message.setId(320);
        message.setRetained(true);
        message.setQos(qos);
        client.publish(topic, message);
    }

    public void subscribe(@NonNull MqttAndroidClient client, @NonNull final String topic, int qos) throws MqttException {
        IMqttToken token = client.subscribe(topic, qos);
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "Subscribe Successfully " + topic);
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.e(TAG, "Subscribe Failed " + topic);

            }
        });
    }

    private void unsubscribe(@NonNull MqttAndroidClient client, @NonNull final String topic) throws MqttException {

        IMqttToken token = client.unsubscribe(topic);

        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "Unsubscribe Successfully " + topic);
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.e(TAG, "Unsubscribe Failed " + topic);
            }
        });
    }

    public void startListenForGreet(MqttAndroidClient client) {
        try {
            subscribe(client, MqttConstants.GREETING_TOPIC, 1);
        } catch (MqttException e) {
            System.out.println("Could not subscribe to " + MqttConstants.GREETING_TOPIC);
            e.printStackTrace();
        }
    }

    public void stopListenForGreet(MqttAndroidClient client) {
        try {
            unsubscribe(client, MqttConstants.GREETING_TOPIC);
        } catch (MqttException e) {
            System.out.println("Could not unsubscribe to " + MqttConstants.GREETING_TOPIC);
            e.printStackTrace();
        }
    }
}