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
package org.openremote.manager.event;

import jakarta.persistence.EntityManager;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.model.attribute.AttributeEvent;

/**
 * An interceptor that can choose to intercept the {@link org.openremote.model.attribute.AttributeEvent} passing through
 * the system; if it is handled by this interceptor then the event will not be passed to any more interceptors and will
 * not reach the DB.
 */
public interface AttributeEventInterceptor {

    int DEFAULT_PRIORITY = 1000;

    /**
     * Gets the priority of this interceptor which is used to determine the order in which the interceptors are called;
     * interceptors with a lower priority are called first.
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * @param em    The current session and transaction on the database, processors may use this to query additional
     *              data.
     * @param event The {@link AttributeEvent} enriched with {@link org.openremote.model.asset.Asset} and
     *              {@link org.openremote.model.attribute.Attribute} data.
     * @return <code>true</code> if interceptor has handled event and subsequent interceptors should be skipped.
     * @throws AssetProcessingException When processing failed and the update can not continue.
     */
    boolean intercept(EntityManager em, AttributeEvent event) throws AssetProcessingException;
}
