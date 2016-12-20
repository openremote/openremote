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

public class PersistenceEvent<T> {

    // TODO: Make configurable
    public static final String PERSISTENCE_EVENT_TOPIC =
        "seda://PersistenceTopic?multipleConsumers=true&discardIfNoConsumers=true";

    public static final String HEADER_ENTITY_TYPE = PersistenceEvent.class.getSimpleName() + ".ENTITY_TYPE";

    public enum Cause {
        INSERT, UPDATE, DELETE
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

    public <T> T getPreviousState(String propertyName) {
        return getPreviousState() != null ? (T) getPreviousState()[getPropertyIndex(propertyName)] : null;
    }

    public <T> T getCurrentState(String propertyName) {
        return (T) getCurrentState()[getPropertyIndex(propertyName)];
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
}
