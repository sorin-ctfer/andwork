package com.example.movinghacker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ResponsePagerAdapter extends FragmentStateAdapter {
    
    private final HttpResponse response;

    public ResponsePagerAdapter(@NonNull Fragment fragment, HttpResponse response) {
        super(fragment);
        this.response = response;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return ResponseBodyFragment.newInstance(response);
            case 1:
                return ResponseHeadersFragment.newInstance(response);
            case 2:
                return ResponsePreviewFragment.newInstance(response);
            default:
                return ResponseBodyFragment.newInstance(response);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
