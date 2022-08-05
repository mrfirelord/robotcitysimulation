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
    suspend fun insert(cityName: String, row: Int, column: Int, robotName: String, status: BuildingStatus) {
        connection.sendPreparedStatement(
            "INSERT INTO `building` (`city_name`, `row`, `column`, `robot_name`, `status`) " +
                    "VALUES               (?,           $row,  $column,  ?,             ?)",
            listOf(cityName, robotName, status.toString())
        )
    }

    suspend fun updateStatus(cityName: String, row: Int, column: Int, status: String) {
        connection.sendPreparedStatement(
            "UPDATE `building` SET `status` = ? WHERE `city_name` = ? AND `row` = $row AND `column` = $column",
            listOf(status, cityName)
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