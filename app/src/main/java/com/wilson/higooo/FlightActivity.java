package com.wilson.higooo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.DLink.DLinkPacket;
import com.DLink.messages.dlink_msg_drone_attitude;
import com.DLink.messages.dlink_msg_drone_battery_status;
import com.DLink.messages.dlink_msg_drone_global_position;
import com.DLink.messages.dlink_msg_drone_status;
import com.DLink.messages.dlink_msg_gps_status;
import com.DLink.messages.dlink_msg_heartbeat;
import com.DLink.messages.dlink_msg_mission_count;
import com.DLink.messages.dlink_msg_rc_battery_status;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolylineOptions;
import com.amap.api.maps2d.model.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

public class FlightActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener, AMap.OnMapClickListener {

    private static final String TAG = "HEISHA";

    private MapView mMapView = null;
    private AMap aMap = null;

    protected TextureView mVideoSurface = null;
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected DJICodecManager djiCodecManager = null;

    private Button btn_add;
    private Button btn_clear;
    private Button btn_upload;
    private Button btn_takeoff;
    private Button btn_video;

    private boolean isAdd = false;
    private final Map<Integer, Marker> mMarker = new ConcurrentHashMap<Integer, Marker>();

    private boolean showVideo = false;

    private double droneLocationLat = 0.0;
    private double droneLocationLon = 0.0;
    private Marker droneMarker = null;

    DLinkBridge.DLinkListener dLinkListener = new DLinkBridge.DLinkListener() {
        @Override
        public void onDroneStatusUpdate(dlink_msg_drone_status drone_status) {
            TextView tv = (TextView)findViewById(R.id.text_drone_arm);
            if(drone_status.armed == 1) {
                tv.setText("ARMED");
            } else {
                tv.setText("DISARMED");
            }
        }

        @Override
        public void onDronePositionUpdate(dlink_msg_drone_global_position drone_global_positon) {
            droneLocationLat = drone_global_positon.lat * 1e-7;
            droneLocationLon = drone_global_positon.lon * 1e-7;
            LatLng pos = new LatLng(droneLocationLat, droneLocationLon);

            final MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(pos);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

            if(droneMarker != null) {
                droneMarker.remove();
            }
            droneMarker = aMap.addMarker(markerOptions);

            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, 18);
            aMap.moveCamera(cu);
        }

        @Override
        public void onDroneBatteryUpdate(dlink_msg_drone_battery_status battery_status) {
            TextView tv = (TextView)findViewById(R.id.text_drone_battery);
            tv.setText("Drone Battery: " + battery_status.percent + "%");
        }

        @Override
        public void onDroneAttitudeUpdate(dlink_msg_drone_attitude drone_attitude) {
            int roll = (int)(drone_attitude.roll * 180 / 3.1415926);
            int pitch = (int)(drone_attitude.pitch * 180 / 3.1415926);
            int yaw = (int)(drone_attitude.yaw * 180 / 3.1415926);
            TextView tv = (TextView)findViewById(R.id.text_drone_attitude);
            tv.setText("Attitude: " + "R" + roll + " P" + pitch + " Y" + yaw);
        }

        @Override
        public void onRCBatteryUpdate(dlink_msg_rc_battery_status rc_battery_status) {
            TextView tvRCBattery = (TextView)findViewById(R.id.text_rc_battery);
            tvRCBattery.setText("RC Battery: " + rc_battery_status.percent + "%");
        }

        @Override
        public void onGPSUpdate(dlink_msg_gps_status gps_status) {
            TextView tv = (TextView)findViewById(R.id.text_gps);
            tv.setText("Satellites: " + gps_status.satellites_visible);
        }
    };

    MQTTService.VideoFeeder videoFeeder = new MQTTService.VideoFeeder() {
        @Override
        public void onDataReceive(byte[] data) {
            //Log.i(TAG, "Video: " + data.length);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);

        initUI();

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        if(aMap == null) {
            aMap = mMapView.getMap();
            aMap.setMapType(2);
            aMap.setOnMapClickListener(this);
            Log.i(TAG, "MAP TYPE: " + aMap.getMapType());
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shenzhen, 18.0f));

        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        if(null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] bytes, int i) {
                if(null != djiCodecManager) {
                    djiCodecManager.sendDataToDecoder(bytes, i);
                }
            }
        };
    }

    private void initUI() {
        btn_add = (Button)findViewById(R.id.btn_add);
        btn_clear = (Button)findViewById(R.id.btn_clear);
        btn_upload = (Button)findViewById(R.id.btn_upload);
        btn_takeoff = (Button)findViewById(R.id.btn_takeoff);
        btn_video = (Button)findViewById(R.id.btn_video);

        btn_add.setAlpha(0.8f);
        btn_clear.setAlpha(0.8f);
        btn_upload.setAlpha(0.8f);
        btn_takeoff.setAlpha(0.8f);
        btn_video.setAlpha(0.8f);

        btn_add.setOnClickListener(this);
        btn_clear.setOnClickListener(this);
        btn_upload.setOnClickListener(this);
        btn_takeoff.setOnClickListener(this);
        btn_video.setOnClickListener(this);

        DLinkBridge.getInstance().setdLinkListener(dLinkListener);
        MQTTService.setVideoFeeder(videoFeeder);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        initPreviewer();
        onProductChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if(djiCodecManager == null) {
            djiCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if(djiCodecManager != null) {
            djiCodecManager.cleanSurface();
            djiCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void initPreviewer() {
        BaseProduct product = DJISDKBridge.getProductInstance();

        if(product==null || !product.isConnected()) {

        } else {
            if(null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if(!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = DJISDKBridge.getCameraInstance();
        if(null != camera) {
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    protected void onProductChange() {
        initPreviewer();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_add:
                if(isAdd == false) {
                    isAdd = true;
                    btn_add.setText("EXIT");
                    MissionPlanner.getInstance().clearWaypoints();
                    aMap.clear();
                } else {
                    isAdd = false;
                    btn_add.setText("ADD");
                }
                break;
            case R.id.btn_upload:
                if(MissionPlanner.getInstance().getWaypointCount() > 0) {
                    missionUpload();
                }
                break;
            case R.id.btn_clear:
                MissionPlanner.getInstance().clearWaypoints();
                aMap.clear();
                break;
            case R.id.btn_video:
                if(showVideo == false) {
                    showVideo = true;
                    findViewById(R.id.video_previewer_surface).setVisibility(View.VISIBLE);
                    btn_video.setText("HIDE");
                } else {
                    showVideo = false;
                    findViewById(R.id.video_previewer_surface).setVisibility(View.GONE);
                    btn_video.setText("VIDEO");
                }
                break;
            case R.id.btn_takeoff:
                MissionPlanner.getInstance().startMission();
                break;
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        if(isAdd == true) {
            markWaypoint(point);
            MissionPlanner.getInstance().addWaypoint(point);
            //aMap.addPolyline(new PolylineOptions().addAll(waypointList).width(3.0f).color(Color.argb(255,0,255,0)));
            if(MissionPlanner.getInstance().getWaypointCount() > 1) {
                int count = MissionPlanner.getInstance().getWaypointCount();
                aMap.addPolyline(new PolylineOptions().add(MissionPlanner.getInstance().getWaypoint(count - 2)).add(MissionPlanner.getInstance().getWaypoint(count -1)).width(3.0f).color(Color.argb(255,0,255,0)));
            }
        }
    }

    private void markWaypoint(LatLng point) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        Marker marker = aMap.addMarker(markerOptions);
        mMarker.put(mMarker.size(), marker);
    }

    private void missionUpload() {
        MissionPlanner.getInstance().startMissionUpload(new MissionPlanner.Callback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Mission uploaded");
                Toast.makeText(FlightActivity.this, "Mission upload success", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
