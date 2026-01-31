package com.runshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runshare.app.data.CheckInEntity
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.data.RunDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 打卡界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { RunDatabase.getInstance(context) }
    val prefsRepository = remember { PreferencesRepository(context) }
    
    val currentUserId by prefsRepository.userId.collectAsState(initial = "")
    val currentUser by database.userDao().getUserFlow(currentUserId).collectAsState(initial = null)
    val checkIns by database.checkInDao().getCheckIns(currentUserId).collectAsState(initial = emptyList())
    
    var todayCheckedIn by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // 检查今天是否已打卡
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val today = getTodayStart()
            val existing = database.checkInDao().getCheckInForDate(currentUserId, today)
            todayCheckedIn = existing != null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日打卡") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 连续打卡天数
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "连续打卡",
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${currentUser?.checkInDays ?: 0}",
                        color = Color(0xFF1B5E20),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "天",
                        color = Color(0xFF2E7D32),
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 打卡按钮
            Button(
                onClick = {
                    if (!todayCheckedIn && currentUserId.isNotEmpty()) {
                        scope.launch {
                            val today = getTodayStart()
                            val checkIn = CheckInEntity(
                                userId = currentUserId,
                                date = today
                            )
                            database.checkInDao().insert(checkIn)
                            
                            // 更新连续打卡天数
                            val user = currentUser
                            if (user != null) {
                                val yesterday = today - 24 * 60 * 60 * 1000
                                val newDays = if (user.lastCheckIn >= yesterday) {
                                    user.checkInDays + 1
                                } else {
                                    1  // 重新开始计算
                                }
                                database.userDao().updateCheckIn(currentUserId, newDays, today)
                            }
                            
                            todayCheckedIn = true
                            showSuccessDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (todayCheckedIn) Color(0xFFBDBDBD) else Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFFBDBDBD)
                ),
                enabled = !todayCheckedIn
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (todayCheckedIn) Icons.Filled.Check else Icons.Filled.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (todayCheckedIn) "已打卡" else "打卡",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 本月打卡日历
            Text(
                text = "本月打卡记录",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CheckInCalendar(checkIns = checkIns)
        }
    }
    
    // 打卡成功弹窗
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Celebration,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("打卡成功！", textAlign = TextAlign.Center) },
            text = {
                Text(
                    text = "已连续打卡 ${currentUser?.checkInDays ?: 1} 天\n继续保持！",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    Text("太棒了")
                }
            }
        )
    }
}

@Composable
private fun CheckInCalendar(checkIns: List<CheckInEntity>) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    
    // 获取本月第一天和天数
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // 获取本月打卡的日期
    val checkedDays = checkIns
        .filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
        .map {
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
        }
        .toSet()
    
    Column {
        // 星期标题
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 日期网格
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val isChecked = day in checkedDays
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChecked) Color(0xFF4CAF50)
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    color = if (isChecked) Color.White else Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTodayStart(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
