package com.ncert7.mathandsciencelab.utils

import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.ncert7.mathandsciencelab.repository.StreakRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * StreakManager is responsible for tracking the user's learning streak.
 * Now uses Firebase-backed StreakRepository for persistent, user-linked data.
 *
 * Streak Logic:
 * - A streak continues if the user opens a concept on consecutive calendar days
 * - Opening multiple times on the same day doesn't increase the streak
 * - Missing a day breaks the streak (resets to 1 on next open)
 * - The streak is calendar-day based, not 24-hour based
 * - Streak data is now persisted in Firestore and synced across devices
 */
class StreakManager(
    private val streakRepository: StreakRepository,
    private val userId: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    /**
     * Records a Concept screen open and updates the streak accordingly.
     *
     * Streak behavior:
     * - First time: streak = 1
     * - Same day: streak unchanged
     * - Next consecutive day: streak + 1
     * - Day(s) skipped: streak resets to 1
     *
     * @return The current streak count after update
     */
    fun onConceptOpened(onStreakUpdated: (Int) -> Unit = {}) {
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val today = getDayIdentifier(now)

                // Get current streak from repository
                val currentStreak = streakRepository.getUserStreak(userId)

                val newStreak = when {
                    // First ever streak event for this user
                    currentStreak == null -> {
                        DebugLogger.debugLog("StreakManager", "First streak event - starting at 1")
                        streakRepository.createStreakForUser(userId)
                        1
                    }

                    // Same calendar day → do NOT increment
                    isSameDay(currentStreak.lastStreakDate, now) -> {
                        DebugLogger.debugLog("StreakManager", "Same day - streak remains ${currentStreak.streakCount}")
                        currentStreak.streakCount
                    }

                    // Next consecutive day → continue streak
                    isConsecutiveDay(currentStreak.lastStreakDate, now) -> {
                        val newCount = currentStreak.streakCount + 1
                        DebugLogger.debugLog("StreakManager", "Consecutive day - streak increased to $newCount")
                        streakRepository.updateStreak(userId, newCount, today)
                        newCount
                    }

                    // Days were skipped → reset streak
                    else -> {
                        DebugLogger.debugLog("StreakManager", "Day(s) skipped - streak reset to 1 (was ${currentStreak.streakCount})")
                        streakRepository.updateStreak(userId, 1, today)
                        1
                    }
                }

                onStreakUpdated(newStreak)
            } catch (e: Exception) {
                DebugLogger.errorLog("StreakManager", "Error updating streak: ${e.message}")
            }
        }
    }

    /**
     * Gets the current streak value with validation.
     * If days were skipped since last activity, returns 0.
     *
     * @param onStreakFetched Callback with current valid streak count
     */
    fun getCurrentStreak(onStreakFetched: (Int) -> Unit) {
        scope.launch {
            try {
                val streak = streakRepository.getUserStreak(userId)

                val streakCount = when {
                    streak == null -> {
                        DebugLogger.debugLog("StreakManager", "No streak data found - returning 0")
                        0
                    }

                    streak.lastStreakDate == 0L -> {
                        DebugLogger.debugLog("StreakManager", "Invalid streak data - returning 0")
                        0
                    }

                    else -> {
                        val now = System.currentTimeMillis()
                        if (isSameDay(streak.lastStreakDate, now) || isConsecutiveDay(streak.lastStreakDate, now)) {
                            DebugLogger.debugLog("StreakManager", "Valid streak: ${streak.streakCount} days")
                            streak.streakCount
                        } else {
                            DebugLogger.debugLog("StreakManager", "Streak expired - was ${streak.streakCount} days")
                            0
                        }
                    }
                }

                onStreakFetched(streakCount)
            } catch (e: Exception) {
                DebugLogger.errorLog("StreakManager", "Error getting streak: ${e.message}")
                onStreakFetched(0)
            }
        }
    }

    /**
     * Clear local streak data on logout (Firestore data persists)
     * The user's streak is preserved and will continue next time they login
     */
    fun resetStreak(onReset: () -> Unit = {}) {
        scope.launch {
            try {
                streakRepository.clearLocalStreakOnLogout()
                DebugLogger.debugLog("StreakManager", "Local streak cleared (Firestore preserved)")
                onReset()
            } catch (e: Exception) {
                DebugLogger.errorLog("StreakManager", "Error clearing local streak: ${e.message}")
            }
        }
    }

    /**
     * Check if two timestamps fall on the same calendar day.
     */
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if time2 is exactly one calendar day after time1.
     */
    private fun isConsecutiveDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = time1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            timeInMillis = time2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        cal1.add(Calendar.DAY_OF_YEAR, 1)

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Get day identifier (start of day timestamp) for consistent day comparison.
     */
    private fun getDayIdentifier(time: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}