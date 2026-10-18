package com.choufwie.hal.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.choufwie.hal.data.Entities.CheckIn;
import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.data.Entities.Win;

import java.util.List;

public final class Daos {
    private Daos() {}

    @Dao
    public interface UserDao {
        @Query("SELECT * FROM users WHERE username = :u LIMIT 1") User byUsername(String u);
        @Query("SELECT * FROM users WHERE id = :id LIMIT 1") User byId(long id);
        @Insert long insert(User u);
        @Update void update(User u);
    }

    @Dao
    public interface CommitmentDao {
        @Query("SELECT * FROM commitments WHERE userId = :uid ORDER BY dueDate ASC") List<Commitment> all(long uid);
        @Insert long insert(Commitment c);
        @Update void update(Commitment c);
        @Delete void delete(Commitment c);
        @Query("DELETE FROM commitments WHERE userId = :uid") void clear(long uid);
        @Query("UPDATE commitments SET focusMinutes = focusMinutes + :min WHERE id = :id") void addFocus(long id, int min);
    }

    @Dao
    public interface CheckInDao {
        /** Oldest first: the last element is the latest check-in. */
        @Query("SELECT * FROM check_ins WHERE userId = :uid AND timestamp >= :from ORDER BY timestamp ASC") List<CheckIn> since(long uid, long from);
        @Insert long insert(CheckIn c);
        @Query("DELETE FROM check_ins WHERE userId = :uid") void clear(long uid);
    }

    @Dao
    public interface WinDao {
        @Query("SELECT * FROM wins WHERE userId = :uid ORDER BY id DESC") List<Win> all(long uid);
        @Insert long insert(Win w);
    }
}
