package com.example.movinghacker;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.movinghacker.ai.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息列表适配器
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;
    private static final int VIEW_TYPE_THINKING = 3;
    private static final int VIEW_TYPE_SUMMARY = 4;
    
    private List<ChatMessage> messages = new ArrayList<>();

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isUser()) {
            return VIEW_TYPE_USER;
        } else if (message.isThinking()) {
            return VIEW_TYPE_THINKING;
        } else if (message.isSummary()) {
            return VIEW_TYPE_SUMMARY;
        } else {
            return VIEW_TYPE_ASSISTANT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_THINKING) {
            View view = inflater.inflate(R.layout.item_message_thinking, parent, false);
            return new ThinkingMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_SUMMARY) {
            View view = inflater.inflate(R.layout.item_message_summary, parent, false);
            return new SummaryMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AssistantMessageViewHolder) {
            ((AssistantMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ThinkingMessageViewHolder) {
            ((ThinkingMessageViewHolder) holder).bind(message);
        } else if (holder instanceof SummaryMessageViewHolder) {
            ((SummaryMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * 用户消息ViewHolder
     */
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageContent;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.message_content);
        }

        public void bind(ChatMessage message) {
            messageContent.setText(message.getContent());
        }
    }

    /**
     * AI助手消息ViewHolder
     */
    static class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageContent;
        private ImageButton copyButton;

        public AssistantMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.message_content);
            copyButton = itemView.findViewById(R.id.copy_button);
        }

        public void bind(ChatMessage message) {
            messageContent.setText(message.getContent());
            
            // 如果是错误消息，可以设置不同的样式
            if (message.isError()) {
                messageContent.setTextColor(
                    itemView.getContext().getColor(android.R.color.holo_red_dark));
            } else {
                messageContent.setTextColor(
                    itemView.getContext().getColor(R.color.app_text));
            }

            View.OnClickListener copyListener = v -> copyToClipboard(message.getContent());
            if (copyButton != null) {
                copyButton.setOnClickListener(copyListener);
            }
            messageContent.setOnLongClickListener(v -> {
                copyToClipboard(message.getContent());
                return true;
            });
        }

        private void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) itemView.getContext()
                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("ai_message", text != null ? text : "");
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(itemView.getContext(), R.string.copied, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 思考过程消息ViewHolder
     */
    static class ThinkingMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView thinkingContent;
        private TextView thinkingType;

        public ThinkingMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            thinkingContent = itemView.findViewById(R.id.thinking_content);
            thinkingType = itemView.findViewById(R.id.thinking_type);
        }

        public void bind(ChatMessage message) {
            thinkingContent.setText(message.getContent());
            
            // 设置思考类型标签
            String type = message.getThinkingType();
            if (type != null) {
                switch (type) {
                    case "function_call":
                        thinkingType.setText("🔧 调用功能");
                        break;
                    case "code_generation":
                        thinkingType.setText("💻 生成代码");
                        break;
                    case "analysis":
                        thinkingType.setText("🔍 分析中");
                        break;
                    case "execution":
                        thinkingType.setText("⚡ 执行中");
                        break;
                    default:
                        thinkingType.setText("💭 思考中");
                }
            } else {
                thinkingType.setText("💭 思考中");
            }
        }
    }

    /**
     * 上下文总结消息ViewHolder
     */
    static class SummaryMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView summaryContent;

        public SummaryMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            summaryContent = itemView.findViewById(R.id.summary_content);
        }

        public void bind(ChatMessage message) {
            summaryContent.setText(message.getContent());
        }
    }
}
