# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.anurag.eduapp.data.firebase.** { *; }
-keep class com.anurag.eduapp.data.model.** { *; }

-keep,includedescriptorclasses class com.anurag.eduapp.**$$serializer { *; }
-keepclassmembers class com.anurag.eduapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.anurag.eduapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Required for Credential Manager to work in release
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# Keep your User model from being obfuscated
-keep class com.anurag.eduapp.data.firebase.model.User { *; }
-keepclassmembers class com.anurag.eduapp.data.firebase.model.User { *; }