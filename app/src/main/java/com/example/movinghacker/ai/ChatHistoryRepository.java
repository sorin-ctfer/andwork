package com.example.movinghacker.ai;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天历史记录仓库
 * 负责聊天记录的保存和加载
 */
public class ChatHistoryRepository {
    private static final String TAG = "ChatHistoryRepository";
    private static final String HISTORY_FILE_NAME = "chat_history.json";
    private static final int MAX_HISTORY_SIZE = 100;  // 最多保存100条消息

    private static ChatHistoryRepository instance;
    private File historyFile;
    private Gson gson;

    private ChatHistoryRepository(Context context) {
        File filesDir = context.getFilesDir();
        historyFile = new File(filesDir, HISTORY_FILE_NAME);
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public static synchronized ChatHistoryRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ChatHistoryRepository(context);
        }
        return instance;
    }

    /**
     * 保存单条消息
     * @param message 消息
     */
    public void saveMessage(ChatMessage message) {
        if (message == null) {
            Log.w(TAG, "Attempted to save null message");
            return;
        }

        try {
            List<ChatMessage> history = loadHistory();
            history.add(message);

            // 限制历史记录数量
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
            }

            saveHistory(history);
            Log.d(TAG, "Message saved, total: " + history.size());
        } catch (Exception e) {
            Log.e(TAG, "Error saving message", e);
        }
    }

    /**
     * 批量保存消息
     * @param messages 消息列表
     */
    public void saveMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            List<ChatMessage> history = loadHistory();
            history.addAll(messages);

            // 限制历史记录数量
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
            }

            saveHistory(history);
            Log.d(TAG, "Messages saved, total: " + history.size());
        } catch (Exception e) {
            Log.e(TAG, "Error saving messages", e);
        }
    }

    /**
     * 加载历史记录
     * @return 消息列表
     */
    public List<ChatMessage> loadHistory() {
        if (!historyFile.exists()) {
            Log.d(TAG, "History file does not exist");
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(historyFile)) {
            Type type = new TypeToken<List<ChatMessage>>(){}.getType();
            List<ChatMessage> history = gson.fromJson(reader, type);
            
            if (history == null) {
                history = new ArrayList<>();
            }
            
            Log.d(TAG, "Loaded " + history.size() + " messages");
            return history;
        } catch (Exception e) {
            Log.e(TAG, "Error loading history", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存完整历史记录
     * @param history 消息列表
     */
    public void saveHistory(List<ChatMessage> history) {
        try (FileWriter writer = new FileWriter(historyFile)) {
            gson.toJson(history, writer);
            Log.d(TAG, "History saved to file");
        } catch (IOException e) {
            Log.e(TAG, "Error writing history file", e);
        }
    }

    /**
     * 清空历史记录
     */
    public void clearHistory() {
        clearAll();
    }

    /**
     * 清空历史记录
     */
    public void clearAll() {
        try {
            if (historyFile.exists()) {
                boolean deleted = historyFile.delete();
                if (deleted) {
                    Log.d(TAG, "History file deleted");
                } else {
                    Log.w(TAG, "Failed to delete history file");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing history", e);
        }
    }

    /**
     * 获取历史记录数量
     * @return 消息数量
     */
    public int getHistorySize() {
        return loadHistory().size();
    }

    /**
     * 获取最近N条消息
     * @param count 消息数量
     * @return 消息列表
     */
    public List<ChatMessage> getRecentMessages(int count) {
        List<ChatMessage> history = loadHistory();
        
        if (history.size() <= count) {
            return history;
        }
        
        return history.subList(history.size() - count, history.size());
    }

    /**
     * 删除指定消息
     * @param messageId 消息ID
     * @return 是否成功
     */
    public boolean deleteMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }

        try {
            List<ChatMessage> history = loadHistory();
            boolean removed = history.removeIf(msg -> messageId.equals(msg.getId()));
            
            if (removed) {
                saveHistory(history);
                Log.d(TAG, "Message deleted: " + messageId);
            }
            
            return removed;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting message", e);
            return false;
        }
    }
}
