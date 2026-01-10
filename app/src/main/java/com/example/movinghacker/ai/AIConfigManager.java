package com.example.movinghacker.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;

/**
 * AI配置管理器
 * 负责AI配置的保存、加载和加密
 */
public class AIConfigManager {
    private static final String TAG = "AIConfigManager";
    private static final String PREFS_NAME = "ai_config";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_MODEL = "model";
    private static final String KEY_HACKING_MODE = "hacking_mode";
    
    // 简单的XOR加密密钥（实际应用中应使用Android Keystore）
    private static final String ENCRYPTION_KEY = "MovingHacker2024";

    private static AIConfigManager instance;
    private SharedPreferences prefs;

    private AIConfigManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AIConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new AIConfigManager(context);
        }
        return instance;
    }

    /**
     * 保存AI配置
     * @param config AI配置
     */
    public void saveConfig(AIConfig config) {
        if (config == null) {
            Log.w(TAG, "Attempted to save null config");
            return;
        }

        try {
            // 加密API Key
            String encryptedKey = encrypt(config.getApiKey());

            prefs.edit()
                    .putString(KEY_PROVIDER, config.getProvider())
                    .putString(KEY_API_KEY, encryptedKey)
                    .putString(KEY_BASE_URL, config.getBaseUrl())
                    .putString(KEY_MODEL, config.getModel())
                    .putBoolean(KEY_HACKING_MODE, config.isHackingMode())
                    .apply();

            Log.d(TAG, "Config saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving config", e);
        }
    }

    /**
     * 加载AI配置
     * @return AI配置
     */
    public AIConfig loadConfig() {
        try {
            String provider = prefs.getString(KEY_PROVIDER, "gemini");  // 默认使用gemini
            String encryptedKey = prefs.getString(KEY_API_KEY, "");
            String baseUrl = prefs.getString(KEY_BASE_URL, getDefaultBaseUrl(provider));
            String model = prefs.getString(KEY_MODEL, getDefaultModel(provider));
            boolean hackingMode = prefs.getBoolean(KEY_HACKING_MODE, true);

            // 解密API Key
            String apiKey = decrypt(encryptedKey);

            AIConfig config = new AIConfig();
            config.setProvider(provider);
            config.setApiKey(apiKey);
            config.setBaseUrl(baseUrl);
            config.setModel(model);
            config.setHackingMode(hackingMode);

            return config;
        } catch (Exception e) {
            Log.e(TAG, "Error loading config", e);
            return getDefaultConfig();
        }
    }

    /**
     * 获取AI配置（loadConfig的别名）
     * @return AI配置
     */
    public AIConfig getConfig() {
        return loadConfig();
    }
    
    /**
     * 获取默认base URL
     */
    private String getDefaultBaseUrl(String provider) {
        if ("gemini".equals(provider)) {
            return "https://generativelanguage.googleapis.com/v1beta";
        } else if ("siliconflow".equals(provider)) {
            return "https://api.siliconflow.cn/v1";
        } else {
            return "https://api.openai.com/v1";
        }
    }

    /**
     * 获取默认模型
     */
    private String getDefaultModel(String provider) {
        if ("gemini".equals(provider)) {
            return "gemini-2.5-flash";
        } else if ("siliconflow".equals(provider)) {
            return "deepseek-ai/DeepSeek-R1-0528-Qwen3-8B";
        } else {
            return "gpt-4";
        }
    }

    /**
     * 获取默认配置
     */
    private AIConfig getDefaultConfig() {
        AIConfig config = new AIConfig();
        config.setProvider("gemini");
        config.setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        config.setModel("gemini-2.5-flash");
        config.setApiKey("");
        config.setHackingMode(true);
        return config;
    }

    /**
     * 检查是否已配置
     * @return 是否已配置
     */
    public boolean isConfigured() {
        AIConfig config = loadConfig();
        return config.isValid();
    }

    /**
     * 清除配置
     */
    public void clearConfig() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Config cleared");
    }

    /**
     * 简单的XOR加密
     * 注意：这只是基础实现，生产环境应使用Android Keystore
     * @param text 明文
     * @return 加密后的Base64字符串
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
     * @param encryptedText 加密的Base64字符串
     * @return 明文
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
