package com.example.movinghacker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.movinghacker.ai.AIConfig;
import com.example.movinghacker.ai.AIConfigManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * AI聊天Fragment
 */
public class AIChatFragment extends Fragment {
    
    private MaterialToolbar toolbar;
    private RecyclerView messageList;
    private LinearLayout emptyState;
    private ProgressBar loading;
    private EditText messageInput;
    private ImageButton sendButton;
    private SwitchMaterial hackingModeSwitch;
    private boolean isLoading;
    
    private AIChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private AIConfigManager configManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        
        configManager = AIConfigManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(AIChatViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
        
        // 检查是否已配置API
        checkConfiguration();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        messageList = view.findViewById(R.id.message_list);
        emptyState = view.findViewById(R.id.empty_state);
        loading = view.findViewById(R.id.loading);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        hackingModeSwitch = view.findViewById(R.id.hacking_mode_switch);
        
        // 设置toolbar
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        AIConfig config = configManager.getConfig();
        if (config != null && hackingModeSwitch != null) {
            hackingModeSwitch.setChecked(config.isHackingMode());
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter();
        messageList.setAdapter(messageAdapter);
        messageList.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // 自动滚动到最新消息
        messageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                messageList.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            }
        });
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> {
            if (isLoading) {
                viewModel.stopGeneration();
            } else {
                sendMessage();
            }
        });

        if (hackingModeSwitch != null) {
            hackingModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AIConfig config = configManager.getConfig();
                if (config != null) {
                    config.setHackingMode(isChecked);
                    viewModel.saveConfig(config);
                }
            });
        }
        
        // 回车发送（可选）
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void observeViewModel() {
        // 观察消息列表
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            messageAdapter.setMessages(messages);
            
            // 显示/隐藏空状态
            if (messages.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                messageList.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                messageList.setVisibility(View.VISIBLE);
            }
        });
        
        // 观察加载状态
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            this.isLoading = Boolean.TRUE.equals(isLoading);
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (Boolean.TRUE.equals(isLoading)) {
                sendButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                sendButton.setContentDescription(getString(R.string.stop));
            } else {
                sendButton.setImageResource(R.drawable.ic_send);
                sendButton.setContentDescription(getString(R.string.send));
            }
            messageInput.setEnabled(!isLoading);
        });
        
        // 观察错误
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkConfiguration() {
        if (!configManager.isConfigured()) {
            // 首次使用，显示配置对话框
            showConfigDialog();
        }
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "请输入消息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查配置
        if (!configManager.isConfigured()) {
            Toast.makeText(requireContext(), "请先配置API", Toast.LENGTH_SHORT).show();
            showConfigDialog();
            return;
        }
        
        // 发送消息
        viewModel.sendMessage(content);
        
        // 清空输入框
        messageInput.setText("");
    }

    private void showConfigDialog() {
        AIConfigDialog dialog = new AIConfigDialog(requireContext());
        dialog.setOnConfigSavedListener(config -> {
            viewModel.saveConfig(config);
            Toast.makeText(requireContext(), "配置已保存", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清空历史")
                .setMessage("确定要清空所有聊天记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    viewModel.clearHistory();
                    Toast.makeText(requireContext(), "历史记录已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.ai_chat_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_config) {
            showConfigDialog();
            return true;
        } else if (id == R.id.action_clear_history) {
            showClearHistoryDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
