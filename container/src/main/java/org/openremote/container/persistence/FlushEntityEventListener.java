/*
 * Copyright 2022, OpenRemote Inc.
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

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * This listener will only push the dirty properties through to the interceptor's onFlushDirty.
 *
 * Unfortunately there are internal and private dependencies which makes extending not ideal but it is fine for our
 * use case.
 */
public class FlushEntityEventListener extends DefaultFlushEntityEventListener {

    @Override
    protected boolean handleInterception(FlushEntityEvent event) {
        SessionImplementor session = event.getSession();
        final boolean intercepted = invokeInterceptor( session, event );

        //now we might need to recalculate the dirtyProperties array
        if ( intercepted && event.isDirtyCheckPossible() ) {
            dirtyCheck( event );
        }

        return intercepted;
    }

    protected boolean invokeInterceptor(SessionImplementor session, FlushEntityEvent event) {
        if (event.getDirtyProperties() == null) {
            return false;
        }
        EntityEntry entry = event.getEntityEntry();
        EntityPersister persister = entry.getPersister();
        Object entity = event.getEntity();
        final Object[] values = event.getPropertyValues();

        // Convert the values, properties names (old and new values)
        // and types to arrays that contains the dirty ones.
        int[] dirtyPropertiesIndexes = event.getDirtyProperties();
        int dirtyPropertiesLength = dirtyPropertiesIndexes.length;
        String[] dirtyPropertiesNames = new String[dirtyPropertiesLength];
        Object[] dirtyPropertiesValues = new Object[dirtyPropertiesLength];
        Object[] loadedPropertiesValues = new Object[dirtyPropertiesLength];
        Type[] dirtyPropertiesTypes = new Type[dirtyPropertiesLength];

        for (int i = 0; i < dirtyPropertiesLength; i++) {
            int dirtyPropertyIndex = dirtyPropertiesIndexes[i];
            dirtyPropertiesNames[i] = persister.getPropertyNames()[dirtyPropertyIndex];
            dirtyPropertiesValues[i] = values[dirtyPropertyIndex];
            dirtyPropertiesTypes[i] = persister.getPropertyTypes()[dirtyPropertyIndex];
            loadedPropertiesValues[i] = entry.getLoadedState()[dirtyPropertyIndex];
        }

        return session
            .getInterceptor()
            .onFlushDirty(entity, entry.getId(), dirtyPropertiesValues, loadedPropertiesValues, dirtyPropertiesNames, dirtyPropertiesTypes);
    }

    /**
     * This method was replaced with the invokeInterceptor(SessionImplementor session, FlushEntityEvent event)
     * method.
     *
     * @deprecated
     */
    @Deprecated
    @Override
    protected boolean invokeInterceptor(SessionImplementor session, Object entity, EntityEntry entry,
                                        final Object[] values, EntityPersister persister) {
        throw new UnsupportedOperationException(
            "Since this method was replaced with another one it was not suppose to be invoked.");
    }
}
