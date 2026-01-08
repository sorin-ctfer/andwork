package com.example.movinghacker;

import android.content.Context;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRequestExecutor {
    private final OkHttpClient client;
    private Context context;

    public HttpRequestExecutor() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // 显式启用HTTP/2和HTTP/1.1支持
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectionSpecs(Arrays.asList(
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.COMPATIBLE_TLS,
                    ConnectionSpec.CLEARTEXT
                ))
                .build();
    }
    
    public void setContext(Context context) {
        this.context = context;
    }

    public HttpResponse execute(HttpRequest request) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(request.getUrl());

        // 添加请求头
        if (request.getHeaders() != null) {
            for (RequestHeader header : request.getHeaders()) {
                builder.addHeader(header.getKey(), header.getValue());
            }
        }

        // 构建请求体
        RequestBody requestBody = buildRequestBody(request);

        // 设置HTTP方法
        switch (request.getMethod().toUpperCase()) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(requestBody != null ? requestBody :
                        RequestBody.create("", null));
                break;
            case "PUT":
                builder.put(requestBody != null ? requestBody :
                        RequestBody.create("", null));
                break;
            case "DELETE":
                if (requestBody != null) {
                    builder.delete(requestBody);
                } else {
                    builder.delete();
                }
                break;
            case "HEAD":
                builder.head();
                break;
            case "PATCH":
                builder.patch(requestBody != null ? requestBody :
                        RequestBody.create("", null));
                break;
            case "OPTIONS":
                builder.method("OPTIONS", null);
                break;
            default:
                throw new IllegalArgumentException("不支持的HTTP方法: " + request.getMethod());
        }

        long startTime = System.currentTimeMillis();

        try (Response response = client.newCall(builder.build()).execute()) {
            long duration = System.currentTimeMillis() - startTime;

            String responseBody = response.body() != null ?
                    response.body().string() : "";

            Map<String, String> responseHeaders = new HashMap<>();
            for (String name : response.headers().names()) {
                responseHeaders.put(name, response.header(name));
            }
            
            // 获取协议版本
            String protocol = response.protocol().toString();

            return new HttpResponse(
                    response.code(),
                    response.message(),
                    responseHeaders,
                    responseBody,
                    duration,
                    responseBody.length(),
                    protocol
            );
        }
    }

    private RequestBody buildRequestBody(HttpRequest request) {
        if (request.getFileUri() != null) {
            return buildMultipartBody(request);
        } else if (request.getBody() != null && !request.getBody().isEmpty()) {
            MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
            
            String body = request.getBody().trim();
            if (body.startsWith("{") || body.startsWith("[")) {
                mediaType = MediaType.parse("application/json; charset=utf-8");
            }
            
            return RequestBody.create(request.getBody(), mediaType);
        }
        
        return null;
    }
    
    private RequestBody buildMultipartBody(HttpRequest request) {
        try {
            okhttp3.MultipartBody.Builder builder = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM);
            
            android.net.Uri fileUri = request.getFileUri();
            String fileName = getFileName(fileUri);
            String mimeType = getMimeType(fileUri);
            
            byte[] fileBytes = readFileBytes(fileUri);
            
            RequestBody fileBody = RequestBody.create(fileBytes, MediaType.parse(mimeType));
            builder.addFormDataPart("file", fileName, fileBody);
            
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                builder.addFormDataPart("data", request.getBody());
            }
            
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    private String getFileName(android.net.Uri uri) {
        if (context == null) return "upload_file";
        
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "upload_file";
    }
    
    private String getMimeType(android.net.Uri uri) {
        if (context == null) return "application/octet-stream";
        
        String type = context.getContentResolver().getType(uri);
        return type != null ? type : "application/octet-stream";
    }
    
    private byte[] readFileBytes(android.net.Uri uri) throws IOException {
        if (context == null) throw new IOException("Context not set");
        
        try (java.io.InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IOException("Cannot open file");
            
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }
}
