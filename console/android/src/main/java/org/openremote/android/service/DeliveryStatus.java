package org.openremote.android.service;


enum DeliveryStatus {
    /**
     * Not delivered to queue.
     */
    PENDING,

    /**
     * Delivered to queue for at least one device, device should soon pick it up.
     */
    QUEUED,

    /**
     * Picked up by receiving device and dismissed by user.
     */
    ACKNOWLEDGED
}
