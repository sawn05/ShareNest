package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sharecare.R;
import com.example.sharecare.models.Notification;
import com.example.sharecare.models.Product;
import com.example.sharecare.utils.DemoStore;
import com.example.sharecare.utils.NotificationUi;
import com.example.sharecare.utils.WindowInsetsUi;
import com.example.sharecare.utils.TimeUi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent, tvTime;

    private Button btnViewRequest;

    private FrameLayout layoutDetailIcon;
    private ImageView ivDetailIcon;

    private String requestId;
    private String productId;
    private boolean demoNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvContent = findViewById(R.id.tvDetailContent);
        tvTime = findViewById(R.id.tvDetailTime);

        btnViewRequest = findViewById(R.id.btnViewRequest);

        layoutDetailIcon = findViewById(R.id.layoutDetailIcon);
        ivDetailIcon = findViewById(R.id.ivDetailIcon);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        String notificationId =
                getIntent().getStringExtra("notificationId");

        if (notificationId == null) {
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            bindDemoNotification(notificationId);
        } else {
            loadNotification(notificationId);
        }

        btnViewRequest.setOnClickListener(v -> {

            if (requestId != null) {

                Intent intent = new Intent(
                        this,
                        RequestDetailActivity.class
                );

                intent.putExtra("requestId", requestId);

                startActivity(intent);

            } else if (productId != null) {
                openProductDetail();
            }
        });
    }

    private void bindDemoNotification(String notificationId) {

        demoNotification = true;

        Notification notification = DemoStore.get()
                .findNotification(notificationId);

        if (notification == null) {
            finish();
            return;
        }

        tvTitle.setText(notification.getTitle());
        tvContent.setText(notification.getContent());

        String type = notification.getType();

        layoutDetailIcon.setBackgroundResource(
                NotificationUi.circleRes(type)
        );

        ivDetailIcon.setImageResource(
                NotificationUi.iconRes(type)
        );

        ivDetailIcon.setColorFilter(
                ContextCompat.getColor(
                        this,
                        NotificationUi.tintRes(type)
                )
        );

        requestId = notification.getRequestId();
        productId = notification.getProductId();

        renderActionButton();

        tvTime.setText(
                TimeUi.relative(notification.getCreatedAt())
        );
    }

    private void renderActionButton() {

        if (requestId == null) {
            btnViewRequest.setText(R.string.view_product);
        }

        if (requestId == null && productId == null) {
            btnViewRequest.setVisibility(View.GONE);
        }
    }

    private void openProductDetail() {

        Product product = demoNotification
                ? DemoStore.get().findProduct(productId)
                : null;

        Intent intent = new Intent(
                this,
                ProductDetailActivity.class
        );

        intent.putExtra("productId", productId);

        if (product == null) {
            startActivity(intent);
            return;
        }

        intent.putExtra("productName", product.getName());
        intent.putExtra("productCategory", product.getCategory());
        intent.putExtra("productAddress", product.getAddress());
        intent.putExtra(
                "productDescription",
                product.getDescription()
        );
        intent.putExtra("productStatus", product.getStatus());
        intent.putExtra("productOwnerName", product.getOwnerName());
        intent.putExtra("productImageRes", product.getImageRes());
        intent.putExtra("productDemo", true);
        intent.putExtra(
                "productDistanceKm",
                product.getDistanceKm()
        );

        startActivity(intent);
    }

    private void loadNotification(String notificationId) {

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        finish();
                        return;
                    }

                    tvTitle.setText(
                            document.getString("title")
                    );

                    String content =
                            document.getString("content");

                    if (content == null) {
                        content = document.getString("message");
                    }

                    tvContent.setText(content);

                    String type = document.getString("type");

                    layoutDetailIcon.setBackgroundResource(
                            NotificationUi.circleRes(type)
                    );

                    ivDetailIcon.setImageResource(
                            NotificationUi.iconRes(type)
                    );

                    ivDetailIcon.setColorFilter(
                            ContextCompat.getColor(
                                    this,
                                    NotificationUi.tintRes(type)
                            )
                    );

                    requestId = document.getString("requestId");

                    productId = document.getString("productId");

                    renderActionButton();

                    tvTime.setText(
                            TimeUi.relative(
                                    document.getTimestamp("createdAt")
                            )
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}
