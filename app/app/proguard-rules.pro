# 1. 放过 SmartRefreshLayout 里的 Material 引用
-dontwarn com.google.android.material.**
-dontwarn com.scwang.smart.refresh.**

# 2. 放过 Google Tink 加密库里的可选云端依赖
-dontwarn com.google.api.client.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.joda.time.**

# 3. WorkManager & Room Database
-keep class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>(...);
}
-dontwarn androidx.work.impl.**