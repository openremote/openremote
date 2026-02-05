/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.agent.protocol.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.container.web.OAuthFilter;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.web.WebTargetBuilder.CONNECTION_TIMEOUT_MILLISECONDS;
import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an {@link IOClient} implementation based on {@link AbstractNettyIOClient}.
 * For custom decoders, the initial message type is of type {@link String}.
 * For custom encoders, the final message type must be of type {@link String}.
 */
public class WebsocketIOClient<T> extends AbstractNettyIOClient<T, InetSocketAddress> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, WebsocketIOClient.class);
    protected static ResteasyClient client;
    // How long since the last read before a PING is sent
    public static final long PING_MILLIS = 10000;
    // How long to wait for a ping response (i.e. pong)
    public static final long PING_TIMEOUT_MILLIS = 10000;
    protected ScheduledFuture<?> pingFuture;
    protected boolean useSsl;
    protected URI uri;
    protected static SslContext sslCtx;
    protected WebSocketClientProtocolHandler handler;
    protected Map<String, List<String>> headers;
    protected OAuthGrant oAuthGrant;
    protected String host;
    protected int port;
    protected boolean pingDisabled;
    protected CompletableFuture<Void> handshakeFuture;

    public WebsocketIOClient(URI uri, Map<String, List<String>> headers, OAuthGrant oAuthGrant) {
        this(uri, headers, oAuthGrant, false);
    }

    public WebsocketIOClient(URI uri, Map<String, List<String>> headers, OAuthGrant oAuthGrant, boolean pingDisabled) {
        this.uri = uri;
        this.headers = headers;
        this.oAuthGrant = oAuthGrant;
        this.pingDisabled = pingDisabled;
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();

        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        // Our own manager uses HTTP(S) and upgrades to WS(S) so this is not appropriate
//        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
//            LOG.warning("Only WS(S) is supported: " + getSocketAddressString());
//            setPermanentError("Only WS(S) is supported");
//            return;
//        }

        useSsl = "wss".equalsIgnoreCase(scheme);
    }

    protected synchronized ResteasyClient getClient() {
        if (client == null) {
            client = createClient(executorService, 1, CONNECTION_TIMEOUT_MILLISECONDS, null);
        }
        return client;
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return NioSocketChannel.class;
    }

    @Override
    public String getClientUri() {
        return uri.toString();
    }

    @Override
    protected EventLoopGroup getWorkerGroup() {
        return new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    }

    @Override
    protected CompletableFuture<Void> startChannel() {
        handshakeFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture;
        try {
            channelFuture = toCompletableFuture(bootstrap.connect(new InetSocketAddress(host, port)));
        } catch (Exception e) {
            channelFuture = CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.allOf(
            channelFuture,
            handshakeFuture
        );
    }

    @Override
    protected boolean isChannelReady() {
        return super.isChannelReady() && handshakeFuture != null && handshakeFuture.isDone();
    }

    @Override
    protected void addEncodersDecoders(Channel channel) throws Exception {
        HttpHeaders hdrs = new DefaultHttpHeaders();

        if (this.headers != null) {
            this.headers.forEach(hdrs::add);
        }

        handler = new WebSocketClientProtocolHandler(
            WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, hdrs)) {

            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                super.userEventTriggered(ctx, evt);
                if (evt instanceof WebSocketClientProtocolHandler.ClientHandshakeStateEvent handshakeStateEvent) {
                    if (handshakeStateEvent == ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                        executorService.submit(WebsocketIOClient.this::onHandshakeDone);
                    }
                }
            }

            @Override
            protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
                if (frame instanceof PongWebSocketFrame) {
                    onPong(ctx);
                }
                super.decode(ctx, frame, out);
            }
        };

        if (!pingDisabled) {
            channel.pipeline().addFirst(new ReadTimeoutHandler(PING_MILLIS, TimeUnit.MILLISECONDS) {
                @Override
                protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
                    doPing(ctx);
                }
            });
        }

        channel.pipeline().addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(8192),
            new WebSocketClientCompressionHandler(0),
            handler);

        channel.pipeline().addLast(new io.netty.handler.codec.MessageToMessageDecoder<WebSocketFrame>() {
            @Override
            protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
                if (msg instanceof TextWebSocketFrame textWebSocketFrame) {
                    out.add(textWebSocketFrame.text());
                } else if (msg instanceof BinaryWebSocketFrame) {
                    out.add(msg.content().retain());
                }
            }
        });

        super.addEncodersDecoders(channel);

        // Put SSL handler first
        if (useSsl) {
            channel.pipeline().addFirst(getSSLContext().newHandler(channel.alloc(), host, port));
        }

        // Put string and bytebuf encoders first (encoders are called in reverse to decoders)
        channel.pipeline().addLast(new MessageToMessageEncoder<String>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) {
                out.add(new TextWebSocketFrame(msg));
            }
        });
        channel.pipeline().addLast(new io.netty.handler.codec.MessageToMessageEncoder<ByteBuf>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
                out.add(new BinaryWebSocketFrame(msg.retain()));
            }
        });
    }

    protected void onHandshakeDone() {
        if (handshakeFuture != null) {
            handshakeFuture.complete(null);
        }
    }

    protected synchronized SslContext getSSLContext() throws SSLException {
        if (sslCtx == null) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .sessionTimeout(getConnectTimeoutMillis())
                    .build();
        }
        return sslCtx;
    }

    private void doPing(ChannelHandlerContext ctx) {
        LOG.finest("Sending PING: " + getClientUri());
        pingFuture = scheduledExecutorService.schedule(() -> {
            ctx.fireExceptionCaught(new Exception("PING failed"));
        }, PING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        ctx.channel().writeAndFlush(new PingWebSocketFrame());
    }

    private void onPong(ChannelHandlerContext ctx) {
        LOG.finest("Received PONG: " + getClientUri());
        if (pingFuture != null) {
            pingFuture.cancel(false);
        }
    }

    /**
     * We need to get the auth header before we connect so we have it for the handshake
     */
    @Override
    protected CompletableFuture<Void> doConnect() {
        return getAuthHeader().thenCompose(authHeader -> {
            // Push auth header into headers
            if (authHeader != null) {
                if (headers == null) {
                    headers = new HashMap<>(1);
                }
                headers.put(HttpHeaderNames.AUTHORIZATION.toString(), Collections.singletonList(authHeader));
            }

            return super.doConnect();
        });
    }

    @Override
    protected void doDisconnect() {
        // Cancel ping task
        if (pingFuture != null) {
            pingFuture.cancel(false);
        }
        handshakeFuture = null;
        super.doDisconnect();
    }

    /**
     * Get the auth header asynchronously
     */
    public CompletableFuture<String> getAuthHeader() {
        if (oAuthGrant == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        executorService.submit(() -> {
            if (oAuthGrant != null) {
                LOG.finest("Retrieving OAuth access token: "  + getClientUri());

                try {
                    OAuthFilter oAuthFilter = new OAuthFilter(getClient(), oAuthGrant);
                    String authHeaderValue = oAuthFilter.getAuthHeader();
                    if (TextUtil.isNullOrEmpty(authHeaderValue)) {
                        throw new RuntimeException("Returned access token is null");
                    }
                    LOG.fine("Retrieved access token via OAuth: " + getClientUri());
                    future.complete(authHeaderValue);
                } catch (Exception e) {
                    future.completeExceptionally(new Exception("Error retrieving OAuth access token for '" + getClientUri() + "': " + e.getMessage()));
                }
            }
        });

        return future;
    }
}
