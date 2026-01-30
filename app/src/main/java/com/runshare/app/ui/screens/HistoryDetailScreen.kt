package com.runshare.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.data.RunDatabase
import com.runshare.app.data.RunEntity
import com.runshare.app.model.MapProvider
import com.runshare.app.ui.components.MapViewComposable
import com.runshare.app.ui.components.RunStatsCard
import com.runshare.app.utils.ShareUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 历史记录详情界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    runId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { RunDatabase.getInstance(context) }
    val prefsRepository = remember { PreferencesRepository(context) }
    val mapProvider by prefsRepository.mapProvider.collectAsState(initial = MapProvider.OSM)

    var run by remember { mutableStateOf<RunEntity?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(runId) {
        run = database.runDao().getRunById(runId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("跑步详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    run?.let { r ->
                        IconButton(onClick = { ShareUtils.shareRunAsText(context, r) }) {
                            Icon(Icons.Filled.Share, contentDescription = "分享")
                        }
                        IconButton(onClick = {
                            val shareData = ShareUtils.ShareData(
                                type = "history",
                                sessionId = ShareUtils.generateSessionId(),
                                runId = r.id,
                                points = r.getRoutePoints(),
                                distance = r.distanceMeters,
                                duration = r.durationMs
                            )
                            val link = ShareUtils.generateShareLink(shareData)
                            qrCodeBitmap = ShareUtils.generateQRCode(link)
                            showShareDialog = true
                        }) {
                            Icon(Icons.Filled.QrCode, contentDescription = "二维码")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        run?.let { r ->
            val routePoints = remember(r) { r.getRoutePoints() }
            val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 地图显示轨迹
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    MapViewComposable(
                        modifier = Modifier.fillMaxSize(),
                        mapProvider = mapProvider,
                        routePoints = routePoints,
                        showCurrentMarker = false,
                        enableTouch = true
                    )
                }

                // 统计数据
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(r.startTime)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RunStatsCard(
                        distance = String.format("%.2f", r.getDistanceKm()),
                        duration = r.getFormattedDuration(),
                        pace = r.getFormattedPace()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 详细数据
                    Text(
                        text = "详细数据",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("开始时间", dateFormat.format(Date(r.startTime)))
                    DetailRow("结束时间", dateFormat.format(Date(r.endTime)))
                    DetailRow("轨迹点数", "${routePoints.size} 个")

                    Spacer(modifier = Modifier.height(24.dp))

                    // 导出GPX按钮
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val file = ShareUtils.exportToGpx(context, r)
                                // TODO: 可以添加文件分享逻辑
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导出GPX文件")
                    }
                }
            }
        } ?: run {
            // 加载中
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // 二维码分享对话框
        if (showShareDialog && qrCodeBitmap != null) {
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("扫描二维码查看") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = qrCodeBitmap!!.asImageBitmap(),
                            contentDescription = "分享二维码",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "扫描此二维码查看跑步轨迹",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
