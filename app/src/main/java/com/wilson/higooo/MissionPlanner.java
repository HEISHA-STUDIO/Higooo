package com.wilson.higooo;

import android.util.Log;
import android.widget.Toast;

import com.DLink.DLinkPacket;
import com.DLink.enums.CMD_ID;
import com.DLink.messages.dlink_msg_command_short;
import com.DLink.messages.dlink_msg_mission_count;
import com.DLink.messages.dlink_msg_mission_item;
import com.DLink.messages.dlink_msg_mission_request;
import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import static com.DLink.messages.dlink_msg_mission_ack.DLINK_MSG_ID_MISSION_ACK;
import static com.DLink.messages.dlink_msg_mission_request.DLINK_MSG_ID_MISSION_REQUEST;

public class MissionPlanner {

    private static MissionPlanner _instance = null;

    private float altitude = 30.0f;
    private float mSpeed = 10.0f;
    private List<LatLng> waypointList = new ArrayList<>();

    private MissionPlanner() {

    }

    public static MissionPlanner getInstance() {
        if(null == _instance) {
            _instance = new MissionPlanner();
        }
        return _instance;
    }

    public void addWaypoint(LatLng point) {
        waypointList.add(point);
    }

    public void clearWaypoints() {
        waypointList.clear();
    }

    public short getWaypointCount() {
        return (short)waypointList.size();
    }

    public LatLng getWaypoint(int i) {
        return waypointList.get(i);
    }

    public void dlink_message_handle(DLinkPacket packet) {
        switch (packet.msgid) {
            case DLINK_MSG_ID_MISSION_REQUEST:
                sendWaypoint(packet);
                break;
            case DLINK_MSG_ID_MISSION_ACK:
                if(callback != null) {
                    callback.onSuccess();
                }
                break;
        }
    }

    private void sendWaypoint(DLinkPacket packet) {
        Log.i("HEISHA", "sendWaypoint");
        dlink_msg_mission_request msg = (dlink_msg_mission_request)packet.unpack();
        Log.i("HEISHA", msg.toString());
        dlink_msg_mission_item waypoint = new dlink_msg_mission_item();
        waypoint.command = 0;
        waypoint.seq = msg.seq;
        waypoint.x = (float)getWaypoint(waypoint.seq).latitude;
        waypoint.y = (float)getWaypoint(waypoint.seq).longitude;
        waypoint.z = altitude;
        Log.i("HEISHA", waypoint.toString());
        DLinkPacket item = waypoint.pack();
        DLinkBridge.getInstance().sendDlinkPacket(item);
    }

    public interface Callback {
        void onSuccess();
    }

    private Callback callback = null;

    public void startMissionUpload(Callback callback) {
        this.callback = callback;

        dlink_msg_mission_count msg = new dlink_msg_mission_count();
        msg.count = getWaypointCount();

        DLinkPacket packet = msg.pack();
        DLinkBridge.getInstance().sendDlinkPacket(packet);
    }

    public void startMission() {
        dlink_msg_command_short command = new dlink_msg_command_short();
        command.command = CMD_ID.DRONE_TAKEOFF;

        DLinkPacket packet = command.pack();
        DLinkBridge.getInstance().sendDlinkPacket(packet);
    }
}
