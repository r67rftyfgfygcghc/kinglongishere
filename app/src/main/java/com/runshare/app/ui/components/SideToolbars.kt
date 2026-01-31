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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 右侧垂直工具栏
 */
@Composable
fun RightToolbar(
    onSearchClick: () -> Unit = {},
    onLayersClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToolbarButton(
            icon = Icons.Filled.Search,
            label = "搜索",
            onClick = onSearchClick
        )
        
        HorizontalDivider(
            modifier = Modifier
                .width(32.dp)
                .padding(vertical = 2.dp),
            color = Color(0xFFEEEEEE)
        )
        
        ToolbarButton(
            icon = Icons.Filled.Build,
            label = "工具",
            onClick = onToolsClick
        )
        
        ToolbarButton(
            icon = Icons.Filled.Layers,
            label = "图层",
            onClick = onLayersClick
        )
        
        ToolbarButton(
            icon = Icons.Filled.FilterList,
            label = "叠加",
            onClick = {}
        )
        
        ToolbarButton(
            icon = Icons.Filled.Group,
            label = "队伍",
            onClick = onTeamClick
        )
        
        HorizontalDivider(
            modifier = Modifier
                .width(32.dp)
                .padding(vertical = 2.dp),
            color = Color(0xFFEEEEEE)
        )
        
        // 比例尺显示
        Text(
            text = "⊥",
            color = Color(0xFF666666),
            fontSize = 12.sp
        )
        Text(
            text = "13.6",
            color = Color(0xFF333333),
            fontSize = 10.sp
        )
    }
}

/**
 * 左侧浮动按钮组
 */
@Composable
fun LeftFloatingButtons(
    onCenterClick: () -> Unit,
    onOrientationClick: () -> Unit = {},
    onModeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 居中按钮
        FloatingButton(
            icon = Icons.Filled.MyLocation,
            label = "居中",
            onClick = onCenterClick
        )
        
        // 横屏按钮
        FloatingButton(
            icon = Icons.Filled.ScreenRotation,
            label = "横屏",
            onClick = onOrientationClick
        )
        
        // 模式按钮
        FloatingButton(
            icon = Icons.Filled.DirectionsWalk,
            label = "两步路",
            onClick = onModeClick,
            showLabel = true
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = Color(0xFF666666),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun FloatingButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    showLabel: Boolean = false
) {
    Row(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = if (showLabel) 12.dp else 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = Color(0xFF666666),
                fontSize = 11.sp
            )
        }
    }
}
