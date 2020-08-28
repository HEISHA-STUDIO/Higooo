package com.wilson.higooo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTService extends Service {

    private static final String TAG = "HEISHA";
    public static final String TOPIC_PUB = "96827e77dfef4cc6-s";
    public static final String TOPIC_SUB = "96827e77dfef4cc6-p";
    public static final String TOPIC_VIDEO = "96827e77dfef4cc6-v";

    private static MqttAndroidClient client;
    private MqttConnectOptions conOpt;

    private String host = "139.199.24.138:9002";
    private Callback callback;

    public static void publishTopic(String topic, byte[] data) {
        try {
            if (client != null) {
                client.publish(topic, data, 0, false);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initConnect();
    }

    private void initConnect() {
        String uri = "tcp://" + host;

        client = new MqttAndroidClient(this, uri, "heisha123456");
        client.setCallback(mqttCallback);

        conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);
        conOpt.setConnectionTimeout(10);
        conOpt.setKeepAliveInterval(20);

        doClientConnection();
    }

    public void doClientConnection() {
        if(!client.isConnected()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        stopSelf();
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.i(TAG, "mqtt connect success");
            try {
                client.subscribe(TOPIC_SUB, 1);
            } catch (MqttException e) {
                e.printStackTrace();
            }

            try {
                client.subscribe(TOPIC_VIDEO, 1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

        }
    };

    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            if(topic.equals(TOPIC_SUB)) {
                if(callback != null) {
                    callback.onDLinkMessageReceive(message.getPayload());
                }
            }

            if(topic.equals(TOPIC_VIDEO)) {
                if(videoFeeder != null) {
                    videoFeeder.onDataReceive(message.getPayload());
                }
            }

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    public class CustomBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new CustomBinder();
    }

    public interface Callback {
        void onDLinkMessageReceive(byte[] data);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface VideoFeeder {
        void onDataReceive(byte[] data);
    }
    private static VideoFeeder videoFeeder = null;

    public static void setVideoFeeder(VideoFeeder videoFeeder) {
        MQTTService.videoFeeder = videoFeeder;
    }
}
