package com.example.movinghacker;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class IdCardOcrClient {
    private static final String HOST = "ocr.tencentcloudapi.com";
    private static final String ENDPOINT = "https://" + HOST + "/";
    private static final String SERVICE = "ocr";
    private static final String TERMINATION = "tc3_request";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String ACTION = "IDCardOCR";
    private static final String VERSION = "2018-11-19";
    private static final String REGION = "ap-guangzhou";

    public static String idCardOcr(Context context, String imageBase64, String imageUrl, String cardSide) throws Exception {
        // 从配置管理器获取密钥
        OcrConfigManager configManager = OcrConfigManager.getInstance(context);
        String secretId = configManager.getSecretId();
        String secretKey = configManager.getSecretKey();
        
        if (secretId == null || secretId.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("未配置腾讯云OCR密钥。请先在设置中配置Secret ID和Secret Key。");
        }

        JSONObject payload = new JSONObject();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            payload.put("ImageUrl", imageUrl);
        } else if (imageBase64 != null && !imageBase64.isEmpty()) {
            payload.put("ImageBase64", imageBase64);
        } else {
            throw new IllegalArgumentException("缺少图片输入（ImageUrl 或 ImageBase64）。");
        }
        if (cardSide != null && !cardSide.isEmpty()) {
            payload.put("CardSide", cardSide);
        }
        JSONObject config = new JSONObject();
        config.put("CropIdCard", true);
        config.put("Quality", true);
        payload.put("Config", config.toString());

        String payloadStr = payload.toString();
        long timestamp = System.currentTimeMillis() / 1000;
        String date = formatUtcDate(timestamp * 1000);

        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:" + HOST + "\n";
        String signedHeaders = "content-type;host";
        String hashedRequestPayload = sha256Hex(payloadStr);
        String canonicalRequest = httpRequestMethod + "\n"
                + canonicalUri + "\n"
                + canonicalQueryString + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + hashedRequestPayload;

        String credentialScope = date + "/" + SERVICE + "/" + TERMINATION;
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = ALGORITHM + "\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + hashedCanonicalRequest;

        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, TERMINATION);
        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

        String authorization = ALGORITHM + " "
                + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Host", HOST);
        connection.setRequestProperty("X-TC-Action", ACTION);
        connection.setRequestProperty("X-TC-Version", VERSION);
        connection.setRequestProperty("X-TC-Region", REGION);
        connection.setRequestProperty("X-TC-Timestamp", String.valueOf(timestamp));
        connection.setRequestProperty("Authorization", authorization);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payloadStr.getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("请求失败 HTTP " + code + ": " + response);
        }
        return response;
    }

    private static String formatUtcDate(long timeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timeMillis));
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(bytes);
    }

    private static byte[] hmacSha256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(byte[] key, byte[] msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(msg);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
