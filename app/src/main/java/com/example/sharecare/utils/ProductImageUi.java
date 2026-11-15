package com.example.sharecare.utils;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.sharecare.R;

public final class ProductImageUi {

    private ProductImageUi() {
    }

    public static void load(
            ImageView view,
            String imageUrl,
            int imageRes
    ) {

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {

            view.setPadding(0, 0, 0, 0);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(view)
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(view);

            return;
        }

        Glide.with(view).clear(view);

        view.setPadding(0, 0, 0, 0);

        if (imageRes != 0) {
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setImageResource(imageRes);
            return;
        }

        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        view.setImageResource(R.drawable.ic_image_placeholder);
    }
}
