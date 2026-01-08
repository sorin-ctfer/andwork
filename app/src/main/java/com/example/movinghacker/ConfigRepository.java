package com.example.movinghacker;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConfigRepository {
    private static final String PREFS_NAME = "web_request_config";
    private static final String KEY_HEADERS = "saved_headers";

    private final SharedPreferences prefs;
    private final Gson gson;

    public ConfigRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveHeaders(List<RequestHeader> headers) {
        String json = gson.toJson(headers);
        prefs.edit().putString(KEY_HEADERS, json).apply();
    }

    public List<RequestHeader> loadHeaders() {
        String json = prefs.getString(KEY_HEADERS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        Type listType = new TypeToken<List<RequestHeader>>() {}.getType();
        List<RequestHeader> headers = gson.fromJson(json, listType);
        return headers != null ? headers : new ArrayList<>();
    }

    public void clearHeaders() {
        prefs.edit().remove(KEY_HEADERS).apply();
    }
}
