package org.openremote.manager.shared.notification;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JreJsonArray;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openremote.model.Constants.PERSISTENCE_JSON_ARRAY_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_JSON_OBJECT_TYPE;
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
    private JsonArray actions = Json.createArray();

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

    public void setActions(JsonArray  actions) {
        this.actions = actions;
    }

    public JsonArray getActions() {
        return actions;
    }

    public List<AlertAction> getActionsAsList() {
        List actions = new ArrayList<>(this.actions.length());
        for (int i = 0; i < actions.size(); i++) {
            actions.add(this.actions.get(i).toNative());

        }

        return actions;
    }

    public void addAction(AlertAction action) {
        this.actions.set(this.actions.length(), action.getValue());
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
