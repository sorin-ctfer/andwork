package com.example.movinghacker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.movinghacker.ai.AIConfig;
import com.example.movinghacker.ai.AIConfigManager;
import com.example.movinghacker.ai.AIResponse;
import com.example.movinghacker.ai.AIService;
import com.example.movinghacker.ai.ChatHistoryRepository;
import com.example.movinghacker.ai.ChatMessage;
import com.example.movinghacker.ai.FunctionCall;
import com.example.movinghacker.ai.FunctionResult;

import java.util.ArrayList;
import java.util.List;

/**
 * AI聊天ViewModel
 */
public class AIChatViewModel extends AndroidViewModel {
    
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    private final AIConfigManager configManager;
    private final ChatHistoryRepository historyRepository;
    private final AIService aiService;
    private AIService.StopToken activeStopToken;
    private Thread activeThread;

    public AIChatViewModel(@NonNull Application application) {
        super(application);
        
        configManager = AIConfigManager.getInstance(application);
        historyRepository = ChatHistoryRepository.getInstance(application);
        aiService = new AIService(application);
        
        // 加载历史记录
        loadHistory();
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    private void loadHistory() {
        List<ChatMessage> history = historyRepository.loadHistory();
        messages.setValue(history);
    }

    public void sendMessage(String content) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        // 创建用户消息
        ChatMessage userMessage = ChatMessage.userMessage(content);
        
        List<ChatMessage> current = messages.getValue() != null ? messages.getValue() : new ArrayList<>();
        final List<ChatMessage> conversation = new ArrayList<>(current);
        conversation.add(userMessage);
        messages.setValue(new ArrayList<>(conversation));
        
        // 保存历史记录
        historyRepository.saveHistory(conversation);
        
        // 设置加载状态
        loading.setValue(true);
        activeStopToken = new AIService.StopToken();
        
        // 调用AI服务
        activeThread = aiService.chatWithFunctionHandling(conversation, activeStopToken, new AIService.ChatCallback() {
            @Override
            public void onSuccess(AIResponse response) {
                loading.postValue(false);
                activeStopToken = null;
                activeThread = null;
                
                // 添加AI回复
                if (response.hasContent()) {
                    ChatMessage assistantMessage = ChatMessage.assistantMessage(response.getContent());
                    
                    synchronized (conversation) {
                        conversation.add(assistantMessage);
                        // 更新UI显示所有消息（包括function调用和结果）
                        messages.postValue(new ArrayList<>(conversation));
                    }
                    
                    // 保存历史
                    historyRepository.saveHistory(conversation);
                }
            }

            @Override
            public void onError(Exception e) {
                loading.postValue(false);
                activeStopToken = null;
                activeThread = null;
                error.postValue("错误: " + e.getMessage());
                
                // 添加错误消息
                ChatMessage errorMessage = ChatMessage.errorMessage("抱歉，发生了错误: " + e.getMessage());
                
                synchronized (conversation) {
                    conversation.add(errorMessage);
                    messages.postValue(new ArrayList<>(conversation));
                }
                historyRepository.saveHistory(conversation);
            }

            @Override
            public void onFunctionCallsRequested(List<FunctionCall> calls) {
                // Function调用信息已在onThinking中显示
                // 更新UI以显示function call消息
                synchronized (conversation) {
                    messages.postValue(new ArrayList<>(conversation));
                }
            }

            @Override
            public void onFunctionExecuted(List<FunctionResult> results) {
                // Function执行结果已在onThinking中显示
                // 更新UI以显示function result消息
                synchronized (conversation) {
                    messages.postValue(new ArrayList<>(conversation));
                }
            }

            @Override
            public void onThinking(String message, String thinkingType) {
                // 添加思考过程消息
                ChatMessage thinkingMessage = ChatMessage.thinkingMessage(message, thinkingType);
                
                synchronized (conversation) {
                    conversation.add(thinkingMessage);
                    // 实时更新UI显示思考过程
                    messages.postValue(new ArrayList<>(conversation));
                }
            }

            @Override
            public void onContextSummary(String summary) {
                // 添加上下文总结消息
                ChatMessage summaryMessage = ChatMessage.summaryMessage(summary);
                
                synchronized (conversation) {
                    conversation.add(summaryMessage);
                    messages.postValue(new ArrayList<>(conversation));
                }
                
                // 保存历史（包含总结）
                historyRepository.saveHistory(conversation);
            }
        });
    }

    public void stopGeneration() {
        if (activeStopToken != null) {
            activeStopToken.stop();
        }
        if (activeThread != null) {
            activeThread.interrupt();
        }
    }

    public void saveConfig(AIConfig config) {
        configManager.saveConfig(config);
        aiService.updateConfig(config);
    }

    public void clearHistory() {
        historyRepository.clearHistory();
        messages.setValue(new ArrayList<>());
    }
}
