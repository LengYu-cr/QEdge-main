package me.lengyu.qedge.utils.proto;
 
import org.json.JSONObject;

public interface packetListener {
    void onResult(boolean success, JSONObject json);
}
