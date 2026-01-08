package com.example.movinghacker;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class FileManagerFragment extends Fragment implements FileAdapter.OnFileClickListener {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private TextView pathText;
    private FloatingActionButton fabUp;
    private FloatingActionButton fabDownload;
    
    private File currentDirectory;
    private Stack<File> navigationStack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.file_list);
        pathText = view.findViewById(R.id.current_path);
        fabUp = view.findViewById(R.id.fab_up);
        fabDownload = view.findViewById(R.id.fab_download);

        navigationStack = new Stack<>();
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FileAdapter(this);
        recyclerView.setAdapter(adapter);

        // 返回上级目录按钮
        fabUp.setOnClickListener(v -> navigateUp());
        
        // 下载文件按钮
        fabDownload.setOnClickListener(v -> showDownloadDialog());

        // 初始目录
        File startDir = Environment.getExternalStorageDirectory();
        if (!startDir.exists() || !startDir.canRead()) {
            startDir = requireContext().getFilesDir();
        }
        
        navigateToDirectory(startDir);
    }

    private void navigateToDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Toast.makeText(getContext(), "无法访问目录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!directory.canRead()) {
            Toast.makeText(getContext(), "没有读取权限", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDirectory = directory;
        pathText.setText(directory.getAbsolutePath());
        
        // 加载文件列表
        loadFiles();
        
        // 更新返回按钮状态
        fabUp.setVisibility(directory.getParent() != null ? View.VISIBLE : View.GONE);
    }

    private void loadFiles() {
        File[] files = currentDirectory.listFiles();
        if (files == null) {
            adapter.setFiles(new ArrayList<>());
            return;
        }

        List<FileItem> fileItems = new ArrayList<>();
        for (File file : files) {
            // 跳过隐藏文件
            if (file.getName().startsWith(".")) {
                continue;
            }
            fileItems.add(new FileItem(file));
        }

        // 排序：文件夹在前，然后按名称排序
        Collections.sort(fileItems, (a, b) -> {
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

    private void navigateUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            navigationStack.push(currentDirectory);
            navigateToDirectory(parent);
        }
    }

    @Override
    public void onFileClick(FileItem item) {
        if (item.isDirectory()) {
            // 进入目录
            navigationStack.push(currentDirectory);
            navigateToDirectory(item.getFile());
        } else {
            // 打开文件
            showFileOptions(item);
        }
    }

    @Override
    public void onFileLongClick(FileItem item) {
        showFileOptions(item);
    }

    private void showFileOptions(FileItem item) {
        String[] options;
        if (item.isDirectory()) {
            options = new String[]{"打开", "重命名", "删除", "信息"};
        } else {
            String ext = item.getFileExtension().toLowerCase();
            boolean isImage = ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("bmp");
            boolean isText = ext.equals("txt") || ext.equals("log") || ext.equals("md") || 
                           ext.equals("java") || ext.equals("xml") || ext.equals("json") || 
                           ext.equals("js") || ext.equals("py") || ext.equals("c") || ext.equals("cpp");
            
            if (isImage) {
                options = new String[]{"预览图片", "编辑", "Hex查看", "重命名", "删除", "信息"};
            } else if (isText) {
                options = new String[]{"编辑", "Hex查看", "重命名", "删除", "信息"};
            } else {
                options = new String[]{"Hex查看", "重命名", "删除", "信息"};
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    handleFileOption(item, options[which]);
                })
                .show();
    }
    
    private void handleFileOption(FileItem item, String option) {
        switch (option) {
            case "打开":
                navigationStack.push(currentDirectory);
                navigateToDirectory(item.getFile());
                break;
            case "预览图片":
                previewImage(item);
                break;
            case "编辑":
                editFile(item);
                break;
            case "Hex查看":
                viewHex(item);
                break;
            case "重命名":
                renameFile(item);
                break;
            case "删除":
                deleteFile(item);
                break;
            case "信息":
                showFileInfo(item);
                break;
        }
    }

    private void previewImage(FileItem item) {
        try {
            android.widget.ImageView imageView = new android.widget.ImageView(requireContext());
            imageView.setImageURI(android.net.Uri.fromFile(item.getFile()));
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            
            new AlertDialog.Builder(requireContext())
                    .setTitle(item.getName())
                    .setView(imageView)
                    .setPositiveButton("关闭", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法预览图片: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void viewHex(FileItem item) {
        new Thread(() -> {
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream(item.getFile());
                byte[] buffer = new byte[Math.min(8192, (int) item.getFile().length())]; // 最多读取8KB
                int bytesRead = fis.read(buffer);
                fis.close();
                
                StringBuilder hexDump = new StringBuilder();
                for (int i = 0; i < bytesRead; i += 16) {
                    // 地址
                    hexDump.append(String.format("%08X  ", i));
                    
                    // Hex字节
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

    private void editFile(FileItem item) {
        // 打开文件编辑器Fragment
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

    private void renameFile(FileItem item) {
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
                    
                    File newFile = new File(currentDirectory, newName);
                    if (newFile.exists()) {
                        Toast.makeText(getContext(), "文件名已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (item.getFile().renameTo(newFile)) {
                        Toast.makeText(getContext(), "重命名成功", Toast.LENGTH_SHORT).show();
                        loadFiles();
                    } else {
                        Toast.makeText(getContext(), "重命名失败", Toast.LENGTH_SHORT).show();
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
        
        // 权限信息
        info.append("\n权限:\n");
        info.append("  可读: ").append(file.canRead() ? "是" : "否").append("\n");
        info.append("  可写: ").append(file.canWrite() ? "是" : "否").append("\n");
        info.append("  可执行: ").append(file.canExecute() ? "是" : "否").append("\n");
        
        // 如果是目录，显示子项数量
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

    private void deleteFile(FileItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除 " + item.getName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (item.getFile().delete()) {
                        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                        loadFiles();
                    } else {
                        Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
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
                    
                    // 如果没有指定文件名，从URL提取
                    if (filename.isEmpty()) {
                        filename = extractFilenameFromUrl(url);
                    }
                    
                    downloadFile(url, filename);
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
    
    private void downloadFile(String url, String filename) {
        File targetFile = new File(currentDirectory, filename);
        
        // 显示进度对话框
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("下载中");
        progressDialog.setMessage("正在下载: " + filename);
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // 使用线程下载
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
                    loadFiles(); // 刷新文件列表
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
}
