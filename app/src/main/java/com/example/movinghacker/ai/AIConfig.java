package com.example.movinghacker.ai;

/**
 * AI配置数据模型
 */
public class AIConfig {
    private String provider;    // "openai", "gemini", "siliconflow", "custom"
    private String apiKey;
    private String baseUrl;
    private String model;
    private boolean hackingMode; // true=Hacking模式, false=Chat模式

    public AIConfig() {
        this.provider = "openai";
        this.baseUrl = "https://api.openai.com/v1";
        this.model = "gpt-4";
        this.hackingMode = true;
    }

    public AIConfig(String provider, String apiKey, String baseUrl, String model) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.hackingMode = true;
        
        // 根据provider设置默认值
        if ("gemini".equals(provider) && (baseUrl == null || baseUrl.isEmpty())) {
            this.baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        }
        if ("gemini".equals(provider) && (model == null || model.isEmpty())) {
            this.model = "gemini-pro";
        }
        if ("siliconflow".equals(provider) && (baseUrl == null || baseUrl.isEmpty())) {
            this.baseUrl = "https://api.siliconflow.cn/v1";
        }
        if ("siliconflow".equals(provider) && (model == null || model.isEmpty())) {
            this.model = "deepseek-ai/DeepSeek-R1-0528-Qwen3-8B";
        }
        if ("zenmux".equals(provider) && (baseUrl == null || baseUrl.isEmpty())) {
            this.baseUrl = "https://zenmux.ai/api/v1";
        }
        if ("zenmux".equals(provider) && (model == null || model.isEmpty())) {
            this.model = "kuaishou/kat-coder-pro-v1-free";
        }
    }

    public boolean isValid() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String getDisplayApiKey() {
        if (apiKey == null || apiKey.length() < 8) {
            return "未配置";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    public String getApiUrl() {
        if ("gemini".equals(provider)) {
            return baseUrl + "/models/" + model + ":generateContent";
        } else {
            return baseUrl + "/chat/completions";
        }
    }

    // Getters and Setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isHackingMode() {
        return hackingMode;
    }

    public void setHackingMode(boolean hackingMode) {
        this.hackingMode = hackingMode;
    }
}
