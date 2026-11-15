package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sharecare.R;
import com.example.sharecare.models.Product;
import com.example.sharecare.models.Request;
import com.example.sharecare.repositories.RequestRepository;
import com.example.sharecare.utils.DemoStore;
import com.example.sharecare.utils.ProductImageUi;
import com.example.sharecare.utils.RequestStatusUi;
import com.example.sharecare.utils.WindowInsetsUi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.os.Build;

import java.util.List;

public class SendRequestActivity extends AppCompatActivity {

    private TextView tvProductName, tvProductAddress, tvProductStatus;
    private EditText edtMessage;
    private Button btnSendRequest;
    private ImageView imgProduct;

    private String productId;
    private String extraProductName;
    private String extraProductAddress;
    private String productImageUrl;
    private int productImageRes;
    private boolean demoProduct;

    private RequestRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_requests);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        tvProductName = findViewById(R.id.tvProductName);
        tvProductAddress = findViewById(R.id.tvProductAddress);
        tvProductStatus = findViewById(R.id.tvProductStatus);
        edtMessage = findViewById(R.id.edtMessage);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        imgProduct = findViewById(R.id.imgProduct);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        repository = new RequestRepository();

        productId = getIntent().getStringExtra("productId");
        extraProductName = getIntent().getStringExtra("productName");
        extraProductAddress = getIntent().getStringExtra(
                "productAddress"
        );
        productImageUrl = getIntent().getStringExtra(
                "productImageUrl"
        );
        productImageRes = getIntent().getIntExtra(
                "productImageRes",
                0
        );
        demoProduct = getIntent().getBooleanExtra(
                "productDemo",
                false
        );

        if (extraProductName != null) {
            tvProductName.setText(extraProductName);
        }

        if (extraProductAddress != null) {
            tvProductAddress.setText(extraProductAddress);
        }

// Tạm thời dùng bài đăng mẫu trên Firestore
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(this, "Không có mã sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindDemoProduct();

        if (!demoProduct
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadProduct();
        }

        btnSendRequest.setOnClickListener(v -> sendRequest());
    }

    private void bindDemoProduct() {

        Product product = DemoStore.get().findProduct(productId);

        if (product == null) {
            return;
        }

        if (extraProductName == null) {
            tvProductName.setText(product.getName());
        }

        if (extraProductAddress == null) {
            tvProductAddress.setText(product.getAddress());
        }

        if (productImageRes == 0) {
            productImageRes = product.getImageRes();
        }

        ProductImageUi.load(
                imgProduct,
                productImageUrl,
                productImageRes
        );

        renderStatus(product.getStatus());
    }

    private void renderStatus(String status) {

        tvProductStatus.setText(RequestStatusUi.label(status));
        tvProductStatus.setBackgroundResource(
                RequestStatusUi.backgroundRes(status)
        );
        tvProductStatus.setTextColor(
                ContextCompat.getColor(
                        this,
                        RequestStatusUi.textColorRes(status)
                )
        );
    }

    private void loadProduct() {

        FirebaseFirestore.getInstance()
                .collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        if (extraProductName == null) {
                            Toast.makeText(this,
                                    "Bài đăng không tồn tại",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        return;
                    }

                    String name = firstString(
                            document,
                            "name",
                            "title"
                    );

                    if (name != null) {
                        tvProductName.setText(name);
                    }

                    String address = firstString(
                            document,
                            "address",
                            "location"
                    );

                    if (address != null) {
                        tvProductAddress.setText(address);
                    }

                    String imageUrl = findImageUrl(document);

                    if (imageUrl != null) {
                        productImageUrl = imageUrl;
                        ProductImageUi.load(
                                imgProduct,
                                productImageUrl,
                                productImageRes
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    if (extraProductName == null) {
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
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

    private void sendRequest() {

        btnSendRequest.setEnabled(false);

        String message = edtMessage.getText()
                .toString()
                .trim();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            sendDemoRequest(message);
            return;
        }

        repository.sendRequest(productId, message)
                .addOnSuccessListener(unused -> {
                    showSystemNotification(
                            "Request sent",
                            "You just sent a request for " + tvProductName.getText().toString()
                    );
                    openSentScreen(
                            tvProductName.getText().toString()
                    );
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    btnSendRequest.setEnabled(true);
                });
    }

    private void sendDemoRequest(String message) {

        try {
            Request request = DemoStore.get().createRequest(
                    productId,
                    message
            );

            openSentScreen(request.getProductName());

        } catch (IllegalStateException e) {
            Toast.makeText(this,
                    e.getMessage(),
                    Toast.LENGTH_LONG).show();

            btnSendRequest.setEnabled(true);
        }
    }

    private void openSentScreen(String productName) {

        Intent intent = new Intent(
                this,
                RequestSentActivity.class
        );

        intent.putExtra("productName", productName);

        startActivity(intent);
        finish();
    }
    @android.annotation.SuppressLint("MissingPermission")
    private void showSystemNotification(String title, String text) {
        requestNotificationPermission();

        String channelId = "sharecare_requests";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "ShareCare",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 200, 100, 200});
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent contentIntent = new Intent(this, NotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        NotificationManagerCompat.from(this).notify(2001, builder.build());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }
}
