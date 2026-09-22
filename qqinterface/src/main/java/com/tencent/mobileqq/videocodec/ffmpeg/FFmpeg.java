package com.tencent.mobileqq.videocodec.ffmpeg;

import android.content.Context;

public class FFmpeg {
    public static FFmpeg getInstance(Context context) {
        return null;
    }

    public boolean isFFmpegCommandRunning() {
        return false;
    }

    public boolean killRunningProcesses() {
        return false;
    }

    public void execute(String[] cmd, FFmpegExecuteResponseCallback callback)
            throws FFmpegCommandAlreadyRunningException {
    }

    public void exit() {
    }
}