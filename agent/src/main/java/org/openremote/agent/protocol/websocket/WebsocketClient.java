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
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.agent.protocol.http.OAuthFilter;
import org.openremote.agent.protocol.http.OAuthGrant;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.http.WebTargetBuilder.CONNECTION_TIMEOUT_MILLISECONDS;
import static org.openremote.agent.protocol.http.WebTargetBuilder.createClient;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class WebsocketClient extends AbstractNettyIoClient<String, InetSocketAddress> {

    public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;

        public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            if (!handshaker.isHandshakeComplete()) {
                try {
                    handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                    synchronized (WebsocketClient.this) {
                        LOG.fine("Connected: " + getSocketAddressString());
                        onConnectionStatusChanged(ConnectionStatus.CONNECTED);
                    }

                    handshakeFuture.setSuccess();
                } catch (WebSocketHandshakeException e) {
                    LOG.log(Level.SEVERE, "Connection failed: " + getSocketAddressString(), e);
                    setPermanentError("Connection failed: " + e.getMessage());
                    handshakeFuture.setFailure(e);
                }
                return;
            }

            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                LOG.severe("Websocket client unexpected FullHttpResponse (getStatus=" + response.status() +
                        ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
            }

            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                WebsocketClient.this.onMessageReceived((InetSocketAddress)ch.remoteAddress(), textFrame.text());
            } else if (frame instanceof PongWebSocketFrame) {
                LOG.finest("Websocket client pong received");
            } else if (frame instanceof CloseWebSocketFrame) {
                System.out.println("WebSocket Client received closing");
                ch.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOG.log(Level.SEVERE, "Websocket client exception caught", cause);
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            ctx.close();
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, WebsocketClient.class);
    protected boolean useSsl;
    protected URI uri;
    protected SslContext sslCtx;
    protected WebSocketClientHandler handler;
    protected MultivaluedMap<String, String> headers;
    protected OAuthGrant oAuthGrant;
    protected String authHeaderValue;
    protected String host;
    protected int port;

    public WebsocketClient(URI uri, ProtocolExecutorService executorService) {
        this(uri, null, null, executorService);
    }

    public WebsocketClient(URI uri, MultivaluedMap<String, String> headers, OAuthGrant oAuthGrant, ProtocolExecutorService executorService) {
        super(executorService);

        this.uri = uri;
        this.headers = headers;
        this.oAuthGrant = oAuthGrant;

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

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            LOG.warning("Only WS(S) is supported: " + getSocketAddressString());
            setPermanentError("Only WS(S) is supported");
            return;
        }

        useSsl = "wss".equalsIgnoreCase(scheme);
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return NioSocketChannel.class;
    }

    @Override
    protected String getSocketAddressString() {
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
    protected void initChannel(Channel channel) {
        try {
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

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to initialise channel: "  + getSocketAddressString(), e);
            setPermanentError(e.getMessage());
            return;
        }

        super.initChannel(channel);
    }

    @Override
    protected void addDecoders(Channel channel) {
        if (sslCtx != null) {
            channel.pipeline().addLast(sslCtx.newHandler(channel.alloc(), host, port));
        }
        channel.pipeline().addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(8192),
            WebSocketClientCompressionHandler.INSTANCE,
            handler);
    }

    @Override
    protected void addEncoders(Channel channel) {
        channel.pipeline().addLast(new MessageToMessageEncoder<String>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
                out.add(new TextWebSocketFrame(msg));
            }
        });
    }

    @Override
    protected void decode(ByteBuf buf, List<String> messages) {
        // Not used
    }

    @Override
    protected void encode(String message, ByteBuf buf) {
        // Not used
    }

    @Override
    public synchronized void connect() {
        if (oAuthGrant != null) {
            LOG.fine("Retrieving OAuth access token: "  + getSocketAddressString());

            ResteasyClient client = createClient(executorService, 1, CONNECTION_TIMEOUT_MILLISECONDS);

            try {
                WebTarget authTarget = client.target(oAuthGrant.getTokenEndpointUri());
                OAuthFilter oAuthFilter = new OAuthFilter(authTarget, oAuthGrant);
                authHeaderValue = oAuthFilter.getAuthHeader();
                if (TextUtil.isNullOrEmpty(authHeaderValue)) {
                    throw new Exception("Returned access token is null");
                }
                LOG.fine("Retrieved access token via OAuth: " + getSocketAddressString());

            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to retrieve OAuth access token: " + getSocketAddressString(), e);
                setPermanentError(e.getMessage());
                return;
            } finally {
                if (client != null) {
                    client.close();
                }
            }
        }

        super.connect();
    }
}
