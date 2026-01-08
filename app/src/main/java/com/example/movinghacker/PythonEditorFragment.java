package com.example.movinghacker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PythonEditorFragment extends Fragment {

    private CodeEditor codeEditor;
    private TextView outputText;
    private ScrollView outputScroll;
    private Button btnNew, btnOpen, btnSave, btnRun, btnPip;
    
    private File currentFile;
    private File editorDir;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Python python;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_python_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        codeEditor = view.findViewById(R.id.code_editor);
        outputText = view.findViewById(R.id.output_text);
        outputScroll = view.findViewById(R.id.output_scroll);
        btnNew = view.findViewById(R.id.btn_new);
        btnOpen = view.findViewById(R.id.btn_open);
        btnSave = view.findViewById(R.id.btn_save);
        btnRun = view.findViewById(R.id.btn_run);
        btnPip = view.findViewById(R.id.btn_pip);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // 获取 Python 实例（已在 Application 中初始化）
        python = Python.getInstance();

        // 设置编辑器
        codeEditor.setEditorLanguage(new EmptyLanguage());
        codeEditor.setTextSize(14);
        codeEditor.setInputType(InputType.TYPE_CLASS_TEXT | 
                                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                                InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        // 创建编辑器目录
        File documentsDir = new File(requireContext().getExternalFilesDir(null).getParentFile().getParentFile().getParentFile().getParentFile(), "Documents");
        editorDir = new File(documentsDir, "MovingHacker/Python");
        if (!editorDir.exists()) {
            editorDir.mkdirs();
        }

        btnNew.setOnClickListener(v -> newFile());
        btnOpen.setOnClickListener(v -> openFile());
        btnSave.setOnClickListener(v -> saveFile());
        btnRun.setOnClickListener(v -> runPython());
        btnPip.setOnClickListener(v -> showPipManager());

        // 默认代码
        codeEditor.setText("# Python 3.11\nprint('Hello, Python!')");
    }

    private void newFile() {
        codeEditor.setText("");
        currentFile = null;
        outputText.setText("");
        Toast.makeText(getContext(), "新建文件", Toast.LENGTH_SHORT).show();
    }

    private void openFile() {
        File[] files = editorDir.listFiles((dir, name) -> name.endsWith(".py"));
        if (files == null || files.length == 0) {
            Toast.makeText(getContext(), "没有找到Python文件", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("打开文件")
                .setItems(fileNames, (dialog, which) -> {
                    currentFile = files[which];
                    loadFile(currentFile);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String content = new String(data, "UTF-8");
            codeEditor.setText(content);
            outputText.setText("");
            Toast.makeText(getContext(), "已打开: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            // 询问文件名
            EditText input = new EditText(requireContext());
            input.setHint("文件名 (例如: script.py)");
            
            new AlertDialog.Builder(requireContext())
                    .setTitle("保存文件")
                    .setView(input)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String fileName = input.getText().toString().trim();
                        if (fileName.isEmpty()) {
                            Toast.makeText(getContext(), "文件名不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!fileName.endsWith(".py")) {
                            fileName += ".py";
                        }
                        currentFile = new File(editorDir, fileName);
                        saveToFile(currentFile);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            saveToFile(currentFile);
        }
    }

    private void saveToFile(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(codeEditor.getText().toString().getBytes("UTF-8"));
            fos.close();
            Toast.makeText(getContext(), "已保存: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void runPython() {
        String code = codeEditor.getText().toString();
        if (code.trim().isEmpty()) {
            Toast.makeText(getContext(), "代码为空", Toast.LENGTH_SHORT).show();
            return;
        }

        outputText.setText("正在执行...\n");
        btnRun.setEnabled(false);

        executorService.execute(() -> {
            StringBuilder output = new StringBuilder();
            try {
                PyObject sys = python.getModule("sys");
                PyObject io = python.getModule("io");
                PyObject builtins = python.getBuiltins();
                
                // 重定向stdout和stderr
                PyObject stringIO = io.callAttr("StringIO");
                sys.put("stdout", stringIO);
                sys.put("stderr", stringIO);
                
                // 获取 __main__ 模块的全局命名空间
                PyObject mainModule = python.getModule("__main__");
                PyObject globals = mainModule.get("__dict__");
                
                // 执行代码 - 使用 compile + eval 方式
                PyObject compiled = builtins.callAttr("compile", code, "<string>", "exec");
                builtins.callAttr("eval", compiled, globals);
                
                // 获取输出
                String result = stringIO.callAttr("getvalue").toString();
                output.append(result);
                
            } catch (PyException e) {
                output.append("错误:\n").append(e.getMessage());
            } catch (Exception e) {
                output.append("执行失败:\n").append(e.getMessage());
            }

            String finalOutput = output.toString();
            mainHandler.post(() -> {
                outputText.setText(finalOutput.isEmpty() ? "执行完成，无输出" : finalOutput);
                outputScroll.fullScroll(View.FOCUS_DOWN);
                btnRun.setEnabled(true);
            });
        });
    }

    private void showPipManager() {
        // 显示已安装的包信息
        StringBuilder packageInfo = new StringBuilder();
        packageInfo.append("已预装的 Python 包:\n\n");
        packageInfo.append("• requests - HTTP 库\n");
        packageInfo.append("• numpy - 数值计算库\n");
        packageInfo.append("• certifi - SSL 证书\n");
        packageInfo.append("• charset-normalizer - 字符编码\n");
        packageInfo.append("• idna - 国际化域名\n");
        packageInfo.append("• urllib3 - HTTP 客户端\n\n");
        packageInfo.append("注意: Chaquopy 环境中的包管理\n");
        packageInfo.append("需要在 build.gradle.kts 中配置。\n\n");
        packageInfo.append("如需添加新包，请在项目配置中\n");
        packageInfo.append("的 chaquopy.pip 部分添加。");

        new AlertDialog.Builder(requireContext())
                .setTitle("Python 包信息")
                .setMessage(packageInfo.toString())
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
