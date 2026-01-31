package com.runshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.data.RunDatabase
import com.runshare.app.data.UserEntity
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 登录/注册界面
 */
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepository = remember { PreferencesRepository(context) }
    val database = remember { RunDatabase.getInstance(context) }

    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF283593),
                        Color(0xFF3949AB)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "跑步分享",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "记录每一步，分享每一刻",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 用户名输入
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    errorMessage = null
                },
                label = { Text("输入昵称", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            )

            // 错误信息
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 开始按钮
            Button(
                onClick = {
                    if (username.trim().isEmpty()) {
                        errorMessage = "请输入昵称"
                        return@Button
                    }
                    if (username.trim().length < 2) {
                        errorMessage = "昵称至少2个字符"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            // 生成或获取用户ID
                            val userId = prefsRepository.getOrCreateUserId()
                            
                            // 保存用户名
                            prefsRepository.setUsername(username.trim())
                            
                            // 创建本地用户记录
                            val user = UserEntity(
                                id = userId,
                                username = username.trim(),
                                createdAt = System.currentTimeMillis()
                            )
                            database.userDao().insert(user)
                            
                            // 标记已登录
                            prefsRepository.setLoggedIn(true)
                            
                            onLoginSuccess()
                        } catch (e: Exception) {
                            errorMessage = "注册失败，请重试"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(25.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF3949AB)
                    )
                } else {
                    Text(
                        text = "开始跑步",
                        color = Color(0xFF3949AB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "注册即表示同意服务条款",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
