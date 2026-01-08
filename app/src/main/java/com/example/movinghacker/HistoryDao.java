package com.example.movinghacker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {
    
    @Insert
    void insert(RequestHistory history);
    
    @Query("SELECT * FROM request_history ORDER BY timestamp DESC LIMIT 100")
    List<RequestHistory> getAllHistory();
    
    @Query("DELETE FROM request_history WHERE id = :id")
    void deleteById(long id);
    
    @Query("DELETE FROM request_history")
    void deleteAll();
    
    @Query("SELECT COUNT(*) FROM request_history")
    int getCount();
    
    @Query("DELETE FROM request_history WHERE id IN (SELECT id FROM request_history ORDER BY timestamp ASC LIMIT :count)")
    void deleteOldest(int count);
}
