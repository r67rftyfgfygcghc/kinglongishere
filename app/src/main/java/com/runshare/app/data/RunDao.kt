package com.runshare.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 跑步记录数据访问对象
 */
@Dao
interface RunDao {

    /**
     * 插入新记录
     */
    @Insert
    suspend fun insert(run: RunEntity): Long

    /**
     * 更新记录
     */
    @Update
    suspend fun update(run: RunEntity)

    /**
     * 删除记录
     */
    @Delete
    suspend fun delete(run: RunEntity)

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM run_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取所有记录（按开始时间倒序）
     */
    @Query("SELECT * FROM run_records ORDER BY startTime DESC")
    fun getAllRuns(): Flow<List<RunEntity>>

    /**
     * 根据ID获取记录
     */
    @Query("SELECT * FROM run_records WHERE id = :id")
    suspend fun getRunById(id: Long): RunEntity?

    /**
     * 获取总跑步次数
     */
    @Query("SELECT COUNT(*) FROM run_records")
    fun getTotalRunCount(): Flow<Int>

    /**
     * 获取总距离（米）
     */
    @Query("SELECT COALESCE(SUM(distanceMeters), 0) FROM run_records")
    fun getTotalDistance(): Flow<Double>

    /**
     * 获取总时长（毫秒）
     */
    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM run_records")
    fun getTotalDuration(): Flow<Long>
}
