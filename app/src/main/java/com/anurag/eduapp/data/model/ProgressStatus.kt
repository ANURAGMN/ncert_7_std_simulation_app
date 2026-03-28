package com.anurag.eduapp.data.model

/**
 * Unified enum representing the status of any learning item (lesson, concept, chapter, etc.)
 */
enum class ProgressStatus(val value: String) {
    COMPLETED("COMPLETED"),
    IN_PROGRESS("IN_PROGRESS"),
    NOT_STARTED("NOT_STARTED"),
    LOCKED("LOCKED");

    companion object {
        fun fromString(status: String): ProgressStatus {
            return entries.find { it.value == status } ?: NOT_STARTED
        }
    }
}
