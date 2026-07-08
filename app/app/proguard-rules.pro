# 1. Basics
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 2. Android & Kotlin Basics
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

# 3. SmartRefreshLayout
-dontwarn com.google.android.material.**
-dontwarn com.scwang.smart.refresh.**
-keep class com.scwang.smart.refresh.layout.** { *; }
-keep interface com.scwang.smart.refresh.layout.** { *; }

# 4. Google Tink & Crypto
-dontwarn com.google.api.client.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.joda.time.**
-keep class com.google.crypto.tink.** { *; }

# 5. WorkManager & Room
-keep class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>(...);
}
-dontwarn androidx.work.impl.**

# 6. TheRouter (Crucial)
-keep class cn.therouter.** { *; }
-keep interface cn.therouter.** { *; }
-keep @cn.therouter.router.annotation.Route class * {*;}
-keep @cn.therouter.router.annotation.FlowTask class * {*;}
-keep @cn.therouter.router.annotation.ServiceProvider class * {*;}
-keepclassmembers class * {
    @cn.therouter.router.annotation.RouteField <fields>;
}

# 7. Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep @dagger.hilt.EntryPoint class * { *; }

# 8. Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepnames class com.squareup.okhttp3.** { *; }
-keepnames interface com.squareup.okhttp3.** { *; }
-dontwarn com.squareup.okhttp3.**
-dontwarn okio.**

# 9. Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.example.sheeps.model.** { *; } # Keep your data models if using Gson

# 10. MMKV
-keep class com.tencent.mmkv.** { *; }

# 11. UtilCode
-keep class com.blankj.utilcode.** { *; }
-keepclassmembers class com.blankj.utilcode.** { *; }

# 12. BRVAH
-keep class com.chad.library.adapter.** { *; }

# 13. Lottie
-keep class com.airbnb.lottie.** { *; }

# 14. Coil
-keep class coil.** { *; }

# 15. XXPermissions & Toaster
-keep class com.hjq.permissions.** { *; }
-keep class com.hjq.toast.** { *; }

# 16. Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class **$serializer {
    public static final **$serializer INSTANCE;
}
# 保持游戏状态及 ViewModel 数据结构不被混淆
-keep class com.example.sheeps.game.state.** { *; }
