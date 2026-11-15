package com.example.sharecare.utils;

import com.example.sharecare.models.Notification;
import com.example.sharecare.models.Product;
import com.example.sharecare.models.Request;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public final class DemoStore {

    public static final String CURRENT_USER_ID = "demo_current_user";
    public static final String CURRENT_USER_NAME = "Nguyễn Văn A";

    private static DemoStore instance;

    private final List<Product> products;
    private final List<Request> requests = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();

    private int requestCounter;
    private int notificationCounter;

    private DemoStore() {
        products = DemoProducts.create();
        seedRequests();
        seedNotifications();
    }

    public static synchronized DemoStore get() {
        if (instance == null) {
            instance = new DemoStore();
        }
        return instance;
    }

    static synchronized void resetForTesting() {
        instance = null;
    }

    public List<Product> getProducts() {
        return new ArrayList<>(products);
    }

    public Product findProduct(String productId) {
        for (Product product : products) {
            if (product.getId().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    public List<Request> getMyRequests() {
        return filterRequests(true);
    }

    public List<Request> getReceivedRequests() {
        return filterRequests(false);
    }

    public Request findRequest(String requestId) {
        for (Request request : requests) {
            if (request.getRequestId().equals(requestId)) {
                return request;
            }
        }
        return null;
    }

    public Request createRequest(String productId, String message) {

        Product product = findProduct(productId);

        if (product == null) {
            throw new IllegalStateException("Bài đăng không tồn tại");
        }

        if (!"AVAILABLE".equals(product.getStatus())) {
            throw new IllegalStateException(
                    "Bài đăng không còn khả dụng"
            );
        }

        for (Request existing : requests) {
            if (productId.equals(existing.getProductId())
                    && CURRENT_USER_ID.equals(
                    existing.getRequesterId()
            )
                    && ("PENDING".equals(existing.getStatus())
                    || "ACCEPTED".equals(existing.getStatus()))) {

                throw new IllegalStateException(
                        "Bạn đã gửi yêu cầu cho bài đăng này"
                );
            }
        }

        Timestamp now = now();

        Request request = new Request();
        request.setRequestId("demo_request_" + (++requestCounter));
        request.setProductId(productId);
        request.setProductName(product.getName());
        request.setProductImageRes(product.getImageRes());
        request.setRequesterId(CURRENT_USER_ID);
        request.setRequesterName(CURRENT_USER_NAME);
        request.setOwnerId(product.getOwnerId());
        request.setOwnerName(product.getOwnerName());
        request.setMessage(message);
        request.setStatus("PENDING");
        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        requests.add(request);

        addNotification(
                CURRENT_USER_ID,
                "REQUEST_SENT",
                "Đã gửi yêu cầu nhận",
                "Yêu cầu nhận " + product.getName()
                        + " đã được gửi và đang chờ phản hồi.",
                request.getRequestId(),
                productId,
                false
        );

        return request;
    }

    public void updateRequestStatus(String requestId, String status) {

        Request request = findRequest(requestId);

        if (request == null) {
            throw new IllegalStateException(
                    "Yêu cầu không tồn tại"
            );
        }

        request.setStatus(status);
        request.setUpdatedAt(now());

        if ("ACCEPTED".equals(status)) {
            Product product = findProduct(request.getProductId());

            if (product != null) {
                product.setStatus("RESERVED");
            }
        }

        if ("ACCEPTED".equals(status)
                || "REJECTED".equals(status)) {

            boolean accepted = "ACCEPTED".equals(status);

            addNotification(
                    request.getRequesterId(),
                    accepted
                            ? "REQUEST_ACCEPTED"
                            : "REQUEST_REJECTED",
                    accepted
                            ? "Yêu cầu của bạn đã được chấp nhận"
                            : "Yêu cầu của bạn đã bị từ chối",
                    accepted
                            ? request.getOwnerName()
                            + " đã chấp nhận yêu cầu nhận "
                            + request.getProductName() + "."
                            : request.getOwnerName()
                            + " đã từ chối yêu cầu nhận "
                            + request.getProductName() + ".",
                    request.getRequestId(),
                    request.getProductId(),
                    false
            );
        }
    }

    public List<Notification> getNotifications() {

        List<Notification> result = new ArrayList<>();

        for (Notification notification : notifications) {
            if (CURRENT_USER_ID.equals(notification.getUserId())) {
                result.add(notification);
            }
        }

        result.sort(
                Comparator.comparingLong(
                        (Notification notification) ->
                                timeOf(notification.getCreatedAt())
                ).reversed()
        );

        return result;
    }

    public Notification findNotification(String notificationId) {
        for (Notification notification : notifications) {
            if (notification.getNotificationId()
                    .equals(notificationId)) {
                return notification;
            }
        }
        return null;
    }

    public void markNotificationRead(String notificationId) {
        Notification notification = findNotification(notificationId);

        if (notification != null) {
            notification.setRead(true);
        }
    }

    private List<Request> filterRequests(boolean mine) {

        List<Request> result = new ArrayList<>();

        for (Request request : requests) {
            boolean matches = mine
                    ? CURRENT_USER_ID.equals(request.getRequesterId())
                    : CURRENT_USER_ID.equals(request.getOwnerId());

            if (matches) {
                result.add(request);
            }
        }

        result.sort(
                Comparator.comparingLong(
                        (Request request) ->
                                timeOf(request.getCreatedAt())
                ).reversed()
        );

        return result;
    }

    private void seedRequests() {

        long now = System.currentTimeMillis();

        addRequest(
                "demo_req_1",
                "007",
                "demo_requester_1",
                "Nguyễn Thị Mai",
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                "Mình rất cần cho em nhỏ mùa đông này. "
                        + "Mong bạn chia sẻ. Cảm ơn bạn!",
                "PENDING",
                now - 2 * 60 * 60 * 1000L
        );

        addRequest(
                "demo_req_2",
                "demo_2",
                "demo_requester_2",
                "Trần Văn Nam",
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                "Nhà mình đang cần nồi cơm điện, "
                        + "bạn có thể chia sẻ không?",
                "PENDING",
                now - 5 * 60 * 60 * 1000L
        );

        addRequest(
                "demo_req_3",
                "demo_3",
                "demo_requester_3",
                "Lê Thị Hoa",
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                "Mình muốn xin cuốn sách này cho con gái.",
                "ACCEPTED",
                now - 24 * 60 * 60 * 1000L
        );

        addRequest(
                "demo_req_4",
                "demo_5",
                "demo_requester_4",
                "Phạm Minh Đức",
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                "Mong bạn chia sẻ máy sưởi cho gia đình mình.",
                "REJECTED",
                now - 2 * 24 * 60 * 60 * 1000L
        );

        addRequest(
                "demo_my_1",
                "demo_6",
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                "demo_owner_6",
                "Đỗ Thị F",
                "Mình cần áo sơ mi trắng size M cho công việc.",
                "PENDING",
                now - 3 * 60 * 60 * 1000L
        );

        addRequest(
                "demo_my_2",
                "demo_4",
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                "demo_owner_4",
                "Phạm Thị D",
                "Mình cần bàn học cho con chuẩn bị năm học mới.",
                "ACCEPTED",
                now - 24 * 60 * 60 * 1000L
        );

        addRequest(
                "demo_my_3",
                "demo_3",
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                "demo_owner_3",
                "Lê Văn C",
                "Mình muốn xin sách để đọc cùng con.",
                "REJECTED",
                now - 3 * 24 * 60 * 60 * 1000L
        );
    }

    private void addRequest(
            String requestId,
            String productId,
            String requesterId,
            String requesterName,
            String ownerId,
            String ownerName,
            String message,
            String status,
            long createdMillis
    ) {
        Product product = findProduct(productId);

        Request request = new Request();
        request.setRequestId(requestId);
        request.setProductId(productId);
        request.setProductName(
                product == null ? null : product.getName()
        );
        request.setProductImageRes(
                product == null ? 0 : product.getImageRes()
        );
        request.setRequesterId(requesterId);
        request.setRequesterName(requesterName);
        request.setOwnerId(ownerId);
        request.setOwnerName(ownerName);
        request.setMessage(message);
        request.setStatus(status);

        Timestamp timestamp = new Timestamp(new Date(createdMillis));
        request.setCreatedAt(timestamp);
        request.setUpdatedAt(timestamp);

        requests.add(request);
    }

    private void seedNotifications() {

        long now = System.currentTimeMillis();

        addNotification(
                CURRENT_USER_ID,
                "NEW_REQUEST",
                "Yêu cầu nhận nhu yếu phẩm mới",
                "Nguyễn Thị Mai đã gửi yêu cầu nhận "
                        + "Áo khoác mùa đông.",
                "demo_req_1",
                "007",
                false,
                now - 2 * 60 * 60 * 1000L
        );

        addNotification(
                CURRENT_USER_ID,
                "REQUEST_ACCEPTED",
                "Yêu cầu của bạn đã được chấp nhận",
                "Phạm Thị D đã chấp nhận yêu cầu nhận "
                        + "Bàn học gỗ của bạn.",
                "demo_my_2",
                "demo_4",
                false,
                now - 24 * 60 * 60 * 1000L
        );

        addNotification(
                CURRENT_USER_ID,
                "REQUEST_REJECTED",
                "Yêu cầu của bạn đã bị từ chối",
                "Lê Văn C đã từ chối yêu cầu nhận "
                        + "Sách tiểu thuyết.",
                "demo_my_3",
                "demo_3",
                true,
                now - 3 * 24 * 60 * 60 * 1000L
        );
    }

    private void addNotification(
            String userId,
            String type,
            String title,
            String content,
            String requestId,
            String productId,
            boolean read
    ) {
        addNotification(
                userId,
                type,
                title,
                content,
                requestId,
                productId,
                read,
                System.currentTimeMillis()
        );
    }

    private void addNotification(
            String userId,
            String type,
            String title,
            String content,
            String requestId,
            String productId,
            boolean read,
            long createdMillis
    ) {
        Notification notification = new Notification();
        notification.setNotificationId(
                "demo_notification_" + (++notificationCounter)
        );
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRequestId(requestId);
        notification.setProductId(productId);
        notification.setRead(read);
        notification.setCreatedAt(
                new Timestamp(new Date(createdMillis))
        );

        notifications.add(notification);
    }

    private Timestamp now() {
        return new Timestamp(new Date());
    }

    private static long timeOf(Timestamp timestamp) {
        return timestamp == null
                ? 0
                : timestamp.toDate().getTime();
    }
}
