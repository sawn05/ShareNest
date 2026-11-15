package com.example.sharecare.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sharecare.R;
import com.example.sharecare.models.Request;
import com.example.sharecare.utils.DemoStore;
import com.example.sharecare.utils.ProductImageUi;
import com.example.sharecare.utils.RequestStatusUi;
import com.example.sharecare.utils.WindowInsetsUi;
import com.example.sharecare.utils.TimeUi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RequestDetailActivity extends AppCompatActivity {

    private TextView tvProductName;
    private TextView tvRequester;
    private TextView tvMessage;
    private TextView tvStatus;
    private TextView tvRequestTime;
    private ImageView imgProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        tvProductName = findViewById(R.id.tvProductName);
        tvRequester = findViewById(R.id.tvRequester);
        tvMessage = findViewById(R.id.tvMessage);
        tvStatus = findViewById(R.id.tvStatus);
        tvRequestTime = findViewById(R.id.tvRequestTime);
        imgProduct = findViewById(R.id.imgRequestProduct);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        String requestId =
                getIntent().getStringExtra("requestId");

        if (requestId == null) {
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            bindDemoRequest(requestId);
            return;
        }

        loadRequest(requestId);
    }

    private void bindDemoRequest(String requestId) {

        Request request = DemoStore.get().findRequest(requestId);

        if (request == null) {
            finish();
            return;
        }

        tvProductName.setText(
                request.getProductName() == null
                        ? "Nhu yếu phẩm"
                        : request.getProductName()
        );

        tvRequester.setText(
                request.getRequesterName() == null
                        ? "Không xác định"
                        : request.getRequesterName()
        );

        tvMessage.setText(
                request.getMessage() == null
                        || request.getMessage().trim().isEmpty()
                        ? getString(R.string.no_message)
                        : request.getMessage()
        );

        renderStatus(request.getStatus());

        tvRequestTime.setText(
                TimeUi.relative(request.getCreatedAt())
        );

        ProductImageUi.load(
                imgProduct,
                request.getProductImageUrl(),
                request.getProductImageRes()
        );
    }

    private void renderStatus(String status) {

        tvStatus.setText(RequestStatusUi.label(status));

        tvStatus.setBackgroundResource(
                RequestStatusUi.backgroundRes(status)
        );

        tvStatus.setTextColor(
                ContextCompat.getColor(
                        this,
                        RequestStatusUi.textColorRes(status)
                )
        );
    }

    private String firstString(DocumentSnapshot document, String... keys) {
        for (String key : keys) {
            String value = document.getString(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String findImageUrl(DocumentSnapshot document) {

        String imageUrl = firstString(
                document,
                "coverImageUrl",
                "imageUrl",
                "image_url"
        );

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl;
        }

        Object images = document.get("imageUrls");

        if (images instanceof List) {
            for (Object image : (List<?>) images) {
                if (image instanceof String
                        && !((String) image).trim().isEmpty()) {
                    return (String) image;
                }
            }
        }

        return null;
    }

    private void loadRequest(String requestId) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(request -> {

                    if (!request.exists()) {
                        finish();
                        return;
                    }

                    String productId =
                            request.getString("productId");

                    String productName =
                            request.getString("productName");

                    tvProductName.setText(
                            productName == null || productName.isEmpty()
                                    ? "Nhu yếu phẩm #" + productId
                                    : productName
                    );

                    String productImageUrl =
                            request.getString("productImageUrl");

                    ProductImageUi.load(
                            imgProduct,
                            productImageUrl,
                            0
                    );

                    String message = request.getString("message");

                    tvMessage.setText(
                            message == null || message.isEmpty()
                                    ? "Không có lời nhắn"
                                    : message
                    );

                    String requesterName =
                            request.getString("requesterName");

                    if (requesterName == null || requesterName.isEmpty()) {
                        requesterName =
                                request.getString("requesterId");
                    }

                    tvRequester.setText(
                            requesterName == null
                                    ? "Không xác định"
                                    : requesterName
                    );

                    String status = request.getString("status");

                    tvStatus.setText(RequestStatusUi.label(status));

                    tvStatus.setBackgroundResource(
                            RequestStatusUi.backgroundRes(status)
                    );

                    tvStatus.setTextColor(
                            ContextCompat.getColor(
                                    this,
                                    RequestStatusUi.textColorRes(status)
                            )
                    );

                    tvRequestTime.setText(
                            TimeUi.relative(request.getTimestamp("createdAt"))
                    );

                    if (productId != null) {

                        db.collection("products")
                                .document(productId)
                                .get()
                                .addOnSuccessListener(product -> {

                                    if (product.exists()) {
                                        String name = firstString(
                                                product,
                                                "name",
                                                "title"
                                        );

                                        if (name != null) {
                                            tvProductName.setText(name);
                                        }

                                        String imageUrl =
                                                findImageUrl(product);

                                        if (imageUrl != null) {
                                            ProductImageUi.load(
                                                    imgProduct,
                                                    imageUrl,
                                                    0
                                            );
                                        }
                                    } else {
                                        tvProductName.setText(
                                                "Nhu yếu phẩm #" + productId
                                        );
                                    }
                                })
                                .addOnFailureListener(e ->
                                        tvProductName.setText(
                                                "Nhu yếu phẩm #" + productId
                                        )
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}
