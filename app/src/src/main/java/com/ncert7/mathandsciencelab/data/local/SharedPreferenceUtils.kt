package com.ncert7.mathandsciencelab.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferenceUtils(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_SELECTED_SUBJECT = "selected_subject"
        private const val KEY_SESSION = "key_current_session"
        private const val KEY_SIMULATION_LAST_SYNCED_DATE = "key_simulation_last_synced_date"

        /**
         * The class level for which content is loaded from Firebase and displayed to all users.
         * Regardless of the user's selected class, the app always loads and displays class 7 content.
         */
        const val CLASS_LEVEL = 7
    }

    fun setUserId(id: String) {
        prefs.edit { putString(KEY_USER_ID, id) }
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun setLanguagePreference(lang: String) {
        prefs.edit { putString(KEY_LANGUAGE, lang) }
    }

    fun getLanguagePreference(): String? {
        return prefs.getString(KEY_LANGUAGE, "en") // default: English
    }

    fun setSubjectSelection(subject: String){
        prefs.edit { putString(KEY_SELECTED_SUBJECT, subject) }
    }
    fun getSubjectSelection(): String? {
        return prefs.getString(KEY_SELECTED_SUBJECT, "science")
    }
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /** Stores current session id **/
    fun setCurrentSession(sessionId:String){
        prefs.edit{putString(KEY_SESSION,sessionId)}
    }

    /** Retrieves current session id **/
    fun getCurrentSession(): String? {
        return prefs.getString(KEY_SESSION, null)
    }
    /** Clears current session id **/
    fun clearCurrentSession() {
        prefs.edit { remove(KEY_SESSION) }
    }

    fun setSimulationLastSyncedDate(day: String) {
        prefs.edit { putString(KEY_SIMULATION_LAST_SYNCED_DATE, day) }
    }

    fun getSimulationLastSyncedDate(): String? {
        return prefs.getString(KEY_SIMULATION_LAST_SYNCED_DATE, null)
    }

    /** Clears all user data on logout **/
    fun clearUserData() {
        prefs.edit {
            remove(KEY_USER_ID)
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_SELECTED_SUBJECT)
            remove(KEY_SESSION)
            remove(KEY_SIMULATION_LAST_SYNCED_DATE)
        }
    }
}
