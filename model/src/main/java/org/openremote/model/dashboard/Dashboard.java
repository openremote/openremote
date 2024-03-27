package org.openremote.model.dashboard;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

import static org.openremote.model.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;

@Entity
@Table(name = "DASHBOARD")
public class Dashboard {

    @Id
    @Column(name = "ID", length = 22, columnDefinition = "char(22)")
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn;

    @NotBlank(message = "{Dashboard.realm.NotBlank}")
    @Size(min = 1, max = 255, message = "{Asset.realm.Size}")
    @Column(name = "REALM", nullable = false, updatable = false)
    protected String realm;

    @Version
    @Column(name = "VERSION", nullable = false)
    protected long version;

    @Column(name = "OWNER_ID", nullable = false)
    protected String ownerId;

    @Column(name = "VIEW_ACCESS", nullable = false)
    protected DashboardAccess viewAccess;

    @Column(name = "EDIT_ACCESS", nullable = false)
    protected DashboardAccess editAccess;

    @NotBlank(message = "{Dashboard.displayName.NotBlank}")
    @Column(name = "DISPLAY_NAME", nullable = false)
    protected String displayName;

    @NotNull(message = "{Dashboard.template.NotNull}")
    @Column(name = "TEMPLATE", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Valid
    protected DashboardTemplate template;


    /* ----------------------------- */

    public Dashboard() {
    }

    public Dashboard(String realm, String displayName, DashboardScreenPreset[] screenPresets) {
        this.realm = realm;
        this.template = new DashboardTemplate(screenPresets);
        this.displayName = displayName;
    }

    public Dashboard setId(String id) {
        this.id = id;
        return this;
    }

    public Dashboard setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public Dashboard setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public Dashboard setVersion(long version) {
        this.version = version;
        return this;
    }

    public Dashboard setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public Dashboard setViewAccess(DashboardAccess access) {
        this.viewAccess = access;
        return this;
    }

    public Dashboard setEditAccess(DashboardAccess access) {
        this.editAccess = access;
        return this;
    }

    public Dashboard setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public Dashboard setTemplate(@Valid DashboardTemplate template) {
        this.template = template;
        return this;
    }


    public String getId() {
        return this.id;
    }

    public Date getCreatedOn() {
        return this.createdOn;
    }

    public String getRealm() {
        return this.realm;
    }

    public long getVersion() {
        return this.version;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public DashboardAccess getViewAccess() {
        return this.viewAccess;
    }

    public DashboardAccess getEditAccess() {
        return this.editAccess;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public DashboardTemplate getTemplate() {
        return this.template;
    }
}
