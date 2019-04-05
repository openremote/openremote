/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.persistence;

import org.apache.camel.Predicate;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;

import java.util.Arrays;
import java.util.Objects;

public class PersistenceEvent<T> {

    // TODO: Make configurable
    public static final String PERSISTENCE_TOPIC =
        "seda://PersistenceTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=25000";

    public static final String HEADER_ENTITY_TYPE = PersistenceEvent.class.getSimpleName() + ".ENTITY_TYPE";

    public enum Cause {
        INSERT, UPDATE, DELETE
    }

    public static Predicate isPersistenceEventForEntityType(Class<?> type) {
        return exchange -> {
            Class<?> entityType = exchange.getIn().getHeader(PersistenceEvent.HEADER_ENTITY_TYPE, Class.class);
            return type.isAssignableFrom(entityType);
        };
    }

    public static Predicate isPersistenceEventForAssetType(AssetType assetType) {
        return isPersistenceEventForAssetType(assetType.getType());
    }

    public static Predicate isPersistenceEventForAssetType(String assetType) {
        return exchange -> {
            if (!(exchange.getIn().getBody() instanceof PersistenceEvent))
                return false;
            PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
            Asset asset = (Asset) persistenceEvent.getEntity();
            return Objects.equals(asset.getType(), assetType);
        };
    }

    final protected Cause cause;
    final protected T entity;
    final protected String[] propertyNames;
    final protected Object[] currentState;
    final protected Object[] previousState;

    public PersistenceEvent(Cause cause, T entity, String[] propertyNames, Object[] currentState, Object[] previousState) {
        this.cause = cause;
        this.entity = entity;
        this.propertyNames = propertyNames;
        this.currentState = currentState;
        this.previousState = previousState;
    }

    public PersistenceEvent(Cause cause, T entity, String[] propertyNames, Object[] currentState) {
        this(cause, entity, propertyNames, currentState, null);
    }

    public Cause getCause() {
        return cause;
    }

    public T getEntity() {
        return entity;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public Object[] getCurrentState() {
        return currentState;
    }

    public Object[] getPreviousState() {
        return previousState;
    }

    @SuppressWarnings("unchecked")
    public <E> E getPreviousState(String propertyName) {
        return getPreviousState() != null ? (E) getPreviousState()[getPropertyIndex(propertyName)] : null;
    }

    @SuppressWarnings("unchecked")
    public <E> E getCurrentState(String propertyName) {
        return (E) getCurrentState()[getPropertyIndex(propertyName)];
    }

    protected int getPropertyIndex(String propertyName) {
        for (int i = 0; i < getPropertyNames().length; i++) {
            String property = getPropertyNames()[i];
            if (property.equals(propertyName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Property not found: " + propertyName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "cause=" + cause +
            ", entity=" + entity +
            ", propertyNames=" + Arrays.toString(propertyNames) +
            ", currentState=" + Arrays.toString(currentState) +
            ", previousState=" + Arrays.toString(previousState) +
            '}';
    }
}
