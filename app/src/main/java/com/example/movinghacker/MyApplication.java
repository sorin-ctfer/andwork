package com.example.movinghacker;

import android.app.Application;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化 Python（必须在 Application 中初始化）
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }
}
