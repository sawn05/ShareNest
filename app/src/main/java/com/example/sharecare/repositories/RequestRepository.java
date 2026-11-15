package com.example.sharecare.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RequestRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public RequestRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // 1. Gửi yêu cầu nhận nhu yếu phẩm
    public Task<Void> sendRequest(
            String productId,
            String message
    ) {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("Bạn chưa đăng nhập");
        }

        String requesterId = auth.getCurrentUser().getUid();

        DocumentReference productRef =
                db.collection("products").document(productId);

        // Mỗi người chỉ có một document yêu cầu cho một sản phẩm.
        // Có thể gửi lại sau khi yêu cầu trước bị từ chối/hủy.
        String requestId = productId + "_" + requesterId;

        DocumentReference requestRef =
                db.collection("requests").document(requestId);

        DocumentReference notificationRef =
                db.collection("notifications").document();

        DocumentReference sentNotificationRef =
                db.collection("notifications").document();

        return db.runTransaction(transaction -> {

            DocumentSnapshot product =
                    transaction.get(productRef);

            if (!product.exists()) {
                throw new IllegalStateException(
                        "Bài đăng không tồn tại"
                );
            }

            String ownerId = product.getString("ownerId");
            String productStatus = product.getString("status");
            String productName = firstString(
                    product,
                    "name",
                    "title"
            );
            String ownerName = firstString(
                    product,
                    "ownerName",
                    "owner_name"
            );
            String productImageUrl = findImageUrl(product);

            if (ownerId == null || ownerId.equals(requesterId)) {
                throw new IllegalStateException(
                        "Bạn không thể gửi yêu cầu cho bài đăng của mình"
                );
            }

            if (!"AVAILABLE".equals(productStatus)) {
                throw new IllegalStateException(
                        "Bài đăng không còn khả dụng"
                );
            }

            DocumentSnapshot oldRequest =
                    transaction.get(requestRef);

            if (oldRequest.exists()) {
                String oldStatus = oldRequest.getString("status");

                if ("PENDING".equals(oldStatus)
                        || "ACCEPTED".equals(oldStatus)) {

                    throw new IllegalStateException(
                            "Bạn đã gửi yêu cầu cho bài đăng này"
                    );
                }
            }

            Map<String, Object> request = new HashMap<>();

            request.put("requestId", requestId);
            request.put("productId", productId);
            request.put("productName", productName);
            request.put("productImageUrl", productImageUrl);
            request.put("requesterId", requesterId);
            request.put("ownerId", ownerId);
            request.put("productOwnerId", ownerId);
            request.put("ownerName", ownerName);
            request.put("quantityRequested", 1);
            request.put("message", message);
            request.put("status", "PENDING");
            request.put("createdAt", FieldValue.serverTimestamp());
            request.put("updatedAt", FieldValue.serverTimestamp());

            transaction.set(requestRef, request);

            // Thông báo cho người đăng bài
            Map<String, Object> notification =
                    new HashMap<>();

            notification.put("userId", ownerId);
            notification.put("title", "Yêu cầu nhận nhu yếu phẩm mới");
            notification.put(
                    "content",
                    "Có người muốn nhận nhu yếu phẩm của bạn."
            );
            notification.put(
                    "message",
                    "Có người muốn nhận nhu yếu phẩm của bạn."
            );
            notification.put("type", "NEW_REQUEST");
            notification.put("requestId", requestId);
            notification.put("productId", productId);
            notification.put("isRead", false);
            notification.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(notificationRef, notification);

            Map<String, Object> sentNotification =
                    new HashMap<>();

            sentNotification.put("userId", requesterId);
            sentNotification.put(
                    "title",
                    "Đã gửi yêu cầu nhận"
            );
            String sentContent = "Yêu cầu nhận "
                    + displayName(productName, "nhu yếu phẩm")
                    + " đã được gửi và đang chờ phản hồi.";
            sentNotification.put("content", sentContent);
            sentNotification.put("message", sentContent);
            sentNotification.put("type", "REQUEST_SENT");
            sentNotification.put("requestId", requestId);
            sentNotification.put("productId", productId);
            sentNotification.put("isRead", false);
            sentNotification.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(
                    sentNotificationRef,
                    sentNotification
            );

            return null;
        });
    }

    private String firstString(
            DocumentSnapshot document,
            String... keys
    ) {
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

        if (images instanceof java.util.List) {
            for (Object image : (java.util.List<?>) images) {
                if (image instanceof String
                        && !((String) image).trim().isEmpty()) {
                    return (String) image;
                }
            }
        }

        return null;
    }

    private String displayName(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value;
    }

    // 2. Chấp nhận yêu cầu
    public Task<Void> acceptRequest(String requestId) {

        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("Bạn chưa đăng nhập");
        }

        String currentUserId = auth.getCurrentUser().getUid();

        DocumentReference requestRef =
                db.collection("requests").document(requestId);

        DocumentReference notificationRef =
                db.collection("notifications").document();

        return db.runTransaction(transaction -> {

            DocumentSnapshot request =
                    transaction.get(requestRef);

            if (!request.exists()) {
                throw new IllegalStateException(
                        "Yêu cầu không tồn tại"
                );
            }

            String ownerId = request.getString("ownerId");

            if (ownerId == null) {
                ownerId = request.getString("productOwnerId");
            }

            String requesterId = request.getString("requesterId");
            String productId = request.getString("productId");
            String status = request.getString("status");

            if (!currentUserId.equals(ownerId)) {
                throw new IllegalStateException(
                        "Bạn không có quyền xử lý yêu cầu này"
                );
            }

            if (!"PENDING".equals(status)) {
                throw new IllegalStateException(
                        "Yêu cầu đã được xử lý"
                );
            }

            DocumentReference productRef =
                    db.collection("products").document(productId);

            DocumentSnapshot product =
                    transaction.get(productRef);

            if (!product.exists()
                    || !"AVAILABLE".equals(product.getString("status"))) {

                throw new IllegalStateException(
                        "Bài đăng không còn khả dụng"
                );
            }

            // Cập nhật yêu cầu
            transaction.update(requestRef,
                    "status", "ACCEPTED",
                    "updatedAt", FieldValue.serverTimestamp()
            );

            // Đánh dấu sản phẩm đã được giữ chỗ
            transaction.update(productRef,
                    "status", "RESERVED"
            );

            // Thông báo cho người gửi yêu cầu
            Map<String, Object> notification =
                    new HashMap<>();

            notification.put("userId", requesterId);
            notification.put(
                    "title",
                    "Yêu cầu của bạn đã được chấp nhận"
            );
            notification.put(
                    "content",
                    "Người đăng bài đã chấp nhận yêu cầu nhận nhu yếu phẩm."
            );
            notification.put(
                    "message",
                    "Người đăng bài đã chấp nhận yêu cầu nhận nhu yếu phẩm."
            );
            notification.put("type", "REQUEST_ACCEPTED");
            notification.put("requestId", requestId);
            notification.put("productId", productId);
            notification.put("isRead", false);
            notification.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(notificationRef, notification);

            return null;
        });
    }

    // 3. Từ chối yêu cầu
    public Task<Void> rejectRequest(String requestId) {

        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("Bạn chưa đăng nhập");
        }

        String currentUserId = auth.getCurrentUser().getUid();

        DocumentReference requestRef =
                db.collection("requests").document(requestId);

        DocumentReference notificationRef =
                db.collection("notifications").document();

        return db.runTransaction(transaction -> {

            DocumentSnapshot request =
                    transaction.get(requestRef);

            if (!request.exists()) {
                throw new IllegalStateException(
                        "Yêu cầu không tồn tại"
                );
            }

            String ownerId = request.getString("ownerId");

            if (ownerId == null) {
                ownerId = request.getString("productOwnerId");
            }

            String requesterId = request.getString("requesterId");
            String productId = request.getString("productId");
            String status = request.getString("status");

            if (!currentUserId.equals(ownerId)) {
                throw new IllegalStateException(
                        "Bạn không có quyền xử lý yêu cầu này"
                );
            }

            if (!"PENDING".equals(status)) {
                throw new IllegalStateException(
                        "Yêu cầu đã được xử lý"
                );
            }

            transaction.update(requestRef,
                    "status", "REJECTED",
                    "updatedAt", FieldValue.serverTimestamp()
            );

            Map<String, Object> notification =
                    new HashMap<>();

            notification.put("userId", requesterId);
            notification.put(
                    "title",
                    "Yêu cầu của bạn đã bị từ chối"
            );
            notification.put(
                    "content",
                    "Người đăng bài đã từ chối yêu cầu nhận nhu yếu phẩm."
            );
            notification.put(
                    "message",
                    "Người đăng bài đã từ chối yêu cầu nhận nhu yếu phẩm."
            );
            notification.put("type", "REQUEST_REJECTED");
            notification.put("requestId", requestId);
            notification.put("productId", productId);
            notification.put("isRead", false);
            notification.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(notificationRef, notification);

            return null;
        });
    }
}