package com.fire.robotsimulation

import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

object Main {
    private val totalDelay = AtomicLong(0)
    private val cityManager = CityManager()

    @JvmStatic
    fun main(args: Array<String>) {
        val connection = MySQLConnectionBuilder.createConnectionPool(
            "jdbc:mysql://127.0.0.1:3306/robotsimulation?user=app&password=159753"
        )
        val buildingDao = BuildingDao(connection.asSuspending)

        val logMsgChannel = Channel<LogMsg>(100)
        val time = measureTimeMillis {
            val logWriter = LogWriter(logMsgChannel)
            runBlocking {
                launch { logWriter.startReceivingMessages() }
                createRobots().map { async { runOneRobot(it, logMsgChannel) } }.awaitAll()
                logWriter.waitForCompletion()
                logMsgChannel.close()
            }
        }
        println(time)
        println(totalDelay)
    }

    private suspend fun runOneRobot(robot: Robot, logChannel: SendChannel<LogMsg>) {
        var land = cityManager.pickUpLand()
        while (land != null) {
            val building = getBuildingWithRandomFeatures()
            logChannel.send(LogMsg(robot.name, land.toString(), building.cost, BuildingStatus.IN_PROGRESS))

            val buildingTime = robot.buildHouse(building)
            totalDelay.accumulateAndGet(buildingTime.toLong()) { l, r -> l + r }

            cityManager.finishBuilding(land)
            logChannel.send(LogMsg(robot.name, land.toString(), building.cost, BuildingStatus.COMPLETED))

            land = cityManager.pickUpLand()
        }
    }

    private fun getBuildingWithRandomFeatures(): Building {
        val features = BuildingFeature.getRandomFeatures()
        return Building(features, features.size * 10)
    }
}