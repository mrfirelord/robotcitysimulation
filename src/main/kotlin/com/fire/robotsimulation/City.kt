package com.fire.robotsimulation

import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

data class City(val name: String, val rows: Int, val columns: Int)

data class Land(val cityName: String, val row: Int, val column: Int) {
    override fun toString() = "$cityName|$row|$column"
}

class Building(val features: List<Feature>, val cost: Int)

const val CITY_COUNT = 1
const val CITY_ROWS = 100
const val CITY_COLUMNS = 100

class CityManager {
    private val landCount = CITY_COUNT * CITY_ROWS * CITY_COLUMNS
    private val lock = ReentrantLock()

    private var emptyLandEndElementIndex = landCount
    private val emptyLands = Array(landCount) { Land("", 0, 0) }
    private val landsWithInProgressBuilding = mutableSetOf<Land>()
    private val cities: List<City>

    init {
        cities = loadCities()
        for (cityIndex in cities.indices) {
            for (row in 0 until CITY_ROWS) {
                for (column in 0 until CITY_COLUMNS) {
                    emptyLands[cityIndex * (CITY_ROWS * CITY_COLUMNS) + row * CITY_COLUMNS + column] =
                        Land(cities[cityIndex].name, row, column)
                }
            }
        }
    }

    /** Returns available land from random city. Returns null if no land is available */
    fun pickUpLand(): Land? {
        lock.lock()
        try {
            if (emptyLandEndElementIndex == 0)
                return null

            val randomIndex = Random.nextInt(landCount)
            val land = emptyLands[randomIndex]
            emptyLandEndElementIndex--
            emptyLands[randomIndex] = emptyLands[emptyLandEndElementIndex]
            landsWithInProgressBuilding.add(land)
            return land
        } finally {
            lock.unlock()
        }
    }

    fun finishBuilding(land: Land) {
        landsWithInProgressBuilding.remove(land)
    }

    fun loadCities(): List<City> = (0 until CITY_COUNT).map { City("City $it", CITY_ROWS, CITY_COLUMNS) }
}

enum class Feature {
    FEATURE0,
    FEATURE1,
    FEATURE2,
    FEATURE3,
    FEATURE4,
    FEATURE5,
    FEATURE6,
    FEATURE7,
    FEATURE8,
    FEATURE9
}
