package com.example.movinghacker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;

/**
 * OCR配置管理器
 * 负责腾讯云OCR API密钥的保存、加载和加密
 */
public class OcrConfigManager {
    private static final String TAG = "OcrConfigManager";
    private static final String PREFS_NAME = "ocr_config";
    private static final String KEY_SECRET_ID = "secret_id";
    private static final String KEY_SECRET_KEY = "secret_key";
    
    // 简单的XOR加密密钥（实际应用中应使用Android Keystore）
    private static final String ENCRYPTION_KEY = "MovingHacker2024OCR";

    private static OcrConfigManager instance;
    private SharedPreferences prefs;

    private OcrConfigManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized OcrConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new OcrConfigManager(context);
        }
        return instance;
    }

    /**
     * 保存OCR配置
     */
    public void saveConfig(String secretId, String secretKey) {
        if (secretId == null || secretKey == null) {
            Log.w(TAG, "Attempted to save null config");
            return;
        }

        try {
            // 加密密钥
            String encryptedId = encrypt(secretId);
            String encryptedKey = encrypt(secretKey);

            prefs.edit()
                    .putString(KEY_SECRET_ID, encryptedId)
                    .putString(KEY_SECRET_KEY, encryptedKey)
                    .apply();

            Log.d(TAG, "OCR config saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving OCR config", e);
        }
    }

    /**
     * 获取Secret ID
     */
    public String getSecretId() {
        try {
            String encryptedId = prefs.getString(KEY_SECRET_ID, "");
            return decrypt(encryptedId);
        } catch (Exception e) {
            Log.e(TAG, "Error loading Secret ID", e);
            return "";
        }
    }

    /**
     * 获取Secret Key
     */
    public String getSecretKey() {
        try {
            String encryptedKey = prefs.getString(KEY_SECRET_KEY, "");
            return decrypt(encryptedKey);
        } catch (Exception e) {
            Log.e(TAG, "Error loading Secret Key", e);
            return "";
        }
    }

    /**
     * 检查是否已配置
     */
    public boolean isConfigured() {
        String secretId = getSecretId();
        String secretKey = getSecretKey();
        return secretId != null && !secretId.trim().isEmpty() 
                && secretKey != null && !secretKey.trim().isEmpty();
    }

    /**
     * 清除配置
     */
    public void clearConfig() {
        prefs.edit().clear().apply();
        Log.d(TAG, "OCR config cleared");
    }

    /**
     * 简单的XOR加密
     */
    private String encrypt(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        try {
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = new byte[textBytes.length];

            for (int i = 0; i < textBytes.length; i++) {
                encrypted[i] = (byte) (textBytes[i] ^ keyBytes[i % keyBytes.length]);
            }

            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting", e);
            return text;
        }
    }

    /**
     * 简单的XOR解密
     */
    private String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }

        try {
            byte[] encrypted = Base64.decode(encryptedText, Base64.NO_WRAP);
            byte[] keyBytes = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] decrypted = new byte[encrypted.length];

            for (int i = 0; i < encrypted.length; i++) {
                decrypted[i] = (byte) (encrypted[i] ^ keyBytes[i % keyBytes.length]);
            }

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting", e);
            return "";
        }
    }
}
