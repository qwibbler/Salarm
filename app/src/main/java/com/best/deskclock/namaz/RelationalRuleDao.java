package com.best.deskclock.namaz;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RelationalRuleDao {

    @Query("SELECT * FROM relational_rules")
    List<RelationalRule> getAll();

    @Query("SELECT * FROM relational_rules WHERE enabled = 1")
    List<RelationalRule> getEnabledRules();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RelationalRule rule);

    @Update
    void update(RelationalRule rule);

    @Delete
    void delete(RelationalRule rule);
}
