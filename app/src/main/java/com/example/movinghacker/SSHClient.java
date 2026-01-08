package com.example.movinghacker;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSH客户端 - 使用JSch实现
 */
public class SSHClient {
    private JSch jsch;
    private Session session;
    private ChannelShell channel;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread outputThread;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private OutputCallback callback;
    private final Object writeLock = new Object();

    public interface OutputCallback {
        void onOutputReceived(String output);
        void onError(Exception e);
        void onDisconnected();
    }

    // Debug method removed

    public SSHClient() {
        jsch = new JSch();
    }

    /**
     * 连接SSH服务器（密码认证）
     */
    public void connect(String host, int port, String username, String password, 
                       OutputCallback callback) throws Exception {
        this.callback = callback;
        
        // 创建会话
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        
        // 配置会话属性
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");  // 跳过主机密钥检查
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);
        
        session.setServerAliveInterval(15000);
        session.setServerAliveCountMax(2);
        session.connect(15000);
        
        // 打开shell通道
        channel = (ChannelShell) session.openChannel("shell");
        
        // 设置终端类型
        channel.setPty(true);
        channel.setPtyType("xterm-256color");
        channel.setPtySize(120, 40, 0, 0);
        
        // 获取输入输出流
        inputStream = channel.getInputStream();
        outputStream = channel.getOutputStream();
        
        // 连接通道
        channel.connect(15000);
        
        isConnected.set(true);
        
        // 启动输出读取线程
        startOutputThread();
    }

    /**
     * 连接SSH服务器（密钥认证）
     */
    public void connectWithKey(String host, int port, String username, 
                              String privateKeyPath, String passphrase,
                              OutputCallback callback) throws Exception {
        this.callback = callback;
        
        // 添加私钥
        if (passphrase != null && !passphrase.isEmpty()) {
            jsch.addIdentity(privateKeyPath, passphrase);
        } else {
            jsch.addIdentity(privateKeyPath);
        }
        
        // 创建会话
        session = jsch.getSession(username, host, port);
        
        // 配置会话属性
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey");
        session.setConfig(config);
        
        session.setServerAliveInterval(15000);
        session.setServerAliveCountMax(2);
        session.connect(15000);
        
        // 打开shell通道
        channel = (ChannelShell) session.openChannel("shell");
        channel.setPty(true);
        channel.setPtyType("xterm-256color");
        channel.setPtySize(120, 40, 0, 0);
        
        // 获取输入输出流
        inputStream = channel.getInputStream();
        outputStream = channel.getOutputStream();
        
        // 连接通道
        channel.connect(15000);
        
        isConnected.set(true);
        
        // 启动输出读取线程
        startOutputThread();
    }

    private void startOutputThread() {
        outputThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int len;
                while (isConnected.get() && (len = inputStream.read(buffer)) != -1) {
                    String output = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    if (callback != null) {
                        callback.onOutputReceived(output);
                    }
                }
            } catch (IOException e) {
                if (isConnected.get() && callback != null) {
                    callback.onError(e);
                }
            } finally {
                if (isConnected.getAndSet(false) && callback != null) callback.onDisconnected();
            }
        }, "ssh-output");
        outputThread.start();
    }

    /**
     * 发送命令
     */
    public void sendCommand(String command) throws IOException {
        if (command == null) command = "";
        if (outputStream == null || !isConnected()) {
            isConnected.set(false);
            throw new IOException("SSH connection not active");
        }

        synchronized (writeLock) {
            try {
                outputStream.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                isConnected.set(false);
                throw e;
            }
        }
    }

    /**
     * 发送原始输入
     */
    public void sendRawInput(String input) throws IOException {
        if (input == null) input = "";
        if (outputStream == null || !isConnected()) {
            isConnected.set(false);
            throw new IOException("SSH connection not active");
        }

        synchronized (writeLock) {
            try {
                outputStream.write(input.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                isConnected.set(false);
                throw e;
            }
        }
    }

    /**
     * 发送Ctrl+C
     */
    public void sendCtrlC() throws IOException {
        if (outputStream == null || !isConnected()) {
            isConnected.set(false);
            throw new IOException("SSH connection not active");
        }

        synchronized (writeLock) {
            try {
                outputStream.write(3);
                outputStream.flush();
            } catch (IOException e) {
                isConnected.set(false);
                throw e;
            }
        }
    }

    /**
     * 发送Ctrl+D
     */
    public void sendCtrlD() throws IOException {
        if (outputStream == null || !isConnected()) {
            isConnected.set(false);
            throw new IOException("SSH connection not active");
        }

        synchronized (writeLock) {
            try {
                outputStream.write(4);
                outputStream.flush();
            } catch (IOException e) {
                isConnected.set(false);
                throw e;
            }
        }
    }

    /**
     * 调整终端大小
     */
    public void resizeTerminal(int width, int height) {
        if (channel != null && channel.isConnected()) {
            channel.setPtySize(width, height, width * 8, height * 16);
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return isConnected.get() && session != null && session.isConnected() && 
               channel != null && channel.isConnected();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        boolean wasConnected = isConnected.getAndSet(false);
        
        if (outputThread != null) {
            outputThread.interrupt();
        }

        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException ignored) {
        }
        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException ignored) {
        }
        
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        if (wasConnected && callback != null) callback.onDisconnected();
    }

    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        if (session != null) {
            return session.getUserName() + "@" + session.getHost() + ":" + session.getPort();
        }
        return "未连接";
    }
}
