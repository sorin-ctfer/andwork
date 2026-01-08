package com.example.movinghacker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "request_history")
public class RequestHistory {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String url;
    private String method;
    private String headers;
    private String body;
    private int statusCode;
    private String statusMessage;
    private long duration;
    private long timestamp;

    public RequestHistory(String url, String method, String headers, String body, 
                         int statusCode, String statusMessage, long duration, long timestamp) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
