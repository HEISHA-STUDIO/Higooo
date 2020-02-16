/*
 *
 * 
 * 
 */

package com.DLink;

import java.io.Serializable;

import com.DLink.DLinkPacket;

public abstract class DLinkMessage implements Serializable {
    private static final long serialVersionUID = -7754622750478538539L;
    // The DLink message classes have been changed to implement Serializable, 
    // this way is possible to pass a mavlink message trought the Service-Acctivity interface
    
    /**
     *  Simply a common interface for all DLink Messages
     */
    
    public int sysid;
    public int compid;
    public int msgid;
    public abstract DLinkPacket pack();
    public abstract void unpack(DLinkPayload payload);
}
