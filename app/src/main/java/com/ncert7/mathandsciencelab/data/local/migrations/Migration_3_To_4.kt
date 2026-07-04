package com.ncert7.mathandsciencelab.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ncert7.mathandsciencelab.debug.DebugLogger

object Migration_3_To_4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `simulation_interactions` (
                    `interactionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `studentId` TEXT NOT NULL,
                    `sessionId` TEXT NOT NULL,
                    `simulationTitle` TEXT NOT NULL,
                    `subjectName` TEXT NOT NULL,
                    `chapterName` TEXT NOT NULL,
                    `elementClicked` TEXT NOT NULL,
                    `elementType` TEXT NOT NULL,
                    `givenAnswer` TEXT NOT NULL,
                    `isCorrect` TEXT NOT NULL,
                    `timeTaken` TEXT NOT NULL,
                    `timestamp` TEXT NOT NULL,
                    `occurredAt` INTEGER NOT NULL,
                    `interactionDate` TEXT NOT NULL,
                    `appName` TEXT NOT NULL DEFAULT '',
                    `isSynced` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`sessionId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_studentId` ON `simulation_interactions`(`studentId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_sessionId` ON `simulation_interactions`(`sessionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_interactionDate` ON `simulation_interactions`(`interactionDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_studentId_interactionDate_isSynced` ON `simulation_interactions`(`studentId`, `interactionDate`, `isSynced`)")
            DebugLogger.debugLog("Migration_3_To_4", "Created simulation_interactions table")
        } catch (e: Exception) {
            DebugLogger.errorLog("Migration_3_To_4", "Migration failed: ${e.message}")
            throw e
        }
    }
}
