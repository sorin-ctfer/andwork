package com.example.movinghacker;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class HttpRequest {
    private final String url;
    private final String method;
    private final List<RequestHeader> headers;
    private final String body;
    private final Uri fileUri;

    public HttpRequest(String url, String method, List<RequestHeader> headers,
                      String body, Uri fileUri) {
        this.url = url;
        this.method = method;
        this.headers = headers != null ? headers : new ArrayList<>();
        this.body = body;
        this.fileUri = fileUri;
    }

    public HttpRequest(String url, String method) {
        this(url, method, new ArrayList<>(), null, null);
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public List<RequestHeader> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public Uri getFileUri() {
        return fileUri;
    }
}
