package com.example.sharenestapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sharenestapp.R;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRow(view, R.id.rowInfo, "Thông tin cá nhân", () -> {
            // TODO
        });
        setupRow(view, R.id.rowPassword, "Đổi mật khẩu", () -> {
            // TODO
        });
        setupRow(view, R.id.rowMyPosts, "Bài đăng của tôi", () -> {
            // TODO
        });
        setupRow(view, R.id.rowMyRequests, "Yêu cầu của tôi", () -> {
            // TODO
        });
        setupRow(view, R.id.rowLogout, "Đăng xuất", () -> {
            // TODO
        });
    }

    private void setupRow(View root, int rowId, String title, Runnable onClick) {
        View row = root.findViewById(rowId);
        ((TextView) row.findViewById(R.id.tvMenuTitle)).setText(title);
        row.setOnClickListener(v -> onClick.run());
    }
}