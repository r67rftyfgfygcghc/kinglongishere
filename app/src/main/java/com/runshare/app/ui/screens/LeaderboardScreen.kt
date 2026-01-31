package com.runshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.data.RunDatabase
import com.runshare.app.data.UserEntity
import kotlinx.coroutines.launch

/**
 * 排行榜界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RunDatabase.getInstance(context) }
    val prefsRepository = remember { PreferencesRepository(context) }

    val leaderboard by database.userDao().getLeaderboard(50).collectAsState(initial = emptyList())
    val currentUserId by prefsRepository.userId.collectAsState(initial = "")

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("总距离", "总次数", "连续打卡")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("排行榜") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab切换
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF283593),
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 排行榜列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortedList = when (selectedTab) {
                    0 -> leaderboard.sortedByDescending { it.totalDistance }
                    1 -> leaderboard.sortedByDescending { it.totalRuns }
                    else -> leaderboard.sortedByDescending { it.checkInDays }
                }

                itemsIndexed(sortedList) { index, user ->
                    LeaderboardItem(
                        rank = index + 1,
                        user = user,
                        valueText = when (selectedTab) {
                            0 -> String.format("%.1f km", user.totalDistance / 1000.0)
                            1 -> "${user.totalRuns} 次"
                            else -> "${user.checkInDays} 天"
                        },
                        isCurrentUser = user.id == currentUserId
                    )
                }

                if (sortedList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无排行数据\n快去跑步吧！",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardItem(
    rank: Int,
    user: UserEntity,
    valueText: String,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFFE8EAF6) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> Color(0xFFE0E0E0)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color.White else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 用户信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "我",
                            color = Color(0xFF1A237E),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFC5CAE9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "已跑 ${user.totalRuns} 次",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // 数值
            Text(
                text = valueText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1A237E)
            )
        }
    }
}
