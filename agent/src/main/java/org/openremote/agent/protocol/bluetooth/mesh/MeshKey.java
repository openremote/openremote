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

import org.openremote.model.util.TextUtil;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Objects;

abstract class MeshKey implements Cloneable {

    protected int id;
    protected String meshUuid;
    protected int keyIndex;
    protected String name;
    protected byte[] key;
    protected byte[] oldKey;

    MeshKey() {
    }

    MeshKey(final int keyIndex, @NotNull final byte[] key) {
        Objects.requireNonNull(key);

        this.keyIndex = keyIndex;
        if (key.length != 16) {
            throw new IllegalArgumentException("Application key must be 16-bytes");
        }
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    /**
     * Returns the meshUuid of the Mesh network
     *
     * @return String meshUuid
     */
    public String getMeshUuid() {
        return meshUuid;
    }

    /**
     * Sets the meshUuid of the mesh network to this application key
     *
     * @param meshUuid mesh network meshUuid
     */
    public void setMeshUuid(final String meshUuid) {
        this.meshUuid = meshUuid;
    }

    /**
     * Returns a friendly name of the application key
     *
     * @return string containing the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a friendly name of the application key
     *
     * @param name friendly name for the application key
     */
    public void setName(@NotNull final String name) throws IllegalArgumentException {
        if (TextUtil.isNullOrEmpty(name))
            throw new IllegalArgumentException("Name cannot be empty!");
        this.name = name;
    }

    /**
     * Returns the application key
     *
     * @return 16 byte application key
     */
    public byte[] getKey() {
        return key;
    }

    /**
     * Sets a network key.
     *
     * <p>
     * In order to change the key call {@link BaseMeshNetwork#updateNetKey(NetworkKey, String)}  or {@link BaseMeshNetwork#updateAppKey(ApplicationKey, String)})}}
     * </p>
     *
     * @param key 16-byte network key
     */
    public void setKey(@NotNull final byte[] key) {
        Objects.requireNonNull(key);
        if (valid(key)) {
            this.key = key;
        }
    }

    /**
     * Returns the application key index
     *
     * @return key index
     */
    public int getKeyIndex() {
        return keyIndex;
    }

    /**
     * Sets the key index of network key
     *
     * @param keyIndex index
     */
    public void setKeyIndex(final int keyIndex) {
        this.keyIndex = keyIndex;
    }

    /**
     * Returns the old app key
     *
     * @return old key
     */
    public byte[] getOldKey() {
        return oldKey;
    }

    /**
     * Set the old key
     *
     * @param oldKey old app key
     */
    public void setOldKey(final byte[] oldKey) {
        this.oldKey = oldKey;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ApplicationKey) {
            final ApplicationKey appKey = (ApplicationKey) obj;

            return Arrays.equals(key, appKey.key) && keyIndex == appKey.keyIndex;
        } else if (obj instanceof NetworkKey) {
            final NetworkKey netKey = (NetworkKey) obj;

            return Arrays.equals(key, netKey.key) && keyIndex == netKey.keyIndex;
        }
        return false;
    }

    public MeshKey clone() throws CloneNotSupportedException {
        return (MeshKey) super.clone();
    }

    protected boolean valid(@NotNull final byte[] key) {
        Objects.requireNonNull(key);
        if (key.length != 16)
            throw new IllegalArgumentException("Key must be of length 16!");
        return true;
    }

    protected boolean distributeKey(@NotNull final byte[] key) {
        Objects.requireNonNull(key);
        if (!Arrays.equals(this.key, oldKey))
            setOldKey(this.key);
        setKey(key);
        return true;
    }
}