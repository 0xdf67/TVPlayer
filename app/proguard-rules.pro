# ProGuard rules for TVPlayer
# Keep data classes used for serialization
-keep class com.example.tvplayer.data.** { *; }

# Keep application and UI classes referenced from AndroidManifest.xml
-keep class com.example.tvplayer.TvPlayerApplication { *; }
-keep class com.example.tvplayer.ui.** { *; }
