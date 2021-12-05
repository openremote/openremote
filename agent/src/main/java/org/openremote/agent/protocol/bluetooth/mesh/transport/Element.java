package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.models.SigModel;
import org.openremote.agent.protocol.bluetooth.mesh.models.VendorModel;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Element {

    int locationDescriptor;

    final Map<Integer, MeshModel> meshModels;

    int elementAddress;

    String name;

    /**
     * Constructs an element within a node
     *
     * @param elementAddress     element address
     * @param locationDescriptor location descriptor
     * @param models             models belonging to this element
     */
    public Element(final int elementAddress, final int locationDescriptor, final Map<Integer, MeshModel> models) {
        this(elementAddress, locationDescriptor, models, "Element: " + MeshAddress.formatAddress(elementAddress, true));
    }

    /**
     * Constructs an element within a node
     *
     * @param elementAddress     element address
     * @param locationDescriptor location descriptor
     * @param models             models belonging to this element
     */
    Element(final int elementAddress, final int locationDescriptor, final Map<Integer, MeshModel> models, final String name) {
        this.elementAddress = elementAddress;
        this.locationDescriptor = locationDescriptor;
        this.meshModels = models;
        this.name = name;
    }

    Element(final int locationDescriptor, final Map<Integer, MeshModel> models) {
        this.locationDescriptor = locationDescriptor;
        this.meshModels = models;
    }

    private void sortModels(final HashMap<Integer, MeshModel> unorderedElements) {
        final Set<Integer> unorderedKeys = unorderedElements.keySet();

        final ArrayList<Integer> orderedKeys = new ArrayList<>(unorderedKeys);
        Collections.sort(orderedKeys);
        for (int key : orderedKeys) {
            meshModels.put(key, unorderedElements.get(key));
        }
    }

    /**
     * Returns the address of the element
     */
    public int getElementAddress() {
        return elementAddress;
    }

    void setElementAddress(final int elementAddress) {
        this.elementAddress = elementAddress;
    }

    /**
     * Returns the location descriptor
     */
    public int getLocationDescriptor() {
        return locationDescriptor;
    }

    void setLocationDescriptor(final int locationDescriptor) {
        this.locationDescriptor = locationDescriptor;
    }

    /**
     * Returns the name of the element
     */
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getSigModelCount() {
        int count = 0;
        for (Map.Entry<Integer, MeshModel> modelEntry : meshModels.entrySet()) {
            if (modelEntry.getValue() instanceof SigModel) {
                count++;
            }
        }
        return count;
    }

    public int getVendorModelCount() {
        int count = 0;
        for (Map.Entry<Integer, MeshModel> modelEntry : meshModels.entrySet()) {
            if (modelEntry.getValue() instanceof VendorModel) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns a list of sig models avaialable in this element
     *
     * @return List containing sig models
     */
    public Map<Integer, MeshModel> getMeshModels() {
        return Collections.unmodifiableMap(meshModels);
    }
}
