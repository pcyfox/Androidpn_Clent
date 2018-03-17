package org.androidpn.client.iq;

import org.jivesoftware.smack.packet.IQ;

public class DeliverConfirmIQ extends IQ {

    private DeliverConfirmIQ() {
    }

    public enum Singleton {
        INSTANCE;
        private DeliverConfirmIQ iq = null;

        Singleton() {
            iq = new DeliverConfirmIQ();
        }

        public DeliverConfirmIQ get() {
            return iq;
        }
    }


    private String uuid;

    @Override
    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append("deliverconfirm").append(" xmlns=\"").append(
                "androidpn:iq:deliverconfirm").append("\">");
        if (uuid != null) {
            buf.append("<uuid>").append(uuid).append("</uuid>");
        }
        buf.append("</").append("deliverconfirm").append("> ");
        return buf.toString();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
