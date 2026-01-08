package com.example.movinghacker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellProcess {
    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread outputThread;
    private OutputCallback callback;
    private String currentDirectory;
    private boolean isRunning;

    public interface OutputCallback {
        void onOutputReceived(String output);
        void onError(Exception e);
    }

    public void start(String homeDir, OutputCallback callback) throws IOException {
        this.callback = callback;
        this.currentDirectory = homeDir;
        
        ProcessBuilder builder = new ProcessBuilder("/system/bin/sh");
        builder.redirectErrorStream(true);
        
        // 设置环境变量
        Map<String, String> env = builder.environment();
        env.put("HOME", homeDir);
        env.put("PATH", "/system/bin:/system/xbin");
        env.put("TERM", "xterm-256color");
        env.put("LANG", "en_US.UTF-8");
        
        // 设置工作目录
        builder.directory(new File(homeDir));
        
        // 启动进程
        process = builder.start();
        isRunning = true;
        
        // 获取输入输出流
        reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        
        // 启动输出读取线程
        startOutputThread();
        
        // 初始化shell环境
        sendCommand("export PS1='$ '");
        sendCommand("cd " + homeDir);
    }

    private void startOutputThread() {
        outputThread = new Thread(() -> {
            try {
                char[] buffer = new char[1024];
                int len;
                while (isRunning && (len = reader.read(buffer)) != -1) {
                    String output = new String(buffer, 0, len);
                    if (callback != null) {
                        callback.onOutputReceived(output);
                    }
                }
            } catch (IOException e) {
                if (isRunning && callback != null) {
                    callback.onError(e);
                }
            }
        }, "local-shell-output");
        outputThread.start();
    }

    public void sendCommand(String command) throws IOException {
        if (writer != null && isRunning) {
            writer.write(command + "\n");
            writer.flush();
        }
    }

    public void sendRawInput(String input) throws IOException {
        if (writer != null && isRunning) {
            writer.write(input);
            writer.flush();
        }
    }

    public void sendCtrlC() throws IOException {
        if (writer != null && isRunning) {
            writer.write("\u0003");  // Ctrl+C
            writer.flush();
        }
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String dir) {
        this.currentDirectory = dir;
    }

    public boolean isRunning() {
        return isRunning && process != null && process.isAlive();
    }

    public void destroy() {
        isRunning = false;
        if (outputThread != null) {
            outputThread.interrupt();
        }
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ignored) {
                process.destroyForcibly();
            }
        }
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
