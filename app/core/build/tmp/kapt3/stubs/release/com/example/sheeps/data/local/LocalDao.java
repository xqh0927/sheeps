package com.example.sheeps.data.local;

import androidx.room.*;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\bg\u0018\u00002\u00020\u0001J\u0014\u0010\u0002\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u00040\u0003H\'J\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0016\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\fJ\"\u0010\r\u001a\b\u0012\u0004\u0012\u00020\n0\u00042\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u0016\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0013J\u000e\u0010\u0014\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0014\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00040\u0003H\'J\u0014\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00160\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00160\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0016\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\u0016H\u00a7@\u00a2\u0006\u0002\u0010\u001bJ\"\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\n0\u00042\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00160\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u0016\u0010\u001e\u001a\u00020\u00112\u0006\u0010\u001f\u001a\u00020 H\u00a7@\u00a2\u0006\u0002\u0010!J\u000e\u0010\"\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0010\u0010#\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010$0\u0003H\'J\u0010\u0010%\u001a\u0004\u0018\u00010$H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0014\u0010&\u001a\b\u0012\u0004\u0012\u00020$0\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0016\u0010\'\u001a\u00020\n2\u0006\u0010(\u001a\u00020$H\u00a7@\u00a2\u0006\u0002\u0010)J\u0016\u0010*\u001a\u00020\u00112\u0006\u0010+\u001a\u00020 H\u00a7@\u00a2\u0006\u0002\u0010!J\u000e\u0010,\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0007\u00a8\u0006-\u00c0\u0006\u0003"}, d2 = {"Lcom/example/sheeps/data/local/LocalDao;", "", "observeAllProgress", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/example/sheeps/data/local/UserProgressEntity;", "getAllProgress", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getDirtyProgress", "insertProgress", "", "progress", "(Lcom/example/sheeps/data/local/UserProgressEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertProgressList", "progressList", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearProgressDirty", "", "levelId", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteAllProgress", "observeAllItems", "Lcom/example/sheeps/data/local/BackpackItemEntity;", "getAllItems", "getDirtyItems", "insertItem", "item", "(Lcom/example/sheeps/data/local/BackpackItemEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertItemList", "items", "clearItemDirty", "itemType", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteAllItems", "observeProfile", "Lcom/example/sheeps/data/local/UserProfileEntity;", "getProfile", "getDirtyProfiles", "insertProfile", "profile", "(Lcom/example/sheeps/data/local/UserProfileEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearProfileDirty", "userId", "deleteProfile", "core_release"})
@androidx.room.Dao()
@kotlin.jvm.JvmSuppressWildcards()
public abstract interface LocalDao {
    
    @androidx.room.Query(value = "SELECT * FROM user_progress")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.sheeps.data.local.UserProgressEntity>> observeAllProgress();
    
    @androidx.room.Query(value = "SELECT * FROM user_progress")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllProgress(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.util.List<com.example.sheeps.data.local.UserProgressEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM user_progress WHERE isDirty = 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDirtyProgress(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.util.List<com.example.sheeps.data.local.UserProgressEntity>> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertProgress(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.UserProgressEntity progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Long> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertProgressList(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.local.UserProgressEntity> progressList, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.util.List<java.lang.Long>> $completion);
    
    @androidx.room.Query(value = "UPDATE user_progress SET isDirty = 0 WHERE levelId = :levelId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearProgressDirty(int levelId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "DELETE FROM user_progress")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAllProgress(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM backpack_items")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.sheeps.data.local.BackpackItemEntity>> observeAllItems();
    
    @androidx.room.Query(value = "SELECT * FROM backpack_items")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllItems(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.util.List<com.example.sheeps.data.local.BackpackItemEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM backpack_items WHERE isDirty = 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDirtyItems(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.util.List<com.example.sheeps.data.local.BackpackItemEntity>> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertItem(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.BackpackItemEntity item, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Long> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertItemList(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.local.BackpackItemEntity> items, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.util.List<java.lang.Long>> $completion);
    
    @androidx.room.Query(value = "UPDATE backpack_items SET isDirty = 0 WHERE itemType = :itemType")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearItemDirty(@org.jetbrains.annotations.NotNull()
    java.lang.String itemType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "DELETE FROM backpack_items")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAllItems(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM user_profile LIMIT 1")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.example.sheeps.data.local.UserProfileEntity> observeProfile();
    
    @androidx.room.Query(value = "SELECT * FROM user_profile LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getProfile(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<com.example.sheeps.data.local.UserProfileEntity> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM user_profile WHERE isDirty = 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDirtyProfiles(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.util.List<com.example.sheeps.data.local.UserProfileEntity>> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertProfile(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.UserProfileEntity profile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Long> $completion);
    
    @androidx.room.Query(value = "UPDATE user_profile SET isDirty = 0 WHERE userId = :userId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearProfileDirty(@org.jetbrains.annotations.NotNull()
    java.lang.String userId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "DELETE FROM user_profile")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteProfile(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<java.lang.Integer> $completion);
}