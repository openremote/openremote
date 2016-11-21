/* OpenRemote, the Home of the Digital Home.
* Copyright 2008-2011, OpenRemote Inc.
*
* See the contributors.txt file in the distribution for a
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

package org.openremote.controllercommand.domain;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;


/**
 * Business entity class for all JPA entities with the common property oid.
 *
 * @author Marcus
 */
@MappedSuperclass
public abstract class BusinessEntity implements Serializable {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 754736926051991600L;

    /**
     * The oid.
     */
    private long oid;

    /**
     * Instantiates a new business entity.
     */
    public BusinessEntity() {
        super();
    }

    /**
     * Instantiates a new business entity.
     *
     * @param oid the oid
     */
    public BusinessEntity(long oid) {
        super();
        this.oid = oid;
    }

    /**
     * Gets the oid.
     *
     * @return the oid
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getOid() {
        return oid;
    }

    /**
     * Sets the oid.
     *
     * @param oid the new oid
     */
    public void setOid(long oid) {
        this.oid = oid;
    }

}
