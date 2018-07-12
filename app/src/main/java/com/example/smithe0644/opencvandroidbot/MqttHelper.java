package com.example.smithe0644.opencvandroidbot;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by WILLIAM LIN on 4/4/2018 for the Android Robot Controller.
 * THIS IS PROBABLY A USEFUL CLASS
 */

public class MqttHelper {

    public MqttAndroidClient mqttAndroidClient;

    static final String serverUri = "tcp://m10.cloudmqtt.com:15345";

    static final String clientId = "AndroidClient";
    static final String subscriptionTopic = "Commands";

    static String username = "Android";
    static String password = "bean";

    public MqttHelper(Context context){
        mqttAndroidClient = new MqttAndroidClient(context,serverUri,clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w("mqtt", serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.w(topic,message.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        Connect();

    }

    public void setCallback(MqttCallbackExtended callback){
        mqttAndroidClient.setCallback(callback);
    }

    private void Connect(){

        MqttConnectOptions params = new MqttConnectOptions();
        params.setAutomaticReconnect(true);
        params.setCleanSession(false);
        params.setUserName(username);
        params.setPassword(password.toCharArray());

        try{

            mqttAndroidClient.connect(params, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions failparams = new DisconnectedBufferOptions();
                    failparams.setBufferEnabled(true);
                    failparams.setBufferSize(100);
                    failparams.setPersistBuffer(false);
                    failparams.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(failparams);
                    publish(subscriptionTopic,"hello Android".getBytes(), 0, false);
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception){
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
        }
    }
    private void subscribe(){
        try{
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("Mqtt", "Subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Subscription failed");

                }
            });
        }catch(MqttException e){
            System.err.println("Exception issue");
            e.printStackTrace();
        }
    }

    public void publish(String topic, byte[] payload, int qos, boolean retained){
        try{
            mqttAndroidClient.publish(topic,payload,qos,retained);
        } catch (MqttException e){
            e.printStackTrace();
        }
    }

}
