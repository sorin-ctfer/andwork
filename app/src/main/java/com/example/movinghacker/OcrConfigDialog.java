package com.example.movinghacker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;

/**
 * OCR配置对话框
 * 用于配置腾讯云OCR API密钥
 */
public class OcrConfigDialog extends Dialog {
    
    private TextInputEditText secretIdInput;
    private TextInputEditText secretKeyInput;
    private Button cancelButton;
    private Button saveButton;
    
    private OcrConfigManager configManager;
    private OnConfigSavedListener listener;

    public interface OnConfigSavedListener {
        void onConfigSaved();
    }

    public OcrConfigDialog(@NonNull Context context) {
        super(context);
        configManager = OcrConfigManager.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_ocr_config);
        
        initViews();
        loadCurrentConfig();
        setupListeners();
    }

    private void initViews() {
        secretIdInput = findViewById(R.id.secret_id_input);
        secretKeyInput = findViewById(R.id.secret_key_input);
        cancelButton = findViewById(R.id.cancel_button);
        saveButton = findViewById(R.id.save_button);
    }

    private void loadCurrentConfig() {
        String secretId = configManager.getSecretId();
        String secretKey = configManager.getSecretKey();
        
        if (secretId != null && !secretId.isEmpty()) {
            secretIdInput.setText(secretId);
        }
        if (secretKey != null && !secretKey.isEmpty()) {
            secretKeyInput.setText(secretKey);
        }
    }

    private void setupListeners() {
        cancelButton.setOnClickListener(v -> dismiss());
        saveButton.setOnClickListener(v -> saveConfig());
    }

    private void saveConfig() {
        String secretId = secretIdInput.getText().toString().trim();
        String secretKey = secretKeyInput.getText().toString().trim();
        
        // 验证输入
        if (secretId.isEmpty()) {
            Toast.makeText(getContext(), "请输入Secret ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (secretKey.isEmpty()) {
            Toast.makeText(getContext(), "请输入Secret Key", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 保存配置
        configManager.saveConfig(secretId, secretKey);
        
        // 通知监听器
        if (listener != null) {
            listener.onConfigSaved();
        }
        
        Toast.makeText(getContext(), "OCR配置已保存", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    public void setOnConfigSavedListener(OnConfigSavedListener listener) {
        this.listener = listener;
    }
}
