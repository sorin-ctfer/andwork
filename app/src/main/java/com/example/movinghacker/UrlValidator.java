package com.example.movinghacker;

import java.util.regex.Pattern;

public class UrlValidator {

    // URL验证正则表达式：必须以http://或https://开头
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)" + // 协议
            "([\\w.-]+)" +    // 域名
            "(:\\d+)?" +      // 可选端口
            "(/.*)?$",        // 可选路径
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 验证URL格式是否有效
     * @param url 要验证的URL字符串
     * @return true如果URL格式有效，false否则
     */
    public static boolean isValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        return URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * 检查URL是否使用HTTP或HTTPS协议
     * @param url 要检查的URL字符串
     * @return true如果使用HTTP或HTTPS协议，false否则
     */
    public static boolean isHttpOrHttps(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmedUrl = url.trim().toLowerCase();
        return trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://");
    }

    /**
     * 获取URL验证错误消息
     * @param url 要验证的URL字符串
     * @return 错误消息，如果URL有效则返回null
     */
    public static String getErrorMessage(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "URL不能为空";
        }

        if (!isHttpOrHttps(url)) {
            return "URL必须以http://或https://开头";
        }

        if (!isValid(url)) {
            return "URL格式无效";
        }

        return null;
    }
}
