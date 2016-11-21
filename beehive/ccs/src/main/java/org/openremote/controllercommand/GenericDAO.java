/* OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2010, OpenRemote Inc.
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
package org.openremote.controllercommand;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * The general DAO for all the DAO. It provides basic CRUD operations using Hibernate.
 *
 * @author Dan 2009-2-6
 */
public class GenericDAO {

    /**
     * Return the persistent instance of the given entity class with the given non-identifier field.
     *
     * @param <T>        a persistent generics
     * @param clazz      a persistent class
     * @param fieldName  field name
     * @param fieldValue field value
     * @return the persistent instance
     */
    public <T> T getByNonIdField(EntityManager entityManager, Class<T> clazz, String fieldName, String fieldValue) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(clazz);
        Root<T> root = query.from(clazz);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(fieldName), fieldValue));
        return entityManager.createQuery(query).getSingleResult();
    }

}
