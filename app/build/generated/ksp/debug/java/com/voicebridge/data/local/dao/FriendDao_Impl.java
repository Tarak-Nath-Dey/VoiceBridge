package com.voicebridge.data.local.dao;

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
import com.voicebridge.data.local.entity.FriendEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FriendDao_Impl implements FriendDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FriendEntity> __insertionAdapterOfFriendEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateOnlineStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFriendshipState;

  private final SharedSQLiteStatement __preparedStmtOfUpdateBlockedStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSharedKey;

  public FriendDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFriendEntity = new EntityInsertionAdapter<FriendEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `friends` (`deviceId`,`username`,`avatarIndex`,`publicKey`,`sharedKey`,`isFriend`,`isBlocked`,`isPendingRequest`,`isOutgoingRequest`,`lastSeen`,`isOnline`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FriendEntity entity) {
        statement.bindString(1, entity.getDeviceId());
        statement.bindString(2, entity.getUsername());
        statement.bindLong(3, entity.getAvatarIndex());
        statement.bindString(4, entity.getPublicKey());
        if (entity.getSharedKey() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSharedKey());
        }
        final int _tmp = entity.isFriend() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.isBlocked() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        final int _tmp_2 = entity.isPendingRequest() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        final int _tmp_3 = entity.isOutgoingRequest() ? 1 : 0;
        statement.bindLong(9, _tmp_3);
        statement.bindLong(10, entity.getLastSeen());
        final int _tmp_4 = entity.isOnline() ? 1 : 0;
        statement.bindLong(11, _tmp_4);
      }
    };
    this.__preparedStmtOfUpdateOnlineStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE friends SET isOnline = ?, lastSeen = ? WHERE deviceId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateFriendshipState = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE friends SET isFriend = ?, isPendingRequest = ?, isOutgoingRequest = ? WHERE deviceId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateBlockedStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE friends SET isBlocked = ? WHERE deviceId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSharedKey = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE friends SET sharedKey = ? WHERE deviceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertFriend(final FriendEntity friend,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFriendEntity.insert(friend);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateOnlineStatus(final String deviceId, final boolean isOnline,
      final long lastSeen, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateOnlineStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isOnline ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, lastSeen);
        _argIndex = 3;
        _stmt.bindString(_argIndex, deviceId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateOnlineStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFriendshipState(final String deviceId, final boolean isFriend,
      final boolean isPending, final boolean isOutgoing,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFriendshipState.acquire();
        int _argIndex = 1;
        final int _tmp = isFriend ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        final int _tmp_1 = isPending ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp_1);
        _argIndex = 3;
        final int _tmp_2 = isOutgoing ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp_2);
        _argIndex = 4;
        _stmt.bindString(_argIndex, deviceId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateFriendshipState.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBlockedStatus(final String deviceId, final boolean isBlocked,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateBlockedStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isBlocked ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, deviceId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateBlockedStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSharedKey(final String deviceId, final String sharedKey,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSharedKey.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sharedKey);
        _argIndex = 2;
        _stmt.bindString(_argIndex, deviceId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateSharedKey.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FriendEntity>> getFriendsFlow() {
    final String _sql = "SELECT * FROM friends WHERE isFriend = 1 AND isBlocked = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"friends"}, new Callable<List<FriendEntity>>() {
      @Override
      @NonNull
      public List<FriendEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAvatarIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarIndex");
          final int _cursorIndexOfPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKey");
          final int _cursorIndexOfSharedKey = CursorUtil.getColumnIndexOrThrow(_cursor, "sharedKey");
          final int _cursorIndexOfIsFriend = CursorUtil.getColumnIndexOrThrow(_cursor, "isFriend");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPendingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingRequest");
          final int _cursorIndexOfIsOutgoingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isOutgoingRequest");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final List<FriendEntity> _result = new ArrayList<FriendEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FriendEntity _item;
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final int _tmpAvatarIndex;
            _tmpAvatarIndex = _cursor.getInt(_cursorIndexOfAvatarIndex);
            final String _tmpPublicKey;
            _tmpPublicKey = _cursor.getString(_cursorIndexOfPublicKey);
            final String _tmpSharedKey;
            if (_cursor.isNull(_cursorIndexOfSharedKey)) {
              _tmpSharedKey = null;
            } else {
              _tmpSharedKey = _cursor.getString(_cursorIndexOfSharedKey);
            }
            final boolean _tmpIsFriend;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFriend);
            _tmpIsFriend = _tmp != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_1 != 0;
            final boolean _tmpIsPendingRequest;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPendingRequest);
            _tmpIsPendingRequest = _tmp_2 != 0;
            final boolean _tmpIsOutgoingRequest;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOutgoingRequest);
            _tmpIsOutgoingRequest = _tmp_3 != 0;
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsOnline;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_4 != 0;
            _item = new FriendEntity(_tmpDeviceId,_tmpUsername,_tmpAvatarIndex,_tmpPublicKey,_tmpSharedKey,_tmpIsFriend,_tmpIsBlocked,_tmpIsPendingRequest,_tmpIsOutgoingRequest,_tmpLastSeen,_tmpIsOnline);
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
  public Flow<List<FriendEntity>> getPendingRequestsFlow() {
    final String _sql = "SELECT * FROM friends WHERE isPendingRequest = 1 AND isBlocked = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"friends"}, new Callable<List<FriendEntity>>() {
      @Override
      @NonNull
      public List<FriendEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAvatarIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarIndex");
          final int _cursorIndexOfPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKey");
          final int _cursorIndexOfSharedKey = CursorUtil.getColumnIndexOrThrow(_cursor, "sharedKey");
          final int _cursorIndexOfIsFriend = CursorUtil.getColumnIndexOrThrow(_cursor, "isFriend");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPendingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingRequest");
          final int _cursorIndexOfIsOutgoingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isOutgoingRequest");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final List<FriendEntity> _result = new ArrayList<FriendEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FriendEntity _item;
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final int _tmpAvatarIndex;
            _tmpAvatarIndex = _cursor.getInt(_cursorIndexOfAvatarIndex);
            final String _tmpPublicKey;
            _tmpPublicKey = _cursor.getString(_cursorIndexOfPublicKey);
            final String _tmpSharedKey;
            if (_cursor.isNull(_cursorIndexOfSharedKey)) {
              _tmpSharedKey = null;
            } else {
              _tmpSharedKey = _cursor.getString(_cursorIndexOfSharedKey);
            }
            final boolean _tmpIsFriend;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFriend);
            _tmpIsFriend = _tmp != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_1 != 0;
            final boolean _tmpIsPendingRequest;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPendingRequest);
            _tmpIsPendingRequest = _tmp_2 != 0;
            final boolean _tmpIsOutgoingRequest;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOutgoingRequest);
            _tmpIsOutgoingRequest = _tmp_3 != 0;
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsOnline;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_4 != 0;
            _item = new FriendEntity(_tmpDeviceId,_tmpUsername,_tmpAvatarIndex,_tmpPublicKey,_tmpSharedKey,_tmpIsFriend,_tmpIsBlocked,_tmpIsPendingRequest,_tmpIsOutgoingRequest,_tmpLastSeen,_tmpIsOnline);
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
  public Flow<List<FriendEntity>> getAllDiscoveredFlow() {
    final String _sql = "SELECT * FROM friends";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"friends"}, new Callable<List<FriendEntity>>() {
      @Override
      @NonNull
      public List<FriendEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAvatarIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarIndex");
          final int _cursorIndexOfPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKey");
          final int _cursorIndexOfSharedKey = CursorUtil.getColumnIndexOrThrow(_cursor, "sharedKey");
          final int _cursorIndexOfIsFriend = CursorUtil.getColumnIndexOrThrow(_cursor, "isFriend");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPendingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingRequest");
          final int _cursorIndexOfIsOutgoingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isOutgoingRequest");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final List<FriendEntity> _result = new ArrayList<FriendEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FriendEntity _item;
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final int _tmpAvatarIndex;
            _tmpAvatarIndex = _cursor.getInt(_cursorIndexOfAvatarIndex);
            final String _tmpPublicKey;
            _tmpPublicKey = _cursor.getString(_cursorIndexOfPublicKey);
            final String _tmpSharedKey;
            if (_cursor.isNull(_cursorIndexOfSharedKey)) {
              _tmpSharedKey = null;
            } else {
              _tmpSharedKey = _cursor.getString(_cursorIndexOfSharedKey);
            }
            final boolean _tmpIsFriend;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFriend);
            _tmpIsFriend = _tmp != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_1 != 0;
            final boolean _tmpIsPendingRequest;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPendingRequest);
            _tmpIsPendingRequest = _tmp_2 != 0;
            final boolean _tmpIsOutgoingRequest;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOutgoingRequest);
            _tmpIsOutgoingRequest = _tmp_3 != 0;
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsOnline;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_4 != 0;
            _item = new FriendEntity(_tmpDeviceId,_tmpUsername,_tmpAvatarIndex,_tmpPublicKey,_tmpSharedKey,_tmpIsFriend,_tmpIsBlocked,_tmpIsPendingRequest,_tmpIsOutgoingRequest,_tmpLastSeen,_tmpIsOnline);
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
  public Object getFriendById(final String deviceId,
      final Continuation<? super FriendEntity> $completion) {
    final String _sql = "SELECT * FROM friends WHERE deviceId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<FriendEntity>() {
      @Override
      @Nullable
      public FriendEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAvatarIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarIndex");
          final int _cursorIndexOfPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKey");
          final int _cursorIndexOfSharedKey = CursorUtil.getColumnIndexOrThrow(_cursor, "sharedKey");
          final int _cursorIndexOfIsFriend = CursorUtil.getColumnIndexOrThrow(_cursor, "isFriend");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPendingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingRequest");
          final int _cursorIndexOfIsOutgoingRequest = CursorUtil.getColumnIndexOrThrow(_cursor, "isOutgoingRequest");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final FriendEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final int _tmpAvatarIndex;
            _tmpAvatarIndex = _cursor.getInt(_cursorIndexOfAvatarIndex);
            final String _tmpPublicKey;
            _tmpPublicKey = _cursor.getString(_cursorIndexOfPublicKey);
            final String _tmpSharedKey;
            if (_cursor.isNull(_cursorIndexOfSharedKey)) {
              _tmpSharedKey = null;
            } else {
              _tmpSharedKey = _cursor.getString(_cursorIndexOfSharedKey);
            }
            final boolean _tmpIsFriend;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFriend);
            _tmpIsFriend = _tmp != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_1 != 0;
            final boolean _tmpIsPendingRequest;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPendingRequest);
            _tmpIsPendingRequest = _tmp_2 != 0;
            final boolean _tmpIsOutgoingRequest;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOutgoingRequest);
            _tmpIsOutgoingRequest = _tmp_3 != 0;
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsOnline;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_4 != 0;
            _result = new FriendEntity(_tmpDeviceId,_tmpUsername,_tmpAvatarIndex,_tmpPublicKey,_tmpSharedKey,_tmpIsFriend,_tmpIsBlocked,_tmpIsPendingRequest,_tmpIsOutgoingRequest,_tmpLastSeen,_tmpIsOnline);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
