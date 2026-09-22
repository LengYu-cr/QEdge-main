package com.tencent.qqnt.kernel.nativeinterface;

public final class VideoPlayUrlInfo {
    public String httpsDomain;
    public boolean isHttps;
    public String url;

    public VideoPlayUrlInfo() {
        this.url = "";
        this.httpsDomain = "";
    }

    public String getHttpsDomain() {
        return this.httpsDomain;
    }

    public boolean getIsHttps() {
        return this.isHttps;
    }

    public String getUrl() {
        return this.url;
    }

    public VideoPlayUrlInfo(String str, boolean z, String str2) {
        this.url = str;
        this.isHttps = z;
        this.httpsDomain = str2;
    }
}
