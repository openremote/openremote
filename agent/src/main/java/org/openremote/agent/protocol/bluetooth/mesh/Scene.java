package org.openremote.agent.protocol.bluetooth.mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Class definitions for creating scenes in a mesh network
 */
public class Scene {
    private final String meshUuid;
    private String name = "nRF Scene";
    protected List<Integer> addresses = new ArrayList<>();
    private int number;

    public Scene(final int number, final String meshUuid) {
        this.number = number;
        this.meshUuid = meshUuid;
    }

    public Scene(final int number, final List<Integer> addresses, final String meshUuid) {
        this.number = number;
        this.addresses.addAll(addresses);
        this.meshUuid = meshUuid;
    }

    public String getMeshUuid() {
        return meshUuid;
    }

    /**
     * Friendly name of the scene
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a friendly name to a scene
     *
     * @param name friendly name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the address of the scene
     *
     * @return 2 byte address
     */
    public List<Integer> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    /**
     * Sets addresses for this group
     *
     * @param addresses list of addresses
     */
    public void setAddresses(final List<Integer> addresses) {
        this.addresses.clear();
        this.addresses.addAll(addresses);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(final int number) {
        this.number = number;
    }

    /**
     * Validates the excene number
     *
     * @param sceneNumber Scene number
     * @return true if is a valid or throws an IllegalArgument exception
     */
    public static boolean isValidSceneNumber(final int sceneNumber) {
        if (sceneNumber > 0x0000 && sceneNumber <= 0xFFFF) return true;
        throw new IllegalArgumentException("Scene number must range from 0x0001 to 0xFFFF!");
    }

    /**
     * Formats the scene number in to a 4 character hexadecimal String
     *
     * @param number Scene number
     * @param add0x  Sets "0x" as prefix if set to true or false otherwise
     */
    public static String formatSceneNumber(final int number, final boolean add0x) {
        return add0x ?
            "0x" + String.format(Locale.US, "%04X", number) :
            String.format(Locale.US, "%04X", number);
    }

    @Override
    public String toString() {
        return "Scene{" +
            "meshUuid='" + meshUuid + '\'' +
            ", name='" + name + '\'' +
            ", addresses=" + addresses +
            ", number=" + number +
            '}';
    }
}

