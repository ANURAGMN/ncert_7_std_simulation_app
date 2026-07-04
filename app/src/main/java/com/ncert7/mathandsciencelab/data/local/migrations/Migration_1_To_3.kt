package com.ncert7.mathandsciencelab.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ncert7.mathandsciencelab.debug.DebugLogger

/**
 * Migration from schema version 1 to 3 (Direct Path)
 *
 * Changes from version 1 to 3:
 * - Added 'streak' table for streak tracking (v2 addition)
 * - Added 'appName' column to sessions, app_analytics, and progress tables (v2 additions)
 * - Removed 'localProfilePhotoUri' column from students table (v3 change)
 *
 * This migration is used by users who skip v2 and go directly from v1.0 to v1.0.2
 */
object Migration_1_To_3 : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            DebugLogger.debugLog("Migration_1_To_3", "Starting direct database migration from v1 to v3")

            // Step 1: Create the streak table (new in v2, required in v3)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `streak` (
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

            // Step 2: Add appName column to sessions table - CHECK IF EXISTS FIRST
            val sessionsCursor = db.query("PRAGMA table_info(`sessions`)")
            var appNameExistsInSessions = false
            while (sessionsCursor.moveToNext()) {
                val columnName = sessionsCursor.getString(1)
                if (columnName == "appName") {
                    appNameExistsInSessions = true
                    break
                }
            }
            sessionsCursor.close()

            if (!appNameExistsInSessions) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `appName` TEXT NOT NULL DEFAULT ''")
                DebugLogger.debugLog("Migration_1_To_3", "Added appName column to sessions")
            }

            // Step 3: Add appName column to app_analytics table - CHECK IF EXISTS FIRST
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
                DebugLogger.debugLog("Migration_1_To_3", "Added appName column to app_analytics")
            }

            // Step 4: Add appName column to progress table - CHECK IF EXISTS FIRST
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
                DebugLogger.debugLog("Migration_1_To_3", "Added appName column to progress")
            }

            // Step 5: Migrate students table to v3 schema (remove localProfilePhotoUri)
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

            // Copy all student data carefully
            db.execSQL(
                """
                INSERT INTO `students_new` 
                (`studentId`, `studentName`, `email`, `phoneNumber`, `studentSchool`, `language`, 
                 `classLevel`, `profilePhotoUrl`, `createdAt`, `updatedAt`, `isSynced`)
                SELECT 
                    `studentId`, `studentName`, `email`, `phoneNumber`, `studentSchool`, `language`, 
                    COALESCE(`classLevel`, 7), `profilePhotoUrl`, 
                    COALESCE(`createdAt`, ${System.currentTimeMillis()}), 
                    COALESCE(`updatedAt`, ${System.currentTimeMillis()}), 
                    COALESCE(`isSynced`, 0)
                FROM `students`
                """.trimIndent()
            )

            // Drop old table and rename new one
            db.execSQL("DROP TABLE `students`")
            db.execSQL("ALTER TABLE `students_new` RENAME TO `students`")

            DebugLogger.debugLog("Migration_1_To_3", "Direct migration from v1 to v3 completed successfully")
        } catch (e: Exception) {
            DebugLogger.errorLog("Migration_1_To_3", "Migration failed: ${e.message}")
            throw e
        }
    }
}
