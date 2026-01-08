package com.example.movinghacker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder> {

    private final List<ModuleItem> modules;
    private final OnModuleClickListener listener;

    public interface OnModuleClickListener {
        void onModuleClick(ModuleItem module);
    }

    public ModuleAdapter(List<ModuleItem> modules, OnModuleClickListener listener) {
        this.modules = modules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_module, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        ModuleItem module = modules.get(position);
        holder.bind(module, listener);
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView nameView;
        private final TextView descriptionView;

        public ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.module_icon);
            nameView = itemView.findViewById(R.id.module_name);
            descriptionView = itemView.findViewById(R.id.module_description);
        }

        public void bind(ModuleItem module, OnModuleClickListener listener) {
            iconView.setImageResource(module.getIconResId());
            nameView.setText(module.getName());
            descriptionView.setText(module.getDescription());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onModuleClick(module);
                }
            });
        }
    }
}
