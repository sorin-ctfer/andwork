package com.example.movinghacker;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebRequestViewModel extends AndroidViewModel {

    private final MutableLiveData<List<RequestHeader>> headers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<HttpResponse> response = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    private final HttpRequestExecutor requestExecutor;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConfigRepository configRepository;
    private final HistoryRepository historyRepository;

    public WebRequestViewModel(@NonNull Application application) {
        super(application);
        requestExecutor = new HttpRequestExecutor();
        requestExecutor.setContext(application);
        configRepository = new ConfigRepository(application);
        historyRepository = new HistoryRepository(application);
        
        // 加载保存的请求头
        List<RequestHeader> savedHeaders = configRepository.loadHeaders();
        headers.setValue(savedHeaders);
    }

    public LiveData<List<RequestHeader>> getHeaders() {
        return headers;
    }

    public LiveData<HttpResponse> getResponse() {
        return response;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void addHeader(String key, String value) {
        List<RequestHeader> currentHeaders = headers.getValue();
        if (currentHeaders != null) {
            currentHeaders.add(new RequestHeader(key, value));
            headers.setValue(currentHeaders);
            saveHeaders();
        }
    }

    public void updateHeader(int position, String key, String value) {
        List<RequestHeader> currentHeaders = headers.getValue();
        if (currentHeaders != null && position >= 0 && position < currentHeaders.size()) {
            currentHeaders.get(position).setKey(key);
            currentHeaders.get(position).setValue(value);
            headers.setValue(currentHeaders);
            saveHeaders();
        }
    }

    public void removeHeader(int position) {
        List<RequestHeader> currentHeaders = headers.getValue();
        if (currentHeaders != null && position >= 0 && position < currentHeaders.size()) {
            currentHeaders.remove(position);
            headers.setValue(currentHeaders);
            saveHeaders();
        }
    }

    public void sendRequest(String url, String method, String body, Uri fileUri) {
        loading.setValue(true);
        error.setValue(null);
        
        executor.execute(() -> {
            try {
                List<RequestHeader> currentHeaders = headers.getValue();
                if (currentHeaders == null) {
                    currentHeaders = new ArrayList<>();
                }
                
                HttpRequest request = new HttpRequest(url, method, new ArrayList<>(currentHeaders), body, fileUri);
                HttpResponse httpResponse = requestExecutor.execute(request);
                
                response.postValue(httpResponse);
                loading.postValue(false);
                
                // 保存到历史记录
                historyRepository.saveRequest(request, httpResponse);
                
            } catch (Exception e) {
                error.postValue(e.getMessage());
                loading.postValue(false);
            }
        });
    }

    private void saveHeaders() {
        List<RequestHeader> currentHeaders = headers.getValue();
        if (currentHeaders != null) {
            configRepository.saveHeaders(currentHeaders);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
