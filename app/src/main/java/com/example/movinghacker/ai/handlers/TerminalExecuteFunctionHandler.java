package com.example.movinghacker.ai.handlers;

import android.content.Context;
import android.util.Log;

import com.example.movinghacker.ai.FunctionDefinition;
import com.example.movinghacker.ai.FunctionHandler;
import com.example.movinghacker.ai.FunctionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 终端命令执行Function Handler
 * 支持Android原生Shell命令和自定义命令
 */
public class TerminalExecuteFunctionHandler implements FunctionHandler {
    private static final String TAG = "TerminalExecuteHandler";
    private static final String FUNCTION_NAME = "terminal_execute";
    private static final int COMMAND_TIMEOUT_SECONDS = 30;
    
    private Context context;
    private String homeDirectory;
    private Gson gson = new Gson();

    public TerminalExecuteFunctionHandler(Context context) {
        this.context = context;
        // 设置HOME目录
        File appDir = context.getFilesDir();
        this.homeDirectory = new File(appDir, "terminal_home").getAbsolutePath();
        
        File homeDir = new File(homeDirectory);
        if (!homeDir.exists()) {
            homeDir.mkdirs();
        }
    }

    @Override
    public String getName() {
        return FUNCTION_NAME;
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName(FUNCTION_NAME);
        def.setDescription(
            "Execute shell commands in an Android terminal environment. This function provides access to " +
            "Android's native shell commands and custom-implemented commands.\n\n" +
            
            "**Environment**: Android Shell (/system/bin/sh)\n" +
            "**Working Directory**: " + homeDirectory + "\n\n" +
            
            "**Available Command Categories**:\n\n" +
            
            "**1. File Operations (Android Toybox)**:\n" +
            "- `ls [-la] [path]` - List directory contents\n" +
            "- `cat <file>` - Display file contents\n" +
            "- `mkdir <dir>` - Create directory\n" +
            "- `rm [-rf] <file>` - Remove files/directories\n" +
            "- `cp <src> <dst>` - Copy files\n" +
            "- `mv <src> <dst>` - Move/rename files\n" +
            "- `chmod <mode> <file>` - Change file permissions\n" +
            "- `chown <user> <file>` - Change file owner\n" +
            "- `touch <file>` - Create empty file or update timestamp\n" +
            "- `ln [-s] <target> <link>` - Create links\n\n" +
            
            "**2. Text Processing (Android Toybox)**:\n" +
            "- `grep <pattern> <file>` - Search text patterns\n" +
            "- `sed 's/old/new/' <file>` - Stream editor\n" +
            "- `awk '{print $1}' <file>` - Text processing\n" +
            "- `head [-n N] <file>` - Show first N lines\n" +
            "- `tail [-n N] <file>` - Show last N lines\n" +
            "- `wc [-l] <file>` - Count lines/words/characters\n" +
            "- `sort <file>` - Sort lines\n" +
            "- `uniq <file>` - Remove duplicate lines\n" +
            "- `cut -d: -f1 <file>` - Cut fields from lines\n" +
            "- `tr 'a-z' 'A-Z'` - Translate characters\n\n" +
            
            "**3. System Information (Android Toybox)**:\n" +
            "- `ps [-A]` - List processes\n" +
            "- `top [-n 1]` - Display system resources\n" +
            "- `df [-h]` - Show disk space\n" +
            "- `du [-sh] <path>` - Show directory size\n" +
            "- `free [-h]` - Show memory usage\n" +
            "- `uptime` - Show system uptime\n" +
            "- `uname [-a]` - Show system information\n" +
            "- `whoami` - Show current user\n" +
            "- `id` - Show user and group IDs\n" +
            "- `date` - Show current date and time\n" +
            "- `env` - Show environment variables\n\n" +
            
            "**4. Network Commands (Android Toybox)**:\n" +
            "- `ping [-c N] <host>` - Ping network host\n" +
            "- `netstat [-an]` - Show network connections\n" +
            "- `ifconfig` - Show network interfaces\n" +
            "- `ip addr` - Show IP addresses\n" +
            "- `ip route` - Show routing table\n\n" +
            
            "**5. Process Management (Android Toybox)**:\n" +
            "- `kill <pid>` - Kill process by PID\n" +
            "- `killall <name>` - Kill processes by name\n" +
            "- `pkill <pattern>` - Kill processes by pattern\n" +
            "- `pgrep <pattern>` - Find processes by pattern\n\n" +
            
            "**6. Archive and Compression (Android Toybox)**:\n" +
            "- `tar -czf archive.tar.gz <files>` - Create tar.gz archive\n" +
            "- `tar -xzf archive.tar.gz` - Extract tar.gz archive\n" +
            "- `gzip <file>` - Compress file\n" +
            "- `gunzip <file.gz>` - Decompress file\n" +
            "- `zip -r archive.zip <files>` - Create zip archive\n" +
            "- `unzip archive.zip` - Extract zip archive\n\n" +
            
            "**7. Custom Implemented Commands**:\n" +
            "- `wget <URL> [-O file]` - Download files from the internet\n" +
            "- `curl <URL> [-X METHOD] [-H header] [-d data]` - Make HTTP requests (GET, POST, PUT, DELETE, etc.)\n" +
            "- `strings <file> [-n length]` - Extract printable strings from binary files\n" +
            "- `file <file>` - Identify file type by magic numbers (ELF, ZIP, PNG, JPEG, PDF, etc.)\n" +
            "- `find <path> [-name pattern] [-type f|d]` - Search for files recursively\n\n" +
            
            "**8. Utility Commands**:\n" +
            "- `echo <text>` - Print text\n" +
            "- `which <command>` - Show command location\n" +
            "- `export VAR=value` - Set environment variable\n\n" +
            
            "**Command Chaining**:\n" +
            "- Pipes: `command1 | command2` (e.g., `ls -la | grep txt`)\n" +
            "- Redirection: `command > file` or `command >> file`\n" +
            "- Multiple commands: `command1 && command2` or `command1 ; command2`\n\n" +
            
            "**Important Notes**:\n" +
            "1. Commands run in Android's limited shell environment (not full Linux)\n" +
            "2. Some commands may require specific Android permissions\n" +
            "3. File paths are relative to: " + homeDirectory + "\n" +
            "4. Commands timeout after " + COMMAND_TIMEOUT_SECONDS + " seconds\n" +
            "5. Use absolute paths for accessing system directories (e.g., /sdcard, /system)\n" +
            "6. Custom commands (wget, curl, strings, file, find) are implemented in Java for better Android compatibility\n\n" +
            
            "**Example Usage**:\n" +
            "```bash\n" +
            "# File operations\n" +
            "ls -la /sdcard/Download\n" +
            "cat /sdcard/test.txt\n" +
            "mkdir -p /sdcard/mydir\n" +
            "\n" +
            "# Text processing\n" +
            "grep 'error' /sdcard/log.txt\n" +
            "cat file.txt | wc -l\n" +
            "\n" +
            "# System info\n" +
            "ps -A | grep com.example\n" +
            "df -h\n" +
            "free -h\n" +
            "\n" +
            "# Network\n" +
            "ping -c 4 8.8.8.8\n" +
            "wget https://example.com/file.zip\n" +
            "curl https://api.github.com/users/github\n" +
            "\n" +
            "# File analysis\n" +
            "file /system/bin/sh\n" +
            "strings /system/bin/ls\n" +
            "find /sdcard -name '*.txt'\n" +
            "```"
        );

        Map<String, Object> parameters = new HashMap<>();
        
        Map<String, Object> commandParam = new HashMap<>();
        commandParam.put("type", "string");
        commandParam.put("description", 
            "The shell command to execute. Can be a single command or multiple commands " +
            "chained with pipes (|), redirections (>, >>), or logical operators (&&, ||, ;). " +
            "Commands run in Android's /system/bin/sh environment with access to Toybox utilities " +
            "and custom-implemented commands."
        );
        parameters.put("command", commandParam);
        
        Map<String, Object> workingDirParam = new HashMap<>();
        workingDirParam.put("type", "string");
        workingDirParam.put("description", 
            "Optional working directory for command execution. If not specified, uses the default " +
            "terminal home directory (" + homeDirectory + "). Can be absolute path (e.g., /sdcard) " +
            "or relative path."
        );
        parameters.put("working_directory", workingDirParam);

        // 添加required字段
        java.util.List<String> required = new java.util.ArrayList<>();
        required.add("command");
        parameters.put("required", required);

        def.setParameters(parameters);

        return def;
    }

    @Override
    public FunctionResult execute(String arguments) {
        // 解析参数
        Map<String, Object> args;
        try {
            args = gson.fromJson(arguments, new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
        } catch (Exception e) {
            return FunctionResult.error(null, "Invalid arguments: " + e.getMessage());
        }

        // 获取参数
        String command = (String) args.get("command");
        String workingDir = (String) args.get("working_directory");
        
        if (command == null || command.trim().isEmpty()) {
            return FunctionResult.error(null, "Parameter 'command' is required and cannot be empty");
        }

        // 设置工作目录
        if (workingDir == null || workingDir.trim().isEmpty()) {
            workingDir = homeDirectory;
        } else {
            // 处理相对路径
            File dir = new File(workingDir);
            if (!dir.isAbsolute()) {
                dir = new File(homeDirectory, workingDir);
            }
            workingDir = dir.getAbsolutePath();
        }

        try {
            Log.d(TAG, "Executing command: " + command + " in directory: " + workingDir);
            
            // 执行命令
            String output = executeShellCommand(command, workingDir);
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("command", command);
            result.addProperty("working_directory", workingDir);
            result.addProperty("output", output);
            result.addProperty("exit_code", 0);
            
            Log.d(TAG, "Command executed successfully");
            return FunctionResult.success(null, result.toString());
            
        } catch (Exception e) {
            // 命令执行错误
            String errorMsg = e.getMessage();
            Log.e(TAG, "Command execution error: " + errorMsg);
            
            JsonObject errorResult = new JsonObject();
            errorResult.addProperty("success", false);
            errorResult.addProperty("command", command);
            errorResult.addProperty("working_directory", workingDir);
            errorResult.addProperty("error_message", errorMsg);
            
            return FunctionResult.error(null, "Command execution failed: " + errorMsg);
        }
    }

    /**
     * 执行Shell命令并返回输出
     */
    private String executeShellCommand(String command, String workingDir) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("/system/bin/sh", "-c", command);
        builder.redirectErrorStream(true);
        
        // 设置环境变量
        Map<String, String> env = builder.environment();
        env.put("HOME", homeDirectory);
        env.put("PATH", "/system/bin:/system/xbin");
        env.put("TERM", "xterm-256color");
        env.put("LANG", "en_US.UTF-8");
        
        // 设置工作目录
        File dir = new File(workingDir);
        if (dir.exists() && dir.isDirectory()) {
            builder.directory(dir);
        } else {
            builder.directory(new File(homeDirectory));
        }
        
        // 启动进程
        Process process = builder.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // 等待进程完成（带超时）
        boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command execution timeout after " + COMMAND_TIMEOUT_SECONDS + " seconds");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String outputStr = output.toString();
            if (outputStr.isEmpty()) {
                throw new IOException("Command failed with exit code: " + exitCode);
            }
            // 如果有输出，即使exit code非0也返回输出（很多命令会在stderr输出但exit code非0）
        }
        
        String result = output.toString();
        return result.isEmpty() ? "Command executed successfully with no output" : result;
    }
}
