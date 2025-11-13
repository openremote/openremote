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
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomX509TrustManager implements X509TrustManager {

	private final X509TrustManager[] trustManagers;

	public CustomX509TrustManager(KeyStore... keyStores) throws Exception {
		List<X509TrustManager> trustManagerList = new ArrayList<>();
		for (KeyStore keyStore : keyStores) {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);
			for (javax.net.ssl.TrustManager tm : tmf.getTrustManagers()) {
				if (tm instanceof X509TrustManager) {
					trustManagerList.add((X509TrustManager) tm);
				}
			}
		}
		trustManagers = trustManagerList.toArray(new X509TrustManager[0]);
	}

	private X509Certificate[] mergeCertificates() {
		ArrayList<X509Certificate> resultingCerts = new ArrayList<>();
		for (X509TrustManager tm : trustManagers) {
			resultingCerts.addAll(Arrays.asList(tm.getAcceptedIssuers()));
		}
		return resultingCerts.toArray(new X509Certificate[0]);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return mergeCertificates();
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		for (X509TrustManager tm : trustManagers) {
			try {
				tm.checkServerTrusted(chain, authType);
				return;
			} catch (CertificateException e) {
				// continue to the next trust manager
			}
		}
		// If none of the trust managers trust the certificate chain, throw an exception
		throw new CertificateException("None of the trust managers trust this certificate chain");
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		for (X509TrustManager tm : trustManagers) {
			try {
				tm.checkClientTrusted(chain, authType);
				return;
			} catch (CertificateException e) {
				// continue to the next trust manager
			}
		}
		// If none of the trust managers trust the certificate chain, throw an exception
		throw new CertificateException("None of the trust managers trust this certificate chain");
	}
}
