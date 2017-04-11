package org.openremote.android.service;


import java.io.Serializable;
import java.util.List;

public class AlertNotification implements Serializable {
    private Long id;

    private String title;

    private String message;

    private List<AlertAction> actions;

    private String appUrl;

    private String userId;

    private DeliveryStatus deliveryStatus;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AlertAction> getActions() {
        return actions;
    }

    public void setActions(List<AlertAction> actions) {
        this.actions = actions;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    @Override
    public String toString() {
        return "AlertNotification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", actions=" + actions +
                ", appUrl='" + appUrl + '\'' +
                ", userId='" + userId + '\'' +
                ", deliveryStatus=" + deliveryStatus +
                '}';
    }



    public enum ActionType {
        ACTION_TYPE1,
        ACTION_TYPE2
    }
}
