/* 
 *
 * 
 *
 */

package com.DLink;

import com.DLink.DLinkPacket;

/**
 * Storage for DLink Packet and Error statistics
 * 
 */
public class DLinkStats /* implements Serializable */{

    public int receivedPacketCount; // total recieved packet count for all sources

    public int crcErrorCount;

    public int lostPacketCount; // total lost packet count for all sources

    public boolean ignoreRadioPackets;

    // stats are nil for a system id until a packet has been received from a system
    public SystemStat[] systemStats; // stats for each system that is known

    public DLinkStats() {
        this(false);
    }

    public DLinkStats(boolean ignoreRadioPackets) {
        this.ignoreRadioPackets = ignoreRadioPackets;
        resetStats();
    }

    /**
     * Check the new received packet to see if has lost someone between this and
     * the last packet
     * 
     * @param packet
     *            Packet that should be checked
     */
    public void newPacket(DLinkPacket packet) {

        if (systemStats[packet.sysid] == null) {
            // only allocate stats for systems that exsist on the network
            systemStats[packet.sysid] = new SystemStat();
        }
        lostPacketCount += systemStats[packet.sysid].newPacket(packet);
        receivedPacketCount++;
    }

    /**
     * Called when a CRC error happens on the parser
     */
    public void crcError() {
        crcErrorCount++;
    }

    public void resetStats() {
        crcErrorCount = 0;
        lostPacketCount = 0;
        receivedPacketCount = 0;
        systemStats = new SystemStat[256];
    }

    // stat structure for every system id
    public static class SystemStat {
        public int lostPacketCount; // the lost count for this source
        public int receivedPacketCount;

        // stats are nil for a component id until a packet has been received from a system
        public ComponentStat[] componentStats; // stats for each component that is known

        public SystemStat() {
            resetStats();
        }

        public int newPacket(DLinkPacket packet) {
            int newLostPackets = 0;
            if (componentStats[packet.compid] == null) {
                // only allocate stats for systems that exsist on the network
                componentStats[packet.compid] = new ComponentStat();
            }
            newLostPackets = componentStats[packet.compid].newPacket(packet);
            lostPacketCount += newLostPackets;
            receivedPacketCount++;
            return newLostPackets;
        }

        public void resetStats() {
            lostPacketCount = 0;
            receivedPacketCount = 0;
            componentStats = new ComponentStat[256];
        }
    }

    // stat structure for every system id
    public static class ComponentStat {
        public int lastPacketSeq;
        public int lostPacketCount; // the lost count for this source
        public int receivedPacketCount;

        public ComponentStat() {
            resetStats();
        }

        public int newPacket(DLinkPacket packet) {
            int newLostPackets = 0;
            if (hasLostPackets(packet)) {
                newLostPackets = updateLostPacketCount(packet);
            }
            lastPacketSeq = packet.seq;
            advanceLastPacketSequence(packet.seq);
            receivedPacketCount++;
            return newLostPackets;
        }

        public void resetStats() {
            lastPacketSeq = -1;
            lostPacketCount = 0;
            receivedPacketCount = 0;
        }

        private int updateLostPacketCount(DLinkPacket packet) {
            int lostPackets;
            if (packet.seq - lastPacketSeq < 0) {
                lostPackets = (packet.seq - lastPacketSeq) + 255;
            } else {
                lostPackets = (packet.seq - lastPacketSeq);
            }
            lostPacketCount += lostPackets;
            return lostPackets;
        }

        private void advanceLastPacketSequence(int lastSeq) {
            // wrap from 255 to 0 if necessary
            lastPacketSeq = (lastSeq + 1) & 0xFF;
        }

        private boolean hasLostPackets(DLinkPacket packet) {
            return lastPacketSeq >=  0 && packet.seq != lastPacketSeq;
        }
    }

}
