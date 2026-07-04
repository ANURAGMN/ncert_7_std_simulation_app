package com.ncert7.mathandsciencelab.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ncert7.mathandsciencelab.debug.DebugLogger

/**
 * Migration from schema version 2 to 3
 *
 * Changes:
 * - Removed unused column: localProfilePhotoUri from students table
 *
 * This approach is safe for production apps on PlayStore
 */
object Migration_2_To_3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            DebugLogger.debugLog("Migration_2_To_3", "Starting database migration from v2 to v3")

            // Step 1: Add appName column to sessions table if it doesn't exist
            val sessions = db.query("PRAGMA table_info(`sessions`)")
            var appNameExistsInSessions = false
            while (sessions.moveToNext()) {
                val columnName = sessions.getString(1)
                if (columnName == "appName") {
                    appNameExistsInSessions = true
                    break
                }
            }
            sessions.close()

            if (!appNameExistsInSessions) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `appName` TEXT NOT NULL DEFAULT ''")
                DebugLogger.debugLog("Migration_2_To_3", "Added appName column to sessions")
            }

            // Step 2: Add appName column to app_analytics table if it doesn't exist
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
                DebugLogger.debugLog("Migration_2_To_3", "Added appName column to app_analytics")
            }

            // Step 3: Add appName column to progress table if it doesn't exist
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
                DebugLogger.debugLog("Migration_2_To_3", "Added appName column to progress")
            }

            // Step 3.5: Add appName column to streak table if it doesn't exist
            val streakCursor = db.query("PRAGMA table_info(`streak`)")
            var appNameExistsInStreak = false
            while (streakCursor.moveToNext()) {
                val columnName = streakCursor.getString(1)
                if (columnName == "appName") {
                    appNameExistsInStreak = true
                    break
                }
            }
            streakCursor.close()

            if (!appNameExistsInStreak) {
                db.execSQL("ALTER TABLE `streak` ADD COLUMN `appName` TEXT NOT NULL DEFAULT ''")
                DebugLogger.debugLog("Migration_2_To_3", "Added appName column to streak")
            }

            // Step 4: Migrate students table to v3 schema (remove localProfilePhotoUri if it exists)
            db.execSQL(
                """
                CREATE TABLE `students_new` (
                    `studentId` TEXT NOT NULL,
                    `studentName` TEXT NOT NULL,
                    `email` TEXT NOT NULL,
                    `phoneNumber` TEXT NOT NULL,
                    `studentSchool` TEXT NOT NULL,
                    `language` TEXT NOT NULL,
                    `classLevel` INTEGER NOT NULL DEFAULT 7,
                    `profilePhotoUrl` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`studentId`)
                )
                """.trimIndent()
            )

            db.execSQL("CREATE UNIQUE INDEX `index_students_new_phoneNumber` ON `students_new`(`phoneNumber`)")

            db.execSQL(
                """
                INSERT INTO `students_new` 
                (`studentId`, `studentName`, `email`, `phoneNumber`, `studentSchool`, `language`, 
                 `classLevel`, `profilePhotoUrl`, `createdAt`, `updatedAt`, `isSynced`)
                SELECT 
                    `studentId`, `studentName`, `email`, `phoneNumber`, `studentSchool`, `language`, 
                    COALESCE(`classLevel`, 7), `profilePhotoUrl`, `createdAt`, `updatedAt`, COALESCE(`isSynced`, 0)
                FROM `students`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS `students`")
            db.execSQL("ALTER TABLE `students_new` RENAME TO `students`")

            DebugLogger.debugLog("Migration_2_To_3", "Migration completed successfully")
        } catch (e: Exception) {
            DebugLogger.errorLog("Migration_2_To_3", "Migration failed: ${e.message}")
            throw e
        }
    }
}
