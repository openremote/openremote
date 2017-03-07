package org.openremote.container.web;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.engines.PassthroughTrustManager;
import org.jboss.resteasy.client.jaxrs.engines.factory.ApacheHttpClient4EngineFactory;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * This tremendous code was copied from Resteasy. Make the private static SPI public again. Sad.
 * <p>
 * TODO https://issues.jboss.org/browse/RESTEASY-1599
 */
public class ExtensibleResteasyClientBuilder extends ResteasyClientBuilder {

    @Override
    public ResteasyClient build() {
        httpEngine(initDefaultEngine43());
        return super.build();
    }

    @Override
    public ResteasyClient buildOld() {
        throw new UnsupportedOperationException("Just don't...");
    }

    /**
     * You want to override this sometimes. You really do.
     */
    public HttpClientBuilder configure(HttpClientBuilder httpClientBuilder) {
        return httpClientBuilder;
    }

    // The rest is copy/paste pretty much

    public ApacheHttpClient43Engine initDefaultEngine43() {
        HttpClient httpClient = null;

        HostnameVerifier verifier = null;
        if (verifier != null) {
            verifier = new ExtensibleResteasyClientBuilder.VerifierWrapper(verifier);
        } else {
            switch (policy) {
                case ANY:
                    verifier = new NoopHostnameVerifier();
                    break;
                case WILDCARD:
                    verifier = new DefaultHostnameVerifier();
                    break;
                case STRICT:
                    verifier = new DefaultHostnameVerifier();
                    break;
            }
        }
        try {
            SSLConnectionSocketFactory sslsf = null;
            SSLContext theContext = sslContext;
            if (disableTrustManager) {
                theContext = SSLContext.getInstance("SSL");
                theContext.init(null, new TrustManager[]{new PassthroughTrustManager()},
                    new SecureRandom());
                verifier = new NoopHostnameVerifier();
                sslsf = new SSLConnectionSocketFactory(theContext, verifier);
            } else if (theContext != null) {
                sslsf = new SSLConnectionSocketFactory(theContext, verifier) {
                    @Override
                    protected void prepareSocket(SSLSocket socket) throws IOException {
                        prepareSocketForSni(socket);
                    }
                };
            } else if (clientKeyStore != null || truststore != null) {
                SSLContext ctx = SSLContexts.custom()
                    .useProtocol(SSLConnectionSocketFactory.TLS)
                    .setSecureRandom(null)
                    .loadKeyMaterial(clientKeyStore,
                        clientPrivateKeyPassword != null ? clientPrivateKeyPassword.toCharArray() : null)
                    .loadTrustMaterial(truststore, TrustSelfSignedStrategy.INSTANCE)
                    .build();
                sslsf = new SSLConnectionSocketFactory(ctx, verifier) {
                    @Override
                    protected void prepareSocket(SSLSocket socket) throws IOException {
                        prepareSocketForSni(socket);
                    }
                };
            } else {
                final SSLContext tlsContext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
                tlsContext.init(null, null, null);
                sslsf = new SSLConnectionSocketFactory(tlsContext, verifier);
            }

            final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslsf)
                .build();

            HttpClientConnectionManager cm = null;
            if (connectionPoolSize > 0) {
                PoolingHttpClientConnectionManager tcm = new PoolingHttpClientConnectionManager(
                    registry, null, null, null, connectionTTL, connectionTTLUnit);
                tcm.setMaxTotal(connectionPoolSize);
                if (maxPooledPerRoute == 0) {
                    maxPooledPerRoute = connectionPoolSize;
                }
                tcm.setDefaultMaxPerRoute(maxPooledPerRoute);
                cm = tcm;

            } else {
                cm = new BasicHttpClientConnectionManager(registry);
            }

            RequestConfig.Builder rcBuilder = RequestConfig.custom();
            if (socketTimeout > -1) {
                rcBuilder.setSocketTimeout((int) socketTimeoutUnits.toMillis(socketTimeout));
            }
            if (establishConnectionTimeout > -1) {
                rcBuilder.setConnectTimeout((int) establishConnectionTimeoutUnits.toMillis(establishConnectionTimeout));
            }
            if (connectionCheckoutTimeoutMs > -1) {
                rcBuilder.setConnectionRequestTimeout(connectionCheckoutTimeoutMs);
            }

            httpClient = configure(HttpClientBuilder.create()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(rcBuilder.build())
                .setProxy(defaultProxy)
                .disableContentCompression()
            ).build();

            ApacheHttpClient43Engine engine =
                (ApacheHttpClient43Engine) ApacheHttpClient4EngineFactory.create(httpClient, true);
            engine.setResponseBufferSize(responseBufferSize);
            engine.setHostnameVerifier(verifier);
            // this may be null.  We can't really support this with Apache Client.
            engine.setSslContext(theContext);
            return engine;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class VerifierWrapper implements X509HostnameVerifier {
        protected HostnameVerifier verifier;

        VerifierWrapper(HostnameVerifier verifier) {
            this.verifier = verifier;
        }

        @Override
        public void verify(String host, SSLSocket ssl) throws IOException {
            if (!verifier.verify(host, ssl.getSession()))
                throw new SSLException(Messages.MESSAGES.hostnameVerificationFailure());
        }

        @Override
        public void verify(String host, X509Certificate cert) throws SSLException {
            throw new SSLException(Messages.MESSAGES.verificationPathNotImplemented());
        }

        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            throw new SSLException(Messages.MESSAGES.verificationPathNotImplemented());
        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return verifier.verify(s, sslSession);
        }
    }
}
