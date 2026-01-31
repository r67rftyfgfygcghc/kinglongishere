package com.runshare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runshare.app.model.LocationPoint

/**
 * 顶部GPS信息栏
 * 显示坐标、定位状态、卫星信号
 */
@Composable
fun TopInfoBar(
    currentLocation: LocationPoint?,
    isLocating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()  // 添加状态栏padding
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：坐标信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GCJ02 标签
            Text(
                text = "GCJ02",
                color = Color(0xFF4CAF50),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x504CAF50))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 坐标显示
            if (currentLocation != null) {
                Text(
                    text = String.format(
                        "%.6f°N,%.6f°E",
                        currentLocation.latitude,
                        currentLocation.longitude
                    ),
                    color = Color.White,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 海拔
                Text(
                    text = "海拔${currentLocation.altitude.toInt()}m",
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "等待定位...",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
        }
        
        // 右侧：定位状态
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 定位状态指示点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (currentLocation != null) Color(0xFF4CAF50)
                        else if (isLocating) Color(0xFFFFC107)
                        else Color(0xFFFF5722)
                    )
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = when {
                    currentLocation != null -> "已定位"
                    isLocating -> "定位中..."
                    else -> "未定位"
                },
                color = Color.White,
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 卫星图标
            Text(
                text = "卫星",
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp
            )
        }
    }
}
