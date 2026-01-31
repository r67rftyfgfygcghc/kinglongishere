package com.runshare.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 用户实体
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val avatarUrl: String = "",
    val totalDistance: Double = 0.0,  // 总距离（米）
    val totalRuns: Int = 0,           // 总跑步次数
    val totalDuration: Long = 0,      // 总时长（毫秒）
    val checkInDays: Int = 0,         // 连续打卡天数
    val lastCheckIn: Long = 0,        // 上次打卡时间
    val groupId: String? = null,      // 所属小组ID
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 打卡记录实体
 */
@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: Long,  // 打卡日期（当天0点时间戳）
    val runId: Long? = null,  // 关联的跑步记录
    val note: String = ""
)

/**
 * 小组实体
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val creatorId: String,
    val memberCount: Int = 0,
    val totalDistance: Double = 0.0,
    val inviteCode: String,  // 邀请码
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 用户DAO
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserFlow(id: String): Flow<UserEntity?>
    
    @Query("SELECT * FROM users ORDER BY totalDistance DESC LIMIT :limit")
    fun getLeaderboard(limit: Int = 50): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE groupId = :groupId ORDER BY totalDistance DESC")
    fun getGroupMembers(groupId: String): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
    
    @Update
    suspend fun update(user: UserEntity)
    
    @Query("UPDATE users SET totalDistance = totalDistance + :distance, totalRuns = totalRuns + 1, totalDuration = totalDuration + :duration WHERE id = :userId")
    suspend fun updateStats(userId: String, distance: Double, duration: Long)
    
    @Query("UPDATE users SET checkInDays = :days, lastCheckIn = :timestamp WHERE id = :userId")
    suspend fun updateCheckIn(userId: String, days: Int, timestamp: Long)
    
    @Query("UPDATE users SET groupId = :groupId WHERE id = :userId")
    suspend fun joinGroup(userId: String, groupId: String?)
}

/**
 * 打卡DAO
 */
@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins WHERE userId = :userId ORDER BY date DESC")
    fun getCheckIns(userId: String): Flow<List<CheckInEntity>>
    
    @Query("SELECT * FROM check_ins WHERE userId = :userId AND date = :date")
    suspend fun getCheckInForDate(userId: String, date: Long): CheckInEntity?
    
    @Query("SELECT COUNT(*) FROM check_ins WHERE userId = :userId")
    suspend fun getTotalCheckIns(userId: String): Int
    
    @Insert
    suspend fun insert(checkIn: CheckInEntity)
}

/**
 * 小组DAO
 */
@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroup(id: String): GroupEntity?
    
    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroupFlow(id: String): Flow<GroupEntity?>
    
    @Query("SELECT * FROM groups WHERE inviteCode = :code")
    suspend fun getGroupByCode(code: String): GroupEntity?
    
    @Query("SELECT * FROM groups ORDER BY totalDistance DESC LIMIT :limit")
    fun getGroupLeaderboard(limit: Int = 20): Flow<List<GroupEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)
    
    @Update
    suspend fun update(group: GroupEntity)
    
    @Query("UPDATE groups SET memberCount = memberCount + 1 WHERE id = :groupId")
    suspend fun incrementMemberCount(groupId: String)
    
    @Query("UPDATE groups SET memberCount = memberCount - 1 WHERE id = :groupId")
    suspend fun decrementMemberCount(groupId: String)
    
    @Query("UPDATE groups SET totalDistance = totalDistance + :distance WHERE id = :groupId")
    suspend fun addDistance(groupId: String, distance: Double)
}
