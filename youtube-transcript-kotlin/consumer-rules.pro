# Consumer ProGuard rules for YouTube Transcript Kotlin library
# These rules will be applied to the consuming app

# Keep the main API classes
-keep class com.youtubetranscript.YouTubeTranscriptApi { *; }
-keep class com.youtubetranscript.models.** { *; }
-keep class com.youtubetranscript.core.** { *; }

# Keep exception classes
-keep class com.youtubetranscript.*Exception { *; }

# Keep OkHttp classes (if not already included in the app)
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *; }

# Keep JSON classes (if not already included in the app)
-dontwarn org.json.**
-keep class org.json.** { *; }
