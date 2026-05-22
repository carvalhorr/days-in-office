# Days in Office — R8 / ProGuard keep rules
#
# Default rules from `proguard-android-optimize.txt` are included via the
# `proguardFiles(...)` call in app/build.gradle.kts. Everything below is the
# project-specific surface that those defaults don't cover.

# ─────────────────────────────────────────────────────────────────────────
# Kotlin metadata — preserve runtime reflection metadata used by Hilt, Room,
# kotlinx.serialization, and Compose.
# ─────────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod,
                Exceptions, RuntimeVisibleAnnotations,
                RuntimeVisibleParameterAnnotations,
                RuntimeVisibleTypeAnnotations
-keep class kotlin.Metadata { *; }

# ─────────────────────────────────────────────────────────────────────────
# Hilt / Dagger
# ─────────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.codegen.OriginatingElement class * { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
}
# Generated Hilt code lives in this package — pull none of it out.
-keep class hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class dagger.hilt.internal.processedrootsentinel.codegen.** { *; }

# ─────────────────────────────────────────────────────────────────────────
# Room — entities, DAOs, generated implementations
# ─────────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers @androidx.room.Dao class * { *; }
# Room's generated _Impl classes
-keep class **_Impl { *; }
-keepclassmembers class **_Impl { *; }

# Type converters used by Room — keep all methods on classes with the
# @TypeConverter annotation
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# ─────────────────────────────────────────────────────────────────────────
# kotlinx.serialization
# ─────────────────────────────────────────────────────────────────────────
# Keep all @Serializable classes and their companions (companions hold the
# generated serializer references).
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * {
    static **$Companion Companion;
    static **$$serializer INSTANCE;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.carvalhorr.daysInOffice.**$$serializer { *; }
-keepclassmembers class com.carvalhorr.daysInOffice.** {
    *** Companion;
}
-keepclasseswithmembers class com.carvalhorr.daysInOffice.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─────────────────────────────────────────────────────────────────────────
# WorkManager workers — must be reachable by name from CoroutineWorker /
# ListenableWorker reflection. Hilt's WorkManagerFactory uses the FQCN.
# ─────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep public class * extends androidx.work.Worker
-keep public class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─────────────────────────────────────────────────────────────────────────
# Android entry-point components declared via the manifest — keep them
# reachable by their FQCN so the system can instantiate them.
# ─────────────────────────────────────────────────────────────────────────
-keep class com.carvalhorr.daysInOffice.app.DaysInOfficeApp { *; }
-keep class com.carvalhorr.daysInOffice.app.MainActivity { *; }
-keep class com.carvalhorr.daysInOffice.widget.** { *; }
-keep class com.carvalhorr.daysInOffice.notification.** { *; }
-keep class com.carvalhorr.daysInOffice.core.detection.receiver.** { *; }
-keep class com.carvalhorr.daysInOffice.core.detection.worker.** { *; }

# AppWidgetProvider + Glance widget receivers
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ─────────────────────────────────────────────────────────────────────────
# Compose runtime — usually shouldn't need extra rules, but the @Composable
# functions plus state holders deserve a belt-and-braces keep for stable
# lambda classes that Compose tooling references reflectively.
# ─────────────────────────────────────────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ─────────────────────────────────────────────────────────────────────────
# Glance app widget (uses kotlinx-serialization-protobuf and proto-lite)
# ─────────────────────────────────────────────────────────────────────────
-keep class androidx.glance.appwidget.proto.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-dontwarn androidx.glance.appwidget.**

# ─────────────────────────────────────────────────────────────────────────
# Play services Location — common keep rules
# ─────────────────────────────────────────────────────────────────────────
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ─────────────────────────────────────────────────────────────────────────
# Coroutines — DebugProbes and friends
# ─────────────────────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.debug.**

# ─────────────────────────────────────────────────────────────────────────
# Reflection-only attributes
# ─────────────────────────────────────────────────────────────────────────
-keepattributes SourceFile, LineNumberTable
