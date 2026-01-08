package com.example.movinghacker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSHTerminalFragment extends Fragment {
    private static final String ARG_CONNECTION = "connection";
    
    private TextView outputView;
    private TextView statusView;
    private EditText inputView;
    private ScrollView scrollView;
    private MaterialButton disconnectButton;
    
    // 快捷键按钮
    private MaterialButton keyTab;
    private MaterialButton keyCtrlC;
    private MaterialButton keyCtrlD;
    private MaterialButton keyClear;
    
    private SSHClient sshClient;
    private SSHConnectionManager.SSHConnection connection;
    private Handler mainHandler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isConnecting = false;
    private volatile boolean isConnected = false;

    private final TerminalOutputBuffer outputBuffer = new TerminalOutputBuffer(80_000);
    private boolean outputUpdateScheduled = false;
    private final Runnable outputUpdateRunnable = this::flushOutputToUi;

    public static SSHTerminalFragment newInstance(SSHConnectionManager.SSHConnection connection) {
        SSHTerminalFragment fragment = new SSHTerminalFragment();
        Bundle args = new Bundle();
        args.putString("name", connection.name);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ssh, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainHandler = new Handler(Looper.getMainLooper());
        
        // 从参数恢复连接信息
        if (getArguments() != null) {
            String name = getArguments().getString("name");
            if (name != null && !name.isEmpty()) {
                SSHConnectionManager manager = new SSHConnectionManager(requireContext());
                for (SSHConnectionManager.SSHConnection c : manager.getConnections()) {
                    if (name.equals(c.name)) {
                        connection = c;
                        break;
                    }
                }
            }
        }

        initializeViews(view);
        sshClient = new SSHClient();
        setupListeners();
        
        // 自动连接
        if (connection != null) {
            connect();
        }
    }

    private void initializeViews(View view) {
        outputView = view.findViewById(R.id.ssh_output);
        statusView = view.findViewById(R.id.connection_status);
        inputView = view.findViewById(R.id.ssh_input);
        scrollView = view.findViewById(R.id.ssh_scroll);
        disconnectButton = view.findViewById(R.id.btn_disconnect);
        
        keyTab = view.findViewById(R.id.ssh_key_tab);
        keyCtrlC = view.findViewById(R.id.ssh_key_ctrl_c);
        keyCtrlD = view.findViewById(R.id.ssh_key_ctrl_d);
        keyClear = view.findViewById(R.id.ssh_key_clear);
        
        // 确保输入框可以获得焦点
        inputView.setFocusable(true);
        inputView.setFocusableInTouchMode(true);
    }

    private void setupListeners() {
        // 断开按钮
        disconnectButton.setOnClickListener(v -> {
            disconnect();
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        // 回车键执行命令
        inputView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendCommand();
                return true;
            }
            return false;
        });
        
        // Tab键
        keyTab.setOnClickListener(v -> {
            executeSshIo("发送Tab", () -> sshClient.sendRawInput("\t"));
            inputView.postDelayed(() -> inputView.requestFocus(), 50);
        });
        
        // Ctrl+C
        keyCtrlC.setOnClickListener(v -> {
            executeSshIo("发送Ctrl+C", sshClient::sendCtrlC);
            inputView.postDelayed(() -> inputView.requestFocus(), 50);
        });
        
        // Ctrl+D
        keyCtrlD.setOnClickListener(v -> {
            executeSshIo("发送Ctrl+D", sshClient::sendCtrlD);
            inputView.postDelayed(() -> inputView.requestFocus(), 50);
        });
        
        // 清屏
        keyClear.setOnClickListener(v -> {
            outputView.setText("");
            inputView.postDelayed(() -> inputView.requestFocus(), 50);
        });
    }

    private void connect() {
        if (isConnecting) {
            return;
        }
        
        isConnecting = true;
        isConnected = false;
        updateStatus("连接中...");
        inputView.setEnabled(false);
        
        executor.execute(() -> {
            try {
                sshClient.connect(connection.host, connection.port, connection.username, 
                        connection.password, new SSHClient.OutputCallback() {
                    @Override
                    public void onOutputReceived(String output) {
                        if (mainHandler != null) {
                            appendOutput(output);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (mainHandler != null) {
                            appendOutput("\n错误: " + e.getMessage() + "\n");
                            showError("SSH错误: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onDisconnected() {
                        isConnected = false;
                        if (mainHandler != null) {
                            mainHandler.post(() -> {
                                if (getView() == null) return;
                                updateStatus("已断开");
                                inputView.setEnabled(false);
                                appendOutput("\n连接已断开\n");
                            });
                        }
                    }
                });
                
                // 连接成功
                isConnected = true;
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        isConnecting = false;
                        updateStatus("已连接: " + sshClient.getConnectionInfo());
                        inputView.setEnabled(true);
                        // 延迟聚焦，确保布局完成
                        inputView.postDelayed(() -> {
                            inputView.requestFocus();
                            inputView.setSelection(inputView.getText().length());
                        }, 200);
                        View root = getView();
                        if (root != null) {
                            Snackbar.make(root, "SSH连接成功", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                }
                
            } catch (Exception e) {
                isConnected = false;
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        isConnecting = false;
                        updateStatus("连接失败");
                        inputView.setEnabled(false);
                        showError("连接失败: " + e.getMessage());
                        appendOutput("\n连接失败: " + e.getMessage() + "\n");
                        
                        // 3秒后返回
                        mainHandler.postDelayed(() -> {
                            if (getActivity() != null) {
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                        }, 3000);
                    });
                }
            }
        });
    }

    private void disconnect() {
        isConnected = false;
        SSHClient client = sshClient;
        if (client != null) {
            executor.execute(client::disconnect);
        }
        updateStatus("未连接");
        inputView.setEnabled(false);
    }

    private void sendCommand() {
        String command = inputView.getText().toString();
        if (command.isEmpty()) {
            return;
        }
        inputView.setText("");
        inputView.postDelayed(() -> {
            inputView.requestFocus();
            inputView.setSelection(inputView.getText().length());
        }, 50);

        executeSshIo("发送命令", () -> sshClient.sendCommand(command));
    }

    private interface IoAction {
        void run() throws IOException;
    }

    private void executeSshIo(String actionName, IoAction action) {
        SSHClient client = sshClient;
        if (!isConnected || client == null || !client.isConnected()) {
            isConnected = false;
            updateStatus("连接已断开");
            inputView.setEnabled(false);
            return;
        }

        executor.execute(() -> {
            try {
                action.run();
            } catch (IOException e) {
                String message = e.getMessage() == null ? "(no message)" : e.getMessage();
                showError(actionName + "失败: " + message);

                boolean connected = client.isConnected();
                if (!connected && mainHandler != null) {
                    mainHandler.post(() -> {
                        isConnected = false;
                        updateStatus("连接已断开");
                        inputView.setEnabled(false);
                    });
                }
            }
        });
    }

    private void updateStatus(String status) {
        if (mainHandler != null) {
            mainHandler.post(() -> statusView.setText(status));
        }
    }

    private void appendOutput(String text) {
        outputBuffer.append(text);
        scheduleOutputUiUpdate();
    }

    private void showError(String message) {
        if (mainHandler != null) {
            mainHandler.post(() -> {
                View root = getView();
                if (root == null) return;
                Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
            });
        }
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
        if (inputView.isEnabled()) {
            inputView.postDelayed(() -> {
                inputView.requestFocus();
                inputView.setSelection(inputView.getText().length());
            }, 40);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isConnected = false;
        if (sshClient != null) {
            sshClient.disconnect();
        }
        executor.shutdownNow();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
    }
}
