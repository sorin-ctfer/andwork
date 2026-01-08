package com.example.movinghacker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView moduleGrid;
    private ModuleAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        moduleGrid = view.findViewById(R.id.module_grid);
        moduleGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<ModuleItem> modules = Arrays.asList(
                new ModuleItem("id_card", "身份证识别",
                        android.R.drawable.ic_menu_camera, "扫描和识别身份证信息"),
                new ModuleItem("web_request", "高级网页请求",
                        android.R.drawable.ic_menu_search, "自定义HTTP请求工具"),
                new ModuleItem("file_editor", "文件编辑器",
                        android.R.drawable.ic_menu_edit, "代码编辑器，支持多种语言"),
                new ModuleItem("dual_file_manager", "文件管理器",
                        android.R.drawable.ic_menu_sort_by_size, "双屏/单屏文件管理，支持搜索"),
                new ModuleItem("python", "Python IDE",
                        android.R.drawable.ic_menu_manage, "Python开发环境和包管理"),
                new ModuleItem("terminal", "Linux终端",
                        android.R.drawable.ic_dialog_info, "命令行终端，支持Shell命令"),
                new ModuleItem("ssh", "SSH客户端",
                        android.R.drawable.ic_menu_share, "远程SSH连接和管理")
        );

        adapter = new ModuleAdapter(modules, this::onModuleClick);
        moduleGrid.setAdapter(adapter);
    }

    private void onModuleClick(ModuleItem module) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToModule(module.getId());
        }
    }
}
