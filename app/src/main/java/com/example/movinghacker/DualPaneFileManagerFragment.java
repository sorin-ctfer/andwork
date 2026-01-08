package com.example.movinghacker;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DualPaneFileManagerFragment extends Fragment {

    private LinearLayout dualPaneContainer;
    private LinearLayout singlePaneContainer;
    
    private RecyclerView leftFileList;
    private RecyclerView rightFileList;
    private RecyclerView singleFileList;
    
    private TextView leftPathText;
    private TextView rightPathText;
    private TextView singlePathText;
    
    private FloatingActionButton fabMenu;
    private FloatingActionButton fabToggleView;
    private FloatingActionButton fabSearch;
    private FloatingActionButton fabSshToggle;
    
    private LinearLayout dragActionZone;
    private FileItem draggedItem = null;
    
    private View rightPreviewContainer;
    private TextView previewText;
    private ImageView previewImage;
    
    private FileAdapter leftAdapter;
    private FileAdapter rightAdapter;
    private FileAdapter singleAdapter;
    
    private File leftCurrentDirectory;
    private File rightCurrentDirectory;
    private File singleCurrentDirectory;
    
    private boolean isLeftPanelActive = true;
    private boolean isDualPaneMode = true;
    private FileItem selectedFileForPreview = null;
    
    private File appDirectory; // 应用默认目录
    
    // SSH远程文件管理
    private RemoteFileManager remoteFileManager;
    private boolean isLeftPanelRemote = false;
    private boolean isRightPanelRemote = false;
    private String leftRemotePath = "/";
    private String rightRemotePath = "/";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_manager_dual, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 监听SSH连接选择结果
        getParentFragmentManager().setFragmentResultListener(
            "ssh_connection_selected", 
            this, 
            (requestKey, result) -> {
                String host = result.getString("host");
                int port = result.getInt("port");
                String username = result.getString("username");
                String password = result.getString("password");
                String name = result.getString("name");
                
                connectToSSH(host, port, username, password, name);
            }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupAdapters();
        setupListeners();
        
        // 设置应用默认目录
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        appDirectory = new File(documentsDir, "MovingHacker");
        if (!appDirectory.exists()) {
            appDirectory.mkdirs();
        }
        
        // 初始化为应用目录
        navigateLeftPanel(appDirectory);
        navigateRightPanel(appDirectory);
        navigateSinglePanel(appDirectory);
    }

    private void initializeViews(View view) {
        dualPaneContainer = view.findViewById(R.id.dual_pane_container);
        singlePaneContainer = view.findViewById(R.id.single_pane_container);
        
        leftFileList = view.findViewById(R.id.left_file_list);
        rightFileList = view.findViewById(R.id.right_file_list);
        singleFileList = view.findViewById(R.id.single_file_list);
        
        leftPathText = view.findViewById(R.id.left_path_text);
        rightPathText = view.findViewById(R.id.right_path_text);
        singlePathText = view.findViewById(R.id.single_path_text);
        
        fabMenu = view.findViewById(R.id.fab_menu);
        fabToggleView = view.findViewById(R.id.fab_toggle_view);
        fabSearch = view.findViewById(R.id.fab_search);
        fabSshToggle = view.findViewById(R.id.fab_ssh_toggle);
        
        dragActionZone = view.findViewById(R.id.drag_action_zone);
        
        rightPreviewContainer = view.findViewById(R.id.right_preview_container);
        previewText = view.findViewById(R.id.preview_text);
        previewImage = view.findViewById(R.id.preview_image);
        
        leftFileList.setLayoutManager(new LinearLayoutManager(getContext()));
        rightFileList.setLayoutManager(new LinearLayoutManager(getContext()));
        singleFileList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupAdapters() {
        // 左侧面板适配器
        leftAdapter = new FileAdapter(new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(FileItem item) {
                isLeftPanelActive = true;
                handleLeftPanelClick(item);
            }

            @Override
            public void onFileLongClick(FileItem item) {
                isLeftPanelActive = true;
                showFileOptions(item, true);
            }
        });
        leftAdapter.setDragEnabled(true);
        leftAdapter.setDragListener(new FileAdapter.OnFileDragListener() {
            @Override
            public void onDragStarted(FileItem item) {
                draggedItem = item;
                dragActionZone.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDragEnded() {
                draggedItem = null;
                dragActionZone.setVisibility(View.GONE);
            }
        });
        leftFileList.setAdapter(leftAdapter);
        
        // 右侧面板适配器
        rightAdapter = new FileAdapter(new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(FileItem item) {
                isLeftPanelActive = false;
                handleRightPanelClick(item);
            }

            @Override
            public void onFileLongClick(FileItem item) {
                isLeftPanelActive = false;
                showFileOptions(item, false);
            }
        });
        rightAdapter.setDragEnabled(true);
        rightAdapter.setDragListener(new FileAdapter.OnFileDragListener() {
            @Override
            public void onDragStarted(FileItem item) {
                draggedItem = item;
                dragActionZone.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDragEnded() {
                draggedItem = null;
                dragActionZone.setVisibility(View.GONE);
            }
        });
        rightFileList.setAdapter(rightAdapter);
        
        // 单屏适配器
        singleAdapter = new FileAdapter(new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(FileItem item) {
                handleSinglePanelClick(item);
            }

            @Override
            public void onFileLongClick(FileItem item) {
                showFileOptions(item, true);
            }
        });
        singleFileList.setAdapter(singleAdapter);
        
        // 设置拖放监听器
        setupDragListeners();
    }

    private void setupListeners() {
        fabMenu.setOnClickListener(v -> showMainMenu());
        fabToggleView.setOnClickListener(v -> toggleViewMode());
        fabSearch.setOnClickListener(v -> showSearchDialog());
        fabSshToggle.setOnClickListener(v -> toggleSSHMode());
        
        // 设置拖放操作区域的监听器
        setupDragActionZone();
    }
    
    private void setupDragActionZone() {
        dragActionZone.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.DragEvent.ACTION_DRAG_STARTED:
                    return true;
                    
                case android.view.DragEvent.ACTION_DRAG_ENTERED:
                    // 拖动进入操作区域，高亮显示
                    dragActionZone.setBackgroundColor(0xC0FF6B00); // 橙色高亮
                    return true;
                    
                case android.view.DragEvent.ACTION_DRAG_EXITED:
                    // 拖动离开操作区域，恢复原色
                    dragActionZone.setBackgroundColor(0x80000000);
                    return true;
                    
                case android.view.DragEvent.ACTION_DROP:
                    // 文件被放到操作区域，打开文件菜单
                    dragActionZone.setBackgroundColor(0x80000000);
                    
                    if (draggedItem != null) {
                        // 隐藏操作区域
                        dragActionZone.setVisibility(View.GONE);
                        
                        // 打开文件操作菜单
                        showFileOptions(draggedItem, isLeftPanelActive);
                    }
                    return true;
                    
                case android.view.DragEvent.ACTION_DRAG_ENDED:
                    // 拖放结束，恢复原色
                    dragActionZone.setBackgroundColor(0x80000000);
                    return true;
                    
                default:
                    return false;
            }
        });
    }
    
    private void toggleViewMode() {
        isDualPaneMode = !isDualPaneMode;
        
        if (isDualPaneMode) {
            // 切换到双屏模式
            dualPaneContainer.setVisibility(View.VISIBLE);
            singlePaneContainer.setVisibility(View.GONE);
            Toast.makeText(getContext(), "双屏模式", Toast.LENGTH_SHORT).show();
        } else {
            // 切换到单屏模式
            dualPaneContainer.setVisibility(View.GONE);
            singlePaneContainer.setVisibility(View.VISIBLE);
            // 同步当前目录到单屏
            navigateSinglePanel(leftCurrentDirectory);
            Toast.makeText(getContext(), "单屏模式", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleSinglePanelClick(FileItem item) {
        if (item.isDirectory()) {
            navigateSinglePanel(item.getFile());
        } else {
            showFileOptions(item, true);
        }
    }
    
    private void navigateSinglePanel(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Toast.makeText(getContext(), "无法访问目录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!directory.canRead()) {
            Toast.makeText(getContext(), "没有读取权限", Toast.LENGTH_SHORT).show();
            return;
        }

        singleCurrentDirectory = directory;
        singlePathText.setText(directory.getAbsolutePath());
        loadFiles(singleCurrentDirectory, singleAdapter);
    }

    private void handleLeftPanelClick(FileItem item) {
        if (item.isDirectory()) {
            // 左侧点击文件夹：只在左侧进入该文件夹
            navigateLeftPanel(item.getFile());
            
            // 如果右侧是本地模式且没有显示预览，也同步导航
            if (!isRightPanelRemote && rightPreviewContainer.getVisibility() == View.GONE) {
                navigateRightPanel(item.getFile());
            }
        } else {
            // 左侧点击文件：右侧显示预览（仅在本地模式）
            if (!isRightPanelRemote) {
                showFilePreview(item);
            }
        }
    }

    private void handleRightPanelClick(FileItem item) {
        if (isRightPanelRemote) {
            // 远程文件点击
            if (item.getName().equals("..")) {
                // 返回上级目录
                navigateRemoteUp(false);
            } else if (item.isDirectory()) {
                // 进入远程目录
                String newPath = rightRemotePath.endsWith("/") ? 
                    rightRemotePath + item.getName() : 
                    rightRemotePath + "/" + item.getName();
                rightRemotePath = newPath;
                rightPathText.setText("SSH: " + remoteFileManager.getConnectionInfo() + ":" + rightRemotePath);
                loadRemoteFiles(rightRemotePath, rightAdapter, false);
            } else {
                // 显示远程文件选项
                showRemoteFileOptions(item, false);
            }
        } else {
            // 本地文件点击（原有逻辑）
            if (item.isDirectory()) {
                navigateRightPanel(item.getFile());
            } else {
                showFilePreview(item);
            }
        }
    }

    private void navigateLeftPanel(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Toast.makeText(getContext(), "无法访问目录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!directory.canRead()) {
            Toast.makeText(getContext(), "没有读取权限", Toast.LENGTH_SHORT).show();
            return;
        }

        leftCurrentDirectory = directory;
        leftPathText.setText(directory.getAbsolutePath());
        loadFiles(leftCurrentDirectory, leftAdapter);
    }

    private void navigateRightPanel(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Toast.makeText(getContext(), "无法访问目录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!directory.canRead()) {
            Toast.makeText(getContext(), "没有读取权限", Toast.LENGTH_SHORT).show();
            return;
        }

        rightCurrentDirectory = directory;
        rightPathText.setText(directory.getAbsolutePath());
        
        // 隐藏预览，显示文件列表
        hidePreview();
        loadFiles(rightCurrentDirectory, rightAdapter);
    }

    private void loadFiles(File directory, FileAdapter adapter) {
        File[] files = directory.listFiles();
        if (files == null) {
            adapter.setFiles(new ArrayList<>());
            return;
        }

        List<FileItem> fileItems = new ArrayList<>();
        
        // 添加返回上级目录选项
        if (directory.getParent() != null) {
            File parentFile = directory.getParentFile();
            FileItem parentItem = new FileItem(parentFile);
            fileItems.add(parentItem);
        }
        
        for (File file : files) {
            // 跳过隐藏文件
            if (file.getName().startsWith(".")) {
                continue;
            }
            fileItems.add(new FileItem(file));
        }

        // 排序：文件夹在前，然后按名称排序
        Collections.sort(fileItems, (a, b) -> {
            // 父目录永远在最前
            if (a.getName().equals("..")) return -1;
            if (b.getName().equals("..")) return 1;
            
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            } else if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            } else {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        adapter.setFiles(fileItems);
    }

    private void showFilePreview(FileItem item) {
        selectedFileForPreview = item;
        String ext = item.getFileExtension().toLowerCase();
        
        // 隐藏文件列表，显示预览
        rightFileList.setVisibility(View.GONE);
        rightPreviewContainer.setVisibility(View.VISIBLE);
        
        // 根据文件类型显示预览
        boolean isImage = ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || 
                         ext.equals("gif") || ext.equals("bmp");
        boolean isText = ext.equals("txt") || ext.equals("log") || ext.equals("md") || 
                        ext.equals("java") || ext.equals("xml") || ext.equals("json") || 
                        ext.equals("js") || ext.equals("py") || ext.equals("c") || ext.equals("cpp");
        
        if (isImage) {
            showImagePreview(item);
        } else if (isText) {
            showTextPreview(item);
        } else {
            // 不支持的类型，显示文件信息
            showFileInfo(item);
            hidePreview();
        }
    }

    private void showImagePreview(FileItem item) {
        previewText.setVisibility(View.GONE);
        previewImage.setVisibility(View.VISIBLE);
        
        try {
            previewImage.setImageURI(android.net.Uri.fromFile(item.getFile()));
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法预览图片: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            hidePreview();
        }
    }

    private void showTextPreview(FileItem item) {
        previewImage.setVisibility(View.GONE);
        previewText.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            try {
                FileInputStream fis = new FileInputStream(item.getFile());
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder content = new StringBuilder();
                String line;
                int lineCount = 0;
                
                // 最多读取1000行
                while ((line = reader.readLine()) != null && lineCount < 1000) {
                    content.append(line).append("\n");
                    lineCount++;
                }
                reader.close();
                
                if (lineCount >= 1000) {
                    content.append("\n... (文件过大，仅显示前1000行)");
                }
                
                requireActivity().runOnUiThread(() -> {
                    previewText.setText(content.toString());
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "无法读取文件: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    hidePreview();
                });
            }
        }).start();
    }

    private void hidePreview() {
        rightFileList.setVisibility(View.VISIBLE);
        rightPreviewContainer.setVisibility(View.GONE);
        previewText.setVisibility(View.GONE);
        previewImage.setVisibility(View.GONE);
        selectedFileForPreview = null;
    }

    private void showMainMenu() {
        List<String> optionsList = new ArrayList<>();
        optionsList.add("返回上级");
        optionsList.add("刷新");
        optionsList.add("下载文件(wget)");
        optionsList.add("编辑当前预览文件");
        optionsList.add("关闭预览");
        optionsList.add("返回应用目录");
        
        if (isRightPanelRemote || isLeftPanelRemote) {
            optionsList.add("断开SSH连接");
        } else {
            optionsList.add("连接SSH");
        }
        
        String[] options = optionsList.toArray(new String[0]);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("操作菜单")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            navigateUpCurrent();
                            break;
                        case 1:
                            refreshAll();
                            break;
                        case 2:
                            showDownloadDialog();
                            break;
                        case 3:
                            editPreviewedFile();
                            break;
                        case 4:
                            hidePreview();
                            break;
                        case 5:
                            returnToAppDirectory();
                            break;
                        case 6:
                            if (isRightPanelRemote || isLeftPanelRemote) {
                                disconnectSSH();
                            } else {
                                connectSSH();
                            }
                            break;
                    }
                })
                .show();
    }
    
    private void navigateUpCurrent() {
        if (isDualPaneMode) {
            if (isLeftPanelActive) {
                if (isLeftPanelRemote) {
                    navigateRemoteUp(true);
                } else {
                    navigateUp(true);
                }
            } else {
                if (isRightPanelRemote) {
                    navigateRemoteUp(false);
                } else {
                    navigateUp(false);
                }
            }
        } else {
            File parent = singleCurrentDirectory.getParentFile();
            if (parent != null) {
                navigateSinglePanel(parent);
            }
        }
    }
    
    private void refreshAll() {
        if (isDualPaneMode) {
            refreshBothPanels();
        } else {
            loadFiles(singleCurrentDirectory, singleAdapter);
        }
    }
    
    private void refreshBothPanels() {
        // 刷新左侧面板
        if (isLeftPanelRemote) {
            loadRemoteFiles(leftRemotePath, leftAdapter, true);
        } else {
            loadFiles(leftCurrentDirectory, leftAdapter);
        }
        
        // 刷新右侧面板
        if (rightPreviewContainer.getVisibility() == View.GONE) {
            if (isRightPanelRemote) {
                loadRemoteFiles(rightRemotePath, rightAdapter, false);
            } else {
                loadFiles(rightCurrentDirectory, rightAdapter);
            }
        }
    }
    
    private void returnToAppDirectory() {
        if (isDualPaneMode) {
            navigateLeftPanel(appDirectory);
            navigateRightPanel(appDirectory);
        } else {
            navigateSinglePanel(appDirectory);
        }
        Toast.makeText(getContext(), "已返回应用目录", Toast.LENGTH_SHORT).show();
    }
    
    private void showSearchDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_file_search, null);
        android.widget.EditText patternInput = dialogView.findViewById(R.id.search_pattern);
        android.widget.EditText pathInput = dialogView.findViewById(R.id.search_path);
        android.widget.CheckBox recursiveCheck = dialogView.findViewById(R.id.search_recursive);
        android.widget.CheckBox caseSensitiveCheck = dialogView.findViewById(R.id.search_case_sensitive);
        
        // 设置默认搜索路径
        File currentDir = isDualPaneMode ? 
            (isLeftPanelActive ? leftCurrentDirectory : rightCurrentDirectory) : 
            singleCurrentDirectory;
        pathInput.setText(currentDir.getAbsolutePath());
        
        new AlertDialog.Builder(requireContext())
                .setTitle("高级文件搜索 (find)")
                .setView(dialogView)
                .setPositiveButton("搜索", (dialog, which) -> {
                    String pattern = patternInput.getText().toString().trim();
                    String searchPath = pathInput.getText().toString().trim();
                    boolean recursive = recursiveCheck.isChecked();
                    boolean caseSensitive = caseSensitiveCheck.isChecked();
                    
                    if (pattern.isEmpty()) {
                        Toast.makeText(getContext(), "请输入搜索模式", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (searchPath.isEmpty()) {
                        searchPath = currentDir.getAbsolutePath();
                    }
                    
                    performFileSearch(pattern, searchPath, recursive, caseSensitive);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void performFileSearch(String pattern, String searchPath, boolean recursive, boolean caseSensitive) {
        File searchDir = new File(searchPath);
        if (!searchDir.exists() || !searchDir.isDirectory()) {
            Toast.makeText(getContext(), "搜索路径无效", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("搜索中");
        progressDialog.setMessage("正在搜索文件...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            List<File> results = new ArrayList<>();
            searchFiles(searchDir, pattern, recursive, caseSensitive, results);
            
            requireActivity().runOnUiThread(() -> {
                progressDialog.dismiss();
                showSearchResults(results, pattern);
            });
        }).start();
    }
    
    private void searchFiles(File directory, String pattern, boolean recursive, boolean caseSensitive, List<File> results) {
        if (!directory.canRead()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        // 将通配符模式转换为正则表达式
        String regex = pattern.replace(".", "\\.")
                             .replace("*", ".*")
                             .replace("?", ".");
        if (!caseSensitive) {
            regex = "(?i)" + regex;
        }
        
        for (File file : files) {
            if (file.getName().startsWith(".")) {
                continue;
            }
            
            // 检查文件名是否匹配
            if (file.getName().matches(regex)) {
                results.add(file);
            }
            
            // 递归搜索子目录
            if (recursive && file.isDirectory()) {
                searchFiles(file, pattern, recursive, caseSensitive, results);
            }
        }
    }
    
    private void showSearchResults(List<File> results, String pattern) {
        if (results.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("搜索结果")
                    .setMessage("未找到匹配 \"" + pattern + "\" 的文件")
                    .setPositiveButton("确定", null)
                    .show();
            return;
        }
        
        // 创建结果列表
        String[] resultNames = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            File file = results.get(i);
            resultNames[i] = file.getAbsolutePath();
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("搜索结果 (" + results.size() + " 个文件)")
                .setItems(resultNames, (dialog, which) -> {
                    File selectedFile = results.get(which);
                    if (selectedFile.isDirectory()) {
                        // 导航到该目录
                        if (isDualPaneMode) {
                            navigateLeftPanel(selectedFile);
                            navigateRightPanel(selectedFile);
                        } else {
                            navigateSinglePanel(selectedFile);
                        }
                    } else {
                        // 导航到文件所在目录并显示文件选项
                        File parentDir = selectedFile.getParentFile();
                        if (parentDir != null) {
                            if (isDualPaneMode) {
                                navigateLeftPanel(parentDir);
                                navigateRightPanel(parentDir);
                            } else {
                                navigateSinglePanel(parentDir);
                            }
                        }
                        showFileOptions(new FileItem(selectedFile), true);
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void navigateUp(boolean isLeft) {
        if (isLeft) {
            File parent = leftCurrentDirectory.getParentFile();
            if (parent != null) {
                navigateLeftPanel(parent);
            }
        } else {
            File parent = rightCurrentDirectory.getParentFile();
            if (parent != null) {
                navigateRightPanel(parent);
            }
        }
    }

    private void editPreviewedFile() {
        if (selectedFileForPreview == null) {
            Toast.makeText(getContext(), "没有正在预览的文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FileEditorFragment editorFragment = new FileEditorFragment();
        Bundle args = new Bundle();
        args.putString("file_path", selectedFileForPreview.getFile().getAbsolutePath());
        editorFragment.setArguments(args);
        
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editorFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showFileOptions(FileItem item, boolean isLeftPanel) {
        String[] options;
        if (item.isDirectory()) {
            options = new String[]{"打开", "重命名", "删除", "信息"};
        } else {
            String ext = item.getFileExtension().toLowerCase();
            boolean isImage = ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || 
                             ext.equals("gif") || ext.equals("bmp");
            boolean isText = ext.equals("txt") || ext.equals("log") || ext.equals("md") || 
                           ext.equals("java") || ext.equals("xml") || ext.equals("json") || 
                           ext.equals("js") || ext.equals("py") || ext.equals("c") || ext.equals("cpp");
            
            if (isImage) {
                options = new String[]{"预览", "编辑", "Hex查看", "重命名", "删除", "信息"};
            } else if (isText) {
                options = new String[]{"预览", "编辑", "Hex查看", "重命名", "删除", "信息"};
            } else {
                options = new String[]{"Hex查看", "重命名", "删除", "信息"};
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    handleFileOption(item, options[which], isLeftPanel);
                })
                .show();
    }

    private void handleFileOption(FileItem item, String option, boolean isLeftPanel) {
        switch (option) {
            case "打开":
                if (isLeftPanel) {
                    navigateLeftPanel(item.getFile());
                    navigateRightPanel(item.getFile());
                } else {
                    navigateRightPanel(item.getFile());
                }
                break;
            case "预览":
                showFilePreview(item);
                break;
            case "编辑":
                editFile(item);
                break;
            case "Hex查看":
                viewHex(item);
                break;
            case "重命名":
                renameFile(item, isLeftPanel);
                break;
            case "删除":
                deleteFile(item, isLeftPanel);
                break;
            case "信息":
                showFileInfo(item);
                break;
        }
    }

    private void editFile(FileItem item) {
        FileEditorFragment editorFragment = new FileEditorFragment();
        Bundle args = new Bundle();
        args.putString("file_path", item.getFile().getAbsolutePath());
        editorFragment.setArguments(args);
        
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editorFragment)
                .addToBackStack(null)
                .commit();
    }

    private void viewHex(FileItem item) {
        new Thread(() -> {
            try {
                FileInputStream fis = new FileInputStream(item.getFile());
                byte[] buffer = new byte[Math.min(8192, (int) item.getFile().length())];
                int bytesRead = fis.read(buffer);
                fis.close();
                
                StringBuilder hexDump = new StringBuilder();
                for (int i = 0; i < bytesRead; i += 16) {
                    hexDump.append(String.format("%08X  ", i));
                    
                    StringBuilder ascii = new StringBuilder();
                    for (int j = 0; j < 16; j++) {
                        if (i + j < bytesRead) {
                            byte b = buffer[i + j];
                            hexDump.append(String.format("%02X ", b));
                            ascii.append((b >= 32 && b < 127) ? (char) b : '.');
                        } else {
                            hexDump.append("   ");
                        }
                        if (j == 7) hexDump.append(" ");
                    }
                    
                    hexDump.append(" |").append(ascii).append("|\n");
                }
                
                if (bytesRead < item.getFile().length()) {
                    hexDump.append("\n... (仅显示前8KB)");
                }
                
                requireActivity().runOnUiThread(() -> {
                    TextView textView = new TextView(requireContext());
                    textView.setText(hexDump.toString());
                    textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                    textView.setTextSize(10);
                    textView.setPadding(20, 20, 20, 20);
                    
                    android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
                    scrollView.addView(textView);
                    
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Hex查看: " + item.getName())
                            .setView(scrollView)
                            .setPositiveButton("关闭", null)
                            .show();
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "无法读取文件: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void renameFile(FileItem item, boolean isLeftPanel) {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setText(item.getName());
        input.setSelectAllOnFocus(true);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("重命名")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "文件名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (newName.contains("/") || newName.contains("\\")) {
                        Toast.makeText(getContext(), "文件名不能包含 / 或 \\", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    File currentDir = isLeftPanel ? leftCurrentDirectory : rightCurrentDirectory;
                    File newFile = new File(currentDir, newName);
                    if (newFile.exists()) {
                        Toast.makeText(getContext(), "文件名已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (item.getFile().renameTo(newFile)) {
                        Toast.makeText(getContext(), "重命名成功", Toast.LENGTH_SHORT).show();
                        refreshBothPanels();
                    } else {
                        Toast.makeText(getContext(), "重命名失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteFile(FileItem item, boolean isLeftPanel) {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除 " + item.getName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (item.getFile().delete()) {
                        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                        refreshBothPanels();
                    } else {
                        Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showFileInfo(FileItem item) {
        File file = item.getFile();
        StringBuilder info = new StringBuilder();
        
        info.append("名称: ").append(item.getName()).append("\n");
        info.append("类型: ").append(item.isDirectory() ? "文件夹" : "文件").append("\n");
        
        if (!item.isDirectory()) {
            info.append("扩展名: ").append(item.getFileExtension()).append("\n");
        }
        
        info.append("大小: ").append(item.getFormattedSize()).append("\n");
        info.append("修改时间: ").append(item.getFormattedDate()).append("\n");
        info.append("路径: ").append(file.getAbsolutePath()).append("\n");
        
        info.append("\n权限:\n");
        info.append("  可读: ").append(file.canRead() ? "是" : "否").append("\n");
        info.append("  可写: ").append(file.canWrite() ? "是" : "否").append("\n");
        info.append("  可执行: ").append(file.canExecute() ? "是" : "否").append("\n");
        
        if (item.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                int fileCount = 0;
                int dirCount = 0;
                for (File child : children) {
                    if (child.isDirectory()) dirCount++;
                    else fileCount++;
                }
                info.append("\n内容:\n");
                info.append("  文件夹: ").append(dirCount).append("\n");
                info.append("  文件: ").append(fileCount).append("\n");
            }
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("文件信息")
                .setMessage(info.toString())
                .setPositiveButton("确定", null)
                .show();
    }

    private void showDownloadDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_wget, null);
        android.widget.EditText urlInput = dialogView.findViewById(R.id.url_input);
        android.widget.EditText filenameInput = dialogView.findViewById(R.id.filename_input);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("下载文件 (wget)")
                .setView(dialogView)
                .setPositiveButton("下载", (dialog, which) -> {
                    String url = urlInput.getText().toString().trim();
                    String filename = filenameInput.getText().toString().trim();
                    
                    if (url.isEmpty()) {
                        Toast.makeText(getContext(), "请输入URL", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (filename.isEmpty()) {
                        filename = extractFilenameFromUrl(url);
                    }
                    
                    // 下载到当前活动面板的目录
                    File targetDir = isLeftPanelActive ? leftCurrentDirectory : rightCurrentDirectory;
                    downloadFile(url, filename, targetDir);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String extractFilenameFromUrl(String url) {
        try {
            String path = url.substring(url.lastIndexOf('/') + 1);
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf('?'));
            }
            if (path.isEmpty() || !path.contains(".")) {
                return "downloaded_file";
            }
            return path;
        } catch (Exception e) {
            return "downloaded_file";
        }
    }

    private void downloadFile(String url, String filename, File targetDir) {
        File targetFile = new File(targetDir, filename);
        
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("下载中");
        progressDialog.setMessage("正在下载: " + filename);
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            try {
                java.net.URL downloadUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) downloadUrl.openConnection();
                connection.connect();
                
                int fileLength = connection.getContentLength();
                
                java.io.InputStream input = connection.getInputStream();
                java.io.FileOutputStream output = new java.io.FileOutputStream(targetFile);
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (fileLength > 0) {
                        int progress = (int) (totalBytesRead * 100 / fileLength);
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.setProgress(progress);
                            progressDialog.setMessage(String.format("正在下载: %s (%d%%)", 
                                filename, progress));
                        });
                    }
                }
                
                output.flush();
                output.close();
                input.close();
                
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "下载完成: " + filename, Toast.LENGTH_SHORT).show();
                    refreshBothPanels();
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "下载失败: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void setupDragListeners() {
        // 左侧面板拖放监听
        leftFileList.setOnDragListener((v, event) -> {
            return handleDragEvent(event, leftCurrentDirectory, false);
        });
        
        // 右侧面板拖放监听
        rightFileList.setOnDragListener((v, event) -> {
            return handleDragEvent(event, rightCurrentDirectory, true);
        });
    }
    
    private boolean handleDragEvent(android.view.DragEvent event, File targetDirectory, boolean isRightPanel) {
        switch (event.getAction()) {
            case android.view.DragEvent.ACTION_DRAG_STARTED:
                // 检查是否可以接受拖放
                return event.getClipDescription().hasMimeType(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN);
                
            case android.view.DragEvent.ACTION_DRAG_ENTERED:
                // 拖动进入目标区域，高亮显示
                if (isRightPanel) {
                    rightFileList.setBackgroundColor(0x2000FF00); // 半透明绿色
                } else {
                    leftFileList.setBackgroundColor(0x2000FF00);
                }
                return true;
                
            case android.view.DragEvent.ACTION_DRAG_EXITED:
                // 拖动离开目标区域，取消高亮
                if (isRightPanel) {
                    rightFileList.setBackgroundColor(0x00000000);
                } else {
                    leftFileList.setBackgroundColor(0x00000000);
                }
                return true;
                
            case android.view.DragEvent.ACTION_DROP:
                // 文件被放下，执行移动操作
                if (isRightPanel) {
                    rightFileList.setBackgroundColor(0x00000000);
                } else {
                    leftFileList.setBackgroundColor(0x00000000);
                }
                
                android.content.ClipData.Item item = event.getClipData().getItemAt(0);
                String sourcePath = item.getText().toString();
                File sourceFile = new File(sourcePath);
                
                if (!sourceFile.exists()) {
                    Toast.makeText(getContext(), "源文件不存在", Toast.LENGTH_SHORT).show();
                    return false;
                }
                
                // 检查是否拖到同一目录
                if (sourceFile.getParent().equals(targetDirectory.getAbsolutePath())) {
                    Toast.makeText(getContext(), "文件已在当前目录", Toast.LENGTH_SHORT).show();
                    return true;
                }
                
                // 直接移动文件
                moveFile(sourceFile, targetDirectory);
                return true;
                
            case android.view.DragEvent.ACTION_DRAG_ENDED:
                // 拖放结束，清理状态
                leftFileList.setBackgroundColor(0x00000000);
                rightFileList.setBackgroundColor(0x00000000);
                return true;
                
            default:
                return false;
        }
    }
    
    private void connectSSH() {
        // 跳转到SSH连接选择界面
        SSHListFragment sshListFragment = new SSHListFragment();
        
        // 设置返回标记，表示从文件管理器调用
        Bundle args = new Bundle();
        args.putBoolean("from_file_manager", true);
        sshListFragment.setArguments(args);
        
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, sshListFragment)
                .addToBackStack(null)
                .commit();
    }
    
    private void disconnectSSH() {
        new AlertDialog.Builder(requireContext())
                .setTitle("断开SSH连接")
                .setMessage("确定要断开SSH连接吗？")
                .setPositiveButton("断开", (dialog, which) -> {
                    if (remoteFileManager != null) {
                        remoteFileManager.disconnect();
                        remoteFileManager = null;
                    }
                    
                    // 重置远程面板状态
                    isLeftPanelRemote = false;
                    isRightPanelRemote = false;
                    
                    // 隐藏SSH切换按钮
                    fabSshToggle.setVisibility(View.GONE);
                    
                    // 恢复本地文件浏览
                    navigateLeftPanel(leftCurrentDirectory != null ? leftCurrentDirectory : appDirectory);
                    navigateRightPanel(rightCurrentDirectory != null ? rightCurrentDirectory : appDirectory);
                    
                    Toast.makeText(getContext(), "SSH连接已断开", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void toggleSSHMode() {
        if (remoteFileManager == null || !remoteFileManager.isConnected()) {
            Toast.makeText(getContext(), "未连接SSH", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 切换右侧面板模式
        isRightPanelRemote = !isRightPanelRemote;
        
        // 隐藏预览
        hidePreview();
        
        if (isRightPanelRemote) {
            // 切换到远程模式
            rightPathText.setText("SSH: " + remoteFileManager.getConnectionInfo() + ":" + rightRemotePath);
            loadRemoteFiles(rightRemotePath, rightAdapter, false);
            Toast.makeText(getContext(), "右侧面板: 远程模式", Toast.LENGTH_SHORT).show();
        } else {
            // 切换到本地模式
            navigateRightPanel(rightCurrentDirectory != null ? rightCurrentDirectory : appDirectory);
            Toast.makeText(getContext(), "右侧面板: 本地模式", Toast.LENGTH_SHORT).show();
        }
        
        // 更新按钮图标
        updateSSHButtonIcon();
    }
    
    private void updateSSHButtonIcon() {
        if (isRightPanelRemote) {
            // 远程模式：显示云图标
            fabSshToggle.setImageResource(android.R.drawable.ic_menu_share);
        } else {
            // 本地模式：显示文件夹图标
            fabSshToggle.setImageResource(android.R.drawable.ic_menu_view);
        }
    }
    
    private void moveFile(File sourceFile, File targetDirectory) {
        File targetFile = new File(targetDirectory, sourceFile.getName());
        
        if (targetFile.exists()) {
            Toast.makeText(getContext(), "目标文件已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("移动文件");
        progressDialog.setMessage("正在移动: " + sourceFile.getName());
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            try {
                boolean success = sourceFile.renameTo(targetFile);
                
                if (!success) {
                    // 如果重命名失败（可能跨分区），尝试复制后删除
                    if (sourceFile.isDirectory()) {
                        copyDirectory(sourceFile, targetFile);
                    } else {
                        copyFileContent(sourceFile, targetFile);
                    }
                    deleteRecursive(sourceFile);
                }
                
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "移动成功", Toast.LENGTH_SHORT).show();
                    refreshAll();
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "移动失败: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void copyFileContent(File source, File target) throws java.io.IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(source);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(target);
        
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        
        fos.flush();
        fos.close();
        fis.close();
    }
    
    private void copyDirectory(File source, File target) throws java.io.IOException {
        if (!target.exists()) {
            target.mkdirs();
        }
        
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, targetFile);
                } else {
                    copyFileContent(file, targetFile);
                }
            }
        }
    }
    
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
    
    // ==================== SSH远程文件管理 ====================
    
    private void connectToSSH(String host, int port, String username, String password, String name) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("连接SSH");
        progressDialog.setMessage("正在连接到 " + username + "@" + host + "...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        remoteFileManager = new RemoteFileManager(host, port, username, password);
        
        remoteFileManager.connect(new RemoteFileManager.ConnectionCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "SSH连接成功: " + name, Toast.LENGTH_SHORT).show();
                    
                    // 将右侧面板切换为远程模式
                    isRightPanelRemote = true;
                    rightRemotePath = "/root";
                    
                    // 显示SSH切换按钮
                    fabSshToggle.setVisibility(View.VISIBLE);
                    updateSSHButtonIcon();
                    
                    // 隐藏预览
                    hidePreview();
                    
                    // 更新路径显示
                    rightPathText.setText("SSH: " + remoteFileManager.getConnectionInfo() + ":" + rightRemotePath);
                    
                    // 加载远程文件列表
                    loadRemoteFiles(rightRemotePath, rightAdapter, false);
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "SSH连接失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void loadRemoteFiles(String remotePath, FileAdapter adapter, boolean isLeftPanel) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setMessage("加载远程文件...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        remoteFileManager.listFiles(remotePath, new RemoteFileManager.CommandCallback() {
            @Override
            public void onSuccess(String output) {
                // 解析 ls -la 输出
                List<FileItem> fileItems = parseRemoteFileList(output, remotePath);
                
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    adapter.setFiles(fileItems);
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "加载远程文件失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private List<FileItem> parseRemoteFileList(String lsOutput, String currentPath) {
        List<FileItem> items = new ArrayList<>();
        String[] lines = lsOutput.split("\n");
        
        // 添加返回上级目录选项
        if (!currentPath.equals("/")) {
            File parentFile = new File("..");
            FileItem parentItem = new FileItem(parentFile);
            items.add(parentItem);
        }
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("total")) {
                continue;
            }
            
            // 解析 ls -la 输出格式
            // drwxr-xr-x 2 user group 4096 Jan 1 12:00 dirname
            // -rw-r--r-- 1 user group 1234 Jan 1 12:00 filename
            
            String[] parts = line.split("\\s+");
            if (parts.length < 9) {
                continue;
            }
            
            String permissions = parts[0];
            boolean isDirectory = permissions.startsWith("d");
            
            // 文件名可能包含空格，需要从第9列开始拼接
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 8; i < parts.length; i++) {
                if (i > 8) nameBuilder.append(" ");
                nameBuilder.append(parts[i]);
            }
            String name = nameBuilder.toString();
            
            // 跳过 . 和 ..
            if (name.equals(".") || name.equals("..")) {
                continue;
            }
            
            String fullPath = currentPath.endsWith("/") ? 
                currentPath + name : currentPath + "/" + name;
            
            RemoteFileItem remoteItem = new RemoteFileItem(name, fullPath, isDirectory);
            
            // 解析文件大小
            try {
                long size = Long.parseLong(parts[4]);
                remoteItem.setSize(size);
            } catch (Exception e) {
                // 忽略解析错误
            }
            
            remoteItem.setPermissions(permissions);
            
            items.add(remoteItem.toFileItem());
        }
        
        // 排序：文件夹在前，然后按名称排序
        Collections.sort(items, (a, b) -> {
            // 父目录永远在最前
            if (a.getName().equals("..")) return -1;
            if (b.getName().equals("..")) return 1;
            
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            } else if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            } else {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        
        return items;
    }
    
    private void navigateRemoteUp(boolean isLeftPanel) {
        String currentPath = isLeftPanel ? leftRemotePath : rightRemotePath;
        
        if (currentPath.equals("/")) {
            Toast.makeText(getContext(), "已在根目录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取父目录路径
        int lastSlash = currentPath.lastIndexOf('/');
        String parentPath = lastSlash <= 0 ? "/" : currentPath.substring(0, lastSlash);
        
        if (isLeftPanel) {
            leftRemotePath = parentPath;
            leftPathText.setText("SSH: " + remoteFileManager.getConnectionInfo() + ":" + leftRemotePath);
            loadRemoteFiles(leftRemotePath, leftAdapter, true);
        } else {
            rightRemotePath = parentPath;
            rightPathText.setText("SSH: " + remoteFileManager.getConnectionInfo() + ":" + rightRemotePath);
            loadRemoteFiles(rightRemotePath, rightAdapter, false);
        }
    }
    
    private void showRemoteFileOptions(FileItem item, boolean isLeftPanel) {
        String[] options = {"预览", "下载到本地", "删除", "重命名", "信息"};
        
        new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            previewRemoteFile(item);
                            break;
                        case 1:
                            downloadRemoteFile(item);
                            break;
                        case 2:
                            deleteRemoteFile(item, isLeftPanel);
                            break;
                        case 3:
                            renameRemoteFile(item, isLeftPanel);
                            break;
                        case 4:
                            showRemoteFileInfo(item);
                            break;
                    }
                })
                .show();
    }
    
    private void previewRemoteFile(FileItem item) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setMessage("读取远程文件...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // 读取前8KB
        remoteFileManager.readFile(item.getFile().getPath(), 8192, new RemoteFileManager.CommandCallback() {
            @Override
            public void onSuccess(String content) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    TextView textView = new TextView(requireContext());
                    textView.setText(content);
                    textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                    textView.setTextSize(12);
                    textView.setPadding(20, 20, 20, 20);
                    
                    android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
                    scrollView.addView(textView);
                    
                    new AlertDialog.Builder(requireContext())
                            .setTitle("预览: " + item.getName())
                            .setView(scrollView)
                            .setPositiveButton("关闭", null)
                            .show();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "读取失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void downloadRemoteFile(FileItem item) {
        // 下载到当前本地目录
        File targetDir = isLeftPanelRemote ? rightCurrentDirectory : leftCurrentDirectory;
        File targetFile = new File(targetDir, item.getName());
        
        if (targetFile.exists()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("文件已存在")
                    .setMessage("本地已存在同名文件，是否覆盖？")
                    .setPositiveButton("覆盖", (dialog, which) -> {
                        performDownload(item, targetFile);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            performDownload(item, targetFile);
        }
    }
    
    private void performDownload(FileItem item, File targetFile) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("下载文件");
        progressDialog.setMessage("正在下载: " + item.getName());
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // 读取完整文件内容
        remoteFileManager.readFile(item.getFile().getPath(), Integer.MAX_VALUE, new RemoteFileManager.CommandCallback() {
            @Override
            public void onSuccess(String content) {
                // 写入本地文件
                new Thread(() -> {
                    try {
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile);
                        fos.write(content.getBytes());
                        fos.close();
                        
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "下载成功: " + item.getName(), Toast.LENGTH_SHORT).show();
                            refreshAll();
                        });
                        
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "下载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void deleteRemoteFile(FileItem item, boolean isLeftPanel) {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除远程文件 " + item.getName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
                    progressDialog.setMessage("删除中...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    
                    remoteFileManager.deleteFile(item.getFile().getPath(), item.isDirectory(), 
                            new RemoteFileManager.CommandCallback() {
                        @Override
                        public void onSuccess(String output) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                                
                                // 刷新远程文件列表
                                if (isLeftPanel) {
                                    loadRemoteFiles(leftRemotePath, leftAdapter, true);
                                } else {
                                    loadRemoteFiles(rightRemotePath, rightAdapter, false);
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "删除失败: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void renameRemoteFile(FileItem item, boolean isLeftPanel) {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setText(item.getName());
        input.setSelectAllOnFocus(true);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("重命名远程文件")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "文件名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (newName.contains("/")) {
                        Toast.makeText(getContext(), "文件名不能包含 /", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String currentPath = isLeftPanel ? leftRemotePath : rightRemotePath;
                    String oldPath = item.getFile().getPath();
                    String newPath = currentPath.endsWith("/") ? 
                        currentPath + newName : currentPath + "/" + newName;
                    
                    android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
                    progressDialog.setMessage("重命名中...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    
                    remoteFileManager.renameFile(oldPath, newPath, new RemoteFileManager.CommandCallback() {
                        @Override
                        public void onSuccess(String output) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "重命名成功", Toast.LENGTH_SHORT).show();
                                
                                // 刷新远程文件列表
                                if (isLeftPanel) {
                                    loadRemoteFiles(leftRemotePath, leftAdapter, true);
                                } else {
                                    loadRemoteFiles(rightRemotePath, rightAdapter, false);
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "重命名失败: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showRemoteFileInfo(FileItem item) {
        StringBuilder info = new StringBuilder();
        
        info.append("名称: ").append(item.getName()).append("\n");
        info.append("类型: ").append(item.isDirectory() ? "文件夹" : "文件").append("\n");
        info.append("大小: ").append(item.getFormattedSize()).append("\n");
        info.append("路径: ").append(item.getFile().getPath()).append("\n");
        info.append("\n这是一个远程文件");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("远程文件信息")
                .setMessage(info.toString())
                .setPositiveButton("确定", null)
                .show();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 断开SSH连接
        if (remoteFileManager != null) {
            remoteFileManager.disconnect();
        }
    }
}
