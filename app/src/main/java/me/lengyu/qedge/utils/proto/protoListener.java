package me.lengyu.qedge.utils.proto;

import org.json.JSONObject;

public interface protoListener {

    void onSuccess(String cmd, JSONObject json);
    void onFailure(String cmd, String error);
}