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
package org.openremote.model;

import java.util.Arrays;
import java.util.List;

public class PersistenceEvent<T> {

    public enum Cause {
        CREATE, UPDATE, DELETE
    }

    final protected Cause cause;
    final protected T entity;
    final protected List<String> propertyNames;
    final protected Object[] currentState;
    final protected Object[] previousState;

    public PersistenceEvent(Cause cause, T entity, String[] propertyNames, Object[] currentState, Object[] previousState) {
        this.cause = cause;
        this.entity = entity;
        this.propertyNames = propertyNames != null ? Arrays.asList(propertyNames) : null;
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

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public Object[] getCurrentState() {
        return currentState;
    }

    public Object[] getPreviousState() {
        return previousState;
    }

    public boolean hasPropertyChanged(String propertyName) {
        return propertyNames != null && propertyNames.contains(propertyName);
    }

    @SuppressWarnings("unchecked")
    public <E> E getPreviousState(String propertyName) {
        if (propertyNames == null || previousState == null) {
            return null;
        }
        int index = propertyNames.indexOf(propertyName);
        if (index < 0) {
            return null;
        }
        return (E) previousState[index];
    }

    @SuppressWarnings("unchecked")
    public <E> E getCurrentState(String propertyName) {
        if (propertyNames == null || currentState == null) {
            return null;
        }
        int index = propertyNames.indexOf(propertyName);
        if (index < 0) {
            return null;
        }
        return (E) currentState[index];
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "cause=" + cause +
            ", entity=" + entity +
            ", propertyNames=" + String.join(",", propertyNames) +
            ", currentState=" + Arrays.toString(currentState) +
            ", previousState=" + Arrays.toString(previousState) +
            '}';
    }
}
