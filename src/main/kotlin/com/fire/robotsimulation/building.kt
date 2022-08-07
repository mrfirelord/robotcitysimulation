package com.fire.robotsimulation

import com.github.jasync.sql.db.SuspendingConnection

enum class BuildingStatus { IN_PROGRESS, COMPLETED }

data class BuildingRow(
    val cityName: String,
    val row: Int,
    val column: Int,
    val robotName: String,
    val status: BuildingStatus
)

class BuildingDao(private val connection: SuspendingConnection) {
    suspend fun clearAllData() {
        connection.sendPreparedStatement("DELETE FROM `temp`", listOf())
    }

    suspend fun insertInProgress(land: Land, robotName: String) {
        connection.sendPreparedStatement(
            "INSERT INTO `building` (`city_name`, `row`,        `column`,       `robot_name`, `status`) " +
                    "VALUES               (?,           ${land.row},  ${land.column}, ?,             ?)",
            values = listOf(land.cityName, robotName, BuildingStatus.IN_PROGRESS.toString()),
            release = true
        )
    }

    suspend fun completeBuilding(land: Land) {
        connection.sendPreparedStatement(
            "UPDATE `building` SET `status` = ? " +
                    "WHERE `city_name` = ? AND `row` = ${land.row} AND `column` = ${land.column}",
            listOf(BuildingStatus.COMPLETED.toString(), land.cityName),
            release = true
        )
    }

    suspend fun getBuildings(connection: SuspendingConnection): List<BuildingRow> =
        connection.sendQuery("select * from building").rows.map {
            BuildingRow(
                cityName = it.getString("city_name") as String,
                row = it.getInt("row") as Int,
                column = it.getInt("column") as Int,
                robotName = it.getString("robot_name") as String,
                status = BuildingStatus.valueOf(it.getString("status") as String)
            )
        }
}