package com.fire.robotsimulation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

enum class BuildingStage { START, FINISH }
class LogMsg(
    private val robotName: String,
    private val land: String,
    private val cost: Int,
    private val buildingStage: BuildingStage
) {
    fun getByteBuffer(): ByteBuffer {
        val logLine = if (this.buildingStage == BuildingStage.START)
            "Robot ${this.robotName} started building on ${this.land}. Cost => ${this.cost}\r\n".toByteArray()
        else
            "Robot ${this.robotName} finished building on ${this.land}. Cost => ${this.cost}\r\n".toByteArray()
        return ByteBuffer.wrap(logLine)
    }
}

class LogWriter(private val incomingChannel: ReceiveChannel<LogMsg>) {
    private val fileChannel: AsynchronousFileChannel = AsynchronousFileChannel.open(
        Paths.get("C:\\temp\\log.log"), StandardOpenOption.CREATE, StandardOpenOption.WRITE
    )
    var currentPosition = 0L
    var numberOfMessagesInProgress = AtomicInteger(0)

    suspend fun startReceivingMessages() {
        withContext(Dispatchers.IO) {
            for (msg in incomingChannel) {
                numberOfMessagesInProgress.incrementAndGet()
                val byteBuffer = msg.getByteBuffer()
                fileChannel.writeWithinCoroutine(byteBuffer, currentPosition)
                currentPosition += byteBuffer.capacity()
            }
        }
    }

    private suspend fun AsynchronousFileChannel.writeWithinCoroutine(buf: ByteBuffer, position: Long): Int =
        suspendCoroutine { cont ->
            write(buf, position, Unit, object : CompletionHandler<Int, Unit> {
                override fun completed(bytesRead: Int, attachment: Unit) {
                    cont.resume(bytesRead)
                    numberOfMessagesInProgress.decrementAndGet()
                }

                override fun failed(exception: Throwable, attachment: Unit) {
                    cont.resumeWithException(exception)
                    numberOfMessagesInProgress.decrementAndGet()
                }
            })
        }

    fun waitForCompletion() {
        while (numberOfMessagesInProgress.get() > 0) {
        }
        return
    }
}