/*
 * Copyright 2021, OpenRemote Inc.
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

