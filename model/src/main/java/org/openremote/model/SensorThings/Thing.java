
package org.openremote.model.SensorThings;

public class Thing {

    public String iotSelfLink;
    public Integer iotId;
    public String description;
    public String name;

    public Thing(String iotSelfLink, Integer iotId, String description, String name) {
        this.iotSelfLink = iotSelfLink;
        this.iotId = iotId;
        this.description = description;
        this.name = name;
    }

    public String getIotSelfLink() {
        return iotSelfLink;
    }

    public void setIotSelfLink(String iotSelfLink) {
        this.iotSelfLink = iotSelfLink;
    }

    public Integer getIotId() {
        return iotId;
    }

    public void setIotId(Integer iotId) {
        this.iotId = iotId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Thing{" +
                "iotSelfLink='" + iotSelfLink + '\'' +
                ", iotId=" + iotId +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
