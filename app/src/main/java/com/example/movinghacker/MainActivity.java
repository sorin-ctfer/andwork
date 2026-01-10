package com.example.movinghacker;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 监听返回栈变化以更新返回按钮
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
            }
        });

        // 处理工具栏返回按钮点击
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (savedInstanceState == null) {
            // 启动时显示主页
            navigateToHome();
        }
    }

    public void navigateToHome() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    public void navigateToModule(String moduleId) {
        Fragment fragment;
        switch (moduleId) {
            case "ai_chat":
                fragment = new AIChatFragment();
                break;
            case "id_card":
                fragment = new IdCardFragment();
                break;
            case "web_request":
                fragment = new WebRequestFragment();
                break;
            case "file_editor":
                fragment = new FileEditorFragment();
                break;
            case "file_manager":
                fragment = new FileManagerFragment();
                break;
            case "dual_file_manager":
                fragment = new DualPaneFileManagerFragment();
                break;
            case "python":
                fragment = new PythonEditorFragment();
                break;
            case "terminal":
                fragment = new TerminalFragment();
                break;
            case "ssh":
                fragment = new SSHListFragment();
                break;
            default:
                return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
