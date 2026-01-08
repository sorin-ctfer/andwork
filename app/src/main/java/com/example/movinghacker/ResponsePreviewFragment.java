package com.example.movinghacker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ResponsePreviewFragment extends Fragment {
    
    private static final String ARG_RESPONSE = "response";
    private HttpResponse response;
    private WebView webView;
    private TextView noPreviewText;

    public static ResponsePreviewFragment newInstance(HttpResponse response) {
        ResponsePreviewFragment fragment = new ResponsePreviewFragment();
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
        View view = inflater.inflate(R.layout.fragment_response_preview, container, false);
        webView = view.findViewById(R.id.web_view);
        noPreviewText = view.findViewById(R.id.no_preview_text);
        
        if (response != null) {
            displayPreview();
        }
        
        return view;
    }

    private void displayPreview() {
        String contentType = getContentType();
        
        if (contentType != null && contentType.contains("text/html")) {
            webView.setVisibility(View.VISIBLE);
            noPreviewText.setVisibility(View.GONE);
            
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            
            webView.loadDataWithBaseURL(null, response.getBody(), "text/html", "UTF-8", null);
        } else {
            webView.setVisibility(View.GONE);
            noPreviewText.setVisibility(View.VISIBLE);
            noPreviewText.setText("此响应类型不支持预览\n内容类型: " + (contentType != null ? contentType : "未知"));
        }
    }

    private String getContentType() {
        if (response.getHeaders() != null) {
            for (String key : response.getHeaders().keySet()) {
                if (key.equalsIgnoreCase("Content-Type")) {
                    return response.getHeaders().get(key);
                }
            }
        }
        return null;
    }
}
