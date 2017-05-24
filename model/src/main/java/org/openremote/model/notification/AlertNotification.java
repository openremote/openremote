package org.openremote.model.notification;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Values;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import static org.openremote.model.Constants.PERSISTENCE_JSON_ARRAY_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "ALERT_NOTIFICATION")
public class AlertNotification {

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    private Long id;

    @NotNull
    @Column(name = "TITLE", nullable = false)
    private String title;

    @NotNull
    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @Column(name = "ACTIONS", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_ARRAY_TYPE)
    private ArrayValue actions = Values.createArray();

    @NotNull
    @Column(name = "APP_URL", nullable = false)
    private String appUrl;

    @NotNull()
    @Column(name = "USER_ID")
    private String userId;

    @NotNull()
    @Column(name = "DELIVERY_STATUS")
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    public AlertNotification() {
    }

    public AlertNotification(String title, String message) {
        this.title = title;
        this.message = message;
    }

    /*  Actuator  */

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

    public void setActions(ArrayValue actions) {
        this.actions = actions;
    }

    public ArrayValue getActions() {
        return actions;
    }

    public void addAction(AlertAction action) {
        this.actions.set(this.actions.length(), action.getObjectValue());
    }

    public void addLinkAction(String title, String url) {
        AlertAction action = new AlertAction();
        action.setActionType(ActionType.LINK);
        action.setTitle(title);
        // TODO: this is currenly on notification, should be on action, but must update console to support that
        setAppUrl(url);
        addAction(action);
    }

    public void addActuatorAction(String title, String assetId, String attributeName, String rawJson ) {
        AlertAction action = new AlertAction(title, ActionType.ACTUATOR, assetId, attributeName, rawJson);
        addAction(action);
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
}
