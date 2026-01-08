package com.example.movinghacker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TerminalFragment extends Fragment {
    private TextView outputView;
    private EditText inputView;
    private ScrollView scrollView;
    
    // 快捷键按钮
    private MaterialButton keyTab;
    private MaterialButton keyCtrlC;
    private MaterialButton keyUp;
    private MaterialButton keyDown;
    private MaterialButton keyClear;
    
    private ShellProcess shellProcess;
    private CommandParser commandParser;
    private String homeDirectory;

    private final TerminalOutputBuffer outputBuffer = new TerminalOutputBuffer(80_000);
    private Handler mainHandler;
    private boolean outputUpdateScheduled = false;
    private final Runnable outputUpdateRunnable = this::flushOutputToUi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // 命令历史
    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_terminal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainHandler = new Handler(Looper.getMainLooper());
        initializeViews(view);
        setupHomeDirectory();
        startShell();
        setupListeners();
        
        // 延迟聚焦，确保布局完成
        inputView.postDelayed(() -> {
            inputView.requestFocus();
            inputView.setSelection(inputView.getText().length());
        }, 100);
    }

    private void initializeViews(View view) {
        outputView = view.findViewById(R.id.terminal_output);
        inputView = view.findViewById(R.id.command_input);
        scrollView = view.findViewById(R.id.terminal_scroll);
        
        keyTab = view.findViewById(R.id.key_tab);
        keyCtrlC = view.findViewById(R.id.key_ctrl_c);
        keyUp = view.findViewById(R.id.key_up);
        keyDown = view.findViewById(R.id.key_down);
        keyClear = view.findViewById(R.id.key_clear);
        
        outputView.setMovementMethod(new ScrollingMovementMethod());
        
        // 确保输入框始终可以获得焦点
        inputView.setFocusable(true);
        inputView.setFocusableInTouchMode(true);
    }

    private void setupHomeDirectory() {
        // 使用应用私有目录作为HOME
        File appDir = requireContext().getFilesDir();
        homeDirectory = new File(appDir, "terminal_home").getAbsolutePath();
        
        File homeDir = new File(homeDirectory);
        if (!homeDir.exists()) {
            homeDir.mkdirs();
        }
    }

    private void startShell() {
        shellProcess = new ShellProcess();
        
        try {
            shellProcess.start(homeDirectory, new ShellProcess.OutputCallback() {
                @Override
                public void onOutputReceived(String output) {
                    appendOutput(output);
                }

                @Override
                public void onError(Exception e) {
                    appendOutput("\n错误: " + e.getMessage() + "\n");
                }
            });
            
            commandParser = new CommandParser(requireContext(), shellProcess, homeDirectory);
            
            // 显示欢迎信息
            appendOutput("MovingHacker Terminal v1.0\n");
            appendOutput("输入 'help' 查看可用命令\n\n");
            
        } catch (IOException e) {
            Snackbar.make(requireView(), "启动Shell失败: " + e.getMessage(), 
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void setupListeners() {
        // 回车键执行命令
        inputView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                executeCommand();
                return true;
            }
            return false;
        });
        
        // Tab键 - 自动补全（简化版）
        keyTab.setOnClickListener(v -> {
            inputView.append("\t");
            inputView.requestFocus();
        });
        
        // Ctrl+C - 中断命令
        keyCtrlC.setOnClickListener(v -> {
            try {
                shellProcess.sendCtrlC();
                appendOutput("^C\n");
                inputView.requestFocus();
            } catch (IOException e) {
                showError(e.getMessage());
            }
        });
        
        // 上箭头 - 上一条命令
        keyUp.setOnClickListener(v -> {
            if (!commandHistory.isEmpty()) {
                if (historyIndex < 0) {
                    historyIndex = commandHistory.size() - 1;
                } else if (historyIndex > 0) {
                    historyIndex--;
                }
                inputView.setText(commandHistory.get(historyIndex));
                inputView.setSelection(inputView.getText().length());
                inputView.requestFocus();
            }
        });
        
        // 下箭头 - 下一条命令
        keyDown.setOnClickListener(v -> {
            if (!commandHistory.isEmpty() && historyIndex >= 0) {
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;
                    inputView.setText(commandHistory.get(historyIndex));
                } else {
                    historyIndex = -1;
                    inputView.setText("");
                }
                inputView.setSelection(inputView.getText().length());
                inputView.requestFocus();
            }
        });
        
        // 清屏
        keyClear.setOnClickListener(v -> {
            outputView.setText("");
            inputView.requestFocus();
        });
    }

    private void executeCommand() {
        String command = inputView.getText().toString().trim();
        if (command.isEmpty()) {
            return;
        }

        // 将命令添加到输出（显示提示符和命令）
        appendOutput("$ " + command + "\n");
        
        // 添加到历史
        commandHistory.add(command);
        historyIndex = -1;
        
        // 清空输入
        inputView.setText("");
        
        // 解析并执行命令
        boolean handled = commandParser.parseAndExecuteAsync(command, new CommandParser.OutputCallback() {
            @Override
            public void onOutput(String output) {
                appendOutput(output);
            }

            @Override
            public void onError(Exception e) {
                String msg = e.getMessage() == null ? "(no message)" : e.getMessage();
                appendOutput("错误: " + e.getClass().getName() + ": " + msg + "\n");
            }

            @Override
            public void onClear() {
                outputBuffer.clear();
                scheduleOutputUiUpdate();
            }

            @Override
            public void onExit() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        }, executor, mainHandler, true);
        
        // 如果不是自定义命令，发送给shell执行
        if (!handled) {
            try {
                shellProcess.sendCommand(command);
                // Shell会自动输出结果
            } catch (IOException e) {
                appendOutput("错误: " + e.getMessage() + "\n");
            }
        }
        
        // 确保输入框保持焦点
        inputView.postDelayed(() -> {
            inputView.requestFocus();
            inputView.setSelection(inputView.getText().length());
        }, 50);
    }

    private void appendOutput(String text) {
        outputBuffer.append(text);
        scheduleOutputUiUpdate();
    }

    private void showError(String message) {
        View root = getView();
        if (root == null) return;
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    private void scheduleOutputUiUpdate() {
        if (mainHandler == null) return;
        if (outputUpdateScheduled) return;
        outputUpdateScheduled = true;
        mainHandler.postDelayed(outputUpdateRunnable, 50);
    }

    private void flushOutputToUi() {
        outputUpdateScheduled = false;
        if (!isAdded()) return;
        View root = getView();
        if (root == null) return;
        outputView.setText(outputBuffer.getText());
        scrollView.postDelayed(() -> scrollView.scrollTo(0, outputView.getBottom()), 30);
        inputView.postDelayed(() -> {
            inputView.requestFocus();
            inputView.setSelection(inputView.getText().length());
        }, 40);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shellProcess != null) {
            shellProcess.destroy();
        }
        executor.shutdownNow();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
    }
}
