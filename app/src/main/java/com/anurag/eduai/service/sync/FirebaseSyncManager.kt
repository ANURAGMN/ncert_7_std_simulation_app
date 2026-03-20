package com.anurag.eduai.service.sync

import androidx.room.RoomDatabase
import com.anurag.eduai.data.local.EduAiDatabase
import com.anurag.eduai.data.local.entities.ChapterEntity
import com.anurag.eduai.data.local.entities.ConceptEntity
import com.anurag.eduai.data.local.entities.SubjectEntity
import com.anurag.eduai.debug.DebugLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Handles synchronization of educational content from Firebase Firestore to local Room database.
 * Uses mapper objects to convert Firestore documents to local entities.
 */
class FirebaseSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: EduAiDatabase
) { private val subjectDao = database.subjectDao()
    private val chapterDao = database.chapterDao()
    private val conceptDao = database.conceptDao()

    companion object {
        private const val CONCEPTS_COLLECTION = "Concept"
        private const val TAG = "FirebaseSyncManager"
    }

    /**
     * Syncs all concepts from Firestore and extracts unique subjects and chapters.
     * Uses a Room Transaction to ensure all or nothing is saved.
     */
    suspend fun syncAllContent(): SyncResult {
        return try {
            DebugLogger.debugLog(TAG, "Starting content sync from Firestore...")

            val snapshot = firestore.collection(CONCEPTS_COLLECTION).get().await()

            if (snapshot.isEmpty) {
                DebugLogger.debugLog(TAG, "No concepts found in Firestore")
                return SyncResult(success = true, message = "No data to sync")
            }

            val subjects = mutableMapOf<String, SubjectEntity>()
            val chapters = mutableMapOf<String, ChapterEntity>()
            val concepts = mutableListOf<ConceptEntity>()

            for (document in snapshot.documents) {
                try {
                    val subjectId = document.getString("subject_id")
                    val chapterId = document.getString("chapter_id")

                    // Map Subject if new
                    if (subjectId != null && !subjects.containsKey(subjectId)) {
                        FirebaseSubjectMapper.map(document)?.let { subjects[subjectId] = it }
                    }

                    // Map Chapter if new
                    if (chapterId != null && !chapters.containsKey(chapterId)) {
                        FirebaseChapterMapper.map(document)?.let { chapters[chapterId] = it }
                    }

                    // Map Concept
                    FirebaseConceptMapper.map(document)?.let { concepts.add(it) }

                } catch (e: Exception) {
                    DebugLogger.errorLog(TAG, "Error mapping document ${document.id}: ${e.message}")
                }
            }

            // Perform insertion in a single transaction
            database.withTransaction {
                DebugLogger.debugLog(TAG, "Database Transaction: Inserting data...")
                subjectDao.insertSubjects(subjects.values.toList())
                chapterDao.insertChapters(chapters.values.toList())
                conceptDao.insertConcepts(concepts)
            }

            val message = "Synced ${subjects.size} subjects, ${chapters.size} chapters, ${concepts.size} concepts"
            DebugLogger.debugLog(TAG, message)
            SyncResult(success = true, message = message)

        } catch (e: Exception) {
            val errorMsg = "Sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }

    /** Syncs concepts for a specific subject */
    suspend fun syncSubjectContent(subjectId: String): SyncResult {
        return try {
            DebugLogger.debugLog(TAG, "Syncing content for subject: $subjectId")

            val snapshot = firestore.collection(CONCEPTS_COLLECTION)
                .whereEqualTo("subject_id", subjectId)
                .get()
                .await()

            if (snapshot.isEmpty) return SyncResult(success = true, message = "No data found")

            val chapters = mutableMapOf<String, ChapterEntity>()
            val concepts = mutableListOf<ConceptEntity>()

            for (document in snapshot.documents) {
                val chapterId = document.getString("chapter_id")
                if (chapterId != null && !chapters.containsKey(chapterId)) {
                    FirebaseChapterMapper.map(document)?.let { chapters[chapterId] = it }
                }
                FirebaseConceptMapper.map(document)?.let { concepts.add(it) }
            }

            database.withTransaction {
                chapterDao.insertChapters(chapters.values.toList())
                conceptDao.insertConcepts(concepts)
            }

            SyncResult(success = true, message = "Synced ${concepts.size} concepts for $subjectId")
        } catch (e: Exception) {
            SyncResult(success = false, message = e.message ?: "Subject sync failed")
        }
    }

    /** Syncs concepts for a specific chapter */
    suspend fun syncChapterContent(chapterId: String): SyncResult {
        return try {
            DebugLogger.debugLog(TAG, "Syncing content for chapter: $chapterId")

            val snapshot = firestore.collection(CONCEPTS_COLLECTION)
                .whereEqualTo("chapter_id", chapterId)
                .get()
                .await()

            val concepts = snapshot.documents.mapNotNull { FirebaseConceptMapper.map(it) }

            conceptDao.insertConcepts(concepts)
            SyncResult(success = true, message = "Synced ${concepts.size} concepts")
        } catch (e: Exception) {
            SyncResult(success = false, message = e.message ?: "Chapter sync failed")
        }
    }
}

/** Result of a sync operation */
data class SyncResult(val success: Boolean, val message: String)

/**
 * Extension helper for Room Transactions using Coroutines
 */
suspend fun <R> EduAiDatabase.withTransaction(block: suspend () -> R): R {
    return (this as RoomDatabase).run {
        withTransaction(block)
    }
}