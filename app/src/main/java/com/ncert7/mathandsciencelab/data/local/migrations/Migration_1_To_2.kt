package com.ncert7.mathandsciencelab.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ncert7.mathandsciencelab.debug.DebugLogger

/**
 * Migration from schema version 1 to 2
 *
 * Changes from version 1 to 2:
 * - Added 'streak' table for streak tracking (new table in v2)
 * - Added 'appName' column to sessions, app_analytics, and progress tables
 *
 * This migration is used by users upgrading from v1.0 to v1.0.1
 */
object Migration_1_To_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            DebugLogger.debugLog("Migration_1_To_2", "Starting database migration from v1 to v2")

            // Step 1: Create the streak table (new in v2)
            db.execSQL(
                """
                CREATE TABLE `streak` (
                    `appName` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL DEFAULT 0,
                    `lastStreakDate` INTEGER NOT NULL,
                    `streakCount` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL,
                    `userId` TEXT NOT NULL PRIMARY KEY
                )
                """.trimIndent()
            )

            // Step 2: Add appName column to sessions table
            // First check if column exists
            val sessionsCursor = db.query("PRAGMA table_info(`sessions`)")
            var appNameExistsInSessions = false
            while (sessionsCursor.moveToNext()) {
                val columnName = sessionsCursor.getString(1) // Column name is at index 1
                if (columnName == "appName") {
                    appNameExistsInSessions = true
                    break
                }
            }
            sessionsCursor.close()

            if (!appNameExistsInSessions) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `appName` TEXT NOT NULL DEFAULT ''")
                DebugLogger.debugLog("Migration_1_To_2", "Added appName column to sessions")
            } else {
                DebugLogger.debugLog("Migration_1_To_2", "appName column already exists in sessions")
            }

            // Step 3: Add appName column to app_analytics table
            val analyticsCursor = db.query("PRAGMA table_info(`app_analytics`)")
            var appNameExistsInAnalytics = false
            while (analyticsCursor.moveToNext()) {
                val columnName = analyticsCursor.getString(1)
                if (columnName == "appName") {
                    appNameExistsInAnalytics = true
                    break
                }
            }
            analyticsCursor.close()

            if (!appNameExistsInAnalytics) {
                db.execSQL("ALTER TABLE `app_analytics` ADD COLUMN `appName` TEXT NOT NULL DEFAULT ''")
                DebugLogger.debugLog("Migration_1_To_2", "Added appName column to app_analytics")
            } else {
                DebugLogger.debugLog("Migration_1_To_2", "appName column already exists in app_analytics")
            }

            // Step 4: Add appName column to progress table
            val progressCursor = db.query("PRAGMA table_info(`progress`)")
            var appNameExistsInProgress = false
            while (progressCursor.moveToNext()) {
                val columnName = progressCursor.getString(1)
                if (columnName == "appName") {
                    appNameExistsInProgress = true
                    break
                }
            }
            progressCursor.close()

            if (!appNameExistsInProgress) {
                db.execSQL("ALTER TABLE `progress` ADD COLUMN `appName` TEXT NOT NULL DEFAULT ''")
                DebugLogger.debugLog("Migration_1_To_2", "Added appName column to progress")
            } else {
                DebugLogger.debugLog("Migration_1_To_2", "appName column already exists in progress")
            }

            DebugLogger.debugLog("Migration_1_To_2", "Migration completed successfully")
        } catch (e: Exception) {
            DebugLogger.errorLog("Migration_1_To_2", "Migration failed: ${e.message}")
            throw e
        }
    }
}
