package io.github.deltacv.papervision.plugin.ipc.stream

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

class JpegStreamClient(
    private val port: Int,
    private val engineBridge: PaperVisionEngineBridge
) {
    private val workerGroup = NioEventLoopGroup()
    private var channel: SocketChannel? = null

    fun start() {
        val bootstrap = Bootstrap()
        bootstrap.group(workerGroup)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(JpegClientHandler(engineBridge))
                    channel = ch
                }
            })

        // Connect asynchronously and wait for the channel to close
        val future = bootstrap.connect(InetSocketAddress.createUnresolved("127.0.0.1", port)).sync()

        future.channel().closeFuture().addListener {
            workerGroup.shutdownGracefully()
        }
    }


    fun ping() {
        val emptyBuffer = Unpooled.buffer(1)
        channel?.writeAndFlush(emptyBuffer)
    }

    fun close() {
        workerGroup.shutdownGracefully()
    }
}

class JpegClientHandler(
    private val engineBridge: PaperVisionEngineBridge
) : ByteToMessageDecoder() {

    val logger = LoggerFactory.getLogger(JpegClientHandler::class.java)

    private val byteBufferPool = ArrayBlockingQueue<ByteBuffer>(5) // Pool of reusable byte arrays for image data

    override fun decode(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
        out: List<Any?>?
    ) {
        if(msg.readableBytes() < 1) {
            return
        }

        println("received ${msg.readableBytes()} bytes")

        // see ByteMessages.kt
        val n = msg.readInt()
        if(n < 1) {
            return
        }

        val tag = ByteArray(n)
        msg.readBytes(tag)

        val id = msg.readInt()

        var buffer = if(byteBufferPool.isEmpty()) {
            ByteBuffer.allocate(msg.readableBytes())
        } else {
            byteBufferPool.take()
        }

        if(buffer.capacity() < msg.readableBytes()) {
            buffer = ByteBuffer.allocate(msg.readableBytes())
        }

        buffer.putInt(n)
        buffer.put(tag)
        buffer.putInt(id)
        msg.readBytes(buffer)

        val array = buffer.array()

        engineBridge.broadcastBytes(array)

        val hashCode = array.hashCode()

        engineBridge.onClientProcess {
            engineBridge.processedBinaryMessagesHashes.find { it == hashCode } ?: return@onClientProcess
            engineBridge.processedBinaryMessagesHashes.remove(hashCode)

            byteBufferPool.offer(buffer)

            println("returned ${array.size} bytes after processing")

            it.removeThis()
        }
    }
}