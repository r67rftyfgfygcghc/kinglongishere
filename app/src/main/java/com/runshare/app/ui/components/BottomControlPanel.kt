package com.runshare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runshare.app.model.RunningState

/**
 * 底部控制面板 - 参考导航App设计
 * 深色背景，显示状态、统计数据、控制按钮
 */
@Composable
fun BottomControlPanel(
    runningState: RunningState,
    distanceKm: Double,
    durationFormatted: String,
    paceFormatted: String,
    onSettingsClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D2D2D),
                        Color(0xFF1A1A1A)
                    )
                )
            )
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        // 状态行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态文字
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (runningState) {
                        RunningState.IDLE -> "准备就绪"
                        RunningState.RUNNING -> "跑步中..."
                        RunningState.PAUSED -> "已暂停"
                        RunningState.STOPPED -> "已完成"
                    },
                    color = when (runningState) {
                        RunningState.RUNNING -> Color(0xFF4CAF50)
                        RunningState.PAUSED -> Color(0xFFFFC107)
                        else -> Color.White
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // 进度指示条
                if (runningState == RunningState.RUNNING) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Row {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(width = 16.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index == 0) Color(0xFF4CAF50)
                                        else Color(0xFF666666)
                                    )
                            )
                            if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
            
            // 滑动切换指示点
            Row {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 1) Color.White
                                else Color(0xFF666666)
                            )
                    )
                    if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 统计数据区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 配速
            StatItem(
                label = "配速",
                value = if (runningState != RunningState.IDLE) paceFormatted else "--'--\"",
                unit = "/km"
            )
            
            // 距离（中间大字）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%.2f", distanceKm),
                    color = Color(0xFFFF5722),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "距离(km)",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
            
            // 时长
            StatItem(
                label = "时长",
                value = if (runningState != RunningState.IDLE) durationFormatted else "00:00",
                unit = ""
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 底部按钮行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设置按钮
            BottomPanelButton(
                icon = Icons.Filled.Settings,
                label = "设置",
                onClick = onSettingsClick
            )
            
            // 中间主按钮
            when (runningState) {
                RunningState.IDLE -> {
                    // 开始按钮
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .clickable(onClick = onStartClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "开始",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                RunningState.RUNNING -> {
                    // 暂停和停止按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 暂停按钮
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107))
                                .clickable(onClick = onPauseClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Pause,
                                contentDescription = "暂停",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // 停止按钮
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF44336))
                                .clickable(onClick = onStopClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "结束",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                RunningState.PAUSED -> {
                    // 继续和停止按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 继续按钮
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .clickable(onClick = onResumeClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "继续",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // 停止按钮
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF44336))
                                .clickable(onClick = onStopClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "结束",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                RunningState.STOPPED -> {
                    // 显示完成状态
                    Text(
                        text = "跑步已完成",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
            
            // 分享/海拔按钮
            BottomPanelButton(
                icon = Icons.Filled.Share,
                label = "分享",
                onClick = onShareClick
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Text(
            text = label,
            color = Color(0xFFAAAAAA),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun BottomPanelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFFAAAAAA),
            fontSize = 11.sp
        )
    }
}
