package com.runshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runshare.app.data.GroupEntity
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.data.RunDatabase
import com.runshare.app.data.UserEntity
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 小组界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { RunDatabase.getInstance(context) }
    val prefsRepository = remember { PreferencesRepository(context) }
    val clipboardManager = LocalClipboardManager.current

    val currentUserId by prefsRepository.userId.collectAsState(initial = "")
    val currentUser by database.userDao().getUserFlow(currentUserId).collectAsState(initial = null)
    
    // 获取当前用户所在小组
    val currentGroup by remember(currentUser?.groupId) {
        if (currentUser?.groupId != null) {
            database.groupDao().getGroupFlow(currentUser!!.groupId!!)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)
    
    val groupMembers by remember(currentUser?.groupId) {
        if (currentUser?.groupId != null) {
            database.userDao().getGroupMembers(currentUser!!.groupId!!)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的小组") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF673AB7),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            snackbarMessage?.let { message ->
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (currentGroup == null) {
                // 没有加入小组
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Group,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFFBDBDBD)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "还没有加入小组",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("创建小组")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("加入小组")
                    }
                }
            } else {
                // 已加入小组
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentGroup!!.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "${currentGroup!!.memberCount} 名成员",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                            
                            // 邀请码
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentGroup!!.inviteCode))
                                    snackbarMessage = "邀请码已复制"
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentGroup!!.inviteCode)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 小组总距离
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.1f", currentGroup!!.totalDistance / 1000.0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = Color(0xFF673AB7)
                                )
                                Text("总距离(km)", color = Color.Gray, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${currentGroup!!.memberCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = Color(0xFF673AB7)
                                )
                                Text("成员数", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "成员排行",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 成员列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groupMembers.sortedByDescending { it.totalDistance }) { member ->
                        val rank = groupMembers.sortedByDescending { it.totalDistance }.indexOf(member) + 1
                        GroupMemberItem(
                            rank = rank,
                            member = member,
                            isCurrentUser = member.id == currentUserId
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 退出小组按钮
                TextButton(
                    onClick = {
                        scope.launch {
                            currentUser?.groupId?.let { groupId ->
                                database.groupDao().decrementMemberCount(groupId)
                            }
                            database.userDao().joinGroup(currentUserId, null)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("退出小组", color = Color.Red)
                }
            }
        }
    }

    // 创建小组弹窗
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                scope.launch {
                    val groupId = UUID.randomUUID().toString().take(8)
                    val inviteCode = generateInviteCode()
                    val group = GroupEntity(
                        id = groupId,
                        name = name,
                        description = description,
                        creatorId = currentUserId,
                        memberCount = 1,
                        inviteCode = inviteCode
                    )
                    database.groupDao().insert(group)
                    database.userDao().joinGroup(currentUserId, groupId)
                    showCreateDialog = false
                    snackbarMessage = "小组创建成功！邀请码: $inviteCode"
                }
            }
        )
    }

    // 加入小组弹窗
    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { inviteCode ->
                scope.launch {
                    val group = database.groupDao().getGroupByCode(inviteCode)
                    if (group != null) {
                        database.groupDao().incrementMemberCount(group.id)
                        database.userDao().joinGroup(currentUserId, group.id)
                        showJoinDialog = false
                        snackbarMessage = "成功加入 ${group.name}"
                    } else {
                        snackbarMessage = "邀请码无效"
                    }
                }
            }
        )
    }
}

@Composable
private fun GroupMemberItem(
    rank: Int,
    member: UserEntity,
    isCurrentUser: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFFEDE7F6) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Text(
                text = "#$rank",
                fontWeight = FontWeight.Bold,
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Gray
                },
                modifier = Modifier.width(40.dp)
            )
            
            // 用户名
            Text(
                text = member.username,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            
            // 距离
            Text(
                text = String.format("%.1f km", member.totalDistance / 1000.0),
                color = Color(0xFF673AB7),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建小组") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("小组名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("小组简介（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (inviteCode: String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入小组") },
        text = {
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase() },
                label = { Text("邀请码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (inviteCode.isNotBlank()) onJoin(inviteCode.trim()) },
                enabled = inviteCode.isNotBlank()
            ) {
                Text("加入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun generateInviteCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}
