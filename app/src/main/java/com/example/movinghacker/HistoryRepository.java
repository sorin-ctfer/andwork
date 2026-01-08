package com.example.movinghacker;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryRepository {
    
    private final HistoryDao historyDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private static final int MAX_HISTORY_COUNT = 100;

    public HistoryRepository(Context context) {
        HistoryDatabase database = HistoryDatabase.getInstance(context);
        historyDao = database.historyDao();
    }

    public void saveRequest(HttpRequest request, HttpResponse response) {
        executor.execute(() -> {
            try {
                // 检查是否超过100条记录
                int count = historyDao.getCount();
                if (count >= MAX_HISTORY_COUNT) {
                    // 删除最旧的记录
                    historyDao.deleteOldest(count - MAX_HISTORY_COUNT + 1);
                }
                
                // 序列化请求头
                String headersJson = gson.toJson(request.getHeaders());
                
                RequestHistory history = new RequestHistory(
                        request.getUrl(),
                        request.getMethod(),
                        headersJson,
                        request.getBody(),
                        response.getStatusCode(),
                        response.getStatusMessage(),
                        response.getDuration(),
                        System.currentTimeMillis()
                );
                
                historyDao.insert(history);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void getAllHistory(HistoryCallback callback) {
        executor.execute(() -> {
            try {
                List<RequestHistory> historyList = historyDao.getAllHistory();
                callback.onSuccess(historyList);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void deleteHistory(long id, DeleteCallback callback) {
        executor.execute(() -> {
            try {
                historyDao.deleteById(id);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void clearAllHistory(DeleteCallback callback) {
        executor.execute(() -> {
            try {
                historyDao.deleteAll();
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public List<RequestHeader> parseHeaders(String headersJson) {
        Type type = new TypeToken<List<RequestHeader>>(){}.getType();
        return gson.fromJson(headersJson, type);
    }

    public interface HistoryCallback {
        void onSuccess(List<RequestHistory> historyList);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
