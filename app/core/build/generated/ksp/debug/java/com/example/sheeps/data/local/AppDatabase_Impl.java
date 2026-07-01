package com.example.sheeps.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LocalDao _localDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_progress` (`levelId` INTEGER NOT NULL, `score` INTEGER NOT NULL, `clearTime` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, `updateTimestamp` INTEGER NOT NULL, PRIMARY KEY(`levelId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `backpack_items` (`itemType` TEXT NOT NULL, `count` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, `updateTimestamp` INTEGER NOT NULL, PRIMARY KEY(`itemType`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`userId` TEXT NOT NULL, `username` TEXT NOT NULL, `points` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, `updateTimestamp` INTEGER NOT NULL, PRIMARY KEY(`userId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1639665e180151f38c858ff28d677ffe')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `user_progress`");
        db.execSQL("DROP TABLE IF EXISTS `backpack_items`");
        db.execSQL("DROP TABLE IF EXISTS `user_profile`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUserProgress = new HashMap<String, TableInfo.Column>(5);
        _columnsUserProgress.put("levelId", new TableInfo.Column("levelId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProgress.put("score", new TableInfo.Column("score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProgress.put("clearTime", new TableInfo.Column("clearTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProgress.put("isDirty", new TableInfo.Column("isDirty", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProgress.put("updateTimestamp", new TableInfo.Column("updateTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserProgress = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserProgress = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserProgress = new TableInfo("user_progress", _columnsUserProgress, _foreignKeysUserProgress, _indicesUserProgress);
        final TableInfo _existingUserProgress = TableInfo.read(db, "user_progress");
        if (!_infoUserProgress.equals(_existingUserProgress)) {
          return new RoomOpenHelper.ValidationResult(false, "user_progress(com.example.sheeps.data.local.UserProgressEntity).\n"
                  + " Expected:\n" + _infoUserProgress + "\n"
                  + " Found:\n" + _existingUserProgress);
        }
        final HashMap<String, TableInfo.Column> _columnsBackpackItems = new HashMap<String, TableInfo.Column>(4);
        _columnsBackpackItems.put("itemType", new TableInfo.Column("itemType", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBackpackItems.put("count", new TableInfo.Column("count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBackpackItems.put("isDirty", new TableInfo.Column("isDirty", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBackpackItems.put("updateTimestamp", new TableInfo.Column("updateTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBackpackItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBackpackItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBackpackItems = new TableInfo("backpack_items", _columnsBackpackItems, _foreignKeysBackpackItems, _indicesBackpackItems);
        final TableInfo _existingBackpackItems = TableInfo.read(db, "backpack_items");
        if (!_infoBackpackItems.equals(_existingBackpackItems)) {
          return new RoomOpenHelper.ValidationResult(false, "backpack_items(com.example.sheeps.data.local.BackpackItemEntity).\n"
                  + " Expected:\n" + _infoBackpackItems + "\n"
                  + " Found:\n" + _existingBackpackItems);
        }
        final HashMap<String, TableInfo.Column> _columnsUserProfile = new HashMap<String, TableInfo.Column>(5);
        _columnsUserProfile.put("userId", new TableInfo.Column("userId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("points", new TableInfo.Column("points", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("isDirty", new TableInfo.Column("isDirty", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("updateTimestamp", new TableInfo.Column("updateTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserProfile = new TableInfo("user_profile", _columnsUserProfile, _foreignKeysUserProfile, _indicesUserProfile);
        final TableInfo _existingUserProfile = TableInfo.read(db, "user_profile");
        if (!_infoUserProfile.equals(_existingUserProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "user_profile(com.example.sheeps.data.local.UserProfileEntity).\n"
                  + " Expected:\n" + _infoUserProfile + "\n"
                  + " Found:\n" + _existingUserProfile);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "1639665e180151f38c858ff28d677ffe", "416d23f7c10760de9872607fb2f83185");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user_progress","backpack_items","user_profile");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `user_progress`");
      _db.execSQL("DELETE FROM `backpack_items`");
      _db.execSQL("DELETE FROM `user_profile`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LocalDao.class, LocalDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LocalDao localDao() {
    if (_localDao != null) {
      return _localDao;
    } else {
      synchronized(this) {
        if(_localDao == null) {
          _localDao = new LocalDao_Impl(this);
        }
        return _localDao;
      }
    }
  }
}
