package com.example.sharecare.utils;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class WindowInsetsUi {

    private WindowInsetsUi() {
    }

    public static void addTopPadding(View view) {

        int initialTop = view.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(
                view,
                (target, insets) -> {

                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    target.setPadding(
                            target.getPaddingLeft(),
                            initialTop + bars.top,
                            target.getPaddingRight(),
                            target.getPaddingBottom()
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(view);
    }
}
