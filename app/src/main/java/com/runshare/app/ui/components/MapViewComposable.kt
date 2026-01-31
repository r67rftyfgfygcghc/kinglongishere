package com.runshare.app.ui.components

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.runshare.app.model.LocationPoint
import com.runshare.app.model.MapProvider
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * 多地图提供商切换的地图组件
 */
@Composable
fun MapViewComposable(
    modifier: Modifier = Modifier,
    mapProvider: MapProvider = MapProvider.OSM,
    currentLocation: LocationPoint? = null,
    routePoints: List<LocationPoint> = emptyList(),
    showCurrentMarker: Boolean = true,
    enableTouch: Boolean = true,
    onMapReady: (centerCallback: () -> Unit) -> Unit = {}
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            createMapView(ctx, mapProvider).also { mapView ->
                mapViewRef = mapView
                // 提供居中回调函数
                onMapReady {
                    mapViewRef?.let { mv ->
                        currentLocation?.let { loc ->
                            mv.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                        }
                    }
                }
            }
        },
        update = { mapView ->
            // 更新地图源
            updateTileSource(mapView, mapProvider)

            // 清除旧的覆盖物（保留缩放控件等）
            mapView.overlays.removeAll { it is Marker || it is Polyline }

            // 绘制轨迹（紫色/粉色如参考设计）
            if (routePoints.isNotEmpty()) {
                val polyline = Polyline().apply {
                    outlinePaint.color = Color.parseColor("#E040FB") // 紫色
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.isAntiAlias = true
                    setPoints(routePoints.map { GeoPoint(it.latitude, it.longitude) })
                }
                mapView.overlays.add(polyline)
                
                // 添加起点标记
                if (routePoints.size > 1) {
                    val startMarker = Marker(mapView).apply {
                        position = GeoPoint(routePoints.first().latitude, routePoints.first().longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "起点"
                        icon = context.getDrawable(android.R.drawable.presence_online)
                    }
                    mapView.overlays.add(startMarker)
                }
            }

            // 添加当前位置标记
            if (showCurrentMarker && currentLocation != null) {
                val marker = Marker(mapView).apply {
                    position = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "当前位置"
                }
                mapView.overlays.add(marker)

                // 移动到当前位置
                mapView.controller.animateTo(
                    GeoPoint(currentLocation.latitude, currentLocation.longitude)
                )
            }

            // 控制触摸事件
            mapView.setOnTouchListener { _, event ->
                if (!enableTouch && event.action == MotionEvent.ACTION_MOVE) {
                    true
                } else {
                    false
                }
            }

            mapView.invalidate()
        }
    )
}

private fun createMapView(context: Context, mapProvider: MapProvider): MapView {
    return MapView(context).apply {
        setMultiTouchControls(true)
        controller.setZoom(16.0)

        // 设置初始位置（北京天安门作为默认）
        controller.setCenter(GeoPoint(39.9042, 116.4074))

        // 设置地图源
        updateTileSource(this, mapProvider)
    }
}

private fun updateTileSource(mapView: MapView, mapProvider: MapProvider) {
    mapView.setTileSource(
        when (mapProvider) {
            MapProvider.OSM -> TileSourceFactory.MAPNIK

            MapProvider.AMAP -> object : XYTileSource(
                "高德地图",
                0, 19, 256, ".png",
                arrayOf(
                    "https://webrd01.is.autonavi.com/appmaptile?",
                    "https://webrd02.is.autonavi.com/appmaptile?",
                    "https://webrd03.is.autonavi.com/appmaptile?",
                    "https://webrd04.is.autonavi.com/appmaptile?"
                )
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                    val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                    val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                    return baseUrl + "lang=zh_cn&size=1&scale=1&style=8&x=$x&y=$y&z=$zoom"
                }
            }

            MapProvider.BAIDU -> object : XYTileSource(
                "百度地图",
                0, 19, 256, ".png",
                arrayOf("https://online3.map.bdimg.com/tile/")
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                    val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                    val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                    // 百度地图需要特殊的坐标转换
                    val baiduY = (1 shl zoom) - 1 - y
                    return baseUrl + "?qt=vtile&x=$x&y=$baiduY&z=$zoom&styles=pl&udt=20230712&scaler=1"
                }
            }

            MapProvider.TENCENT -> object : XYTileSource(
                "腾讯地图",
                0, 19, 256, ".png",
                arrayOf(
                    "https://rt0.map.gtimg.com/tile?",
                    "https://rt1.map.gtimg.com/tile?",
                    "https://rt2.map.gtimg.com/tile?",
                    "https://rt3.map.gtimg.com/tile?"
                )
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                    val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                    val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                    // 腾讯地图需要Y轴翻转
                    val tencentY = (1 shl zoom) - 1 - y
                    return baseUrl + "z=$zoom&x=$x&y=$tencentY&styleid=1&version=277"
                }
            }
        }
    )
}

/**
 * 将地图缩放到显示所有轨迹点
 */
fun MapView.zoomToFitPoints(points: List<LocationPoint>, padding: Double = 50.0) {
    if (points.isEmpty()) return

    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }

    val boundingBox = org.osmdroid.util.BoundingBox(
        maxLat + padding / 111000.0,
        maxLon + padding / 111000.0,
        minLat - padding / 111000.0,
        minLon - padding / 111000.0
    )

    post {
        zoomToBoundingBox(boundingBox, true)
    }
}
