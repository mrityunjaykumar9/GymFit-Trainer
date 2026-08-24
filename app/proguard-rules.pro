# Room
-keep class com.freegymtrainer.app.data.local.entity.** { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep line numbers for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
