package com.wilson.higooo;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MQTTServcieConnection implements ServiceConnection {

    private MQTTService mqttService;
    private MQTTService.Callback callback;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mqttService = ((MQTTService.CustomBinder)service).getService();
        mqttService.setCallback(callback);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    public void setCallback(MQTTService.Callback callBack) {
        this.callback = callBack;
    }
}
