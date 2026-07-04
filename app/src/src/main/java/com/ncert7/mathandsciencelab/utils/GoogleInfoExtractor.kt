package com.ncert7.mathandsciencelab.utils

import com.ncert7.mathandsciencelab.data.firebase.model.User
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ncert7.mathandsciencelab.config.AppConfig


class GoogleInfoExtractor {

    companion object {

        fun extractUserInfo(googleIdTokenCredential: GoogleIdTokenCredential): User {
            return User(
                id = googleIdTokenCredential.id,
                email = googleIdTokenCredential.id, // id is mostly email address
                displayName = googleIdTokenCredential.displayName,
                profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString(),
                schoolName = "",
                phoneNumber = "",
                studentClass = 7, // default value
                appName = AppConfig.APP_NAME // set app identifier
            )
        }

        fun extractAndLogUserInfo(googleIdTokenCredential: GoogleIdTokenCredential): User {
            val userInfo = extractUserInfo(googleIdTokenCredential)

            DebugLogger.debugLog("GoogleUserInfo", "User ID: ${userInfo.id}")
            DebugLogger.debugLog("GoogleUserInfo", "Email: ${userInfo.email}")
            DebugLogger.debugLog("GoogleUserInfo", "Display Name: ${userInfo.displayName}")
            DebugLogger.debugLog("GoogleUserInfo", "Profile Picture: ${userInfo.profilePictureUri}")

            return userInfo
        }
    }
}