package com.example.sharecare.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.sharecare.models.Notification;
import com.example.sharecare.models.Request;

import org.junit.Before;
import org.junit.Test;

public class DemoStoreTest {

    private DemoStore store;

    @Before
    public void setUp() {
        DemoStore.resetForTesting();
        store = DemoStore.get();
    }

    @Test
    public void providesSeededProductsWithImages() {
        assertEquals(6, store.getProducts().size());
        assertNotNull(store.findProduct("007"));
        assertNotEquals(0, store.findProduct("007").getImageRes());
    }

    @Test
    public void providesSeededRequestsAndNotifications() {
        assertEquals(3, store.getMyRequests().size());
        assertEquals(4, store.getReceivedRequests().size());
        assertEquals(3, store.getNotifications().size());
    }

    @Test
    public void sendingRequestAddsPendingRequestAndNotification() {
        Request request = store.createRequest(
                "demo_2",
                "Mình cần nồi cơm điện cho gia đình."
        );

        assertEquals("PENDING", request.getStatus());
        assertEquals("Nồi cơm điện", request.getProductName());
        assertEquals(
                DemoStore.CURRENT_USER_NAME,
                request.getRequesterName()
        );

        assertTrue(
                store.getMyRequests()
                        .stream()
                        .anyMatch(item -> request.getRequestId()
                                .equals(item.getRequestId()))
        );

        assertEquals(
                "REQUEST_SENT",
                store.getNotifications().get(0).getType()
        );
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsDuplicateActiveRequest() {
        store.createRequest("demo_2", "Yêu cầu lần một");
        store.createRequest("demo_2", "Yêu cầu lần hai");
    }

    @Test
    public void acceptingRequestUpdatesStatusAndReservesProduct() {
        Request pending = store.getReceivedRequests()
                .stream()
                .filter(item -> "PENDING".equals(item.getStatus()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        store.updateRequestStatus(pending.getRequestId(), "ACCEPTED");

        assertEquals(
                "ACCEPTED",
                store.findRequest(pending.getRequestId()).getStatus()
        );

        assertEquals(
                "RESERVED",
                store.findProduct(pending.getProductId()).getStatus()
        );
    }

    @Test
    public void notificationsOnlyBelongToCurrentUser() {
        Request pending = store.getReceivedRequests()
                .stream()
                .filter(item -> "PENDING".equals(item.getStatus()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        store.updateRequestStatus(pending.getRequestId(), "ACCEPTED");

        assertEquals(3, store.getNotifications().size());

        store.getNotifications().forEach(item ->
                assertEquals(
                        DemoStore.CURRENT_USER_ID,
                        item.getUserId()
                )
        );
    }

    @Test
    public void markingNotificationReadPersists() {
        Notification notification = store.getNotifications().get(0);

        store.markNotificationRead(notification.getNotificationId());

        assertTrue(
                store.findNotification(notification.getNotificationId())
                        .isRead()
        );
    }
}
