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
package org.openremote.agent.controller2.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Used to handle native sensor change callbacks and notify the component sensor listeners
 */
public class SensorPropertyChangeListener implements PropertyChangeListener {

    private static final Logger LOG = Logger.getLogger(SensorPropertyChangeListener.class.getName());

    final protected List<SensorListener> listeners = new CopyOnWriteArrayList<>();
    final protected String resourceKey;

    public SensorPropertyChangeListener(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public void addListener(SensorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SensorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        LOG.fine("Propagating controller change event to " + listeners.size() + " sensor listeners: " + evt.getNewValue());
        listeners.stream().forEach(sensorListener -> {
            sensorListener.onUpdate(resourceKey, evt.getNewValue());
        });
    }
}
