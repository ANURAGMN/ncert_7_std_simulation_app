package com.ncert7.mathandsciencelab.service.sync


import android.content.Context
import com.ncert7.mathandsciencelab.data.local.dao.ChapterDao
import com.ncert7.mathandsciencelab.data.local.dao.ConceptDao
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.dao.SubjectDao
import com.ncert7.mathandsciencelab.data.local.entities.ChapterEntity
import com.ncert7.mathandsciencelab.data.local.entities.ConceptEntity
import com.ncert7.mathandsciencelab.data.local.entities.SubjectEntity
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
/**
 * Handles synchronization of educational content and user progress from Firebase Firestore to local Room database.
 * Uses mapper objects to convert Firestore documents to local entities.
 */
class FirebaseSyncManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val conceptDao: ConceptDao,
    private val progressDao: ProgressDao? = null,
    private val context: Context? = null
) {
    companion object {
        private const val CONCEPTS_COLLECTION = "Concept"
        private const val TAG = "FirebaseSyncManager"
    }

    /**
     * Syncs all concepts from Firestore and extracts unique subjects and chapters to populate the
     * local database. Also detects and notifies about new simulation concepts.
     */
    suspend fun syncAllContent(): SyncResult {
        return try {
            DebugLogger.debugLog(TAG, "Starting content sync from Firestore...")

            // Fetch all concept documents from Firestore
            val snapshot = firestore.collection(CONCEPTS_COLLECTION).get().await()

            if (snapshot.isEmpty) {
                DebugLogger.debugLog(TAG, "No concepts found in Firestore")
                return SyncResult(success = true, message = "No data to sync")
            }

            val subjects =
                mutableMapOf<String, SubjectEntity>()
            val chapters =
                mutableMapOf<String, ChapterEntity>()
            val concepts = mutableListOf<ConceptEntity>()

            // Process each document
            for (document in snapshot.documents) {
                try {
                    // Extract and store unique subjects
                    val subjectId = document.getString("subject_id")
                    if (subjectId != null && !subjects.containsKey(subjectId)) {
                        val subject = FirebaseSubjectMapper.map(document)
                        subjects[subjectId] = subject
                    }

                    val chapterId = document.getString("chapter_id")
                    if (chapterId != null && !chapters.containsKey(chapterId)) {
                        val chapter = FirebaseChapterMapper.map(document)
                        chapters[chapterId] = chapter
                    }

                    // Map concept
                    val concept = FirebaseConceptMapper.map(document)
                    concepts.add(concept)
                } catch (e: Exception) {
                    DebugLogger.errorLog(TAG, "Error mapping document ${document.id}: ${e.message}")
                }
            }

            // Insert into local database in correct order (subjects -> chapters -> concepts)
            DebugLogger.debugLog(TAG, "Inserting ${subjects.size} subjects...")
            subjectDao.insertSubjects(subjects.values.toList())

            DebugLogger.debugLog(TAG, "Inserting ${chapters.size} chapters...")
            chapterDao.insertChapters(chapters.values.toList())

            DebugLogger.debugLog(TAG, "Inserting ${concepts.size} concepts...")

            // Detect new simulations BEFORE inserting
            var newSimulations = emptyList<ConceptEntity>()
            if (context != null) {
                newSimulations = NewSimulationNotifier.getNewSimulations(concepts, conceptDao)
            }

            conceptDao.insertConcepts(concepts)

            // Show toast AFTER inserting
            if (context != null && newSimulations.isNotEmpty()) {
                NewSimulationNotifier.showNotification(context, newSimulations)
            }

            val message =
                "Synced ${subjects.size} subjects, ${chapters.size} chapters, ${concepts.size} concepts"
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

            val snapshot =
                firestore
                    .collection(CONCEPTS_COLLECTION)
                    .whereEqualTo("subject_id", subjectId)
                    .get()
                    .await()

            if (snapshot.isEmpty) {
                return SyncResult(success = true, message = "No concepts found for subject")
            }

            val chapters =
                mutableMapOf<String, ChapterEntity>()
            val concepts = mutableListOf<ConceptEntity>()

            for (document in snapshot.documents) {
                try {
                    // Extract unique chapters (chapter_id is now unique string)
                    val chapterId = document.getString("chapter_id")
                    if (chapterId != null && !chapters.containsKey(chapterId)) {
                        val chapter = FirebaseChapterMapper.map(document)
                        chapters[chapterId] = chapter
                    }

                    // Map concept
                    val concept = FirebaseConceptMapper.map(document)
                    concepts.add(concept)
                } catch (e: Exception) {
                    DebugLogger.errorLog(TAG, "Error mapping document: ${e.message}")
                }
            }

            chapterDao.insertChapters(chapters.values.toList())

            // Detect new simulations BEFORE inserting
            var newSimulations = emptyList<ConceptEntity>()
            if (context != null) {
                newSimulations = NewSimulationNotifier.getNewSimulations(concepts, conceptDao)
            }

            conceptDao.insertConcepts(concepts)

            // Show toast AFTER inserting
            if (context != null && newSimulations.isNotEmpty()) {
                NewSimulationNotifier.showNotification(context, newSimulations)
            }

            val message =
                "Synced ${chapters.size} chapters, ${concepts.size} concepts for subject $subjectId"
            DebugLogger.debugLog(TAG, message)

            SyncResult(success = true, message = message)
        } catch (e: Exception) {
            val errorMsg = "Subject sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }

    /** Syncs concepts for a specific chapter */
    suspend fun syncChapterContent(chapterId: String): SyncResult {
        return try {
            DebugLogger.debugLog(TAG, "Syncing content for chapter: $chapterId")

            val snapshot =
                firestore
                    .collection(CONCEPTS_COLLECTION)
                    .whereEqualTo("chapter_id", chapterId)
                    .get()
                    .await()

            if (snapshot.isEmpty) {
                return SyncResult(success = true, message = "No concepts found for chapter")
            }

            val concepts =
                snapshot.documents.mapNotNull { document ->
                    try {
                        FirebaseConceptMapper.map(document)
                    } catch (e: Exception) {
                        DebugLogger.errorLog(TAG, "Error mapping concept: ${e.message}")
                        null
                    }
                }

            // Detect new simulations BEFORE inserting
            var newSimulations = emptyList<ConceptEntity>()
            if (context != null) {
                newSimulations = NewSimulationNotifier.getNewSimulations(concepts, conceptDao)
            }

            conceptDao.insertConcepts(concepts)

            // Show toast AFTER inserting
            if (context != null && newSimulations.isNotEmpty()) {
                NewSimulationNotifier.showNotification(context, newSimulations)
            }

            val message = "Synced ${concepts.size} concepts for chapter $chapterId"
            DebugLogger.debugLog(TAG, message)

            SyncResult(success = true, message = message)
        } catch (e: Exception) {
            val errorMsg = "Chapter sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }

    /**
     * Syncs user's progress from Firestore to local database
     * Called when an existing user logs in to restore their progress history
     * Progress is fetched from users/{userId}/progress subcollection
     */
    suspend fun syncUserProgress(userId: String): SyncResult {
        return try {
            if (progressDao == null) {
                DebugLogger.debugLog(TAG, "ProgressDao not provided, skipping progress sync")
                return SyncResult(success = true, message = "Progress sync skipped - ProgressDao not available")
            }

            DebugLogger.debugLog(TAG, "Starting user progress sync from Firestore for userId: $userId")

            // Progress is stored at: progress/{userId}/records/{itemType}_{itemId}
            val snapshot = firestore
                .collection("progress")
                .document(userId)
                .collection("records")
                .get()
                .await()

            if (snapshot.isEmpty) {
                DebugLogger.debugLog(TAG, "No progress found for user: $userId")
                return SyncResult(success = true, message = "No progress data to sync")
            }

            val progressList = snapshot.documents.mapNotNull { document ->
                try {
                    FirebaseProgressMapper.map(document, userId)
                } catch (e: Exception) {
                    DebugLogger.errorLog(TAG, "Error mapping progress document ${document.id}: ${e.message}")
                    null // Skip malformed documents
                }
            }

            if (progressList.isNotEmpty()) {
                // Use REPLACE strategy to overwrite any local progress with Firestore data
                progressDao.insertProgressList(progressList)
                DebugLogger.debugLog(TAG, " Synced ${progressList.size} progress entries for user: $userId")
            }

            val message = "Synced ${progressList.size} progress entries for user $userId"
            SyncResult(success = true, message = message)
        } catch (e: Exception) {
            val errorMsg = "User progress sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }
}

/** Result of a sync operation */
data class SyncResult(val success: Boolean, val message: String)


