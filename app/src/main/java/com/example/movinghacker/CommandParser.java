package com.example.movinghacker;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * 命令解析器 - 拦截自定义命令并执行
 */
public class CommandParser {
    private CustomCommands customCommands;
    private ShellProcess shellProcess;
    private String homeDirectory;
    private String currentDirectory;

    public CommandParser(Context context, ShellProcess shellProcess, String homeDirectory) {
        this.customCommands = new CustomCommands(context);
        this.shellProcess = shellProcess;
        this.homeDirectory = homeDirectory;
        this.currentDirectory = homeDirectory;
        this.customCommands.setCurrentDirectory(currentDirectory);
    }

    /**
     * 解析并执行命令
     * @return true if command was handled by custom implementation
     */
    public boolean parseAndExecute(String commandLine, OutputCallback callback) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return false;
        }

        String[] args = parseCommandLine(commandLine.trim());
        if (args.length == 0) {
            return false;
        }

        String command = args[0];
        String result = null;

        // 检查是否是自定义命令
        switch (command) {
            case "wget":
                result = customCommands.wget(args);
                break;

            case "curl":
                result = customCommands.curl(args);
                break;

            case "strings":
                result = customCommands.strings(args);
                break;

            case "file":
                result = customCommands.file(args);
                break;

            case "find":
                result = customCommands.find(args);
                break;

            case "cd":
                result = customCommands.cd(args);
                if (result.equals("HOME")) {
                    currentDirectory = homeDirectory;
                    customCommands.setCurrentDirectory(currentDirectory);
                    try {
                        shellProcess.sendCommand("cd " + homeDirectory);
                    } catch (IOException e) {
                        callback.onError(e);
                    }
                    return true;
                } else if (result.startsWith("ERROR:")) {
                    callback.onOutput(result);
                    return true;
                } else {
                    // 成功改变目录
                    currentDirectory = result;
                    customCommands.setCurrentDirectory(currentDirectory);
                    shellProcess.setCurrentDirectory(currentDirectory);
                    try {
                        shellProcess.sendCommand("cd " + currentDirectory);
                    } catch (IOException e) {
                        callback.onError(e);
                    }
                    return true;
                }

            case "pwd":
                // 返回当前目录
                callback.onOutput(currentDirectory + "\n");
                return true;

            case "clear":
            case "cls":
                // 清屏命令
                callback.onClear();
                return true;

            case "exit":
            case "quit":
                // 退出命令
                callback.onExit();
                return true;

            case "help":
                result = getHelpText();
                break;

            default:
                // 不是自定义命令，返回false让shell处理
                return false;
        }

        // 输出自定义命令的结果
        if (result != null) {
            callback.onOutput(result);
            return true;
        }

        return false;
    }

    public boolean parseAndExecuteAsync(String commandLine, OutputCallback callback, Executor executor, Handler mainHandler, boolean debug) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return false;
        }

        String[] args = parseCommandLine(commandLine.trim());
        if (args.length == 0) {
            return false;
        }

        String command = args[0];

        switch (command) {
            case "wget":
            case "curl":
            case "strings":
            case "file":
            case "find":
                executor.execute(() -> {
                    try {
                        String result;
                        switch (command) {
                            case "wget":
                                result = customCommands.wget(args);
                                break;
                            case "curl":
                                result = customCommands.curl(args);
                                break;
                            case "strings":
                                result = customCommands.strings(args);
                                break;
                            case "file":
                                result = customCommands.file(args);
                                break;
                            case "find":
                                result = customCommands.find(args);
                                break;
                            default:
                                result = "";
                        }
                        String finalResult = result == null ? "" : result;
                        mainHandler.post(() -> callback.onOutput(finalResult));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                });
                return true;

            case "cd":
                String result = customCommands.cd(args);
                if (result.equals("HOME")) {
                    currentDirectory = homeDirectory;
                    customCommands.setCurrentDirectory(currentDirectory);
                    try {
                        shellProcess.sendCommand("cd " + homeDirectory);
                    } catch (IOException e) {
                        callback.onError(e);
                    }
                    return true;
                } else if (result.startsWith("ERROR:")) {
                    callback.onOutput(result);
                    return true;
                } else {
                    currentDirectory = result;
                    customCommands.setCurrentDirectory(currentDirectory);
                    shellProcess.setCurrentDirectory(currentDirectory);
                    try {
                        shellProcess.sendCommand("cd " + currentDirectory);
                    } catch (IOException e) {
                        callback.onError(e);
                    }
                    return true;
                }

            case "pwd":
                callback.onOutput(currentDirectory + "\n");
                return true;

            case "clear":
            case "cls":
                callback.onClear();
                return true;

            case "exit":
            case "quit":
                callback.onExit();
                return true;

            case "help":
                callback.onOutput(getHelpText());
                return true;
            default:
                return false;
        }
    }

    /**
     * 解析命令行为参数数组
     */
    private String[] parseCommandLine(String commandLine) {
        // 简单的参数解析，支持引号
        java.util.List<String> args = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);

            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
                continue;
            }

            if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args.toArray(new String[0]);
    }

    private String getHelpText() {
        return "可用命令:\n" +
                "\n系统命令:\n" +
                "  ls, cat, mkdir, rm, cp, mv, chmod, ps, top, df, du, free\n" +
                "  grep, sed, awk, head, tail, wc, sort, uniq, cut\n" +
                "  ping, netstat, ifconfig, ip, kill, date, echo, env\n" +
                "\n自定义命令:\n" +
                "  wget <URL> [-O file]     - 下载文件\n" +
                "  curl <URL> [-X METHOD]   - HTTP请求\n" +
                "  strings <file> [-n len]  - 提取可打印字符串\n" +
                "  file <file>              - 识别文件类型\n" +
                "  find <path> [-name pat]  - 查找文件\n" +
                "  cd <dir>                 - 改变目录\n" +
                "  pwd                      - 显示当前目录\n" +
                "  clear                    - 清屏\n" +
                "  help                     - 显示此帮助\n" +
                "  exit                     - 退出终端\n";
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public interface OutputCallback {
        void onOutput(String output);
        void onError(Exception e);
        void onClear();
        void onExit();
    }
}
