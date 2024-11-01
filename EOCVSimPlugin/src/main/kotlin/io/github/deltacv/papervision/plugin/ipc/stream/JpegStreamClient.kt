package io.github.deltacv.papervision.plugin.ipc.stream

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class JpegStreamClient(
    private val port: Int,
    private val engineBridge: PaperVisionEngineBridge
) {
    private val workerGroup = NioEventLoopGroup()

    fun start() {
        val bootstrap = Bootstrap()
        bootstrap.group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(LoggingHandler(LogLevel.INFO))
                    ch.pipeline().addLast(JpegClientHandler(engineBridge))
                }
            })

        // Connect asynchronously and wait for the channel to close
        val future = bootstrap.connect(InetSocketAddress.createUnresolved("127.0.0.1", port)).sync()

        future.channel().closeFuture().addListener {
            workerGroup.shutdownGracefully()
        }
    }

    fun close() {
        workerGroup.shutdownGracefully()
    }
}

class JpegClientHandler(
    private val engineBridge: PaperVisionEngineBridge
) : SimpleChannelInboundHandler<ByteBuf>() {

    val logger = LoggerFactory.getLogger(JpegClientHandler::class.java)

    var buffer: ByteBuffer? = null
    var bytes: ByteArray? = null

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        if (bytes == null || bytes!!.size != msg.capacity()) {
            buffer = ByteBuffer.wrap(bytes)
        }

        msg.readBytes(buffer)

        engineBridge.broadcastBytes(bytes!!)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
    }
}