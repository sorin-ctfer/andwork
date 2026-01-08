package com.example.movinghacker;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH连接管理器 - 保存和加载SSH连接配置
 */
public class SSHConnectionManager {
    private static final String PREFS_NAME = "ssh_connections";
    private static final String KEY_CONNECTIONS = "connections";
    private static final String KEY_LAST_CONNECTION = "last_connection";
    
    private final SharedPreferences prefs;
    private final Gson gson;
    
    public static class SSHConnection {
        public String name;
        public String host;
        public int port;
        public String username;
        public String password;  // 注意：实际应用中应该加密存储
        
        public SSHConnection(String name, String host, int port, String username, String password) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }
        
        @Override
        public String toString() {
            return name + " (" + username + "@" + host + ":" + port + ")";
        }
    }
    
    public SSHConnectionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    /**
     * 保存连接配置
     */
    public void saveConnection(SSHConnection connection) {
        List<SSHConnection> connections = getConnections();
        
        // 检查是否已存在同名连接
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).name.equals(connection.name)) {
                connections.set(i, connection);
                saveConnections(connections);
                return;
            }
        }
        
        // 添加新连接
        connections.add(connection);
        saveConnections(connections);
    }
    
    /**
     * 获取所有连接配置
     */
    public List<SSHConnection> getConnections() {
        String json = prefs.getString(KEY_CONNECTIONS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<SSHConnection>>(){}.getType();
        return gson.fromJson(json, type);
    }
    
    /**
     * 删除连接配置
     */
    public void deleteConnection(String name) {
        List<SSHConnection> connections = getConnections();
        connections.removeIf(conn -> conn.name.equals(name));
        saveConnections(connections);
    }
    
    /**
     * 保存最后使用的连接
     */
    public void saveLastConnection(SSHConnection connection) {
        String json = gson.toJson(connection);
        prefs.edit().putString(KEY_LAST_CONNECTION, json).apply();
    }
    
    /**
     * 获取最后使用的连接
     */
    public SSHConnection getLastConnection() {
        String json = prefs.getString(KEY_LAST_CONNECTION, null);
        if (json == null) {
            return null;
        }
        return gson.fromJson(json, SSHConnection.class);
    }
    
    private void saveConnections(List<SSHConnection> connections) {
        String json = gson.toJson(connections);
        prefs.edit().putString(KEY_CONNECTIONS, json).apply();
    }
}
