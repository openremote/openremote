/*
 * Copyright 2024, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.mqtt;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

public class CustomX509TrustManagerFactorySpi extends TrustManagerFactorySpi {

  private final KeyStore[] keyStores;
  private final X509TrustManager trustManager;

  public CustomX509TrustManagerFactorySpi(KeyStore... keyStores) throws KeyStoreException {
    this.keyStores = keyStores;
    this.trustManager = createTrustManager();
  }

  @Override
  protected void engineInit(KeyStore keyStore) throws KeyStoreException {
    // Do nothing, since we have already initialized the SPI from the above constructor, and we
    // don't want to
    // overwrite the object's fields.
  }

  @Override
  protected void engineInit(javax.net.ssl.ManagerFactoryParameters spec) {
    throw new UnsupportedOperationException("ManagerFactoryParameters not supported");
  }

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return new TrustManager[] {trustManager};
  }

  private X509TrustManager createTrustManager() throws KeyStoreException {
    try {
      if (keyStores == null) {
        throw new KeyStoreException("KeyStores not initialized");
      }
      List<X509TrustManager> trustManagerList = new ArrayList<>();
      for (KeyStore keyStore : keyStores) {
        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        for (TrustManager tm : tmf.getTrustManagers()) {
          if (tm instanceof X509TrustManager) {
            trustManagerList.add((X509TrustManager) tm);
          }
        }
      }
      return new CustomX509TrustManager(trustManagerList.toArray(new X509TrustManager[0]));
    } catch (Exception e) {
      throw new KeyStoreException("Error creating CustomX509TrustManager", e);
    }
  }

  public static class CustomX509TrustManager implements X509TrustManager {

    private final X509TrustManager[] trustManagers;

    public CustomX509TrustManager(X509TrustManager... trustManagers) {
      this.trustManagers = trustManagers;
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
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      for (X509TrustManager tm : trustManagers) {
        try {
          tm.checkServerTrusted(chain, authType);
          return;
        } catch (CertificateException ignored) {
          // continue to the next trust manager
        }
      }
      // If none of the trust managers trust the certificate chain, throw an exception
      throw new CertificateException("None of the trust managers trust this certificate chain");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
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

  // Register the provider
  public static class CustomProvider extends Provider {
    public CustomProvider() {
      super("CustomX509TrustManagerFactory", "1.0", "Custom X509 TrustManager Factory");
      put("TrustManagerFactory.CustomX509TrustManagerFactory", this.getClass().getName());
    }
  }
}
