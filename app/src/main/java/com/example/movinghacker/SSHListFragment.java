package com.example.movinghacker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class SSHListFragment extends Fragment {
    private RecyclerView connectionsList;
    private LinearLayout emptyState;
    private MaterialButton btnAddConnection;
    
    private SSHConnectionManager connectionManager;
    private SSHConnectionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ssh_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connectionsList = view.findViewById(R.id.connections_list);
        emptyState = view.findViewById(R.id.empty_state);
        btnAddConnection = view.findViewById(R.id.btn_add_connection);

        connectionManager = new SSHConnectionManager(requireContext());

        // 检查是否从文件管理器调用
        Bundle args = getArguments();
        boolean fromFileManager = args != null && args.getBoolean("from_file_manager", false);

        setupRecyclerView(fromFileManager);
        loadConnections();

        btnAddConnection.setOnClickListener(v -> showAddConnectionDialog());
    }

    private void setupRecyclerView(boolean fromFileManager) {
        connectionsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        if (fromFileManager) {
            // 从文件管理器调用：点击连接后返回
            adapter = new SSHConnectionAdapter(
                    connectionManager.getConnections(),
                    this::onConnectionSelectedForFileManager,
                    this::onConnectionDelete
            );
        } else {
            // 正常调用：点击连接后打开SSH终端
            adapter = new SSHConnectionAdapter(
                    connectionManager.getConnections(),
                    this::onConnectionClick,
                    this::onConnectionDelete
            );
        }
        
        connectionsList.setAdapter(adapter);
    }

    private void loadConnections() {
        List<SSHConnectionManager.SSHConnection> connections = connectionManager.getConnections();
        adapter.updateConnections(connections);
        
        if (connections.isEmpty()) {
            connectionsList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            connectionsList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showAddConnectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ssh_connect, null);
        
        TextInputEditText nameInput = dialogView.findViewById(R.id.input_name);
        TextInputEditText hostInput = dialogView.findViewById(R.id.input_host);
        TextInputEditText portInput = dialogView.findViewById(R.id.input_port);
        TextInputEditText usernameInput = dialogView.findViewById(R.id.input_username);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.input_password);
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String host = hostInput.getText().toString().trim();
            String portStr = portInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            
            if (name.isEmpty() || host.isEmpty() || username.isEmpty()) {
                Snackbar.make(requireView(), "请填写连接名称、主机地址和用户名", 
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                port = 22;
            }
            
            SSHConnectionManager.SSHConnection connection = 
                    new SSHConnectionManager.SSHConnection(name, host, port, username, password);
            connectionManager.saveConnection(connection);
            
            dialog.dismiss();
            loadConnections();
            Snackbar.make(requireView(), "连接已保存", Snackbar.LENGTH_SHORT).show();
        });
        
        dialog.show();
    }

    private void onConnectionClick(SSHConnectionManager.SSHConnection connection) {
        // 导航到SSH终端Fragment
        SSHTerminalFragment fragment = SSHTerminalFragment.newInstance(connection);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void onConnectionSelectedForFileManager(SSHConnectionManager.SSHConnection connection) {
        // 将连接信息传回文件管理器
        Bundle result = new Bundle();
        result.putString("host", connection.host);
        result.putInt("port", connection.port);
        result.putString("username", connection.username);
        result.putString("password", connection.password);
        result.putString("name", connection.name);
        
        // 使用Fragment Result API返回
        getParentFragmentManager().setFragmentResult("ssh_connection_selected", result);
        
        // 返回上一个Fragment
        getParentFragmentManager().popBackStack();
    }

    private void onConnectionDelete(SSHConnectionManager.SSHConnection connection) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除连接")
                .setMessage("确定要删除连接 \"" + connection.name + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    connectionManager.deleteConnection(connection.name);
                    loadConnections();
                    Snackbar.make(requireView(), "连接已删除", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConnections();
    }
}
