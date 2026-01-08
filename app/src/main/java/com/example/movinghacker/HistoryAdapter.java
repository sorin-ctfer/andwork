package com.example.movinghacker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<RequestHistory> historyList;
    private final OnHistoryActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnHistoryActionListener {
        void onHistoryClick(RequestHistory history);
        void onDeleteClick(RequestHistory history);
    }

    public HistoryAdapter(List<RequestHistory> historyList, OnHistoryActionListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        RequestHistory history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView methodText;
        private final TextView statusText;
        private final TextView urlText;
        private final TextView timeText;
        private final TextView durationText;
        private final MaterialButton deleteButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            methodText = itemView.findViewById(R.id.method_text);
            statusText = itemView.findViewById(R.id.status_text);
            urlText = itemView.findViewById(R.id.url_text);
            timeText = itemView.findViewById(R.id.time_text);
            durationText = itemView.findViewById(R.id.duration_text);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(RequestHistory history) {
            methodText.setText(history.getMethod());
            statusText.setText(String.valueOf(history.getStatusCode()));
            urlText.setText(history.getUrl());
            timeText.setText(dateFormat.format(new Date(history.getTimestamp())));
            durationText.setText(history.getDuration() + " ms");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryClick(history);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(history);
                }
            });
        }
    }
}
