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
class LogMsg(val robotName: String, val land: String, val cost: Int, val buildingStage: BuildingStage)

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
                val byteBuffer = getByteBuffer(msg)
                fileChannel.writeWithinCoroutine(byteBuffer, currentPosition)
                currentPosition += byteBuffer.capacity()
            }
        }
    }

    private fun getByteBuffer(msg: LogMsg): ByteBuffer {
        val logLine = if (msg.buildingStage == BuildingStage.START)
            "Robot ${msg.robotName} started building on ${msg.land}. Cost => ${msg.cost}\r\n".toByteArray()
        else
            "Robot ${msg.robotName} finished building on ${msg.land}. Cost => ${msg.cost}\r\n".toByteArray()
        val byteBuffer = ByteBuffer.wrap(logLine)
        return byteBuffer
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