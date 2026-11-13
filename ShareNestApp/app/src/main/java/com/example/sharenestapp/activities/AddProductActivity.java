package com.example.sharenestapp.activities;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sharenestapp.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddProductActivity extends AppCompatActivity {

    private static final int MAX_IMAGES = 5;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    // Phải trùng với collection mà HomeActivity đang đọc.
    private static final String PRODUCT_COLLECTION = "products";

    private EditText edtProductName;
    private EditText edtQuantity;
    private EditText edtDescription;

    private Spinner spCategory;
    private Spinner spCondition;

    private TextView txtImageCount;
    private TextView txtProductAddress;

    private LinearLayout layoutImagePreview;
    private LinearLayout btnTakePhoto;
    private LinearLayout btnChooseImages;
    private LinearLayout btnUseCurrentLocation;
    private LinearLayout btnChooseLocation;

    private ImageView btnBack;
    private Button btnPublishProduct;

    private FirebaseFirestore firestore;

    private FirebaseAuth auth;
    private FusedLocationProviderClient locationClient;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final ArrayList<SelectedImage> selectedImages =
            new ArrayList<>();

    private double selectedLatitude = Double.NaN;
    private double selectedLongitude = Double.NaN;
    private String selectedAddress = "";

    private boolean isPublishing = false;

    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<Intent> locationPickerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    // Một ảnh có thể đến từ thư viện hoặc từ camera.
    private static class SelectedImage {
        Uri uri;
        Bitmap bitmap;

        SelectedImage(Uri uri) {
            this.uri = uri;
        }

        SelectedImage(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }
    private static final String CLOUD_NAME = "ripafvgc";

    private static final String UPLOAD_PRESET = "sharenest_products";

    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/"
                    + CLOUD_NAME
                    + "/image/upload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_product);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        initViews();
        registerLaunchers();
        setupSpinners();
        setupListeners();
        updateImagePreview();
    }

    // =========================
    // 1. ÁNH XẠ GIAO DIỆN
    // =========================

    private void initViews() {

        edtProductName = findViewById(R.id.edtProductName);
        edtQuantity = findViewById(R.id.edtQuantity);
        edtDescription = findViewById(R.id.edtDescription);

        spCategory = findViewById(R.id.spCategory);
        spCondition = findViewById(R.id.spCondition);

        txtImageCount = findViewById(R.id.txtImageCount);
        txtProductAddress = findViewById(R.id.txtProductAddress);

        layoutImagePreview =
                findViewById(R.id.layoutImagePreview);

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseImages = findViewById(R.id.btnChooseImages);

        btnUseCurrentLocation =
                findViewById(R.id.btnUseCurrentLocation);

        btnChooseLocation =
                findViewById(R.id.btnChooseLocation);

        btnBack = findViewById(R.id.btnBack);
        btnPublishProduct =
                findViewById(R.id.btnPublishProduct);
    }

    // =========================
    // 2. ĐĂNG KÝ ACTIVITY RESULT
    // =========================

    private void registerLaunchers() {

        // Xin quyền GPS.
        locationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> {
                            if (granted) {
                                getCurrentLocation();
                            } else {
                                showToast("Bạn chưa cấp quyền vị trí");
                            }
                        }
                );

        // Nhận kết quả từ SelectLocationActivity.
        locationPickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() != Activity.RESULT_OK
                                    || result.getData() == null) {
                                return;
                            }

                            Intent data = result.getData();

                            double latitude = data.getDoubleExtra(
                                    "latitude",
                                    Double.NaN
                            );

                            double longitude = data.getDoubleExtra(
                                    "longitude",
                                    Double.NaN
                            );

                            if (!isValidCoordinates(latitude, longitude)) {
                                showToast("Tọa độ không hợp lệ");
                                return;
                            }

                            selectedLatitude = latitude;
                            selectedLongitude = longitude;

                            String address =
                                    data.getStringExtra("address");

                            selectedAddress =
                                    address == null ? "" : address.trim();

                            if (selectedAddress.isEmpty()) {
                                selectedAddress = formatCoordinates(
                                        latitude,
                                        longitude
                                );
                            }

                            txtProductAddress.setText(selectedAddress);
                        }
                );

        // Chọn nhiều ảnh từ thư viện.
        galleryLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetMultipleContents(),
                        uris -> {

                            if (uris == null || uris.isEmpty()) {
                                return;
                            }

                            int added = 0;

                            for (Uri uri : uris) {

                                if (selectedImages.size() >= MAX_IMAGES) {
                                    break;
                                }

                                if (uri != null && isValidImage(uri)) {
                                    selectedImages.add(
                                            new SelectedImage(uri)
                                    );
                                    added++;
                                }
                            }

                            if (added == 0) {
                                showToast("Không thêm được ảnh hợp lệ");
                            }

                            updateImagePreview();
                        }
                );

        // Chụp ảnh bằng ứng dụng camera.
        // TakePicturePreview trả về ảnh xem trước.
        cameraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicturePreview(),
                        bitmap -> {

                            if (bitmap == null) {
                                return;
                            }

                            if (selectedImages.size() >= MAX_IMAGES) {
                                showToast("Chỉ được chọn tối đa 5 ảnh");
                                return;
                            }

                            selectedImages.add(
                                    new SelectedImage(bitmap)
                            );

                            updateImagePreview();
                        }
                );
    }

    // =========================
    // 3. DANH MỤC VÀ TÌNH TRẠNG
    // =========================

    private void setupSpinners() {

        String[] categories = {
                "Chọn danh mục",
                "Quần áo",
                "Sách",
                "Thực phẩm",
                "Đồ gia dụng",
                "Nội thất",
                "Đồ điện tử",
                "Khác"
        };

        String[] conditions = {
                "Chọn tình trạng",
                "Mới",
                "Còn tốt",
                "Đã qua sử dụng"
        };

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> conditionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        conditions
                );

        conditionAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spCondition.setAdapter(conditionAdapter);
    }

    // =========================
    // 4. XỬ LÝ SỰ KIỆN
    // =========================

    private void setupListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnTakePhoto.setOnClickListener(v -> openCamera());

        btnChooseImages.setOnClickListener(
                v -> openGallery()
        );

        layoutImagePreview.setOnClickListener(
                v -> openGallery()
        );

        btnUseCurrentLocation.setOnClickListener(
                v -> requestCurrentLocation()
        );

        btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AddProductActivity.this,
                    SelectLocationActivity.class
            );

            locationPickerLauncher.launch(intent);
        });

        btnPublishProduct.setOnClickListener(
                v -> showPublishConfirmDialog()
        );
    }

    // =========================
    // 5. CHỌN ẢNH
    // =========================

    private void openGallery() {

        if (selectedImages.size() >= MAX_IMAGES) {
            showToast("Bạn đã chọn đủ 5 ảnh");
            return;
        }

        galleryLauncher.launch("image/*");
    }

    private void openCamera() {

        if (selectedImages.size() >= MAX_IMAGES) {
            showToast("Bạn đã chọn đủ 5 ảnh");
            return;
        }

        cameraLauncher.launch(null);
    }

    private boolean isValidImage(Uri uri) {
        try {
            String mimeType = getContentResolver().getType(uri);

            if (!"image/jpeg".equals(mimeType)
                    && !"image/png".equals(mimeType)) {
                return false;
            }

            try (InputStream stream =
                         getContentResolver().openInputStream(uri)) {

                if (stream == null) return false;

                byte[] buffer = new byte[8192];
                int count;
                long total = 0;

                while ((count = stream.read(buffer)) != -1) {
                    total += count;

                    if (total > MAX_IMAGE_BYTES) {
                        return false;
                    }
                }

                return total > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // =========================
    // 6. HIỂN THỊ ẢNH ĐÃ CHỌN
    // =========================


    private void updateImagePreview() {

        txtImageCount.setText(
                selectedImages.size() + "/" + MAX_IMAGES + " ảnh"
        );

        layoutImagePreview.removeAllViews();

        // Chưa chọn ảnh: hiển thị icon và chữ ở giữa
        if (selectedImages.isEmpty()) {

            layoutImagePreview.setOrientation(LinearLayout.VERTICAL);
            layoutImagePreview.setGravity(android.view.Gravity.CENTER);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.gallery);

            LinearLayout.LayoutParams iconParams =
                    new LinearLayout.LayoutParams(
                            dp(32),
                            dp(32)
                    );

            iconParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(iconParams);

            TextView label = new TextView(this);
            label.setText("Thêm ảnh");
            label.setTextColor(0xFF247B53);
            label.setTextSize(13);
            label.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams labelParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            labelParams.topMargin = dp(6);
            label.setLayoutParams(labelParams);

            layoutImagePreview.addView(icon);
            layoutImagePreview.addView(label);

            return;
        }

        // Đã chọn ảnh: hiển thị danh sách ảnh
        layoutImagePreview.setOrientation(LinearLayout.HORIZONTAL);
        layoutImagePreview.setGravity(android.view.Gravity.CENTER_VERTICAL);

        for (int i = 0; i < selectedImages.size(); i++) {

            final int index = i;
            SelectedImage item = selectedImages.get(i);

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams itemParams =
                    new LinearLayout.LayoutParams(
                            dp(82),
                            dp(110)
                    );

            itemParams.setMargins(dp(3), 0, dp(3), 0);
            itemLayout.setLayoutParams(itemParams);

            ImageView imageView = new ImageView(this);

            imageView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            dp(76),
                            dp(76)
                    )
            );

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (item.uri != null) {
                imageView.setImageURI(item.uri);
            } else if (item.bitmap != null) {
                imageView.setImageBitmap(item.bitmap);
            }

            ImageView removeButton = new ImageView(this);

            removeButton.setImageResource(
                    android.R.drawable.ic_menu_close_clear_cancel
            );

            removeButton.setContentDescription("Xóa ảnh");

            LinearLayout.LayoutParams removeParams =
                    new LinearLayout.LayoutParams(
                            dp(22),
                            dp(22)
                    );

            removeParams.topMargin = dp(5);
            removeButton.setLayoutParams(removeParams);

            removeButton.setOnClickListener(v -> {
                selectedImages.remove(index);
                updateImagePreview();
            });

            itemLayout.addView(imageView);
            itemLayout.addView(removeButton);

            layoutImagePreview.addView(itemLayout);
        }
    }


    // =========================
    // 7. LẤY GPS
    // =========================

    private void requestCurrentLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            getCurrentLocation();

        } else {

            locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
        }
    }

    private void getCurrentLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        txtProductAddress.setText(
                "Đang lấy vị trí hiện tại..."
        );

        locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                new CancellationTokenSource().getToken()
        ).addOnSuccessListener(location -> {

            if (location == null) {
                txtProductAddress.setText(
                        "Không lấy được vị trí. Hãy thử lại."
                );
                return;
            }

            selectedLatitude = location.getLatitude();
            selectedLongitude = location.getLongitude();

            selectedAddress = formatCoordinates(
                    selectedLatitude,
                    selectedLongitude
            );

            txtProductAddress.setText(selectedAddress);

            reverseGeocode(
                    selectedLatitude,
                    selectedLongitude
            );

        }).addOnFailureListener(e -> {
            showToast("Lỗi GPS: " + e.getMessage());
        });
    }

    // =========================
    // 8. ĐỔI TỌA ĐỘ THÀNH ĐỊA CHỈ
    // =========================

    private void reverseGeocode(
            double latitude,
            double longitude
    ) {

        executor.execute(() -> {

            String address = formatCoordinates(
                    latitude,
                    longitude
            );

            try {
                Geocoder geocoder =
                        new Geocoder(
                                this,
                                new Locale("vi", "VN")
                        );

                List<Address> results =
                        geocoder.getFromLocation(
                                latitude,
                                longitude,
                                1
                        );

                if (results != null && !results.isEmpty()) {

                    String line =
                            results.get(0).getAddressLine(0);

                    if (line != null && !line.isEmpty()) {
                        address = line;
                    }
                }

            } catch (Exception ignored) {
                // Nếu Geocoder lỗi, vẫn dùng tọa độ.
            }

            String finalAddress = address;

            runOnUiThread(() -> {

                // Tránh ghi đè khi người dùng đã chọn
                // một vị trí khác trong lúc Geocoder chạy.
                if (Math.abs(selectedLatitude - latitude) > 0.000001
                        || Math.abs(selectedLongitude - longitude) > 0.000001) {
                    return;
                }

                selectedAddress = finalAddress;
                txtProductAddress.setText(finalAddress);
            });
        });
    }

    // =========================
    // 9. KIỂM TRA FORM
    // =========================

    private boolean validateForm() {

        String name =
                edtProductName.getText().toString().trim();

        String quantityText =
                edtQuantity.getText().toString().trim();

        String description =
                edtDescription.getText().toString().trim();

        if (name.isEmpty()) {
            edtProductName.setError("Nhập tên vật phẩm");
            edtProductName.requestFocus();
            return false;
        }

        if (spCategory.getSelectedItemPosition() == 0) {
            showToast("Hãy chọn danh mục");
            return false;
        }

        if (quantityText.isEmpty()) {
            edtQuantity.setError("Nhập số lượng");
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityText);

            if (quantity <= 0) {
                edtQuantity.setError(
                        "Số lượng phải lớn hơn 0"
                );
                return false;
            }

        } catch (NumberFormatException e) {
            edtQuantity.setError("Số lượng không hợp lệ");
            return false;
        }

        if (spCondition.getSelectedItemPosition() == 0) {
            showToast("Hãy chọn tình trạng");
            return false;
        }

        if (description.isEmpty()) {
            edtDescription.setError("Nhập mô tả");
            return false;
        }

        if (selectedImages.isEmpty()) {
            showToast("Hãy thêm ít nhất 1 ảnh");
            return false;
        }

        if (!isValidCoordinates(
                selectedLatitude,
                selectedLongitude
        )) {
            showToast("Hãy chọn vị trí vật phẩm");
            return false;
        }

//        if (auth.getCurrentUser() == null) {
//            showToast("Bạn cần đăng nhập trước khi đăng bài");
//            return false;
//        }

        return true;
    }

    // =========================
    // 10. ĐĂNG BÀI
    // =========================

    private void publishProduct() {

        if (isPublishing) return;

        if (!validateForm()) return;

        isPublishing = true;

        btnPublishProduct.setEnabled(false);
        btnPublishProduct.setText("Đang chuẩn bị đăng bài...");

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            startPublishing(user.getUid());
            return;
        }

        // Chưa đăng nhập thì tự đăng nhập ẩn danh.
        auth.signInAnonymously()
                .addOnSuccessListener(result -> {

                    FirebaseUser anonymousUser = result.getUser();

                    if (anonymousUser == null) {
                        uploadFailed("Không tạo được tài khoản ẩn danh");
                        return;
                    }

                    startPublishing(anonymousUser.getUid());
                })
                .addOnFailureListener(e -> {

                    uploadFailed(
                            "Lỗi đăng nhập ẩn danh: " + e.getMessage()
                    );
                });
    }

    private void startPublishing(String ownerId) {

        btnPublishProduct.setText("Đang tải ảnh...");

        String productId = firestore
                .collection(PRODUCT_COLLECTION)
                .document()
                .getId();

        ArrayList<String> imageUrls = new ArrayList<>();

        // Upload tất cả ảnh trước khi lưu Firestore
        uploadNextImage(
                productId,
                ownerId,
                0,
                imageUrls
        );
    }
    // =========================
    // 11. UPLOAD TỪNG ẢNH
    // =========================

    private void uploadNextImage(
            String productId,
            String ownerId,
            int index,
            ArrayList<String> imageUrls
    ) {

        if (index >= selectedImages.size()) {

            // Tất cả ảnh đã upload thành công
            saveProductToFirestore(
                    productId,
                    ownerId,
                    imageUrls
            );

            return;
        }

        btnPublishProduct.setText(
                "Đang tải ảnh "
                        + (index + 1)
                        + "/"
                        + selectedImages.size()
        );

        SelectedImage item = selectedImages.get(index);

        // Upload ảnh trong luồng nền
        executor.execute(() -> {

            try {

                String imageUrl = uploadImageToCloudinary(item);

                runOnUiThread(() -> {

                    imageUrls.add(imageUrl);

                    Log.d(
                            "CLOUDINARY",
                            "Upload thành công: " + imageUrl
                    );

                    // Upload ảnh tiếp theo
                    uploadNextImage(
                            productId,
                            ownerId,
                            index + 1,
                            imageUrls
                    );
                });

            } catch (Exception e) {

                Log.e(
                        "CLOUDINARY",
                        "Upload ảnh thất bại",
                        e
                );

                runOnUiThread(() ->
                        uploadFailed(
                                "Lỗi tải ảnh "
                                        + (index + 1)
                                        + ": "
                                        + e.getMessage()
                        )
                );
            }
        });
    }

    private String uploadImageToCloudinary(
            SelectedImage item
    ) throws Exception {

        byte[] imageBytes;

        if (item.uri != null) {

            try (InputStream input =
                         getContentResolver().openInputStream(item.uri);
                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                if (input == null) {
                    throw new Exception("Không đọc được ảnh");
                }

                byte[] buffer = new byte[8192];
                int length;

                while ((length = input.read(buffer)) != -1) {

                    output.write(buffer, 0, length);

                    if (output.size() > MAX_IMAGE_BYTES) {
                        throw new Exception("Ảnh vượt quá 10 MB");
                    }
                }

                imageBytes = output.toByteArray();
            }

        } else if (item.bitmap != null) {

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            boolean compressed = item.bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    85,
                    output
            );

            if (!compressed) {
                throw new Exception("Không nén được ảnh");
            }

            imageBytes = output.toByteArray();

        } else {
            throw new Exception("Ảnh không hợp lệ");
        }

        if (imageBytes.length == 0
                || imageBytes.length > MAX_IMAGE_BYTES) {

            throw new Exception("Dung lượng ảnh không hợp lệ");
        }

        String boundary =
                "ShareNest" + UUID.randomUUID();

        HttpURLConnection connection = null;

        try {

            URL url = new URL(CLOUDINARY_UPLOAD_URL);

            connection = (HttpURLConnection)
                    url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);

            connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            String mimeType = "image/jpeg";

            if (item.uri != null) {
                String detected =
                        getContentResolver().getType(item.uri);

                if ("image/png".equals(detected)) {
                    mimeType = "image/png";
                }
            }

            String extension = "image/png".equals(mimeType)
                    ? ".png" : ".jpg";

            try (OutputStream output =
                         connection.getOutputStream()) {

                String presetPart =
                        "--" + boundary + "\r\n"
                                + "Content-Disposition: form-data; "
                                + "name=\"upload_preset\"\r\n\r\n"
                                + UPLOAD_PRESET + "\r\n";

                output.write(
                        presetPart.getBytes(StandardCharsets.UTF_8)
                );

                String filePart =
                        "--" + boundary + "\r\n"
                                + "Content-Disposition: form-data; "
                                + "name=\"file\"; filename=\""
                                + UUID.randomUUID()
                                + extension
                                + "\"\r\n"
                                + "Content-Type: "
                                + mimeType
                                + "\r\n\r\n";

                output.write(
                        filePart.getBytes(StandardCharsets.UTF_8)
                );

                output.write(imageBytes);

                output.write(
                        "\r\n".getBytes(StandardCharsets.UTF_8)
                );

                String ending =
                        "--" + boundary + "--\r\n";

                output.write(
                        ending.getBytes(StandardCharsets.UTF_8)
                );
            }

            int responseCode =
                    connection.getResponseCode();

            InputStream responseStream =
                    responseCode >= 200 && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            String response;

            try (InputStream input = responseStream;
                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                if (input == null) {
                    throw new Exception(
                            "Cloudinary không trả về dữ liệu"
                    );
                }

                byte[] buffer = new byte[4096];
                int length;

                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }

                response = output.toString("UTF-8");
            }

            if (responseCode < 200 || responseCode >= 300) {

                String message = response;

                try {
                    JSONObject errorJson =
                            new JSONObject(response);

                    message = errorJson
                            .getJSONObject("error")
                            .optString("message", response);

                } catch (Exception ignored) {
                }

                throw new Exception(
                        "Cloudinary HTTP "
                                + responseCode
                                + ": "
                                + message
                );
            }

            JSONObject json = new JSONObject(response);

            String secureUrl =
                    json.optString("secure_url", "");

            if (!secureUrl.startsWith("https://")) {

                throw new Exception(
                        "Cloudinary không trả về URL ảnh hợp lệ"
                );
            }

            return secureUrl;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    // =========================
    // 12. LƯU FIRESTORE
    // =========================

    private void saveProductToFirestore(
            String productId,
            String ownerId,
            ArrayList<String> imageUrls
    ) {

        btnPublishProduct.setText("Đang đăng bài...");

        String name =
                edtProductName.getText().toString().trim();

        String category =
                spCategory.getSelectedItem().toString();

        int quantity = Integer.parseInt(
                edtQuantity.getText().toString().trim()
        );

        String description =
                edtDescription.getText().toString().trim();

        String condition;

        switch (spCondition.getSelectedItemPosition()) {
            case 1:
                condition = "NEW";
                break;

            case 2:
                condition = "GOOD";
                break;

            case 3:
                condition = "USED";
                break;

            default:
                condition = "GOOD";
                break;
        }

        Map<String, Object> product = new HashMap<>();

        product.put("name", name);
        product.put("category", category);
        product.put("quantity", quantity);
        product.put("quantityAvailable", quantity);
        product.put("quantityReserved", 0);
        product.put("quantityCompleted", 0);

        product.put("condition", condition);
        product.put("description", description);

        product.put("address", selectedAddress);
        product.put("latitude", selectedLatitude);
        product.put("longitude", selectedLongitude);

        product.put("imageUrls", imageUrls);
        product.put(
                "coverImageUrl",
                imageUrls.isEmpty() ? "" : imageUrls.get(0)
        );

        product.put("ownerId", ownerId);
        product.put("status", "AVAILABLE");
        product.put("moderationStatus", "VISIBLE");
        product.put("isDeleted", false);

        product.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        product.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        firestore.collection(PRODUCT_COLLECTION)
                .document(productId)
                .set(product)
                .addOnSuccessListener(unused -> {

                    isPublishing = false;

                    Intent intent = new Intent(
                            AddProductActivity.this,
                            PublishSuccessActivity.class
                    );

                    intent.putExtra("productId", productId);

                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {

                    isPublishing = false;

                    btnPublishProduct.setEnabled(true);
                    btnPublishProduct.setText("Đăng bài");

                    showToast(
                            "Lỗi lưu Firestore: " + e.getMessage()
                    );
                });
    }

    private void uploadFailed(String message) {

        isPublishing = false;

        btnPublishProduct.setEnabled(true);
        btnPublishProduct.setText("Đăng bài");

        showToast(message);
    }
    private void showPublishConfirmDialog() {
        if (!validateForm()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng bài")
                .setMessage(
                        "Bạn có chắc chắn muốn đăng vật phẩm này?\n\n" +
                                "Sau khi đăng, mọi người có thể nhìn thấy " +
                                "vật phẩm và gửi yêu cầu nhận."
                )
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("Đăng bài", (dialog, which) -> {
                    publishProduct();
                })
                .show();
    }
    // =========================
    // 13. HÀM HỖ TRỢ
    // =========================

    private boolean isValidCoordinates(
            double latitude,
            double longitude
    ) {
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    private String formatCoordinates(
            double latitude,
            double longitude
    ) {
        return String.format(
                Locale.US,
                "%.6f, %.6f",
                latitude,
                longitude
        );
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density + 0.5f
        );
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
