package org.openremote.test

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.ReferenceCountUtil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE

class RawClient {

    @ChannelHandler.Sharable
    class RawMessageHandler extends ChannelInboundHandlerAdapter {

        @Override
        void channelRead(ChannelHandlerContext ctx, Object message) {
            try {
                ByteBuf byteBuf = (ByteBuf) message
                int readBytes = byteBuf.readableBytes()
                heapBuffer.writeBytes(byteBuf)
                readableBytesSem.release(readBytes)
            } finally {
                ReferenceCountUtil.release(message)
            }
        }

        @Override
        void channelInactive(ChannelHandlerContext ctx) {
            disconnectLatch.countDown()
            ctx.close().addListener(CLOSE_ON_FAILURE)
        }
    }

    final RawMessageHandler handler
    EventLoopGroup workerGroup
    Channel m_channel

    private boolean connected
    private ByteBuf heapBuffer
    private CountDownLatch disconnectLatch
    private final Semaphore readableBytesSem

    @SuppressWarnings("FutureReturnValueIgnored")
    private RawClient(String host, int port) {
        handler = new RawMessageHandler()
        heapBuffer = Unpooled.buffer(128)
        disconnectLatch = new CountDownLatch(1)
        readableBytesSem = new Semaphore(0, true)

        workerGroup = new NioEventLoopGroup()

        try {
            Bootstrap b = new Bootstrap()
            b.group(workerGroup)
            b.channel(NioSocketChannel.class)
            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.handler(new ChannelInitializer<SocketChannel>() {

                @Override
                void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline()
                    pipeline.addLast("handler", handler)
                }
            })

            // Start the client.
            m_channel = b.connect(host, port).sync().channel()
            this.connected = true
        } catch (Exception ex) {
            LOG.error("Error received in client setup", ex)
            workerGroup.shutdownGracefully()
        }
    }

    static RawClient connect(String host, int port) {
        return new RawClient(host, port)
    }

    RawClient isConnected() {
        if (!this.connected) {
            throw new IllegalStateException("Can't connect the client")
        }
        return this
    }

    RawClient write(int... bytes) {
        ByteBuf buff = Unpooled.buffer(bytes.length)
        buff.clear()
        for (int b : bytes) {
            buff.writeByte((byte) b)
        }
        m_channel.write(buff).addListener(CLOSE_ON_FAILURE)
        return this
    }

    RawClient writeWithSize(String str) {
        ByteBuf buff = Unpooled.buffer(str.length() + 2)
        buff.writeBytes(encodeString(str))
        m_channel.write(buff).addListener(CLOSE_ON_FAILURE)
        return this
    }

    /**
     * Write just String bytes not length
     */
    RawClient write(String str) {
        ByteBuf out = Unpooled.buffer(str.length())
        byte[] raw
        try {
            raw = str.getBytes("UTF-8")
            // NB every Java platform has got UTF-8 encoding by default, so this
            // exception are never raised.
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex)
        }
        out.writeBytes(raw)
        m_channel.write(out).addListener(CLOSE_ON_FAILURE)
        return this
    }

    RawClient flush() {
        m_channel.flush()
        return this
    }

    RawClient read(int expectedByte) {
        try {
            readableBytesSem.acquire(1)
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting data")
        }
        byte b = heapBuffer.readByte()
        if (b != expectedByte) {
            throw new IllegalStateException(String.format("Expected byte 0x%02X but found 0x%02X", b, expectedByte))
        }
        return this
    }

    /**
     * Expect the closing of the underling channel, with timeout
     */
    void closed(long timeout) throws InterruptedException {
        disconnectLatch.await(timeout, TimeUnit.MILLISECONDS)
    }

    /**
     * Expect closing the connect with unbound time
     */
    void closed() throws InterruptedException {
        disconnectLatch.await()
    }

    /**
     * Return the IoBuffer with string encoded as MSB, LSB and UTF-8 encoded string content.
     */
    static ByteBuf encodeString(String str) {
        byte[] raw
        try {
            raw = str.getBytes("UTF-8")
            // NB every Java platform has got UTF-8 encoding by default, so this
            // exception are never raised.
        } catch (UnsupportedEncodingException ex) {
            return null
        }
        return encodeFixedLengthContent(raw)
    }

    /**
     * Return the IoBuffer with string encoded as MSB, LSB and bytes array content.
     */
    static ByteBuf encodeFixedLengthContent(byte[] content) {
        ByteBuf out = Unpooled.buffer(2)
        out.writeShort(content.length)
        out.writeBytes(content)
        return out
    }
}
