package com.example.movinghacker;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Color;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<FileItem> fileList;
    private OnFileClickListener listener;
    private OnFileDragListener dragListener;
    private boolean dragEnabled = false;

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
    }
    
    public interface OnFileDragListener {
        void onDragStarted(FileItem item);
        void onDragEnded();
    }

    public FileAdapter(OnFileClickListener listener) {
        this.fileList = new ArrayList<>();
        this.listener = listener;
    }
    
    public void setDragEnabled(boolean enabled) {
        this.dragEnabled = enabled;
    }
    
    public void setDragListener(OnFileDragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setFiles(List<FileItem> files) {
        this.fileList = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem item = fileList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView nameView;
        TextView infoView;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.file_icon);
            nameView = itemView.findViewById(R.id.file_name);
            infoView = itemView.findViewById(R.id.file_info);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFileClick(fileList.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (dragEnabled) {
                        // 启动拖放
                        startDrag(position);
                        return true;
                    } else if (listener != null) {
                        // 普通长按
                        listener.onFileLongClick(fileList.get(position));
                        return true;
                    }
                }
                return false;
            });
        }
        
        private void startDrag(int position) {
            FileItem item = fileList.get(position);
            
            // 创建拖放数据
            ClipData.Item clipItem = new ClipData.Item(item.getFile().getAbsolutePath());
            ClipData dragData = new ClipData(
                item.getName(),
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                clipItem
            );
            
            // 创建拖放阴影
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(itemView);
            
            // 开始拖放
            itemView.startDragAndDrop(dragData, shadowBuilder, item, 0);
            
            // 设置半透明
            itemView.setAlpha(0.5f);
            
            // 通知拖放开始
            if (dragListener != null) {
                dragListener.onDragStarted(item);
            }
            
            // 监听拖放结束
            itemView.setOnDragListener((v, event) -> {
                if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    // 恢复透明度
                    itemView.setAlpha(1.0f);
                    
                    // 通知拖放结束
                    if (dragListener != null) {
                        dragListener.onDragEnded();
                    }
                    
                    // 移除监听器
                    itemView.setOnDragListener(null);
                }
                return true;
            });
        }

        void bind(FileItem item) {
            iconView.setImageResource(item.getIconResource());
            nameView.setText(item.getName());
            
            String info = item.getFormattedSize();
            if (!info.isEmpty()) {
                info += "  •  ";
            }
            info += item.getFormattedDate();
            infoView.setText(info);

            // 重置透明度
            itemView.setAlpha(1.0f);
            
            // 选中状态
            itemView.setBackgroundColor(item.isSelected() ? 
                    0x20000000 : 0x00000000);
        }
    }
}
