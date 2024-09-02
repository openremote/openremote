/*
 * Copyright 2024, OpenRemote Inc.
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

package org.openremote.agent.protocol.mqtt;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;

public class CustomX509TrustManagerFactory extends TrustManagerFactory {
	public CustomX509TrustManagerFactory(KeyStore... keystores) throws KeyStoreException {
		super(new CustomX509TrustManagerFactorySpi(keystores), new Provider("CustomX509TrustManagerFactory", "1.0", "Custom X509 Trust Manager Factory") {}, TrustManagerFactory.getDefaultAlgorithm());
	}
}
