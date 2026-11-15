package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sharecare.R;
import com.example.sharecare.utils.WindowInsetsUi;

public class RequestSentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_sent);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        TextView tvSentMessage = findViewById(R.id.tvSentMessage);
        String productName = getIntent().getStringExtra(
                "productName"
        );

        tvSentMessage.setText(
                getString(
                        R.string.request_sent_message,
                        productName == null
                                ? getString(R.string.product_name_sample)
                                : productName
                )
        );

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        findViewById(R.id.btnViewRequests).setOnClickListener(v -> {
            Intent intent = new Intent(
                    this,
                    MyRequestsActivity.class
            );
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            startActivity(intent);
        });

        findViewById(R.id.btnContinueBrowsing).setOnClickListener(v -> {
            Intent intent = new Intent(
                    this,
                    ProductListActivity.class
            );
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            startActivity(intent);
        });
    }
}
