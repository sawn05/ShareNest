
package com.example.sharenestapp.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
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

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.LinearLayout;
public class SelectLocationActivity extends AppCompatActivity {

    private static final String MAP_STYLE =
            "https://tiles.openfreemap.org/styles/liberty";

    private MapView mapView;
    private MapLibreMap map;
    private EditText edtSearchLocation;
    private TextView txtSelectedAddress, txtRadius;
    private FusedLocationProviderClient locationClient;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private double selectedLatitude = 21.0285;
    private double selectedLongitude = 105.8542;
    private String selectedAddress = "";
    private int radiusKm = 5;
    private int addressRequestVersion = 0;

    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Phải khởi tạo MapLibre trước khi nạp XML chứa MapView.
        MapLibre.getInstance(getApplicationContext());

        setContentView(R.layout.activity_select_location);

        locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        mapView = findViewById(R.id.mapView);
        edtSearchLocation = findViewById(R.id.edtSearchLocation);
        txtSelectedAddress = findViewById(R.id.txtSelectedAddress);
        txtRadius = findViewById(R.id.txtRadius);

        ImageView btnBack = findViewById(R.id.btnBack);
        LinearLayout btnCurrentLocation =
                findViewById(R.id.btnCurrentLocation);
        Button btnConfirmLocation =
                findViewById(R.id.btnConfirmLocation);
        SeekBar seekRadius = findViewById(R.id.seekRadius);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        getCurrentLocation();
                    } else {
                        showToast("Bạn chưa cấp quyền vị trí");
                    }
                }
        );

        btnBack.setOnClickListener(v -> finish());

        btnCurrentLocation.setOnClickListener(
                v -> requestCurrentLocation()
        );

        btnConfirmLocation.setOnClickListener(
                v -> confirmLocation()
        );

        seekRadius.setMax(29);
        seekRadius.setProgress(4);
        txtRadius.setText("5 km");

        seekRadius.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        radiusKm = progress + 1;
                        txtRadius.setText(radiusKm + " km");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                }
        );

        edtSearchLocation.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        searchAddress(
                                edtSearchLocation.getText().toString().trim()
                        );
                        return true;
                    }
                    return false;
                }
        );

        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(readyMap -> {
            map = readyMap;

            map.setStyle(MAP_STYLE, style -> {
                map.setCameraPosition(
                        new CameraPosition.Builder()
                                .target(new LatLng(
                                        selectedLatitude,
                                        selectedLongitude
                                ))
                                .zoom(15.0)
                                .build()
                );

                updateFromMapCenter();
            });

            map.addOnCameraIdleListener(() -> updateFromMapCenter());
        });
    }

    private void updateFromMapCenter() {
        if (map == null) return;

        LatLng center = map.getCameraPosition().target;

        if (center == null) return;

        selectedLatitude = center.getLatitude();
        selectedLongitude = center.getLongitude();

        // Đổi ngay tọa độ, sau đó mới tìm tên địa chỉ.
        selectedAddress = String.format(
                Locale.US,
                "%.6f, %.6f",
                selectedLatitude,
                selectedLongitude
        );

        txtSelectedAddress.setText(selectedAddress);

        reverseGeocode(selectedLatitude, selectedLongitude);
    }

    private void moveMapTo(double latitude, double longitude) {
        selectedLatitude = latitude;
        selectedLongitude = longitude;

        if (map != null) {
            map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            new LatLng(latitude, longitude),
                            16.0
                    )
            );
        } else {
            reverseGeocode(latitude, longitude);
        }
    }

    private void requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            permissionLauncher.launch(
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

        locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                new CancellationTokenSource().getToken()
        ).addOnSuccessListener(location -> {
            if (location != null) {
                moveMapTo(
                        location.getLatitude(),
                        location.getLongitude()
                );
            } else {
                showToast("Không lấy được GPS. Hãy bật vị trí.");
            }
        }).addOnFailureListener(e ->
                showToast("Lỗi GPS: " + e.getMessage())
        );
    }

    private void searchAddress(String keyword) {
        if (keyword.isEmpty()) {
            showToast("Hãy nhập địa chỉ");
            return;
        }

        txtSelectedAddress.setText("Đang tìm địa chỉ...");

        executor.execute(() -> {
            try {
                Geocoder geocoder =
                        new Geocoder(this, new Locale("vi", "VN"));

                List<Address> results =
                        geocoder.getFromLocationName(keyword, 1);

                runOnUiThread(() -> {
                    if (results != null && !results.isEmpty()) {
                        Address address = results.get(0);
                        moveMapTo(
                                address.getLatitude(),
                                address.getLongitude()
                        );
                    } else {
                        txtSelectedAddress.setText(
                                "Không tìm thấy địa chỉ"
                        );
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    txtSelectedAddress.setText(
                            "Không tìm được địa chỉ"
                    );
                    showToast("Lỗi tìm kiếm: " + e.getMessage());
                });
            }
        });
    }

    private void reverseGeocode(double latitude, double longitude) {
        int requestVersion = ++addressRequestVersion;

        executor.execute(() -> {
            String addressText = String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    latitude,
                    longitude
            );

            try {
                Geocoder geocoder =
                        new Geocoder(this, new Locale("vi", "VN"));

                List<Address> results =
                        geocoder.getFromLocation(
                                latitude,
                                longitude,
                                1
                        );

                if (results != null && !results.isEmpty()) {
                    String line = results.get(0).getAddressLine(0);

                    if (line != null && !line.isEmpty()) {
                        addressText = line;
                    }
                }
            } catch (Exception ignored) {
                // Nếu Geocoder lỗi vẫn giữ tọa độ đã chọn.
            }

            String finalAddress = addressText;

            runOnUiThread(() -> {
                if (requestVersion != addressRequestVersion
                        || isFinishing() || isDestroyed()) {
                    return;
                }

                selectedAddress = finalAddress;
                txtSelectedAddress.setText(finalAddress);
            });
        });
    }

    private void confirmLocation() {
        if (map != null) {
            LatLng center = map.getCameraPosition().target;

            if (center != null) {
                double latitude = center.getLatitude();
                double longitude = center.getLongitude();

                if (Math.abs(latitude - selectedLatitude) > 0.000001
                        || Math.abs(longitude - selectedLongitude) > 0.000001) {
                    selectedLatitude = latitude;
                    selectedLongitude = longitude;

                    selectedAddress = String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            latitude,
                            longitude
                    );
                }
            }
        }

        Intent result = new Intent();

        result.putExtra("latitude", selectedLatitude);
        result.putExtra("longitude", selectedLongitude);
        result.putExtra("address", selectedAddress);
        result.putExtra("radiusKm", radiusKm);

        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        addressRequestVersion++;
        executor.shutdownNow();
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
