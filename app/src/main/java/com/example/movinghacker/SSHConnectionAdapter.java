package com.example.movinghacker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class SSHConnectionAdapter extends RecyclerView.Adapter<SSHConnectionAdapter.ViewHolder> {
    private List<SSHConnectionManager.SSHConnection> connections;
    private final OnConnectionClickListener clickListener;
    private final OnConnectionDeleteListener deleteListener;

    public interface OnConnectionClickListener {
        void onConnectionClick(SSHConnectionManager.SSHConnection connection);
    }

    public interface OnConnectionDeleteListener {
        void onConnectionDelete(SSHConnectionManager.SSHConnection connection);
    }

    public SSHConnectionAdapter(List<SSHConnectionManager.SSHConnection> connections,
                               OnConnectionClickListener clickListener,
                               OnConnectionDeleteListener deleteListener) {
        this.connections = connections;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ssh_connection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SSHConnectionManager.SSHConnection connection = connections.get(position);
        holder.bind(connection);
    }

    @Override
    public int getItemCount() {
        return connections.size();
    }

    public void updateConnections(List<SSHConnectionManager.SSHConnection> newConnections) {
        this.connections = newConnections;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView connectionName;
        private final TextView connectionInfo;
        private final MaterialButton btnConnect;
        private final MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            connectionName = itemView.findViewById(R.id.connection_name);
            connectionInfo = itemView.findViewById(R.id.connection_info);
            btnConnect = itemView.findViewById(R.id.btn_connect);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(SSHConnectionManager.SSHConnection connection) {
            connectionName.setText(connection.name);
            connectionInfo.setText(connection.username + "@" + connection.host + ":" + connection.port);

            btnConnect.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onConnectionClick(connection);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onConnectionDelete(connection);
                }
            });
        }
    }
}
