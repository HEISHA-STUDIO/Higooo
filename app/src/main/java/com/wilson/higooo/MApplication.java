package com.wilson.higooo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.secneo.sdk.Helper;

public class MApplication extends Application {

    private DJISDKBridge djisdkBridge;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Helper.install(MApplication.this);

        if(djisdkBridge == null) {
            djisdkBridge = new DJISDKBridge();
            djisdkBridge.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("HEISHA", "MApplication onCreate");
        djisdkBridge.onCreate();
    }
}
