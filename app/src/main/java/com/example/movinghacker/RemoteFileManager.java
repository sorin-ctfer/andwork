package com.example.movinghacker;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 远程文件管理器 - 使用SSH命令管理远程文件
 */
public class RemoteFileManager {
    
    private Session session;
    private String host;
    private int port;
    private String username;
    private String password;
    
    public RemoteFileManager(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
    
    public interface ConnectionCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface CommandCallback {
        void onSuccess(String output);
        void onError(String error);
    }
    
    /**
     * 连接到SSH服务器
     */
    public void connect(ConnectionCallback callback) {
        new Thread(() -> {
            try {
                JSch jsch = new JSch();
                session = jsch.getSession(username, host, port);
                session.setPassword(password);
                
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.setTimeout(30000);
                
                session.connect();
                callback.onSuccess();
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
    
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
    
    /**
     * 执行SSH命令
     */
    public void executeCommand(String command, CommandCallback callback) {
        new Thread(() -> {
            try {
                if (!isConnected()) {
                    callback.onError("未连接到服务器");
                    return;
                }
                
                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                
                InputStream in = channel.getInputStream();
                InputStream err = channel.getErrStream();
                
                channel.connect();
                
                // 读取输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder output = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                // 读取错误输出
                BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
                StringBuilder errOutput = new StringBuilder();
                
                while ((line = errReader.readLine()) != null) {
                    errOutput.append(line).append("\n");
                }
                
                channel.disconnect();
                
                if (errOutput.length() > 0 && output.length() == 0) {
                    callback.onError(errOutput.toString());
                } else {
                    callback.onSuccess(output.toString());
                }
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    /**
     * 列出目录文件
     */
    public void listFiles(String path, CommandCallback callback) {
        // 使用 ls -la 命令列出文件
        String command = "ls -la " + escapePath(path);
        executeCommand(command, callback);
    }
    
    /**
     * 读取文件内容
     */
    public void readFile(String path, int maxBytes, CommandCallback callback) {
        // 使用 head 命令读取文件前N字节
        String command = "head -c " + maxBytes + " " + escapePath(path);
        executeCommand(command, callback);
    }
    
    /**
     * 删除文件
     */
    public void deleteFile(String path, boolean isDirectory, CommandCallback callback) {
        String command;
        if (isDirectory) {
            command = "rm -rf " + escapePath(path);
        } else {
            command = "rm -f " + escapePath(path);
        }
        executeCommand(command, callback);
    }
    
    /**
     * 重命名文件
     */
    public void renameFile(String oldPath, String newPath, CommandCallback callback) {
        String command = "mv " + escapePath(oldPath) + " " + escapePath(newPath);
        executeCommand(command, callback);
    }
    
    /**
     * 查找文件
     */
    public void findFiles(String searchPath, String pattern, boolean recursive, CommandCallback callback) {
        String command = "find " + escapePath(searchPath);
        if (!recursive) {
            command += " -maxdepth 1";
        }
        command += " -name '" + pattern + "'";
        executeCommand(command, callback);
    }
    
    /**
     * 转义路径中的特殊字符
     */
    private String escapePath(String path) {
        return "'" + path.replace("'", "'\\''") + "'";
    }
    
    public String getConnectionInfo() {
        return username + "@" + host;
    }
}
