package com.ncert7.mathandsciencelab.debug

import android.util.Log
import com.ncert7.mathandsciencelab.BuildConfig


object DebugLogger {
    fun debugLog(tag: String, message: String){
        if (BuildConfig.DEBUG){
            Log.d(tag, message)
        }
    }

    fun errorLog(tag: String, message: String){
        if (BuildConfig.DEBUG){
            Log.e(tag, message)
        }
    }
}