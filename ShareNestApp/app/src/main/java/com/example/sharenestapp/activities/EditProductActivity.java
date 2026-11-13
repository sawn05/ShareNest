package com.example.sharenestapp.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sharenestapp.R;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProductActivity extends AppCompatActivity {
    private static final int MAX_IMAGES = 5;
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final String CLOUD_NAME = "ripafvgc";
    private static final String UPLOAD_PRESET = "sharenest_products";
    private static final String UPLOAD_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private EditText edtProductName, edtQuantity, edtDescription;
    private Spinner spCategory, spCondition;
    private TextView txtImageCount, txtProductAddress;
    private LinearLayout layoutImagePreview, btnTakePhoto, btnChooseImages, btnUseCurrentLocation, btnChooseLocation;
    private ImageView btnBack;
    private Button btnPublishProduct;
    private FirebaseFirestore db;
    private String productId, ownerId, address = "";
    private double latitude = Double.NaN, longitude = Double.NaN;
    private boolean saving = false, loaded = false;
    private final ArrayList<Photo> photos = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> gallery, locationPermission;
    private ActivityResultLauncher<Void> camera;
    private ActivityResultLauncher<Intent> mapPicker;

    private static class Photo {
        String remoteUrl;
        Uri uri;
        Bitmap bitmap;
        Photo(String remoteUrl, Uri uri, Bitmap bitmap) {
            this.remoteUrl = remoteUrl; this.uri = uri; this.bitmap = bitmap;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_edit_product);
        productId = getIntent().getStringExtra("productId");
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu productId", Toast.LENGTH_LONG).show(); finish(); return;
        }
        db = FirebaseFirestore.getInstance();
        edtProductName = findViewById(R.id.edtProductName);
        edtQuantity = findViewById(R.id.edtQuantity);
        edtDescription = findViewById(R.id.edtDescription);
        spCategory = findViewById(R.id.spCategory);
        spCondition = findViewById(R.id.spCondition);
        txtImageCount = findViewById(R.id.txtImageCount);
        txtProductAddress = findViewById(R.id.txtProductAddress);
        layoutImagePreview = findViewById(R.id.layoutImagePreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        btnChooseLocation = findViewById(R.id.btnChooseLocation);
        btnBack = findViewById(R.id.btnBack);
        btnPublishProduct = findViewById(R.id.btnPublishProduct);
        spCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Chọn danh mục", "Quần áo", "Sách", "Thực phẩm", "Đồ gia dụng", "Nội thất", "Đồ điện tử", "Khác"}));
        spCondition.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Chọn tình trạng", "Mới", "Còn tốt", "Đã qua sử dụng"}));
        gallery = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null) for (Uri uri : uris) {
                if (photos.size() >= MAX_IMAGES) break;
                if (uri != null && validUri(uri)) photos.add(new Photo(null, uri, null));
            }
            renderPhotos();
        });
        camera = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bmp -> {
            if (bmp != null && photos.size() < MAX_IMAGES) photos.add(new Photo(null, null, bmp));
            renderPhotos();
        });
        mapPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
            Intent data = result.getData();
            double lat = data.getDoubleExtra("latitude", Double.NaN);
            double lng = data.getDoubleExtra("longitude", Double.NaN);
            if (!validCoordinates(lat, lng)) return;
            latitude = lat; longitude = lng;
            address = data.getStringExtra("address");
            if (address == null || address.trim().isEmpty()) address = coordinates(lat, lng);
            txtProductAddress.setText(address);
        });
        locationPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) getCurrentLocation(); else toast("Chưa cấp quyền vị trí");
        });
        btnBack.setOnClickListener(v -> finish());
        layoutImagePreview.setOnClickListener(v -> gallery.launch("image/*"));
        btnChooseImages.setOnClickListener(v -> gallery.launch("image/*"));
        btnTakePhoto.setOnClickListener(v -> {
            if (photos.size() >= MAX_IMAGES) toast("Tối đa 5 ảnh"); else camera.launch(null);
        });
        btnChooseLocation.setOnClickListener(v -> mapPicker.launch(new Intent(this, SelectLocationActivity.class)));
        btnUseCurrentLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) getCurrentLocation();
            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });
        btnPublishProduct.setEnabled(false);
        btnPublishProduct.setOnClickListener(v -> save());
        loadProduct();
    }

    private void loadProduct() {
        db.collection("products").document(productId).get().addOnSuccessListener(doc -> {
            if (!doc.exists() || Boolean.TRUE.equals(doc.getBoolean("isDeleted"))) { toast("Không tìm thấy bài đăng"); finish(); return; }
            String uid = FirebaseAuth.getInstance().getUid();
            ownerId = doc.getString("ownerId");
            if (uid == null || !uid.equals(ownerId)) { toast("Bạn không có quyền chỉnh sửa bài này"); finish(); return; }
            edtProductName.setText(value(doc, "name"));
            Long qty = doc.getLong("quantity");
            edtQuantity.setText(String.valueOf(qty == null ? 1 : qty));
            edtDescription.setText(value(doc, "description"));
            select(spCategory, value(doc, "category"));
            String condition = value(doc, "condition");
            spCondition.setSelection("NEW".equals(condition) ? 1 : "GOOD".equals(condition) ? 2 : "USED".equals(condition) ? 3 : 0);
            address = value(doc, "address");
            Double lat = doc.getDouble("latitude"), lng = doc.getDouble("longitude");
            if (lat != null) latitude = lat;
            if (lng != null) longitude = lng;
            txtProductAddress.setText(address.isEmpty() ? "Chưa chọn địa chỉ" : address);
            photos.clear();
            Object raw = doc.get("imageUrls");
            if (raw instanceof List<?>) {
                for (Object item : (List<?>) raw) {
                    if (item instanceof String && !((String)item).isEmpty() && photos.size() < MAX_IMAGES)
                        photos.add(new Photo((String)item, null, null));
                }
            }
            if (photos.isEmpty() && !value(doc, "coverImageUrl").isEmpty()) photos.add(new Photo(value(doc, "coverImageUrl"), null, null));
            loaded = true; btnPublishProduct.setEnabled(true); renderPhotos();
        }).addOnFailureListener(e -> toast("Không tải được dữ liệu: " + e.getMessage()));
    }

    private String value(DocumentSnapshot doc, String key) {
        String v = doc.getString(key); return v == null ? "" : v;
    }
    private void select(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) if (value.equals(spinner.getItemAtPosition(i).toString())) { spinner.setSelection(i); return; }
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }
    private boolean validCoordinates(double lat, double lng) {
        return !Double.isNaN(lat) && !Double.isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
    private String coordinates(double lat, double lng) { return String.format(Locale.US, "%.6f, %.6f", lat, lng); }

    private void renderPhotos() {
        txtImageCount.setText(photos.size() + "/" + MAX_IMAGES + " ảnh");
        layoutImagePreview.removeAllViews();
        if (photos.isEmpty()) {
            layoutImagePreview.setOrientation(LinearLayout.VERTICAL);
            layoutImagePreview.setGravity(Gravity.CENTER);
            ImageView icon = new ImageView(this); icon.setImageResource(R.drawable.gallery);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(32), dp(32)); ip.gravity = Gravity.CENTER_HORIZONTAL;
            layoutImagePreview.addView(icon, ip);
            TextView label = new TextView(this); label.setText("Thêm ảnh"); label.setTextColor(0xFF247B53); label.setTextSize(13); label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(6);
            layoutImagePreview.addView(label, lp); return;
        }
        layoutImagePreview.setOrientation(LinearLayout.HORIZONTAL);
        layoutImagePreview.setGravity(Gravity.CENTER_VERTICAL);
        for (Photo photo : new ArrayList<>(photos)) {
            LinearLayout column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL); column.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, dp(110), 1);
            layoutImagePreview.addView(column, colParams);
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            column.addView(image, new LinearLayout.LayoutParams(dp(56), dp(76)));
            if (photo.remoteUrl != null) {
                // Dùng Glide (đã có trong dự án) để tải ảnh đang lưu trên Firestore.
                com.bumptech.glide.Glide.with(this).load(photo.remoteUrl).into(image);
            } else if (photo.uri != null) image.setImageURI(photo.uri);
            else if (photo.bitmap != null) image.setImageBitmap(photo.bitmap);
            ImageView remove = new ImageView(this);
            remove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            remove.setContentDescription("Xóa ảnh");
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(22), dp(22)); rp.topMargin = dp(4);
            column.addView(remove, rp);
            remove.setOnClickListener(v -> { photos.remove(photo); renderPhotos(); });
        }
    }

    private boolean validUri(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (!"image/jpeg".equals(mime) && !"image/png".equals(mime)) { toast("Chỉ nhận JPG, PNG"); return false; }
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return false;
            byte[] buffer = new byte[8192]; long size = 0; int n;
            while ((n = in.read(buffer)) != -1) { size += n; if (size > MAX_BYTES) { toast("Ảnh vượt 10 MB"); return false; } }
            return size > 0;
        } catch (Exception e) { toast("Không đọc được ảnh"); return false; }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        txtProductAddress.setText("Đang lấy vị trí...");
        LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(loc -> {
                    if (loc == null) { toast("Không lấy được vị trí"); return; }
                    latitude = loc.getLatitude(); longitude = loc.getLongitude();
                    address = coordinates(latitude, longitude); txtProductAddress.setText(address);
                    double lat = latitude, lng = longitude;
                    executor.execute(() -> {
                        try {
                            List<Address> result = new Geocoder(this, new Locale("vi", "VN")).getFromLocation(lat, lng, 1);
                            if (result != null && !result.isEmpty()) {
                                String resolved = result.get(0).getAddressLine(0);
                                if (resolved != null) runOnUiThread(() -> {
                                    if (latitude == lat && longitude == lng) { address = resolved; txtProductAddress.setText(resolved); }
                                });
                            }
                        } catch (Exception ignored) { }
                    });
                }).addOnFailureListener(e -> toast("Lỗi GPS: " + e.getMessage()));
    }

    private void save() {
        if (!loaded || saving) return;
        String name = edtProductName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String qtyText = edtQuantity.getText().toString().trim();
        if (name.isEmpty()) { edtProductName.setError("Nhập tên vật phẩm"); return; }
        if (spCategory.getSelectedItemPosition() == 0) { toast("Chọn danh mục"); return; }
        if (spCondition.getSelectedItemPosition() == 0) { toast("Chọn tình trạng"); return; }
        if (description.isEmpty()) { edtDescription.setError("Nhập mô tả"); return; }
        int quantity;
        try { quantity = Integer.parseInt(qtyText); if (quantity <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { edtQuantity.setError("Số lượng phải lớn hơn 0"); return; }
        if (photos.isEmpty()) { toast("Cần ít nhất 1 ảnh"); return; }
        if (!validCoordinates(latitude, longitude)) { toast("Chọn vị trí"); return; }
        String category = spCategory.getSelectedItem().toString();
        String condition = spCondition.getSelectedItemPosition() == 1 ? "NEW" : spCondition.getSelectedItemPosition() == 2 ? "GOOD" : "USED";
        saving = true; btnPublishProduct.setEnabled(false); btnPublishProduct.setText("Đang tải ảnh...");
        ArrayList<Photo> snapshot = new ArrayList<>(photos);
        executor.execute(() -> {
            try {
                ArrayList<String> urls = new ArrayList<>();
                for (Photo p : snapshot) urls.add(p.remoteUrl != null ? p.remoteUrl : upload(p));
                runOnUiThread(() -> commitUpdate(name, category, condition, description, quantity, urls));
            } catch (Exception e) { runOnUiThread(() -> failed("Lỗi tải ảnh: " + e.getMessage())); }
        });
    }

    private String upload(Photo photo) throws Exception {
        byte[] bytes;
        String mime = "image/jpeg";
        if (photo.uri != null) {
            if ("image/png".equals(getContentResolver().getType(photo.uri))) mime = "image/png";
            try (InputStream in = getContentResolver().openInputStream(photo.uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) throw new Exception("Không đọc được ảnh");
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) != -1) { out.write(buffer, 0, n); if (out.size() > MAX_BYTES) throw new Exception("Ảnh vượt 10 MB"); }
                bytes = out.toByteArray();
            }
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (photo.bitmap == null || !photo.bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)) throw new Exception("Không nén được ảnh");
            bytes = out.toByteArray();
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new Exception("Dung lượng ảnh không hợp lệ");
        String boundary = "ShareNest" + UUID.randomUUID();
        HttpURLConnection connection = (HttpURLConnection)new URL(UPLOAD_URL).openConnection();
        try {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setConnectTimeout(20000); connection.setReadTimeout(60000);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n" + UPLOAD_PRESET + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo" + ("image/png".equals(mime) ? ".png" : ".jpg") + "\"\r\nContent-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(bytes); out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            try (InputStream in = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) throw new Exception("Không có phản hồi Cloudinary");
                byte[] buffer = new byte[4096]; int n; while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                JSONObject response = new JSONObject(out.toString("UTF-8"));
                if (code < 200 || code >= 300) throw new Exception(response.optJSONObject("error") == null ? "HTTP " + code : response.optJSONObject("error").optString("message"));
                String url = response.optString("secure_url");
                if (!url.startsWith("https://")) throw new Exception("URL Cloudinary không hợp lệ");
                return url;
            }
        } finally { connection.disconnect(); }
    }

    private void commitUpdate(String name, String category, String condition, String description, int quantity, ArrayList<String> urls) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || !uid.equals(ownerId)) { failed("Phiên đăng nhập đã thay đổi"); return; }
        final String finalAddress = address;
        final double finalLat = latitude, finalLng = longitude;
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference ref = db.collection("products").document(productId);
            DocumentSnapshot doc = transaction.get(ref);
            if (!doc.exists() || Boolean.TRUE.equals(doc.getBoolean("isDeleted")))
                throw new FirebaseFirestoreException("Bài đăng không còn tồn tại", FirebaseFirestoreException.Code.ABORTED);
            if (!uid.equals(doc.getString("ownerId")))
                throw new FirebaseFirestoreException("Không có quyền chỉnh sửa", FirebaseFirestoreException.Code.PERMISSION_DENIED);
            long reserved = doc.getLong("quantityReserved") == null ? 0 : doc.getLong("quantityReserved");
            long completed = doc.getLong("quantityCompleted") == null ? 0 : doc.getLong("quantityCompleted");
            if (quantity < reserved + completed)
                throw new FirebaseFirestoreException("Số lượng mới phải >= số đã đặt + đã hoàn thành (" + (reserved + completed) + ")", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            Map<String, Object> changes = new HashMap<>();
            changes.put("name", name); changes.put("category", category);
            changes.put("condition", condition); changes.put("description", description);
            changes.put("quantity", quantity);
            changes.put("quantityAvailable", quantity - reserved - completed);
            changes.put("address", finalAddress); changes.put("latitude", finalLat); changes.put("longitude", finalLng);
            changes.put("imageUrls", urls); changes.put("coverImageUrl", urls.get(0));
            changes.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(ref, changes);
            return null;
        }).addOnSuccessListener(unused -> { saving = false; toast("Đã cập nhật vật phẩm"); setResult(Activity.RESULT_OK); finish(); })
          .addOnFailureListener(e -> failed("Không lưu được Firestore: " + e.getMessage()));
    }
    private void failed(String msg) { saving = false; btnPublishProduct.setEnabled(true); btnPublishProduct.setText("Lưu thay đổi"); toast(msg); }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
