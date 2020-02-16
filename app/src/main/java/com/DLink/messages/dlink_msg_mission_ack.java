/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java dlink generator tool. It should not be modified by hand.
 */

// MESSAGE MISSION_ACK PACKING
package com.DLink.messages;
import com.DLink.DLinkPacket;
import com.DLink.DLinkMessage;
import com.DLink.DLinkPayload;
        
/**
* 
        Acknowledgment message during waypoint handling.
      
*/
public class dlink_msg_mission_ack extends DLinkMessage{

    public static final int DLINK_MSG_ID_MISSION_ACK = 103;
    public static final int DLINK_MSG_LENGTH = 1;
    private static final long serialVersionUID = DLINK_MSG_ID_MISSION_ACK;


      
    /**
    * Result
    */
    public short result;
    

    /**
    * Generates the payload for a dlink message for a message of this type
    * @return
    */
    public DLinkPacket pack(){
        DLinkPacket packet = new DLinkPacket(DLINK_MSG_LENGTH);
        packet.sysid = 0;
        packet.compid = 0;
        packet.msgid = DLINK_MSG_ID_MISSION_ACK;
              
        packet.payload.putUnsignedByte(result);
        
        return packet;
    }

    /**
    * Decode a mission_ack message into this class fields
    *
    * @param payload The message to decode
    */
    public void unpack(DLinkPayload payload) {
        payload.resetIndex();
              
        this.result = payload.getUnsignedByte();
        
    }

    /**
    * Constructor for a new message, just initializes the msgid
    */
    public dlink_msg_mission_ack(){
        msgid = DLINK_MSG_ID_MISSION_ACK;
    }

    /**
    * Constructor for a new message, initializes the message with the payload
    * from a dlink packet
    *
    */
    public dlink_msg_mission_ack(DLinkPacket dLinkPacket){
        this.sysid = dLinkPacket.sysid;
        this.compid = dLinkPacket.compid;
        this.msgid = DLINK_MSG_ID_MISSION_ACK;
        unpack(dLinkPacket.payload);        
    }

      
    /**
    * Returns a string with the MSG name and data
    */
    public String toString(){
        return "DLINK_MSG_ID_MISSION_ACK - sysid:"+sysid+" compid:"+compid+" result:"+result+"";
    }
}
        