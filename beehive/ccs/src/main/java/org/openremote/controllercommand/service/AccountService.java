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
package org.openremote.controllercommand.service;

import org.apache.commons.codec.binary.Base64;
import org.openremote.controllercommand.GenericDAO;
import org.openremote.controllercommand.domain.User;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;

import javax.persistence.EntityManager;


/**
 * Account service implementation.
 *
 * @author Dan Cong
 */
public class AccountService {
    public static final String HTTP_AUTH_HEADER_NAME = "Authorization";
    public static final String HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX = "Basic ";

    protected GenericDAO genericDAO;

    public void setGenericDAO(GenericDAO genericDAO) {
        this.genericDAO = genericDAO;
    }

    public User loadByUsername(EntityManager entityManager, String username) {
        return genericDAO.getByNonIdField(entityManager, User.class, "username", username);
    }

    public User loadByHTTPBasicCredentials(EntityManager entityManager, String credentials) {
        if (credentials.startsWith(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX)) {
            credentials = credentials.replaceAll(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX, "");
            credentials = new String(Base64.decodeBase64(credentials.getBytes()));
            String[] arr = credentials.split(":");
            if (arr.length == 2) {
                String username = arr[0];
                String password = arr[1];
                User user = loadByUsername(entityManager, username);
                String encodedPassword = new Md5PasswordEncoder().encodePassword(password, username);
                if (user != null && user.getPassword().equals(encodedPassword)) {
                    return user;
                }
            }
        }
        // let's be lax and not throw a BAD_REQUEST to allow the user to retry
        return null;
    }
}
