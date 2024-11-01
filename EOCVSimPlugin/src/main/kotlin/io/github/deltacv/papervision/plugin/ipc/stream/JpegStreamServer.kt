package io.github.deltacv.papervision.plugin.ipc.stream

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

class JpegStreamServer(
    queueSize: Int = 2
) {

    val logger by loggerForThis()

    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()

    private val byteBufferPool = EvictingBlockingQueue<ByteBuffer>(ArrayBlockingQueue(queueSize + 2)) // Pool of reusable byte arrays for image data
    private val frameQueue = EvictingBlockingQueue(ArrayBlockingQueue<ByteBuffer>(queueSize))

    var port = 0
        private set

    fun start() {
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 100)
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
        val size = 4 + tag.tag.size + 4 + bytes.size
        var buffer = byteBufferPool.find { it.capacity() == size }

        if (buffer == null) {
            buffer = ByteBuffer.allocate(size)
            byteBufferPool.add(buffer)
        }

        buffer.clear()
            .putInt(tag.tag.size)
            .put(tag.tag)
            .putInt(id)
            .put(bytes)

        frameQueue.offer(buffer)
    }
}

private class JpegServerHandler(
    val frameQueue: EvictingBlockingQueue<ByteBuffer>,
    val byteArrayPool: EvictingBlockingQueue<ByteBuffer>
) : SimpleChannelInboundHandler<ByteBuf>() {

    val logger by loggerForThis()

    override fun channelActive(ctx: ChannelHandlerContext) {
        val frame = frameQueue.poll() ?: return // Get the next frame from the queue

        val buf = Unpooled.wrappedBuffer(frame)
        ctx.writeAndFlush(buf) // fluuuuush

        byteArrayPool.offer(frame)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        // Client disconnected
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        // Server does not expect incoming messages in this example
    }
}