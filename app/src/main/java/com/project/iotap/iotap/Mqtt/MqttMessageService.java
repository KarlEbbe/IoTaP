package com.project.iotap.iotap.Mqtt;


import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.app.Service;
        import android.app.TaskStackBuilder;
        import android.content.Context;
        import android.content.Intent;
        import android.os.IBinder;
        import android.support.annotation.NonNull;
        import android.support.v4.app.NotificationCompat;
        import android.util.Log;

        import com.project.iotap.iotap.Activities.MainActivity;
        import com.project.iotap.iotap.R;

        import org.eclipse.paho.android.service.MqttAndroidClient;
        import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
        import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
        import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttMessageService extends Service {

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    private String currentTopic = Constants.GREET_SUBSCRIBE_TOPIC;

    public MqttMessageService() {
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
                String message = new String(mqttMessage.toString());
                //setMessageNotification(s, new String(mqttMessage.getPayload()));
                if(!message.equals("Unsubscribe") && currentTopic == Constants.GREET_SUBSCRIBE_TOPIC) {
                    pahoMqttClient.subscribe(mqttAndroidClient, message, 1);
                    pahoMqttClient.unSubscribe(mqttAndroidClient,currentTopic);
                    currentTopic = message;
                }else if (message.equals("Unsubscribe")){
                    pahoMqttClient.subscribe(mqttAndroidClient, Constants.GREET_SUBSCRIBE_TOPIC, 1);
                    pahoMqttClient.unSubscribe(mqttAndroidClient,currentTopic);
                    currentTopic = Constants.GREET_SUBSCRIBE_TOPIC;
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

    //This code is imported. For some reason it wont find "R.drawable.ic_message_black_24dp".
    //The only difference from the other program is that it does not have direct access
    // to the MainActivity
/*
    private void setMessageNotification(@NonNull String topic, @NonNull String msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_message_black_24dp)
                        .setContentTitle(topic)
                        .setContentText(msg);
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(100, mBuilder.build());
    }
    */
}
