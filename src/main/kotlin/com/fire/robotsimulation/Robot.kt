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

    fun finishBuilding(building: Building) {
        earnedGold += building.cost
        // add/update skills
    }

    override fun toString() = name
}

fun createRobots(): List<Robot> = ROBOT_NAMES.map { Robot(it) }