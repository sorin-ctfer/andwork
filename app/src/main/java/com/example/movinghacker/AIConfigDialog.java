package com.example.movinghacker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.movinghacker.ai.AIConfig;
import com.example.movinghacker.ai.AIConfigManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI配置对话框
 * 支持OpenAI、Gemini和自定义API
 */
public class AIConfigDialog extends Dialog {

    // Gemini模型列表
    private static final List<String> GEMINI_MODELS = Arrays.asList(
            // --- 最新稳定版 (推荐) ---
            "gemini-2.5-pro",          // 最强推理能力，支持 Deep Think
            "gemini-2.5-flash",        // 平衡速度与智能，默认推荐
            "gemini-2.5-flash-lite",   // 极致速度，低成本

            // --- Gemini 3.0 预览版 (尝鲜) ---
            "gemini-3-pro-preview",    // 下一代最强模型预览
            "gemini-3-flash-preview",  // 下一代高速模型预览

            // --- Gemini 2.0 系列 (上一代稳定版) ---
            "gemini-2.0-flash",        // 2.0 稳定版
            "gemini-2.0-flash-thinking-exp", // 思考模型实验版

            // --- 实验性/指向最新 ---
            "gemini-exp"               // 始终指向最新的实验性模型
    );

    // OpenAI模型列表
    private static final List<String> OPENAI_MODELS = Arrays.asList(
            "gpt-4",
            "gpt-4-turbo",
            "gpt-3.5-turbo"
    );

    private static final List<String> SILICONFLOW_MODELS = Arrays.asList(
            "deepseek-ai/DeepSeek-R1-0528-Qwen3-8B",
            "deepseek-ai/DeepSeek-R1-Distill-Qwen-14B",
            "deepseek-ai/DeepSeek-R1-Distill-Qwen-32B",
            "deepseek-ai/DeepSeek-R1-Distill-Qwen-7B"
    );

    private static final List<String> ZENMUX_MODELS = Arrays.asList(
            "kuaishou/kat-coder-pro-v1-free",
            "google/gemini-3-flash-preview-free",
            "google/gemini-2.5-flash-preview-free",
            "anthropic/claude-3.5-sonnet",
            "openai/gpt-4o",
            "openai/gpt-4o-mini"
    );

    private RadioGroup providerRadioGroup;
    private RadioButton providerOpenAI;
    private RadioButton providerGemini;
    private RadioButton providerSiliconFlow;
    private RadioButton providerZenmux;
    private RadioButton providerCustom;
    private TextInputEditText apiUrlInput;
    private TextInputEditText apiKeyInput;
    private AutoCompleteTextView modelInput;
    private SwitchMaterial hackingModeSwitch;
    private Button cancelButton;
    private Button saveButton;

    private AIConfigManager configManager;
    private OnConfigSavedListener listener;
    private ArrayAdapter<String> modelSuggestionsAdapter;

    public interface OnConfigSavedListener {
        void onConfigSaved(AIConfig config);
    }

    public AIConfigDialog(@NonNull Context context) {
        super(context);
        configManager = AIConfigManager.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_ai_config);

        initViews();
        loadCurrentConfig();
        setupListeners();
    }

    private void initViews() {
        providerRadioGroup = findViewById(R.id.provider_radio_group);
        providerOpenAI = findViewById(R.id.provider_openai);
        providerGemini = findViewById(R.id.provider_gemini);
        providerSiliconFlow = findViewById(R.id.provider_siliconflow);
        providerZenmux = findViewById(R.id.provider_zenmux);
        providerCustom = findViewById(R.id.provider_custom);
        apiUrlInput = findViewById(R.id.api_url_input);
        apiKeyInput = findViewById(R.id.api_key_input);
        modelInput = findViewById(R.id.model_input);
        hackingModeSwitch = findViewById(R.id.hacking_mode_switch);
        cancelButton = findViewById(R.id.cancel_button);
        saveButton = findViewById(R.id.save_button);

        // 初始化AutoCompleteTextView adapter
        modelSuggestionsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        modelInput.setAdapter(modelSuggestionsAdapter);
        modelInput.setThreshold(1);
    }

    private void loadCurrentConfig() {
        AIConfig config = configManager.getConfig();
        if (config != null) {
            // 设置provider
            String provider = config.getProvider();
            if ("openai".equals(provider)) {
                providerOpenAI.setChecked(true);
                updateModelSuggestions(OPENAI_MODELS);
            } else if ("gemini".equals(provider)) {
                providerGemini.setChecked(true);
                updateModelSuggestions(GEMINI_MODELS);
            } else if ("siliconflow".equals(provider)) {
                providerSiliconFlow.setChecked(true);
                updateModelSuggestions(SILICONFLOW_MODELS);
            } else if ("zenmux".equals(provider)) {
                providerZenmux.setChecked(true);
                updateModelSuggestions(ZENMUX_MODELS);
            } else {
                providerCustom.setChecked(true);
                updateModelSuggestions(Arrays.asList(config.getModel()));
            }

            apiUrlInput.setText(config.getBaseUrl());
            apiKeyInput.setText(config.getApiKey());
            modelInput.setText(config.getModel());
            hackingModeSwitch.setChecked(config.isHackingMode());
        } else {
            // 设置默认值
            providerGemini.setChecked(true);
            apiUrlInput.setText("https://generativelanguage.googleapis.com/v1beta");
            updateModelSuggestions(GEMINI_MODELS);
            modelInput.setText("gemini-2.5-flash");
            hackingModeSwitch.setChecked(true);
        }

    }

    private void setupListeners() {
        // Provider切换监听
        providerRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.provider_openai) {
                apiUrlInput.setText("https://api.openai.com/v1");
                updateModelSuggestions(OPENAI_MODELS);
                modelInput.setText("gpt-4");
            } else if (checkedId == R.id.provider_gemini) {
                apiUrlInput.setText("https://generativelanguage.googleapis.com/v1beta");
                updateModelSuggestions(GEMINI_MODELS);
                modelInput.setText("gemini-2.5-flash");
            } else if (checkedId == R.id.provider_siliconflow) {
                apiUrlInput.setText("https://api.siliconflow.cn/v1");
                updateModelSuggestions(SILICONFLOW_MODELS);
                modelInput.setText("deepseek-ai/DeepSeek-R1-0528-Qwen3-8B");
            } else if (checkedId == R.id.provider_zenmux) {
                apiUrlInput.setText("https://zenmux.ai/api/v1");
                updateModelSuggestions(ZENMUX_MODELS);
                modelInput.setText("kuaishou/kat-coder-pro-v1-free");
            } else {
                // Custom - 保持当前模型列表
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
        saveButton.setOnClickListener(v -> saveConfig());
    }

    /**
     * 更新模型建议列表
     */
    private void updateModelSuggestions(List<String> models) {
        modelSuggestionsAdapter.clear();
        modelSuggestionsAdapter.addAll(models);
        modelSuggestionsAdapter.notifyDataSetChanged();
    }

    private void saveConfig() {
        String apiUrl = apiUrlInput.getText().toString().trim();
        String apiKey = apiKeyInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();

        // 验证输入
        if (apiUrl.isEmpty()) {
            Toast.makeText(getContext(), "请输入API地址", Toast.LENGTH_SHORT).show();
            return;
        }

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "请输入API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        if (model == null || model.isEmpty()) {
            Toast.makeText(getContext(), "请输入模型名称", Toast.LENGTH_SHORT).show();
            return;
        }

        // 确定provider
        String provider = "custom";
        if (providerOpenAI.isChecked()) {
            provider = "openai";
        } else if (providerGemini.isChecked()) {
            provider = "gemini";
        } else if (providerSiliconFlow != null && providerSiliconFlow.isChecked()) {
            provider = "siliconflow";
        } else if (providerZenmux != null && providerZenmux.isChecked()) {
            provider = "zenmux";
        }

        // 创建配置对象
        AIConfig config = new AIConfig(provider, apiKey, apiUrl, model);
        config.setHackingMode(hackingModeSwitch.isChecked());

        // 保存配置
        configManager.saveConfig(config);

        // 通知监听器
        if (listener != null) {
            listener.onConfigSaved(config);
        }

        Toast.makeText(getContext(), "配置已保存", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    public void setOnConfigSavedListener(OnConfigSavedListener listener) {
        this.listener = listener;
    }
}
