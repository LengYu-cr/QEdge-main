package mqq.app;

import java.util.HashMap;

public class Packet {
    public boolean autoResend;
    public boolean quickSendEnable;
    public int quickSendStrategy;
    private boolean noResponse;
    private byte[] sendData;
    private String ssoCommand;
    private long timeout = 30000;
    private boolean isSupportRetry;
    private HashMap<String, Object> attributes = new HashMap<>();

    public Packet(String str) {
    }

    public void addRequestPacket(String str, Object jceStruct) {
    }

    public HashMap<String, Object> getAttributes() {
        return this.attributes;
    }

    public void putSendData(byte[] bArr) {
        this.sendData = bArr;
    }

    public void setAttributes(HashMap<String, Object> hashMap) {
        this.attributes = hashMap;
    }

    public void setIsSupportRetry(boolean z) {
        this.isSupportRetry = z;
    }

    public void setNoResponse() {
        this.noResponse = true;
    }

    public void setQuickSend(boolean z, int i) {
        this.quickSendEnable = z;
        this.quickSendStrategy = i;
    }

    public void setSSOCommand(String str) {
        this.ssoCommand = str;
    }

    public void setTimeout(long j) {
        this.timeout = j;
    }

    public Object toMsg() {
        return null;
    }
}
