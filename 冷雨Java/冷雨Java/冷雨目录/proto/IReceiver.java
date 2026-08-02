public interface IReceiver {

    void onReceive(byte[] data);

}

public interface protoListener {

    void onSuccess(String cmd, JSONObject json);
    void onFailure(String cmd, String error);
    boolean onResponse();

}