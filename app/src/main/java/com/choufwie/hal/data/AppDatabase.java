package com.choufwie.hal.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.choufwie.hal.data.Entities.CheckIn;
import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.data.Entities.Win;

/** Local-only database. Call it off the UI thread. */
@Database(entities = {User.class, Commitment.class, CheckIn.class, Win.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract Daos.UserDao users();
    public abstract Daos.CommitmentDao commitments();
    public abstract Daos.CheckInDao checkIns();
    public abstract Daos.WinDao wins();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context c) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null)
                    INSTANCE = Room.databaseBuilder(c.getApplicationContext(), AppDatabase.class, "hal.db")
                            .fallbackToDestructiveMigration().build();
            }
        }
        return INSTANCE;
    }
}
