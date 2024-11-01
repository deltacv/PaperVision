package io.github.deltacv.papervision.plugin.ipc.stream

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.IdleStateHandler
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

internal data class FrameData(
    val tag: ByteMessageTag,
    val id: Int,
    val bytes: ByteBuffer
)

class JpegStreamServer(
    queueSize: Int = 2
) {

    val logger by loggerForThis()

    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()

    // Replace EvictingBlockingQueue with ConcurrentLinkedQueue
    private val byteBufferPool = ConcurrentLinkedQueue<ByteBuffer>()
    private val frameQueue = ConcurrentLinkedQueue<FrameData>()

    var port = 0
        private set

    fun start() {
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 100)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(JpegServerHandler(frameQueue, byteBufferPool))
                }
            })

        val future = bootstrap.bind(InetSocketAddress("127.0.0.1", 0)).sync()

        val address = future.channel().localAddress() as InetSocketAddress
        port = address.port

        logger.info("JpegStreamServer started on port $port")

        future.channel().closeFuture().addListener {
            logger.warn("JpegStreamServer closed")
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }

    fun close() {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    fun submit(tag: ByteMessageTag, id: Int, bytes: ByteArray) {
        val size = bytes.size
        var buffer = byteBufferPool.find { it.capacity() == size }

        if (buffer == null) {
            buffer = ByteBuffer.allocate(size)
            byteBufferPool.add(buffer)
        } else {
            byteBufferPool.remove(buffer)
        }

        System.arraycopy(bytes, 0, buffer.array(), 0, size)

        frameQueue.offer(FrameData(tag, id, buffer))
        println("offered frame ${buffer.capacity()}")
    }
}

private class JpegServerHandler(
    val frameQueue: ConcurrentLinkedQueue<FrameData>,
    val byteArrayPool: ConcurrentLinkedQueue<ByteBuffer>
) : ChannelOutboundHandlerAdapter() {

    override fun write(ctx: ChannelHandlerContext, msg: Any?, promise: ChannelPromise) {
        val frame = frameQueue.poll() ?: return

        val buf = Unpooled.buffer(frame.bytes.capacity())

        // see ByteMessages.kt
        buf.writeInt(frame.tag.tag.size)
        buf.writeBytes(frame.tag.tag)
        buf.writeInt(frame.id)
        buf.writeBytes(frame.bytes.array())

        ctx.write(buf, promise)

        buf.release()

        println("sent buf ${buf.capacity()}")

        // Return the buffer to the pool
        byteArrayPool.offer(frame.bytes)
    }

}