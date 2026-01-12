package com.appswithlove.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

object DatabaseFactory {
    private const val DATABASE_NAME = "toggl2float.db"

    fun createDriver(): SqlDriver {
        val dbPath = getDatabasePath()
        val dbFile = File(dbPath)
        val dbExists = dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        if (!dbExists) {
            TimeTrackingDatabase.Schema.create(driver)
        } else {
            runMigrations(driver)
        }
        return driver
    }

    private fun runMigrations(driver: SqlDriver) {
        // Migration 1: Add Float project/phase cache tables
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS floatProject (
                project_id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                color TEXT,
                active INTEGER NOT NULL DEFAULT 1,
                cached_at TEXT NOT NULL
            )
        """.trimIndent(), 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS floatPhase (
                phase_id INTEGER PRIMARY KEY,
                project_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                color TEXT,
                active INTEGER NOT NULL DEFAULT 1,
                cached_at TEXT NOT NULL
            )
        """.trimIndent(), 0)

        driver.execute(null, """
            CREATE INDEX IF NOT EXISTS idx_phase_project ON floatPhase(project_id)
        """.trimIndent(), 0)
    }

    private fun getDatabasePath(): String {
        val appDir = File(System.getProperty("user.home"), ".toggl2float")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return File(appDir, DATABASE_NAME).absolutePath
    }
}
