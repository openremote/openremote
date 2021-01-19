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
package org.openremote.manager.asset;

import org.openremote.container.concurrent.GlobalLock;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent.Source;

import javax.persistence.EntityManager;

/**
 * Process update of an asset attribute (value), from a {@link Source}.
 *
 * Implementations don't have to lock the whole context through {@link GlobalLock}, the
 * caller of this has already obtained the lock. Internal synchronization and thread-safety
 * must still be maintained if shared state of a processor can be modified by concurrent tasks
 * maintained by that processor.
 */
public interface AssetUpdateProcessor {

    /**
     * @param em        The current session and transaction on the database, processors may use this to query additional data.
     * @param asset     The current asset state with the old value and old value timestamp.
     * @param attribute The attribute to be updated, with new value and value timestamp already set. Value and timestamp can be mutated by processors.
     * @param source    The source of the update.
     * @return <code>true</code> if processing is complete and subsequent processor should be skipped.
     * @throws AssetProcessingException When processing failed and the update can not continue.
     */
    boolean processAssetUpdate(EntityManager em, Asset<?> asset, Attribute<?> attribute, Source source) throws AssetProcessingException;

    /* TODO Processors should be transactional, so an exception in one processor can roll back the update in others */
    // void commit();
    // void rollback();
}
