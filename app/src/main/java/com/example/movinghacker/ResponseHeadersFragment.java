package com.example.movinghacker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Map;

public class ResponseHeadersFragment extends Fragment {
    
    private static final String ARG_RESPONSE = "response";
    private HttpResponse response;
    private TextView headersText;

    public static ResponseHeadersFragment newInstance(HttpResponse response) {
        ResponseHeadersFragment fragment = new ResponseHeadersFragment();
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
        View view = inflater.inflate(R.layout.fragment_response_headers, container, false);
        headersText = view.findViewById(R.id.headers_text);
        
        if (response != null) {
            displayHeaders();
        }
        
        return view;
    }

    private void displayHeaders() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("协议: %s\n", response.getProtocol()));
        sb.append(String.format("状态: %d %s\n", response.getStatusCode(), response.getStatusMessage()));
        sb.append(String.format("耗时: %d ms\n", response.getDuration()));
        sb.append(String.format("大小: %d bytes\n\n", response.getSize()));
        
        Map<String, String> headers = response.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        } else {
            sb.append("无响应头");
        }
        
        headersText.setText(sb.toString());
    }
}
