package com.example.movinghacker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;

public class FileEditorFragment extends Fragment {

    private CodeEditor codeEditor;
    private TextInputEditText filenameInput;
    private Spinner languageSpinner;
    private MaterialButton saveButton;
    private MaterialButton openButton;
    private MaterialButton newButton;

    private File editorDirectory;
    private String currentFilePath;

    private final String[] languages = {
        "Plain Text", "Java", "Python", "JavaScript", "PHP", "Go", 
        "C", "C++", "C#", "HTML", "CSS", "XML", "JSON", "SQL", 
        "Kotlin", "Swift", "Ruby", "Rust", "TypeScript", "Bash"
    };

    private final String[] extensions = {
        ".txt", ".java", ".py", ".js", ".php", ".go",
        ".c", ".cpp", ".cs", ".html", ".css", ".xml", ".json", ".sql",
        ".kt", ".swift", ".rb", ".rs", ".ts", ".sh"
    };

    private final ActivityResultLauncher<Intent> openFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            openFile(uri);
                        }
                    }
                });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupEditorDirectory();
        setupCodeEditor();
        setupLanguageSpinner();
        setupListeners();
        
        // 检查是否从文件管理器传入文件路径
        Bundle args = getArguments();
        if (args != null && args.containsKey("file_path")) {
            String filePath = args.getString("file_path");
            loadFileFromPath(filePath);
        }
    }

    private void initializeViews(View view) {
        codeEditor = view.findViewById(R.id.code_editor);
        filenameInput = view.findViewById(R.id.filename_input);
        languageSpinner = view.findViewById(R.id.language_spinner);
        saveButton = view.findViewById(R.id.save_button);
        openButton = view.findViewById(R.id.open_button);
        newButton = view.findViewById(R.id.new_button);
    }

    private void setupEditorDirectory() {
        // 创建编辑器文件保存目录
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        editorDirectory = new File(documentsDir, "MovingHacker/Editor");
        if (!editorDirectory.exists()) {
            editorDirectory.mkdirs();
        }
    }

    private void setupCodeEditor() {
        // 基础配置
        codeEditor.setTypefaceText(android.graphics.Typeface.MONOSPACE);
        codeEditor.setTextSize(14);
        codeEditor.setLineSpacing(2f, 1.1f);
        
        // 启用功能
        codeEditor.setWordwrap(false);
        codeEditor.setLineNumberEnabled(true);
        codeEditor.setPinLineNumber(true);
        
        // 禁用自动大小写转换和自动更正
        codeEditor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        
        // 设置Tab宽度
        codeEditor.setTabWidth(4);
        
        // 设置默认文本
        codeEditor.setText("// 开始编写代码...\n");
        
        // 设置默认语言（Plain Text）
        setEditorLanguage(0);
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateFileExtension(position);
                setEditorLanguage(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setEditorLanguage(int languagePosition) {
        try {
            Language language;
            // 只有Java有完整的语法高亮支持
            // 其他语言使用EmptyLanguage（基础编辑功能）
            if (languagePosition == 1) { // Java
                language = new JavaLanguage();
                Snackbar.make(requireView(), "已启用Java语法高亮", 
                        Snackbar.LENGTH_SHORT).show();
            } else {
                language = new EmptyLanguage();
                if (languagePosition > 0) {
                    Snackbar.make(requireView(), "当前语言暂无语法高亮，使用纯文本模式", 
                            Snackbar.LENGTH_SHORT).show();
                }
            }
            codeEditor.setEditorLanguage(language);
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(requireView(), "语言设置失败: " + e.getMessage(), 
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveFile());
        openButton.setOnClickListener(v -> openFileDialog());
        newButton.setOnClickListener(v -> newFile());
    }

    private void updateFileExtension(int languagePosition) {
        String currentFilename = filenameInput.getText() != null ? 
                filenameInput.getText().toString() : "untitled";
        
        // 移除旧扩展名
        int lastDot = currentFilename.lastIndexOf('.');
        if (lastDot > 0) {
            currentFilename = currentFilename.substring(0, lastDot);
        }
        
        // 添加新扩展名
        String newFilename = currentFilename + extensions[languagePosition];
        filenameInput.setText(newFilename);
    }

    private void saveFile() {
        String filename = filenameInput.getText() != null ? 
                filenameInput.getText().toString().trim() : "";
        
        if (filename.isEmpty()) {
            Snackbar.make(requireView(), "请输入文件名", Snackbar.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = new File(editorDirectory, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(codeEditor.getText().toString().getBytes());
            fos.close();

            currentFilePath = file.getAbsolutePath();
            Snackbar.make(requireView(), "文件已保存: " + file.getAbsolutePath(), 
                    Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Snackbar.make(requireView(), "保存失败: " + e.getMessage(), 
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void openFileDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        
        // 尝试打开编辑器目录
        if (editorDirectory.exists()) {
            intent.putExtra("android.provider.extra.INITIAL_URI", 
                    Uri.fromFile(editorDirectory));
        }
        
        openFileLauncher.launch(intent);
    }

    private void openFile(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();

                codeEditor.setText(content.toString());
                
                // 尝试从URI获取文件名
                String filename = getFileName(uri);
                if (filename != null) {
                    filenameInput.setText(filename);
                    detectLanguageFromFilename(filename);
                }

                Snackbar.make(requireView(), "文件已打开", Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Snackbar.make(requireView(), "打开失败: " + e.getMessage(), 
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void detectLanguageFromFilename(String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot);
        }

        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i].equalsIgnoreCase(extension)) {
                languageSpinner.setSelection(i);
                break;
            }
        }
    }

    private void newFile() {
        codeEditor.setText("");
        filenameInput.setText("untitled.txt");
        languageSpinner.setSelection(0);
        currentFilePath = null;
        Snackbar.make(requireView(), "新建文件", Snackbar.LENGTH_SHORT).show();
    }
    
    private void loadFileFromPath(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                Snackbar.make(requireView(), "文件不存在", Snackbar.LENGTH_SHORT).show();
                return;
            }
            
            // 读取文件内容
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            // 设置编辑器内容
            codeEditor.setText(content.toString());
            
            // 设置文件名
            filenameInput.setText(file.getName());
            
            // 检测语言
            detectLanguageFromFilename(file.getName());
            
            // 保存当前文件路径
            currentFilePath = filePath;
            
            Snackbar.make(requireView(), "文件已打开: " + file.getName(), 
                    Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(requireView(), "打开失败: " + e.getMessage(), 
                    Snackbar.LENGTH_LONG).show();
        }
    }

    public File getEditorDirectory() {
        return editorDirectory;
    }
}
