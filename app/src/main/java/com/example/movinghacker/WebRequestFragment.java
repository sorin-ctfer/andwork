package com.example.movinghacker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class WebRequestFragment extends Fragment {

    private TextInputEditText urlInput;
    private Spinner methodSpinner;
    private MaterialButton sendButton;
    private MaterialButton historyButton;
    private MaterialButton addHeaderButton;
    private ProgressBar progress;
    private TextView resultPlaceholder;
    private RecyclerView headersRecycler;
    private TextView noHeadersText;
    private TextView bodyLabel;
    private RadioGroup bodyTypeGroup;
    private RadioButton bodyTypeForm;
    private RadioButton bodyTypeJson;
    private RadioButton bodyTypeText;
    private RadioButton bodyTypeFile;
    private TextInputLayout bodyInputLayout;
    private TextInputEditText bodyInput;
    private LinearLayout fileUploadSection;
    private MaterialButton selectFileButton;
    private TextView selectedFileInfo;
    
    private android.net.Uri selectedFileUri;
    private LinearLayout responseContainer;
    private TabLayout responseTabs;
    private ViewPager2 responsePager;
    
    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), 
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        displayFileInfo(uri);
                    }
                });

    private WebRequestViewModel viewModel;
    private final List<RequestHeader> headers = new ArrayList<>();
    private HeaderAdapter headerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web_request_optimized, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WebRequestViewModel.class);
        
        initializeViews(view);
        setupMethodSpinner();
        setupHeadersRecycler();
        setupListeners();
        observeViewModel();
        
        // 加载历史请求数据（如果有）
        loadHistoryData();
    }
    
    private void loadHistoryData() {
        Bundle args = getArguments();
        if (args != null) {
            String url = args.getString("url");
            String method = args.getString("method");
            String body = args.getString("body");
            @SuppressWarnings("unchecked")
            ArrayList<RequestHeader> historyHeaders = (ArrayList<RequestHeader>) args.getSerializable("headers");
            
            if (url != null) {
                urlInput.setText(url);
            }
            
            if (method != null) {
                // 设置HTTP方法
                String[] methods = new String[]{"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"};
                for (int i = 0; i < methods.length; i++) {
                    if (methods[i].equals(method)) {
                        methodSpinner.setSelection(i);
                        break;
                    }
                }
            }
            
            if (body != null && !body.isEmpty()) {
                bodyInput.setText(body);
                // 显示请求体区域
                updateBodyVisibility();
            }
            
            if (historyHeaders != null && !historyHeaders.isEmpty()) {
                // 清除现有请求头并添加历史请求头
                for (RequestHeader header : historyHeaders) {
                    viewModel.addHeader(header.getKey(), header.getValue());
                }
            }
        }
    }

    private void initializeViews(View view) {
        urlInput = view.findViewById(R.id.url_input);
        methodSpinner = view.findViewById(R.id.method_spinner);
        sendButton = view.findViewById(R.id.send_button);
        historyButton = view.findViewById(R.id.history_button);
        addHeaderButton = view.findViewById(R.id.add_header_button);
        progress = view.findViewById(R.id.progress);
        resultPlaceholder = view.findViewById(R.id.result_placeholder);
        headersRecycler = view.findViewById(R.id.headers_recycler);
        noHeadersText = view.findViewById(R.id.no_headers_text);
        bodyLabel = view.findViewById(R.id.body_label);
        bodyTypeGroup = view.findViewById(R.id.body_type_group);
        bodyTypeForm = view.findViewById(R.id.body_type_form);
        bodyTypeJson = view.findViewById(R.id.body_type_json);
        bodyTypeText = view.findViewById(R.id.body_type_text);
        bodyTypeFile = view.findViewById(R.id.body_type_file);
        bodyInputLayout = view.findViewById(R.id.body_input_layout);
        bodyInput = view.findViewById(R.id.body_input);
        fileUploadSection = view.findViewById(R.id.file_upload_section);
        selectFileButton = view.findViewById(R.id.select_file_button);
        selectedFileInfo = view.findViewById(R.id.selected_file_info);
        responseContainer = view.findViewById(R.id.response_container);
        responseTabs = view.findViewById(R.id.response_tabs);
        responsePager = view.findViewById(R.id.response_pager);
    }

    private void setupMethodSpinner() {
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"}
        );
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        methodSpinner.setAdapter(methodAdapter);
    }

    private void setupHeadersRecycler() {
        headersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        headerAdapter = new HeaderAdapter(headers, new HeaderAdapter.OnHeaderActionListener() {
            @Override
            public void onDeleteHeader(int position) {
                viewModel.removeHeader(position);
            }

            @Override
            public void onEditHeader(int position) {
                editHeader(position);
            }
        });
        headersRecycler.setAdapter(headerAdapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> onSendRequest());
        historyButton.setOnClickListener(v -> navigateToHistory());
        addHeaderButton.setOnClickListener(v -> showAddHeaderDialog());
        
        // 监听HTTP方法变化以显示/隐藏请求体
        methodSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateBodyVisibility();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // 监听请求体类型变化以自动设置Content-Type
        bodyTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateContentTypeHeader();
            updateBodyInputVisibility();
        });
        
        selectFileButton.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
    }
    
    private void observeViewModel() {
        viewModel.getHeaders().observe(getViewLifecycleOwner(), headerList -> {
            headers.clear();
            headers.addAll(headerList);
            headerAdapter.notifyDataSetChanged();
            updateHeadersVisibility();
        });
        
        viewModel.getResponse().observe(getViewLifecycleOwner(), this::showResponse);
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });
        
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::setLoading);
    }
    
    private void updateBodyInputVisibility() {
        if (bodyTypeFile.isChecked()) {
            bodyInputLayout.setVisibility(View.GONE);
            fileUploadSection.setVisibility(View.VISIBLE);
        } else {
            bodyInputLayout.setVisibility(View.VISIBLE);
            fileUploadSection.setVisibility(View.GONE);
        }
    }
    
    private void displayFileInfo(android.net.Uri uri) {
        try {
            String fileName = getFileName(uri);
            long fileSize = getFileSize(uri);
            String mimeType = getMimeType(uri);
            
            String info = String.format("文件: %s\n大小: %s\n类型: %s", 
                fileName, formatFileSize(fileSize), mimeType);
            selectedFileInfo.setText(info);
        } catch (Exception e) {
            selectedFileInfo.setText("文件信息获取失败");
        }
    }
    
    private String getFileName(android.net.Uri uri) {
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
    
    private long getFileSize(android.net.Uri uri) {
        try (android.database.Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (index >= 0) {
                    return cursor.getLong(index);
                }
            }
        }
        return 0;
    }
    
    private String getMimeType(android.net.Uri uri) {
        String type = requireContext().getContentResolver().getType(uri);
        return type != null ? type : "application/octet-stream";
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
    
    private void updateBodyVisibility() {
        String method = (String) methodSpinner.getSelectedItem();
        boolean needsBody = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
        
        bodyLabel.setVisibility(needsBody ? View.VISIBLE : View.GONE);
        bodyTypeGroup.setVisibility(needsBody ? View.VISIBLE : View.GONE);
        bodyInputLayout.setVisibility(needsBody ? View.VISIBLE : View.GONE);
    }
    
    private void updateContentTypeHeader() {
        String contentType;
        if (bodyTypeForm.isChecked()) {
            contentType = "application/x-www-form-urlencoded";
        } else if (bodyTypeJson.isChecked()) {
            contentType = "application/json";
        } else {
            contentType = "text/plain";
        }
        
        // 查找并更新或添加Content-Type头
        boolean found = false;
        for (RequestHeader header : headers) {
            if (header.getKey().equalsIgnoreCase("Content-Type")) {
                header.setValue(contentType);
                found = true;
                break;
            }
        }
        
        if (!found) {
            headers.add(new RequestHeader("Content-Type", contentType));
        }
        
        headerAdapter.notifyDataSetChanged();
        updateHeadersVisibility();
    }

    private void onSendRequest() {
        String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";

        if (url.isEmpty()) {
            urlInput.setError("请输入URL");
            return;
        }

        String errorMessage = UrlValidator.getErrorMessage(url);
        if (errorMessage != null) {
            urlInput.setError(errorMessage);
            Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show();
            return;
        }

        urlInput.setError(null);
        
        String body = null;
        android.net.Uri fileUri = null;
        String method = (String) methodSpinner.getSelectedItem();
        
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
            if (bodyTypeFile.isChecked()) {
                if (selectedFileUri == null) {
                    Snackbar.make(requireView(), "请选择要上传的文件", Snackbar.LENGTH_LONG).show();
                    return;
                }
                fileUri = selectedFileUri;
            } else {
                body = bodyInput.getText() != null ? bodyInput.getText().toString() : "";
                
                if (bodyTypeJson.isChecked() && !body.isEmpty()) {
                    if (!isValidJson(body)) {
                        bodyInput.setError("JSON格式无效");
                        Snackbar.make(requireView(), "JSON格式无效", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                }
            }
        }

        sendHttpRequest(url, method, body, fileUri);
    }
    
    private boolean isValidJson(String json) {
        try {
            new org.json.JSONObject(json);
            return true;
        } catch (Exception e1) {
            try {
                new org.json.JSONArray(json);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private void sendHttpRequest(String url, String method, String body, android.net.Uri fileUri) {
        viewModel.sendRequest(url, method, body, fileUri);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading);
        historyButton.setEnabled(!loading);
        urlInput.setEnabled(!loading);
        methodSpinner.setEnabled(!loading);
    }

    private void showResponse(HttpResponse response) {
        if (response == null) return;
        
        // 隐藏占位符，显示响应容器
        resultPlaceholder.setVisibility(View.GONE);
        responseContainer.setVisibility(View.VISIBLE);
        
        // 设置ViewPager和TabLayout
        ResponsePagerAdapter adapter = new ResponsePagerAdapter(this, response);
        responsePager.setAdapter(adapter);
        
        new TabLayoutMediator(responseTabs, responsePager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("响应体");
                    break;
                case 1:
                    tab.setText("响应头");
                    break;
                case 2:
                    tab.setText("预览");
                    break;
            }
        }).attach();

        if (response.isSuccessful()) {
            Snackbar.make(requireView(), "请求成功", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(requireView(), "请求失败: " + response.getStatusCode(),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void showError(String error) {
        resultPlaceholder.setVisibility(View.VISIBLE);
        responseContainer.setVisibility(View.GONE);
        resultPlaceholder.setText("请求失败:\n" + error);
        Snackbar.make(requireView(), "错误: " + error, Snackbar.LENGTH_LONG).show();
    }

    private void navigateToHistory() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HistoryFragment())
                .addToBackStack(null)
                .commit();
    }

    private void showAddHeaderDialog() {
        showHeaderDialog(-1, null, null);
    }

    private void editHeader(int position) {
        RequestHeader header = headers.get(position);
        showHeaderDialog(position, header.getKey(), header.getValue());
    }

    private void showHeaderDialog(int editPosition, String initialKey, String initialValue) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_header, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextInputLayout keyInputLayout = dialogView.findViewById(R.id.key_input_layout);
        TextInputEditText keyInput = dialogView.findViewById(R.id.key_input);
        TextInputLayout valueInputLayout = dialogView.findViewById(R.id.value_input_layout);
        TextInputEditText valueInput = dialogView.findViewById(R.id.value_input);

        boolean isEdit = editPosition >= 0;
        dialogTitle.setText(isEdit ? "编辑请求头" : "添加请求头");

        if (isEdit) {
            keyInput.setText(initialKey);
            valueInput.setText(initialValue);
        }

        // 设置常用请求头快捷按钮
        Chip chipUserAgent = dialogView.findViewById(R.id.chip_user_agent);
        Chip chipContentType = dialogView.findViewById(R.id.chip_content_type);
        Chip chipAuthorization = dialogView.findViewById(R.id.chip_authorization);
        Chip chipAccept = dialogView.findViewById(R.id.chip_accept);

        chipUserAgent.setOnClickListener(v -> {
            keyInput.setText("User-Agent");
            valueInput.setText("Mozilla/5.0");
        });

        chipContentType.setOnClickListener(v -> {
            keyInput.setText("Content-Type");
            valueInput.setText("application/json");
        });

        chipAuthorization.setOnClickListener(v -> {
            keyInput.setText("Authorization");
            valueInput.setText("Bearer ");
        });

        chipAccept.setOnClickListener(v -> {
            keyInput.setText("Accept");
            valueInput.setText("application/json");
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(isEdit ? "保存" : "添加", (d, which) -> {
                    String key = keyInput.getText() != null ? keyInput.getText().toString().trim() : "";
                    String value = valueInput.getText() != null ? valueInput.getText().toString().trim() : "";

                    if (key.isEmpty()) {
                        Snackbar.make(requireView(), "键名不能为空", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEdit) {
                        updateHeader(editPosition, key, value);
                    } else {
                        addHeader(key, value);
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        dialog.show();
    }

    private void addHeader(String key, String value) {
        viewModel.addHeader(key, value);
        Snackbar.make(requireView(), "已添加请求头", Snackbar.LENGTH_SHORT).show();
    }

    private void updateHeader(int position, String key, String value) {
        viewModel.updateHeader(position, key, value);
        Snackbar.make(requireView(), "已更新请求头", Snackbar.LENGTH_SHORT).show();
    }

    private void updateHeadersVisibility() {
        if (headers.isEmpty()) {
            headersRecycler.setVisibility(View.GONE);
            noHeadersText.setVisibility(View.VISIBLE);
        } else {
            headersRecycler.setVisibility(View.VISIBLE);
            noHeadersText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // ViewModel会自动保存
    }
}
