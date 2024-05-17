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
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import jakarta.ws.rs.ProcessingException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.container.web.OAuthFilter;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    /**
     * Extracts the text from the {@link WebSocketFrame} and sends it to the next handler in the pipeline
     */
    protected class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;

        public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) {
            Channel ch = ctx.channel();

            if (!handshaker.isHandshakeComplete()) {
                try {
                    handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                    handshakeFuture.setSuccess();
                } catch (WebSocketHandshakeException e) {
                    handshakeFuture.setFailure(e);
                }
                return;
            }

            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                LOG.severe("Websocket client unexpected FullHttpResponse (getStatus=" + response.status() +
                    ", content=" + response.content().toString(CharsetUtil.UTF_8) + "):" + getClientUri());
            }

            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                String str = textFrame.text();
                ctx.fireChannelRead(str);
            } else if (frame instanceof PongWebSocketFrame) {
                LOG.finest("Received PONG: " + getClientUri());
                if (pingCounter != null) {
                    pingCounter.set(0);
                }
            } else if (frame instanceof CloseWebSocketFrame) {
                ch.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
                ctx.close();
            }
            WebsocketIOClient.this.onDecodeException(ctx, cause);
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, WebsocketIOClient.class);
    protected static ResteasyClient client;
    public static final long PING_MILLIS = 60000;
    protected ScheduledFuture<?> pingFuture;
    protected AtomicInteger pingCounter;
    protected boolean useSsl;
    protected URI uri;
    protected SslContext sslCtx;
    protected WebSocketClientHandler handler;
    protected Map<String, List<String>> headers;
    protected OAuthGrant oAuthGrant;
    protected String authHeaderValue;
    protected String host;
    protected int port;
    protected boolean pingDisabled;

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
        return new NioEventLoopGroup(1);
    }

    @Override
    protected ChannelFuture startChannel() {
        return bootstrap.connect(new InetSocketAddress(host, port));
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        if (useSsl) {
            sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        HttpHeaders hdrs = new DefaultHttpHeaders();

        if (this.headers != null) {
            this.headers.forEach(hdrs::add);
        }

        if (authHeaderValue != null) {
            hdrs.set(HttpHeaderNames.AUTHORIZATION, authHeaderValue);
        }

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
        handler =
            new WebSocketClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                    uri, WebSocketVersion.V13, null, true, hdrs));


        super.initChannel(channel);
    }

    protected void onHandshakeComplete() {
        // Start ping task
        if (!pingDisabled) {
            LOG.fine("Starting PING task: " + getClientUri());
            pingCounter = new AtomicInteger();
            pingFuture = executorService.scheduleWithFixedDelay(() -> {
                try {
                    if (pingCounter.get() > 2) {
                        LOG.info("No PING response so reconnecting: " + this);
                        doDisconnect();
                        scheduleDoConnect(1000);
                        return;
                    }
                    LOG.finest("Sending PING: " + getClientUri());
                    channel.writeAndFlush(new PingWebSocketFrame());
                    pingCounter.incrementAndGet();
                } catch (Exception ignored) {
                }
            }, PING_MILLIS, PING_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void addEncodersDecoders(Channel channel) {

        if (sslCtx != null) {
            channel.pipeline().addLast(sslCtx.newHandler(channel.alloc(), host, port));
        }

        channel.pipeline().addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(8192),
            WebSocketClientCompressionHandler.INSTANCE,
            handler);

        channel.pipeline().addLast(new MessageToMessageEncoder<ByteBuf>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
                out.add(new TextWebSocketFrame(msg));
            }
        });

        super.addEncodersDecoders(channel);

        // Put string encoder first (encoders are called in reverse to decoders)
        channel.pipeline().addLast(new MessageToMessageEncoder<String>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) {
                out.add(new TextWebSocketFrame(msg));
            }
        });
    }

    @Override
    protected Future<Void> doConnect() {

        if (oAuthGrant != null) {
            LOG.fine("Retrieving OAuth access token: "  + getClientUri());

            try {
                OAuthFilter oAuthFilter = new OAuthFilter(getClient(), oAuthGrant);
                authHeaderValue = oAuthFilter.getAuthHeader();
                if (TextUtil.isNullOrEmpty(authHeaderValue)) {
                    throw new RuntimeException("Returned access token is null");
                }
                LOG.fine("Retrieved access token via OAuth: " + getClientUri());

            } catch (SocketException | ProcessingException e) {
                return CompletableFuture.failedFuture(new RuntimeException("Failed to retrieve OAuth access token for '" + getClientUri() + "': Connection error"));
            } catch (Exception e) {
                return CompletableFuture.failedFuture(new RuntimeException("Failed to retrieve OAuth access token '" + getClientUri() + "': " + e.getMessage()));
            }
        }

        return super.doConnect();
    }

    @Override
    protected void doDisconnect() {
        // Cancel ping task
        if (pingFuture != null) {
            pingFuture.cancel(false);
        }

        super.doDisconnect();
    }

    @Override
    protected CompletableFuture<Void> createConnectedFuture(final ChannelFuture channelStartFuture) {
        CompletableFuture<Void> connectedFuture = super.createConnectedFuture(channelStartFuture);
        return connectedFuture.thenRunAsync(() -> {
            ChannelPromise handshakeFuture = handler.handshakeFuture;
            handshakeFuture.awaitUninterruptibly();

            if (handshakeFuture.isSuccess()) {
                onHandshakeComplete();
            }
        });
    }
}
