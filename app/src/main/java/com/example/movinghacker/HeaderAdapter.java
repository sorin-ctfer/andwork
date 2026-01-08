package com.example.movinghacker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder> {

    private final List<RequestHeader> headers;
    private final OnHeaderActionListener listener;

    public interface OnHeaderActionListener {
        void onDeleteHeader(int position);
        void onEditHeader(int position);
    }

    public HeaderAdapter(List<RequestHeader> headers, OnHeaderActionListener listener) {
        this.headers = headers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_header, parent, false);
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        RequestHeader header = headers.get(position);
        holder.bind(header, position, listener);
    }

    @Override
    public int getItemCount() {
        return headers.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView keyView;
        private final TextView valueView;
        private final ImageView deleteButton;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            keyView = itemView.findViewById(R.id.header_key);
            valueView = itemView.findViewById(R.id.header_value);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(RequestHeader header, int position, OnHeaderActionListener listener) {
            keyView.setText(header.getKey());
            valueView.setText(header.getValue());

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteHeader(position);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditHeader(position);
                }
            });
        }
    }
}
