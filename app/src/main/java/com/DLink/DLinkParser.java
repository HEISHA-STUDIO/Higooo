/* 
 *
 * 
 * 
 */

package com.DLink;

import com.DLink.DLinkPacket;
import com.DLink.DLinkStats;

public class DLinkParser {

    /**
     * States from the parsing state machine
     */
    enum DLink_states {
        DLINK_PARSE_STATE_UNINIT, DLINK_PARSE_STATE_IDLE, DLINK_PARSE_STATE_GOT_STX, DLINK_PARSE_STATE_GOT_LENGTH, DLINK_PARSE_STATE_GOT_SEQ, DLINK_PARSE_STATE_GOT_SYSID, DLINK_PARSE_STATE_GOT_COMPID, DLINK_PARSE_STATE_GOT_MSGID, DLINK_PARSE_STATE_GOT_CRC1, DLINK_PARSE_STATE_GOT_PAYLOAD
    }

    DLink_states state = DLink_states.DLINK_PARSE_STATE_UNINIT;

    public DLinkStats stats;
    private DLinkPacket m;

    public DLinkParser() {
        this(false);
    }

    public DLinkParser(boolean ignoreRadioPacketStats) {
        stats = new DLinkStats(ignoreRadioPacketStats);
    }

    /**
     * This is a convenience function which handles the complete DLink
     * parsing. the function will parse one byte at a time and return the
     * complete packet once it could be successfully decoded. Checksum and other
     * failures will be silently ignored.
     * 
     * @param c
     *            The char to parse
     */
    public DLinkPacket dlink_parse_char(int c) {

        switch (state) {
        case DLINK_PARSE_STATE_UNINIT:
        case DLINK_PARSE_STATE_IDLE:

            if (c == DLinkPacket.DLINK_STX) {
                state = DLink_states.DLINK_PARSE_STATE_GOT_STX;
            }
            break;

        case DLINK_PARSE_STATE_GOT_STX:
            m = new DLinkPacket(c);
            state = DLink_states.DLINK_PARSE_STATE_GOT_LENGTH;
            break;

        case DLINK_PARSE_STATE_GOT_LENGTH:
            m.seq = c;
            state = DLink_states.DLINK_PARSE_STATE_GOT_SEQ;
            break;

        case DLINK_PARSE_STATE_GOT_SEQ:
            m.sysid = c;
            state = DLink_states.DLINK_PARSE_STATE_GOT_SYSID;
            break;

        case DLINK_PARSE_STATE_GOT_SYSID:
            m.compid = c;
            state = DLink_states.DLINK_PARSE_STATE_GOT_COMPID;
            break;

        case DLINK_PARSE_STATE_GOT_COMPID:
            m.msgid = c;
            if (m.len == 0) {
                state = DLink_states.DLINK_PARSE_STATE_GOT_PAYLOAD;
            } else {
                state = DLink_states.DLINK_PARSE_STATE_GOT_MSGID;
            }
            break;

        case DLINK_PARSE_STATE_GOT_MSGID:
            m.payload.add((byte) c);
            if (m.payloadIsFilled()) {
                state = DLink_states.DLINK_PARSE_STATE_GOT_PAYLOAD;
            }
            break;

        case DLINK_PARSE_STATE_GOT_PAYLOAD:
            m.generateCRC();
            // Check first checksum byte
            if (c != m.crc.getLSB()) {
                state = DLink_states.DLINK_PARSE_STATE_IDLE;
                if (c == DLinkPacket.DLINK_STX) {
                    state = DLink_states.DLINK_PARSE_STATE_GOT_STX;
                    m.crc.start_checksum();
                }
                stats.crcError();
            } else {
                state = DLink_states.DLINK_PARSE_STATE_GOT_CRC1;
            }
            break;

        case DLINK_PARSE_STATE_GOT_CRC1:
            // Check second checksum byte
            if (c != m.crc.getMSB()) {
                state = DLink_states.DLINK_PARSE_STATE_IDLE;
                if (c == DLinkPacket.DLINK_STX) {
                    state = DLink_states.DLINK_PARSE_STATE_GOT_STX;
                    m.crc.start_checksum();
                }
                stats.crcError();
            } else { // Successfully received the message
                stats.newPacket(m);
                state = DLink_states.DLINK_PARSE_STATE_IDLE;
                return m;
            }

            break;

        }
        return null;
    }
}
