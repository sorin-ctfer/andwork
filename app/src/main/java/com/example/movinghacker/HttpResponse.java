package com.example.movinghacker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse implements Serializable {
    private final int statusCode;
    private final String statusMessage;
    private final Map<String, String> headers;
    private final String body;
    private final long duration;
    private final long size;
    private final String protocol;

    public HttpResponse(int statusCode, String statusMessage,
                       Map<String, String> headers, String body,
                       long duration, long size, String protocol) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body != null ? body : "";
        this.duration = duration;
        this.size = size;
        this.protocol = protocol != null ? protocol : "HTTP/1.1";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public long getDuration() {
        return duration;
    }

    public long getSize() {
        return size;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
