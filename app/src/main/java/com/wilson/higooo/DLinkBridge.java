package com.wilson.higooo;

import android.util.Log;

import com.DLink.DLinkPacket;
import com.DLink.enums.HEISHA_PRODUCT_MODEL;
import com.DLink.messages.dlink_msg_drone_attitude;
import com.DLink.messages.dlink_msg_drone_battery_status;
import com.DLink.messages.dlink_msg_drone_global_position;
import com.DLink.messages.dlink_msg_drone_status;
import com.DLink.messages.dlink_msg_gps_status;
import com.DLink.messages.dlink_msg_heartbeat;
import com.DLink.messages.dlink_msg_rc_battery_status;

import static com.DLink.messages.dlink_msg_drone_attitude.DLINK_MSG_ID_DRONE_ATTITUDE;
import static com.DLink.messages.dlink_msg_drone_battery_status.DLINK_MSG_ID_DRONE_BATTERY_STATUS;
import static com.DLink.messages.dlink_msg_drone_global_position.DLINK_MSG_ID_DRONE_GLOBAL_POSITION;
import static com.DLink.messages.dlink_msg_drone_status.DLINK_MSG_ID_drone_status;
import static com.DLink.messages.dlink_msg_gps_status.DLINK_MSG_ID_GPS_STATUS;
import static com.DLink.messages.dlink_msg_heartbeat.DLINK_MSG_ID_HEARTBEAT;
import static com.DLink.messages.dlink_msg_rc_battery_status.DLINK_MSG_ID_RC_BATTERY_STATUS;

public class DLinkBridge {
    private static DLinkBridge _instance = null;
    public static int dLinkIndex = 0;

    private DLinkBridge() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    synchronized public static DLinkBridge getInstance() {
        if (null == _instance) {
            _instance = new DLinkBridge();
        }
        return _instance;
    }

    public void handle_message(DLinkPacket packet) {
        switch (packet.msgid) {
            case DLINK_MSG_ID_RC_BATTERY_STATUS:
                dlink_msg_rc_battery_status rc_battery_status = (dlink_msg_rc_battery_status)packet.unpack();
                if(null != dLinkListener) {
                    dLinkListener.onRCBatteryUpdate(rc_battery_status);
                }
                break;
            case DLINK_MSG_ID_DRONE_BATTERY_STATUS:
                dlink_msg_drone_battery_status drone_battery_status = (dlink_msg_drone_battery_status)packet.unpack();
                if(null != dLinkListener) {
                    dLinkListener.onDroneBatteryUpdate(drone_battery_status);
                }
                break;
            case DLINK_MSG_ID_DRONE_ATTITUDE:
                dlink_msg_drone_attitude drone_attitude = (dlink_msg_drone_attitude)packet.unpack();
                if(null != dLinkListener) {
                    dLinkListener.onDroneAttitudeUpdate(drone_attitude);
                }
                break;
            case DLINK_MSG_ID_DRONE_GLOBAL_POSITION:
                dlink_msg_drone_global_position drone_global_position = (dlink_msg_drone_global_position)packet.unpack();
                if(null != dLinkListener) {
                    dLinkListener.onDronePositionUpdate(drone_global_position);
                }
                break;
            case DLINK_MSG_ID_GPS_STATUS:
                dlink_msg_gps_status gps_status = (dlink_msg_gps_status)packet.unpack();
                if(null != dLinkListener) {
                    dLinkListener.onGPSUpdate(gps_status);
                }
                break;
            case DLINK_MSG_ID_drone_status:
                dlink_msg_drone_status drone_status = (dlink_msg_drone_status)packet.unpack();
                if(null != dLinkListener) {
                    dLinkListener.onDroneStatusUpdate(drone_status);
                }
                break;
        }
    }

    public String getModelName(short model_id) {
        switch (model_id) {
            case HEISHA_PRODUCT_MODEL.UNKNOWN:
                return "UNKNOWN";
            case HEISHA_PRODUCT_MODEL.C300M2:
                return "C300 M2";
        }

        return "UNKNOWN";
    }

    public void sendDlinkPacket(DLinkPacket packet) {
        packet.seq = dLinkIndex++;
        MQTTService.publishTopic(MQTTService.TOPIC_PUB, packet.encodePacket());
    }

    private DLinkListener dLinkListener = null;

    public void setdLinkListener(DLinkListener dLinkListener) {
        this.dLinkListener = dLinkListener;
    }

    public interface DLinkListener {
        void onDroneStatusUpdate(dlink_msg_drone_status drone_status);
        void onDronePositionUpdate(dlink_msg_drone_global_position drone_global_positon);
        void onDroneBatteryUpdate(dlink_msg_drone_battery_status battery_status);
        void onDroneAttitudeUpdate(dlink_msg_drone_attitude drone_attitude);
        void onRCBatteryUpdate(dlink_msg_rc_battery_status rc_battery_status);
        void onGPSUpdate(dlink_msg_gps_status gps_status);
    }

}
