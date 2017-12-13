package com.project.iotap.iotap.Mqtt;

import java.util.Random;

/**
 * Created by brijesh on 20/4/17.
 */

public class Constants {

  //  public static final String MQTT_BROKER_URL = "tcp://iot.eclipse.org:1883";

    private static Random rdm = new Random();


    public static final String MQTT_BROKER_URL = "tcp://m23.cloudmqtt.com:16532";
    //public static final String PUBLISH_TOPIC = "androidkt/topic";

    public static final String CLIENT_ID = "android";

    public static final String CLIENT_USER = "enqeeoyx";
    public static final String CLIENT_PASSWORD = "uBY_2qW7Eesw";

    public static final int CLIENT_TOPIC = rdm.nextInt();
    public static final String GREET_SUBSCRIBE_TOPIC = "/Greeting/Prox";

    //public static final String GREET_PUBLISH_TOPIC = "Greeting/Phone";


}

