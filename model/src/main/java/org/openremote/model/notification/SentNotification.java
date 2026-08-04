/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "NOTIFICATION")
@Schema(description = "One target-specific notification delivery record, including delivery and acknowledgement state.")
public class SentNotification {

    /**
     * Columns that {@link SentNotification}s can be ordered by server-side; each maps to a fixed persisted column
     * (see the notification service) so ordering stays injection-safe and consistent across paginated results.
     */
    public enum SortField {
        TITLE,
        SOURCE,
        STATUS,
        SENT_ON,
        DELIVERED_ON
    }

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    @SequenceGenerator(name = PERSISTENCE_SEQUENCE_ID_GENERATOR, initialValue = 1000, allocationSize = 1)
    @Schema(description = "Server-assigned numeric delivery-record identifier.", accessMode = Schema.AccessMode.READ_ONLY, example = "42")
    protected Long id;

    @Column(name = "NAME")
    @Schema(description = "Human-readable notification name.", example = "High temperature alert")
    protected String name;

    @NotNull
    @Column(name = "TYPE", nullable = false, length = 50)
    @Schema(description = "Notification message type discriminator.", example = "push")
    protected String type;

    @NotNull
    @Column(name = "TARGET", length = 50)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Kind of delivery target.")
    protected Notification.TargetType target;

    @NotNull
    @Column(name = "TARGET_ID")
    @Schema(description = "User, asset, realm, or custom target identifier.")
    protected String targetId;

    @NotNull()
    @Column(name = "SOURCE", length = 50)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Kind of actor that created the notification.")
    protected Notification.Source source;

    @NotNull
    @Column(name = "REALM", nullable = false, updatable = false)
    @Schema(description = "Realm associated with the delivery record.", example = "building")
    protected String realm;

    @Column(name = "SOURCE_ID", length = 43)
    @Schema(description = "Identifier of the user, rule, or component that created the notification.")
    protected String sourceId;

    @Column(name = "MESSAGE")
    @JdbcTypeCode(SqlTypes.JSON)
    @Schema(description = "Typed message payload as it was submitted.")
    protected AbstractNotificationMessage message;

    @Column(name = "ERROR", length = 4096)
    @Schema(description = "Delivery error detail when sending failed.")
    protected String error;

    @Column(name = "SENT_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Schema(description = "Time the notification was sent or attempted.", accessMode = Schema.AccessMode.READ_ONLY)
    protected Instant sentOn;

    @Column(name = "DELIVERED_ON", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Schema(description = "Time the target marked the notification delivered.", accessMode = Schema.AccessMode.READ_ONLY)
    protected Instant deliveredOn;

    @Column(name = "ACKNOWLEDGED_ON", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Schema(description = "Time the target acknowledged the notification.", accessMode = Schema.AccessMode.READ_ONLY)
    protected Instant acknowledgedOn;

    @Column(name = "ACKNOWLEDGEMENT")
    @Schema(description = "Optional JSON acknowledgement value serialized as text.")
    protected String acknowledgement;

    public Long getId() {
        return id;
    }

    public SentNotification setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public SentNotification setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public SentNotification setType(String type) {
        this.type = type;
        return this;
    }

    public Instant getSentOn() {
        return sentOn;
    }

    public SentNotification setSentOn(Instant sentOn) {
        this.sentOn = sentOn;
        return this;
    }

    public Notification.Source getSource() {
        return source;
    }

    public SentNotification setSource(Notification.Source source) {
        this.source = source;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public SentNotification setSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public Notification.TargetType getTarget() {
        return target;
    }

    public SentNotification setTarget(Notification.TargetType target) {
        this.target = target;
        return this;
    }

    public String getTargetId() {
        return targetId;
    }

    public SentNotification setTargetId(String targetId) {
        this.targetId = targetId;
        return this;
    }

    public AbstractNotificationMessage getMessage() {
        return message;
    }

    public SentNotification setMessage(AbstractNotificationMessage message) {
        this.message = message;
        return this;
    }

    public Instant getDeliveredOn() {
        return deliveredOn;
    }

    public SentNotification setDeliveredOn(Instant deliveredOn) {
        this.deliveredOn = deliveredOn;
        return this;
    }

    public Instant getAcknowledgedOn() {
        return acknowledgedOn;
    }

    public SentNotification setAcknowledgedOn(Instant acknowledgedOn) {
        this.acknowledgedOn = acknowledgedOn;
        return this;
    }

    public String getAcknowledgement() {
        return acknowledgement;
    }

    public SentNotification setAcknowledgement(String acknowledgement) {
        this.acknowledgement = acknowledgement;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public SentNotification setRealm(String realm) {
        this.realm = realm;
        return this;
    }


    public String getError() {
        return error;
    }

    public SentNotification setError(String error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", target=" + target +
            ", targetId='" + targetId + '\'' +
            ", source=" + source +
            ", sourceId='" + sourceId + '\'' +
            ", message=" + message +
            ", sentOn=" + sentOn +
            ", deliveredOn=" + deliveredOn +
            ", acknowledgedOn=" + acknowledgedOn +
            ", acknowledgement='" + acknowledgement + '\'' +
            '}';
    }
}
