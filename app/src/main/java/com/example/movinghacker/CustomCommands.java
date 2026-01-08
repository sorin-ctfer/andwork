package com.example.movinghacker;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 自定义实现Android原生shell不支持的命令
 */
public class CustomCommands {
    private Context context;
    private String currentDirectory;
    private OkHttpClient httpClient;

    public CustomCommands(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public void setCurrentDirectory(String dir) {
        this.currentDirectory = dir;
    }

    /**
     * wget命令实现 - 下载文件
     */
    public String wget(String[] args) {
        if (args.length < 2) {
            return "用法: wget <URL> [-O output_file]\n";
        }

        String url = args[1];
        String outputFile = null;

        // 解析参数
        for (int i = 2; i < args.length; i++) {
            if ("-O".equals(args[i]) && i + 1 < args.length) {
                outputFile = args[i + 1];
                i++;
            }
        }

        // 如果没有指定输出文件，从URL提取文件名
        if (outputFile == null) {
            outputFile = url.substring(url.lastIndexOf('/') + 1);
            if (outputFile.isEmpty()) {
                outputFile = "index.html";
            }
        }

        // 构建完整路径
        File file = new File(currentDirectory, outputFile);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                return "错误: HTTP " + response.code() + "\n";
            }

            // 下载文件
            InputStream inputStream = response.body().byteStream();
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            outputStream.close();
            inputStream.close();

            return String.format("已下载: %s (%d 字节)\n", file.getAbsolutePath(), totalBytes);

        } catch (Exception e) {
            return "错误: " + e.getMessage() + "\n";
        }
    }

    /**
     * curl命令实现 - HTTP请求
     */
    public String curl(String[] args) {
        if (args.length < 2) {
            return "用法: curl <URL> [-X METHOD] [-H header] [-d data] [-o output]\n";
        }

        String url = args[1];
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        String method = "GET";
        List<String> headers = new ArrayList<>();
        String data = null;
        String outputFile = null;

        // 解析参数
        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "-X":
                    if (i + 1 < args.length) {
                        method = args[++i];
                    }
                    break;
                case "-H":
                    if (i + 1 < args.length) {
                        headers.add(args[++i]);
                    }
                    break;
                case "-d":
                case "--data":
                    if (i + 1 < args.length) {
                        data = args[++i];
                    }
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    }
                    break;
            }
        }

        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);

            // 添加自定义请求头
            for (String header : headers) {
                String[] parts = header.split(":", 2);
                if (parts.length == 2) {
                    requestBuilder.addHeader(parts[0].trim(), parts[1].trim());
                }
            }

            // 设置请求方法和数据
            if (data != null) {
                requestBuilder.method(method, 
                    okhttp3.RequestBody.create(data, 
                        okhttp3.MediaType.parse("application/x-www-form-urlencoded")));
            } else if ("GET".equals(method) || "HEAD".equals(method)) {
                // GET和HEAD请求不能有body
                requestBuilder.method(method, null);
            } else {
                // POST, PUT, DELETE等需要空body
                requestBuilder.method(method, 
                    okhttp3.RequestBody.create("", 
                        okhttp3.MediaType.parse("text/plain")));
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (!response.isSuccessful()) {
                    return "HTTP " + response.code() + " " + response.message() + "\n";
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                if (responseBody == null) responseBody = "";

                // 如果指定了输出文件
                if (outputFile != null) {
                    File file = new File(currentDirectory, outputFile);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(responseBody.getBytes());
                    fos.close();
                    return String.format("已保存到: %s\n", file.getAbsolutePath());
                }

                return responseBody + "\n";
            }

        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.isEmpty()) message = "(no message)";
            String cause = e.getCause() == null ? "" : (e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            return "错误: " + e.getClass().getName() + ": " + message + (cause.isEmpty() ? "" : ("\n原因: " + cause)) + "\n";
        }
    }

    /**
     * strings命令实现 - 提取文件中的可打印字符串
     */
    public String strings(String[] args) {
        if (args.length < 2) {
            return "用法: strings <file> [-n min_length]\n";
        }

        String filename = args[1];
        int minLength = 4;  // 默认最小长度

        // 解析参数
        for (int i = 2; i < args.length; i++) {
            if ("-n".equals(args[i]) && i + 1 < args.length) {
                try {
                    minLength = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    return "错误: 无效的长度参数\n";
                }
            }
        }

        File file = new File(currentDirectory, filename);
        if (!file.exists()) {
            return "错误: 文件不存在\n";
        }

        StringBuilder result = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(file);
            StringBuilder currentString = new StringBuilder();

            int b;
            while ((b = fis.read()) != -1) {
                // 可打印ASCII字符 (32-126)
                if (b >= 32 && b <= 126) {
                    currentString.append((char) b);
                } else {
                    if (currentString.length() >= minLength) {
                        result.append(currentString).append("\n");
                    }
                    currentString.setLength(0);
                }
            }

            // 处理最后一个字符串
            if (currentString.length() >= minLength) {
                result.append(currentString).append("\n");
            }

            fis.close();

        } catch (IOException e) {
            return "错误: " + e.getMessage() + "\n";
        }

        return result.toString();
    }

    /**
     * file命令实现 - 识别文件类型
     */
    public String file(String[] args) {
        if (args.length < 2) {
            return "用法: file <file>\n";
        }

        String filename = args[1];
        File file = new File(currentDirectory, filename);

        if (!file.exists()) {
            return filename + ": 文件不存在\n";
        }

        if (file.isDirectory()) {
            return filename + ": directory\n";
        }

        try {
            // 读取文件头部字节来判断类型
            FileInputStream fis = new FileInputStream(file);
            byte[] header = new byte[16];
            int bytesRead = fis.read(header);
            fis.close();

            if (bytesRead < 4) {
                return filename + ": empty or very small file\n";
            }

            // 检查文件魔数
            String fileType = detectFileType(header, bytesRead);
            return filename + ": " + fileType + "\n";

        } catch (IOException e) {
            return filename + ": 无法读取文件\n";
        }
    }

    private String detectFileType(byte[] header, int length) {
        // ELF可执行文件
        if (length >= 4 && header[0] == 0x7F && header[1] == 'E' && 
            header[2] == 'L' && header[3] == 'F') {
            return "ELF executable";
        }

        // ZIP文件
        if (length >= 4 && header[0] == 'P' && header[1] == 'K' && 
            header[2] == 0x03 && header[3] == 0x04) {
            return "ZIP archive";
        }

        // PNG图片
        if (length >= 8 && header[0] == (byte)0x89 && header[1] == 'P' && 
            header[2] == 'N' && header[3] == 'G') {
            return "PNG image";
        }

        // JPEG图片
        if (length >= 2 && header[0] == (byte)0xFF && header[1] == (byte)0xD8) {
            return "JPEG image";
        }

        // PDF文件
        if (length >= 4 && header[0] == '%' && header[1] == 'P' && 
            header[2] == 'D' && header[3] == 'F') {
            return "PDF document";
        }

        // 检查是否是文本文件
        boolean isText = true;
        for (int i = 0; i < length; i++) {
            byte b = header[i];
            if (b < 9 || (b > 13 && b < 32 && b != 27) || b == 127) {
                isText = false;
                break;
            }
        }

        if (isText) {
            return "ASCII text";
        }

        return "data";
    }

    /**
     * find命令实现 - 查找文件
     */
    public String find(String[] args) {
        if (args.length < 2) {
            return "用法: find <path> [-name pattern] [-type f|d]\n";
        }

        String searchPath = args[1];
        String namePattern = null;
        String type = null;

        // 解析参数
        for (int i = 2; i < args.length; i++) {
            if ("-name".equals(args[i]) && i + 1 < args.length) {
                namePattern = args[++i];
            } else if ("-type".equals(args[i]) && i + 1 < args.length) {
                type = args[++i];
            }
        }

        File startDir = new File(currentDirectory, searchPath);
        if (!startDir.exists()) {
            return "错误: 路径不存在\n";
        }

        StringBuilder result = new StringBuilder();
        findFiles(startDir, namePattern, type, result);

        return result.toString();
    }

    private void findFiles(File dir, String namePattern, String type, StringBuilder result) {
        if (!dir.canRead()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            boolean matches = true;

            // 检查类型
            if (type != null) {
                if ("f".equals(type) && !file.isFile()) {
                    matches = false;
                } else if ("d".equals(type) && !file.isDirectory()) {
                    matches = false;
                }
            }

            // 检查名称模式
            if (matches && namePattern != null) {
                String pattern = namePattern.replace("*", ".*").replace("?", ".");
                if (!file.getName().matches(pattern)) {
                    matches = false;
                }
            }

            if (matches) {
                result.append(file.getAbsolutePath()).append("\n");
            }

            // 递归搜索子目录
            if (file.isDirectory()) {
                findFiles(file, namePattern, type, result);
            }
        }
    }

    /**
     * cd命令实现 - 改变目录
     */
    public String cd(String[] args) {
        if (args.length < 2) {
            // cd without arguments goes to home
            return "HOME";
        }

        String targetPath = args[1];
        File targetDir;

        if (targetPath.startsWith("/")) {
            // 绝对路径
            targetDir = new File(targetPath);
        } else if ("..".equals(targetPath)) {
            // 上级目录
            targetDir = new File(currentDirectory).getParentFile();
        } else if (".".equals(targetPath)) {
            // 当前目录
            return currentDirectory;
        } else if ("~".equals(targetPath)) {
            // Home目录
            return "HOME";
        } else {
            // 相对路径
            targetDir = new File(currentDirectory, targetPath);
        }

        if (targetDir == null || !targetDir.exists()) {
            return "ERROR: 目录不存在\n";
        }

        if (!targetDir.isDirectory()) {
            return "ERROR: 不是目录\n";
        }

        try {
            return targetDir.getCanonicalPath();
        } catch (IOException e) {
            return "ERROR: " + e.getMessage() + "\n";
        }
    }
}
