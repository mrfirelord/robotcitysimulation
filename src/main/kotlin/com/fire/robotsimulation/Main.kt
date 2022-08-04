package com.fire.robotsimulation

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlin.random.Random
import kotlin.system.measureTimeMillis

object Main {
    private var totalDelay = 0L
    private val cityManager = CityManager()

    @JvmStatic
    fun main(args: Array<String>) {
        val channel = Channel<NotifierMsg>(100)
        val time = measureTimeMillis {
            val alternativeNotifier = AlternativeNotifier(channel)
            runBlocking {
                launch { alternativeNotifier.startReceivingMessages() }
                createRobots().map { async { runOneRobot(it, channel) } }.awaitAll()
                alternativeNotifier.waitForCompletion()
                channel.close()
            }
        }
        println(time)
        println(totalDelay)
    }

    private suspend fun runOneRobot(robot: Robot, notifierChannel: SendChannel<NotifierMsg>) {
        var land = cityManager.pickUpLand()
        while (land != null) {
            val building = getBuildingWithRandomFeatures()
            notifierChannel.send(NotifierMsg(robot.name, land.toString(), building.cost, BuildingStage.START))

            buildHouse(robot, building)

            cityManager.finishBuilding(land)
            notifierChannel.send(NotifierMsg(robot.name, land.toString(), building.cost, BuildingStage.FINISH))
            robot.finishBuilding(building)

            land = cityManager.pickUpLand()
        }
    }

    private fun getBuildingWithRandomFeatures(): Building {
        val features = mutableListOf<Feature>()
        var randomBits = Random.nextBits(Feature.values().size)
        var featureIndex = 0
        var totalCost = 0
        while (randomBits > 0) {
            if (randomBits and 1 == 1) {
                features.add(Feature.values()[featureIndex])
                totalCost += 10
            }

            featureIndex++
            randomBits = randomBits shr 1
        }

        return Building(features, totalCost)
    }

    private suspend fun buildHouse(robot: Robot, building: Building) {
        val buildingTime = building.features.size * 10L
        totalDelay += buildingTime
//        delay(buildingTime)
    }
}