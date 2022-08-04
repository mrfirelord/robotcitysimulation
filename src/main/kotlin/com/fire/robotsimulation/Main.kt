package com.fire.robotsimulation

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
            logChannel.send(LogMsg(robot.name, land.toString(), building.cost, BuildingStage.START))

            buildHouse(robot, building)

            cityManager.finishBuilding(land)
            logChannel.send(LogMsg(robot.name, land.toString(), building.cost, BuildingStage.FINISH))
            robot.finishBuilding(building)

            land = cityManager.pickUpLand()
        }
    }

    private fun getBuildingWithRandomFeatures(): Building {
        val features = BuildingFeature.getRandomFeatures()
        return Building(features, features.size * 10)
    }

    private suspend fun buildHouse(robot: Robot, building: Building) {
        val buildingTime = building.features.size * 10L
        totalDelay.accumulateAndGet(buildingTime) { l, r -> l + r }
//        delay(buildingTime)
    }
}