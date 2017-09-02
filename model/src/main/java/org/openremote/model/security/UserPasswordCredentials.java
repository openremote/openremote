/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.security;

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Optional;

public class UserPasswordCredentials {

    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    protected String username;
    protected String password;

    protected UserPasswordCredentials() {
    }

    public UserPasswordCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ObjectValue toObjectValue() {
        ObjectValue value = Values.createObject();
        value.put(USERNAME_KEY, Values.create(getUsername()));
        value.put(PASSWORD_KEY, Values.create(getPassword()));
        return value;
    }

    public static boolean isUserPasswordCredentials(AbstractValueHolder valueHolder) {
        return valueHolder != null
            && valueHolder.getValueAsObject().filter(UserPasswordCredentials::isUserPasswordCredentials).isPresent();
    }

    public static boolean isUserPasswordCredentials(Value value) {
        return Values.getObject(value)
            .filter(objectValue ->
                    objectValue.getString(USERNAME_KEY).filter(s -> !s.isEmpty()).isPresent()
                    && objectValue.getString(PASSWORD_KEY).filter(s -> !s.isEmpty()).isPresent()
            ).isPresent();
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<UserPasswordCredentials> fromValue(Value value) {
        return Values.getObject(value)
            .filter(UserPasswordCredentials::isUserPasswordCredentials)
            .map(objectValue ->
                new UserPasswordCredentials(objectValue.getString(USERNAME_KEY).get(), objectValue.getString(PASSWORD_KEY).get())
            );
    }
}
