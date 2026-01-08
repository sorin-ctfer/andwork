package com.example.movinghacker;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseBodyFragment extends Fragment {
    
    private static final String ARG_RESPONSE = "response";
    private HttpResponse response;
    private TextView bodyText;
    private TextInputEditText searchInput;
    private MaterialButton searchButton;
    private MaterialButton clearSearchButton;
    private TextView searchResultText;
    private NestedScrollView bodyScroll;
    
    private String originalBody;
    private List<Integer> searchPositions = new ArrayList<>();
    private int currentSearchIndex = -1;

    public static ResponseBodyFragment newInstance(HttpResponse response) {
        ResponseBodyFragment fragment = new ResponseBodyFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RESPONSE, response);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            response = (HttpResponse) getArguments().getSerializable(ARG_RESPONSE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_response_body, container, false);
        
        bodyText = view.findViewById(R.id.body_text);
        searchInput = view.findViewById(R.id.search_input);
        searchButton = view.findViewById(R.id.search_button);
        clearSearchButton = view.findViewById(R.id.clear_search_button);
        searchResultText = view.findViewById(R.id.search_result_text);
        bodyScroll = view.findViewById(R.id.body_scroll);
        
        if (response != null) {
            displayBody();
            setupSearch();
        }
        
        return view;
    }

    private void displayBody() {
        String body = response.getBody();
        
        // 尝试格式化JSON
        if (isJson(body)) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Object json = JsonParser.parseString(body);
                String formatted = gson.toJson(json);
                originalBody = formatted;
                bodyText.setText(formatted);
            } catch (Exception e) {
                originalBody = body;
                bodyText.setText(body);
            }
        } else {
            originalBody = body;
            bodyText.setText(body);
        }
    }

    private void setupSearch() {
        searchButton.setOnClickListener(v -> performSearch());
        clearSearchButton.setOnClickListener(v -> clearSearch());
        
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = searchInput.getText() != null ? searchInput.getText().toString() : "";
        
        if (query.isEmpty()) {
            return;
        }
        
        searchPositions.clear();
        currentSearchIndex = -1;
        
        // 查找所有匹配位置
        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(originalBody);
        
        while (matcher.find()) {
            searchPositions.add(matcher.start());
        }
        
        if (searchPositions.isEmpty()) {
            searchResultText.setText("未找到匹配内容");
            searchResultText.setVisibility(View.VISIBLE);
            clearSearchButton.setVisibility(View.GONE);
            bodyText.setText(originalBody);
        } else {
            currentSearchIndex = 0;
            highlightSearchResults(query);
            searchResultText.setText(String.format("找到 %d 个匹配项", searchPositions.size()));
            searchResultText.setVisibility(View.VISIBLE);
            clearSearchButton.setVisibility(View.VISIBLE);
            
            // 滚动到第一个匹配位置
            scrollToPosition(searchPositions.get(0));
        }
    }

    private void highlightSearchResults(String query) {
        SpannableString spannable = new SpannableString(originalBody);
        
        for (int position : searchPositions) {
            spannable.setSpan(
                new BackgroundColorSpan(Color.YELLOW),
                position,
                position + query.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        bodyText.setText(spannable);
    }

    private void scrollToPosition(int position) {
        bodyText.post(() -> {
            int line = bodyText.getLayout().getLineForOffset(position);
            int y = bodyText.getLayout().getLineTop(line);
            bodyScroll.smoothScrollTo(0, y);
        });
    }

    private void clearSearch() {
        searchInput.setText("");
        searchPositions.clear();
        currentSearchIndex = -1;
        bodyText.setText(originalBody);
        searchResultText.setVisibility(View.GONE);
        clearSearchButton.setVisibility(View.GONE);
    }

    private boolean isJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        text = text.trim();
        return (text.startsWith("{") && text.endsWith("}")) || 
               (text.startsWith("[") && text.endsWith("]"));
    }
}
