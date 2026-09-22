package com.tencent.mobileqq.videocodec.ffmpeg;

public interface FFmpegExecuteResponseCallback {
    void onStart();

    void onSuccess(String message);

    void onFailure(String message);

    void onProgress(String message);

    void onFinish(boolean success);
}