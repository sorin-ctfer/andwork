package com.example.movinghacker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView historyRecycler;
    private TextView emptyText;
    private MaterialButton clearAllButton;
    
    private HistoryRepository historyRepository;
    private HistoryAdapter historyAdapter;
    private final List<RequestHistory> historyList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        historyRepository = new HistoryRepository(requireContext());
        
        initializeViews(view);
        setupRecycler();
        setupListeners();
        loadHistory();
    }

    private void initializeViews(View view) {
        historyRecycler = view.findViewById(R.id.history_recycler);
        emptyText = view.findViewById(R.id.empty_text);
        clearAllButton = view.findViewById(R.id.clear_all_button);
    }

    private void setupRecycler() {
        historyRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new HistoryAdapter(historyList, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onHistoryClick(RequestHistory history) {
                replayRequest(history);
            }

            @Override
            public void onDeleteClick(RequestHistory history) {
                deleteHistory(history);
            }
        });
        historyRecycler.setAdapter(historyAdapter);
    }

    private void setupListeners() {
        clearAllButton.setOnClickListener(v -> clearAllHistory());
    }

    private void loadHistory() {
        historyRepository.getAllHistory(new HistoryRepository.HistoryCallback() {
            @Override
            public void onSuccess(List<RequestHistory> list) {
                requireActivity().runOnUiThread(() -> {
                    historyList.clear();
                    historyList.addAll(list);
                    historyAdapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Snackbar.make(requireView(), "加载历史记录失败: " + error, 
                            Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void replayRequest(RequestHistory history) {
        // 解析请求头
        List<RequestHeader> headers = historyRepository.parseHeaders(history.getHeaders());
        
        // 创建WebRequestFragment并传递数据
        WebRequestFragment fragment = new WebRequestFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", history.getUrl());
        bundle.putString("method", history.getMethod());
        bundle.putSerializable("headers", new ArrayList<>(headers));
        bundle.putString("body", history.getBody());
        fragment.setArguments(bundle);
        
        // 替换当前Fragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        
        Snackbar.make(requireView(), "已加载历史请求，可修改后重新发送", Snackbar.LENGTH_SHORT).show();
    }

    private void deleteHistory(RequestHistory history) {
        historyRepository.deleteHistory(history.getId(), new HistoryRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    historyList.remove(history);
                    historyAdapter.notifyDataSetChanged();
                    updateEmptyState();
                    Snackbar.make(requireView(), "已删除", Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Snackbar.make(requireView(), "删除失败: " + error, 
                            Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void clearAllHistory() {
        historyRepository.clearAllHistory(new HistoryRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    historyList.clear();
                    historyAdapter.notifyDataSetChanged();
                    updateEmptyState();
                    Snackbar.make(requireView(), "已清空全部历史", Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Snackbar.make(requireView(), "清空失败: " + error, 
                            Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateEmptyState() {
        if (historyList.isEmpty()) {
            historyRecycler.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            historyRecycler.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }
}
