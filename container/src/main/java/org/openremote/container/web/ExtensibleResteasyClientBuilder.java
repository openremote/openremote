package org.openremote.container.web;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.engines.PassthroughTrustManager;
import org.jboss.resteasy.client.jaxrs.engines.factory.ApacheHttpClient4EngineFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * This tremendous code was copied from Resteasy. Make the private static SPI public again. Sad.
 * <p>
 * TODO https://issues.jboss.org/browse/RESTEASY-1599
 */
public class ExtensibleResteasyClientBuilder extends ResteasyClientBuilder {

    @Override
    public ResteasyClient build() {
        httpEngine(initDefaultEngine43(this));
        return super.build();
    }

    @SuppressWarnings("deprecation")
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

    static public ApacheHttpClient43Engine initDefaultEngine43(ExtensibleResteasyClientBuilder that) {

        HttpClient httpClient = null;

        HostnameVerifier verifier = null;
        if (that.verifier != null) {
            verifier = new ExtensibleResteasyClientBuilder.VerifierWrapper(that.verifier);
        } else {
            switch (that.policy) {
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
            SSLContext theContext = that.sslContext;
            if (that.disableTrustManager) {
                theContext = SSLContext.getInstance("SSL");
                theContext.init(null, new TrustManager[]{new PassthroughTrustManager()},
                    new SecureRandom());
                verifier = new NoopHostnameVerifier();
                sslsf = new SSLConnectionSocketFactory(theContext, verifier);
            } else if (theContext != null) {
                sslsf = new SSLConnectionSocketFactory(theContext, verifier) {
                    @Override
                    protected void prepareSocket(SSLSocket socket) throws IOException {
                        that.prepareSocketForSni(socket);
                    }
                };
            } else if (that.clientKeyStore != null || that.truststore != null) {
                SSLContext ctx = SSLContexts.custom()
                    .setProtocol(SSLConnectionSocketFactory.TLS)
                    .setSecureRandom(null)
                    .loadKeyMaterial(that.clientKeyStore,
                        that.clientPrivateKeyPassword != null ? that.clientPrivateKeyPassword.toCharArray() : null)
                    .loadTrustMaterial(that.truststore, TrustSelfSignedStrategy.INSTANCE)
                    .build();
                sslsf = new SSLConnectionSocketFactory(ctx, verifier) {
                    @Override
                    protected void prepareSocket(SSLSocket socket) throws IOException {
                        that.prepareSocketForSni(socket);
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
            if (that.connectionPoolSize > 0) {
                PoolingHttpClientConnectionManager tcm = new PoolingHttpClientConnectionManager(
                    registry, null, null, null, that.connectionTTL, that.connectionTTLUnit);
                tcm.setMaxTotal(that.connectionPoolSize);
                if (that.maxPooledPerRoute == 0) {
                    that.maxPooledPerRoute = that.connectionPoolSize;
                }
                tcm.setDefaultMaxPerRoute(that.maxPooledPerRoute);
                cm = tcm;

            } else {
                cm = new BasicHttpClientConnectionManager(registry);
            }

            RequestConfig.Builder rcBuilder = RequestConfig.custom();
            if (that.socketTimeout > -1) {
                rcBuilder.setSocketTimeout((int) that.socketTimeoutUnits.toMillis(that.socketTimeout));
            }
            if (that.establishConnectionTimeout > -1) {
                rcBuilder.setConnectTimeout((int) that.establishConnectionTimeoutUnits.toMillis(that.establishConnectionTimeout));
            }
            if (that.connectionCheckoutTimeoutMs > -1) {
                rcBuilder.setConnectionRequestTimeout(that.connectionCheckoutTimeoutMs);
            }

            // The magic configure()
            httpClient = that.configure(
                HttpClientBuilder.create()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(rcBuilder.build())
                    .setProxy(that.defaultProxy)
                    .disableContentCompression()
            ).build();

            ApacheHttpClient43Engine engine =
                (ApacheHttpClient43Engine) ApacheHttpClient4EngineFactory.create(httpClient, true);
            engine.setResponseBufferSize(that.responseBufferSize);
            engine.setHostnameVerifier(verifier);
            // this may be null.  We can't really support this with Apache Client.
            engine.setSslContext(theContext);
            return engine;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class VerifierWrapper implements HostnameVerifier {
        protected HostnameVerifier verifier;

        VerifierWrapper(HostnameVerifier verifier) {
            this.verifier = verifier;
        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return verifier.verify(s, sslSession);
        }
    }
}
