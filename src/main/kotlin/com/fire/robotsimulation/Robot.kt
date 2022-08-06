package com.fire.robotsimulation

val ROBOT_NAMES = listOf(
    "Sark",
    "Clank",
    "Axel",
    "Ittron",
    "Rubber",
    "Gigabit",
    "Cybel",
    "Asm",
    "Eliz",
    "Tinker",
    "Mechan",
    "Ofm",
    "Ebezoid",
    "Spanner",
    "Drillbit"
)

class Robot(val name: String) {
    var earnedGold: Int = 0

    /** Returns time spend on building */
    suspend fun buildHouse(building: Building): Int {
        val result = building.features.size * 10
//        delay(buildingTime)
        earnedGold += building.cost
        return result
    }

    override fun toString() = name
}

fun createRobots(): List<Robot> = ROBOT_NAMES.map { Robot(it) }