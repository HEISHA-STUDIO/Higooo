package com.wilson.higooo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.DLink.DLinkPacket;
import com.DLink.DLinkParser;
import com.DLink.enums.COMPONENT;
import com.DLink.messages.dlink_msg_heartbeat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

import static com.DLink.messages.dlink_msg_drone_attitude.DLINK_MSG_ID_DRONE_ATTITUDE;
import static com.DLink.messages.dlink_msg_drone_battery_status.DLINK_MSG_ID_DRONE_BATTERY_STATUS;
import static com.DLink.messages.dlink_msg_drone_global_position.DLINK_MSG_ID_DRONE_GLOBAL_POSITION;
import static com.DLink.messages.dlink_msg_drone_status.DLINK_MSG_ID_DRONE_STATUS;
import static com.DLink.messages.dlink_msg_gps_status.DLINK_MSG_ID_GPS_STATUS;
import static com.DLink.messages.dlink_msg_heartbeat.DLINK_MSG_ID_HEARTBEAT;
import static com.DLink.messages.dlink_msg_mission_ack.DLINK_MSG_ID_MISSION_ACK;
import static com.DLink.messages.dlink_msg_mission_item.DLINK_MSG_ID_MISSION_ITEM;
import static com.DLink.messages.dlink_msg_mission_request.DLINK_MSG_ID_MISSION_REQUEST;
import static com.DLink.messages.dlink_msg_rc_battery_status.DLINK_MSG_ID_RC_BATTERY_STATUS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MQTTService.Callback{

    private static final String TAG = "HEISHA";

    private static final String[] REQUIRED_PERMISSION_LIST = new String[] {
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    private List<String> missingPermission = new ArrayList<>();
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    private TextView mTextProduct;
    private TextView mTextRC;
    private TextView mTextDrone;

    private MQTTServcieConnection mqttServcieConnection;
    private DLinkParser dLinkParser;
    private DLinkPacket dLinkPacket;

    private Button btn_rc;
    private Button btn_drone;
    private Button btn_lock;
    private Button btn_unlock;
    private Button btn_charge;
    private Button btn_flight_view;

    MQTTService.VideoFeeder videoFeeder = new MQTTService.VideoFeeder() {
        @Override
        public void onDataReceive(byte[] data) {
            Log.i(TAG, "C: " + data[0] + " DD: " + (data[1]+1) + " L: " + data.length);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        setContentView(R.layout.activity_main);

        mTextProduct = (TextView)findViewById(R.id.prodect_model);
        mTextRC = (TextView)findViewById(R.id.rc_status);
        mTextDrone = (TextView)findViewById(R.id.drone_status);

        btn_rc = (Button)findViewById(R.id.btn_rc);
        btn_drone = (Button)findViewById(R.id.btn_drone);
        btn_lock = (Button)findViewById(R.id.btn_lock);
        btn_unlock = (Button)findViewById(R.id.btn_unlock);
        btn_charge = (Button)findViewById(R.id.btn_charge);
        btn_flight_view = (Button)findViewById(R.id.btn_flight);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISDKBridge.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        btn_rc.setOnClickListener(this);
        btn_drone.setOnClickListener(this);
        btn_lock.setOnClickListener(this);
        btn_unlock.setOnClickListener(this);
        btn_charge.setOnClickListener(this);
        btn_flight_view.setOnClickListener(this);

        dLinkParser = new DLinkParser();

        if(mqttServcieConnection == null) {
            mqttServcieConnection = new MQTTServcieConnection();
            mqttServcieConnection.setCallback(this);

            Intent intent = new Intent(this, MQTTService.class);
            bindService(intent, mqttServcieConnection, Context.BIND_AUTO_CREATE);
        }

        //MQTTService.setVideoFeeder(videoFeeder);

        new Thread(updateUI).start();
    }

    private void checkAndRequestPermissions() {
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }

        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        } else {
            Log.i(TAG, "All permissions granted");
            //startSDKRegistration();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_PERMISSION_CODE) {
            for(int i = grantResults.length - 1; i>=0; i--) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }

        if(missingPermission.isEmpty()) {
            startSDKRegistration();
            Log.i(TAG, "All permissions granted");
        } else {
            Log.i(TAG, "Permissions missing");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            Log.i(TAG, djiError.getDescription());
                            if(DJISDKError.REGISTRATION_SUCCESS == djiError) {
                                DJISDKManager.getInstance().startConnectionToProduct();
                            }
                        }

                        @Override
                        public void onProductDisconnect() {

                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {

                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long l, long l1) {

                        }
                    });
                }
            });
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = DJISDKBridge.getProductInstance();

        mTextProduct.setText("Changed");

        if(null != mProduct && mProduct.isConnected()) {

            if(null != mProduct.getModel()) {
                mTextProduct.setText("" + mProduct.getModel().getDisplayName());
            } else {
                mTextProduct.setText("RC connected");
            }
        } else {
            mTextProduct.setText("No product connected");
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_flight:
                Intent intent = new Intent(this, FlightActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_rc:
                DLinkBridge.getInstance().sendTurnOnOffRCCMD();
                break;
            case R.id.btn_drone:
                DLinkBridge.getInstance().sendTurnOnOffDroneCMD();
                break;
            case R.id.btn_lock:
                DLinkBridge.getInstance().sendLockCMD();
                break;
            case R.id.btn_unlock:
                DLinkBridge.getInstance().sendUnLockCMD();
                break;
            case R.id.btn_charge:
                DLinkBridge.getInstance().sendChargeCMD();
                break;
        }
    }

    @Override
    public void onDLinkMessageReceive(byte[] data) {
        for(int i=0; i<data.length; i++) {
            int ch = 0xFF & data[i];

            if ((dLinkPacket = dLinkParser.dlink_parse_char(ch)) != null) {
                DLinkBridge.getInstance().handle_message(dLinkPacket);
                dlink_message_handle(dLinkPacket);
            }
        }
    }

    private void dlink_message_handle(DLinkPacket packet) {
        switch (packet.msgid) {
            case DLINK_MSG_ID_HEARTBEAT:
                handle_dlink_heartbeat(packet);
                break;
            case DLINK_MSG_ID_MISSION_ITEM:
            case DLINK_MSG_ID_MISSION_ACK:
            case DLINK_MSG_ID_MISSION_REQUEST:
                MissionPlanner.getInstance().dlink_message_handle(packet);
                break;
            case DLINK_MSG_ID_RC_BATTERY_STATUS:
            case DLINK_MSG_ID_DRONE_BATTERY_STATUS:
            case DLINK_MSG_ID_DRONE_ATTITUDE:
            case DLINK_MSG_ID_DRONE_GLOBAL_POSITION:
            case DLINK_MSG_ID_DRONE_STATUS:
            case DLINK_MSG_ID_GPS_STATUS:
                DLinkBridge.getInstance().handle_message(packet);
                break;
        }
    }

    private boolean c200m2_isconnected = false;
    private boolean rc_on = false;
    private boolean drone_on = false;
    private long last_heartbeat_timestamp = 0;

    private void handle_dlink_heartbeat(DLinkPacket dLinkPacket) {
        dlink_msg_heartbeat msg = (dlink_msg_heartbeat)dLinkPacket.unpack();

        last_heartbeat_timestamp = System.currentTimeMillis();
        if(!c200m2_isconnected) {
            c200m2_isconnected = true;
            mTextProduct.setText(DLinkBridge.getInstance().getModelName(msg.product_model));
        }

        if((msg.system_status & COMPONENT.DJI_COMPONENT_RC)>0) {
            rc_on = true;
            mTextRC.setText("Remote Contoller: ON");
        } else {
            rc_on = false;
            mTextRC.setText("Remote Contoller: OFF");
        }

        if((msg.system_status & COMPONENT.DJI_COMPONENT_FC)>0) {
            drone_on = true;
            mTextDrone.setText("DJI Drone: ON");
        } else {
            drone_on = false;
            mTextDrone.setText("DJI Drone: OFF");
        }

        if(drone_on && rc_on) {
            btn_flight_view.setEnabled(true);
        } else {
            btn_flight_view.setEnabled(true);
        }
    }

    Runnable updateUI = new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if((System.currentTimeMillis() - last_heartbeat_timestamp) > 3000) {
                    c200m2_isconnected = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextProduct.setText("Waiting for connection");
                            btn_flight_view.setEnabled(true);
                            mTextDrone.setText("DJI Drone: OFF");
                            mTextRC.setText("Remote Controller: OFF");
                        }
                    });
                }
            }
        }
    };

}
