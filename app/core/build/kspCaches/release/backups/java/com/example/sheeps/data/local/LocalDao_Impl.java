package com.example.sheeps.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LocalDao_Impl implements LocalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserProgressEntity> __insertionAdapterOfUserProgressEntity;

  private final EntityInsertionAdapter<BackpackItemEntity> __insertionAdapterOfBackpackItemEntity;

  private final EntityInsertionAdapter<UserProfileEntity> __insertionAdapterOfUserProfileEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearProgressDirty;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllProgress;

  private final SharedSQLiteStatement __preparedStmtOfClearItemDirty;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllItems;

  private final SharedSQLiteStatement __preparedStmtOfClearProfileDirty;

  private final SharedSQLiteStatement __preparedStmtOfDeleteProfile;

  public LocalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserProgressEntity = new EntityInsertionAdapter<UserProgressEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_progress` (`levelId`,`score`,`clearTime`,`isDirty`,`updateTimestamp`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProgressEntity entity) {
        statement.bindLong(1, entity.getLevelId());
        statement.bindLong(2, entity.getScore());
        statement.bindLong(3, entity.getClearTime());
        final int _tmp = entity.isDirty() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getUpdateTimestamp());
      }
    };
    this.__insertionAdapterOfBackpackItemEntity = new EntityInsertionAdapter<BackpackItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `backpack_items` (`itemType`,`count`,`isDirty`,`updateTimestamp`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BackpackItemEntity entity) {
        statement.bindString(1, entity.getItemType());
        statement.bindLong(2, entity.getCount());
        final int _tmp = entity.isDirty() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindLong(4, entity.getUpdateTimestamp());
      }
    };
    this.__insertionAdapterOfUserProfileEntity = new EntityInsertionAdapter<UserProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_profile` (`userId`,`username`,`points`,`isDirty`,`updateTimestamp`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProfileEntity entity) {
        statement.bindString(1, entity.getUserId());
        statement.bindString(2, entity.getUsername());
        statement.bindLong(3, entity.getPoints());
        final int _tmp = entity.isDirty() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getUpdateTimestamp());
      }
    };
    this.__preparedStmtOfClearProgressDirty = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET isDirty = 0 WHERE levelId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllProgress = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM user_progress";
        return _query;
      }
    };
    this.__preparedStmtOfClearItemDirty = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE backpack_items SET isDirty = 0 WHERE itemType = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllItems = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM backpack_items";
        return _query;
      }
    };
    this.__preparedStmtOfClearProfileDirty = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_profile SET isDirty = 0 WHERE userId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteProfile = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM user_profile";
        return _query;
      }
    };
  }

  @Override
  public Object insertProgress(final UserProgressEntity progress,
      final Continuation<Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfUserProgressEntity.insertAndReturnId(progress);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertProgressList(final List<UserProgressEntity> progressList,
      final Continuation<List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfUserProgressEntity.insertAndReturnIdsList(progressList);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertItem(final BackpackItemEntity item, final Continuation<Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfBackpackItemEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertItemList(final List<BackpackItemEntity> items,
      final Continuation<List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfBackpackItemEntity.insertAndReturnIdsList(items);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertProfile(final UserProfileEntity profile,
      final Continuation<Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfUserProfileEntity.insertAndReturnId(profile);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearProgressDirty(final int levelId, final Continuation<Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearProgressDirty.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, levelId);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearProgressDirty.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllProgress(final Continuation<Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllProgress.acquire();
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllProgress.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearItemDirty(final String itemType, final Continuation<Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearItemDirty.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, itemType);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearItemDirty.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllItems(final Continuation<Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllItems.acquire();
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllItems.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearProfileDirty(final String userId, final Continuation<Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearProfileDirty.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, userId);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearProfileDirty.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteProfile(final Continuation<Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteProfile.acquire();
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteProfile.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<UserProgressEntity>> observeAllProgress() {
    final String _sql = "SELECT * FROM user_progress";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_progress"}, new Callable<List<UserProgressEntity>>() {
      @Override
      @NonNull
      public List<UserProgressEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLevelId = CursorUtil.getColumnIndexOrThrow(_cursor, "levelId");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfClearTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clearTime");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final List<UserProgressEntity> _result = new ArrayList<UserProgressEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserProgressEntity _item;
            final int _tmpLevelId;
            _tmpLevelId = _cursor.getInt(_cursorIndexOfLevelId);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final long _tmpClearTime;
            _tmpClearTime = _cursor.getLong(_cursorIndexOfClearTime);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _item = new UserProgressEntity(_tmpLevelId,_tmpScore,_tmpClearTime,_tmpIsDirty,_tmpUpdateTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllProgress(final Continuation<List<UserProgressEntity>> $completion) {
    final String _sql = "SELECT * FROM user_progress";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<UserProgressEntity>>() {
      @Override
      @NonNull
      public List<UserProgressEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLevelId = CursorUtil.getColumnIndexOrThrow(_cursor, "levelId");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfClearTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clearTime");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final List<UserProgressEntity> _result = new ArrayList<UserProgressEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserProgressEntity _item;
            final int _tmpLevelId;
            _tmpLevelId = _cursor.getInt(_cursorIndexOfLevelId);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final long _tmpClearTime;
            _tmpClearTime = _cursor.getLong(_cursorIndexOfClearTime);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _item = new UserProgressEntity(_tmpLevelId,_tmpScore,_tmpClearTime,_tmpIsDirty,_tmpUpdateTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDirtyProgress(final Continuation<List<UserProgressEntity>> $completion) {
    final String _sql = "SELECT * FROM user_progress WHERE isDirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<UserProgressEntity>>() {
      @Override
      @NonNull
      public List<UserProgressEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLevelId = CursorUtil.getColumnIndexOrThrow(_cursor, "levelId");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfClearTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clearTime");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final List<UserProgressEntity> _result = new ArrayList<UserProgressEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserProgressEntity _item;
            final int _tmpLevelId;
            _tmpLevelId = _cursor.getInt(_cursorIndexOfLevelId);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final long _tmpClearTime;
            _tmpClearTime = _cursor.getLong(_cursorIndexOfClearTime);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _item = new UserProgressEntity(_tmpLevelId,_tmpScore,_tmpClearTime,_tmpIsDirty,_tmpUpdateTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BackpackItemEntity>> observeAllItems() {
    final String _sql = "SELECT * FROM backpack_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"backpack_items"}, new Callable<List<BackpackItemEntity>>() {
      @Override
      @NonNull
      public List<BackpackItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfItemType = CursorUtil.getColumnIndexOrThrow(_cursor, "itemType");
          final int _cursorIndexOfCount = CursorUtil.getColumnIndexOrThrow(_cursor, "count");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final List<BackpackItemEntity> _result = new ArrayList<BackpackItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BackpackItemEntity _item;
            final String _tmpItemType;
            _tmpItemType = _cursor.getString(_cursorIndexOfItemType);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _item = new BackpackItemEntity(_tmpItemType,_tmpCount,_tmpIsDirty,_tmpUpdateTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllItems(final Continuation<List<BackpackItemEntity>> $completion) {
    final String _sql = "SELECT * FROM backpack_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BackpackItemEntity>>() {
      @Override
      @NonNull
      public List<BackpackItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfItemType = CursorUtil.getColumnIndexOrThrow(_cursor, "itemType");
          final int _cursorIndexOfCount = CursorUtil.getColumnIndexOrThrow(_cursor, "count");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final List<BackpackItemEntity> _result = new ArrayList<BackpackItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BackpackItemEntity _item;
            final String _tmpItemType;
            _tmpItemType = _cursor.getString(_cursorIndexOfItemType);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _item = new BackpackItemEntity(_tmpItemType,_tmpCount,_tmpIsDirty,_tmpUpdateTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDirtyItems(final Continuation<List<BackpackItemEntity>> $completion) {
    final String _sql = "SELECT * FROM backpack_items WHERE isDirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BackpackItemEntity>>() {
      @Override
      @NonNull
      public List<BackpackItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfItemType = CursorUtil.getColumnIndexOrThrow(_cursor, "itemType");
          final int _cursorIndexOfCount = CursorUtil.getColumnIndexOrThrow(_cursor, "count");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final List<BackpackItemEntity> _result = new ArrayList<BackpackItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BackpackItemEntity _item;
            final String _tmpItemType;
            _tmpItemType = _cursor.getString(_cursorIndexOfItemType);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _item = new BackpackItemEntity(_tmpItemType,_tmpCount,_tmpIsDirty,_tmpUpdateTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserProfileEntity> observeProfile() {
    final String _sql = "SELECT * FROM user_profile LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_profile"}, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _result = new UserProfileEntity(_tmpUserId,_tmpUsername,_tmpPoints,_tmpIsDirty,_tmpUpdateTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getProfile(final Continuation<UserProfileEntity> $completion) {
    final String _sql = "SELECT * FROM user_profile LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _result = new UserProfileEntity(_tmpUserId,_tmpUsername,_tmpPoints,_tmpIsDirty,_tmpUpdateTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDirtyProfiles(final Continuation<List<UserProfileEntity>> $completion) {
    final String _sql = "SELECT * FROM user_profile WHERE isDirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<UserProfileEntity>>() {
      @Override
      @NonNull
      public List<UserProfileEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfUpdateTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTimestamp");
          final List<UserProfileEntity> _result = new ArrayList<UserProfileEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserProfileEntity _item;
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            final boolean _tmpIsDirty;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp != 0;
            final long _tmpUpdateTimestamp;
            _tmpUpdateTimestamp = _cursor.getLong(_cursorIndexOfUpdateTimestamp);
            _item = new UserProfileEntity(_tmpUserId,_tmpUsername,_tmpPoints,_tmpIsDirty,_tmpUpdateTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
