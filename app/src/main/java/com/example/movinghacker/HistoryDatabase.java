package com.example.movinghacker;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {RequestHistory.class}, version = 1, exportSchema = false)
public abstract class HistoryDatabase extends RoomDatabase {
    
    private static volatile HistoryDatabase INSTANCE;
    
    public abstract HistoryDao historyDao();
    
    public static HistoryDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (HistoryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            HistoryDatabase.class,
                            "history_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
