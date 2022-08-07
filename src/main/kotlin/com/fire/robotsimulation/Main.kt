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
    private val connectionPool = MySQLConnectionBuilder.createConnectionPool {
        host = "127.0.0.1"
        port = 3306
        database = "robotsimulation"
        username = "app"
        password = "159753"
        maxActiveConnections = 100
    }
    private val buildingDao = BuildingDao(connectionPool.asSuspending)

    @JvmStatic
    fun main(args: Array<String>) {
        val logMsgChannel = Channel<LogMsg>(100)
        val time = measureTimeMillis {
            val logWriter = LogWriter(logMsgChannel)
            runBlocking {
                async { buildingDao.clearAllData() }.join()

                launch { logWriter.startReceivingMessages() }
                createRobots().map {
                    launch(Dispatchers.Default) { runOneRobot(it, logMsgChannel) }
                }.joinAll()
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
            // TODO make inserting into db and building house parallel
            buildingDao.insertInProgress(land, robotName = robot.name)

            val buildingTime = robot.buildHouse(building)
            cityManager.finishBuilding(land)
            buildingDao.completeBuilding(land)

            logChannel.send(LogMsg(robot.name, land.toString(), building.cost, BuildingStatus.COMPLETED))
            totalDelay.accumulateAndGet(buildingTime.toLong()) { l, r -> l + r }
            land = cityManager.pickUpLand()
        }
    }

    private fun getBuildingWithRandomFeatures(): Building {
        val features = BuildingFeature.getRandomFeatures()
        return Building(features, features.size * 10)
    }
}