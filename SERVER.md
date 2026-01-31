# 位置共享服务器

这是一个简单的Node.js WebSocket服务器，用于支持RunShare App的实时位置共享功能。

## 快速开始

### 1. 安装依赖

```bash
npm init -y
npm install ws express
```

### 2. 创建服务器文件 `server.js`

```javascript
const WebSocket = require('ws');
const express = require('express');
const http = require('http');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server, path: '/ws' });

// 存储用户连接
const clients = new Map(); // userId -> { ws, username, location }
const subscriptions = new Map(); // userId -> Set<subscriberWs>

wss.on('connection', (ws) => {
    let userId = null;
    
    ws.on('message', (data) => {
        try {
            const msg = JSON.parse(data);
            
            switch (msg.type) {
                case 'auth':
                    userId = msg.userId;
                    clients.set(userId, {
                        ws,
                        username: msg.username,
                        location: null
                    });
                    console.log(`用户 ${msg.username} (${userId}) 已连接`);
                    break;
                    
                case 'location':
                    if (userId && clients.has(userId)) {
                        const client = clients.get(userId);
                        client.location = msg.data;
                        
                        // 通知所有订阅者
                        const subs = subscriptions.get(userId);
                        if (subs) {
                            const locationMsg = JSON.stringify({
                                type: 'location',
                                data: msg.data
                            });
                            subs.forEach(subWs => {
                                if (subWs.readyState === WebSocket.OPEN) {
                                    subWs.send(locationMsg);
                                }
                            });
                        }
                    }
                    break;
                    
                case 'subscribe':
                    const friendId = msg.friendId;
                    if (!subscriptions.has(friendId)) {
                        subscriptions.set(friendId, new Set());
                    }
                    subscriptions.get(friendId).add(ws);
                    
                    // 如果好友在线，发送当前位置
                    const friend = clients.get(friendId);
                    if (friend && friend.location) {
                        ws.send(JSON.stringify({
                            type: 'location',
                            data: friend.location
                        }));
                    }
                    console.log(`用户订阅了 ${friendId}`);
                    break;
            }
        } catch (e) {
            console.error('消息解析错误:', e);
        }
    });
    
    ws.on('close', () => {
        if (userId) {
            clients.delete(userId);
            
            // 通知订阅者该用户已离线
            const subs = subscriptions.get(userId);
            if (subs) {
                const offlineMsg = JSON.stringify({
                    type: 'offline',
                    userId: userId
                });
                subs.forEach(subWs => {
                    if (subWs.readyState === WebSocket.OPEN) {
                        subWs.send(offlineMsg);
                    }
                });
            }
            console.log(`用户 ${userId} 已断开`);
        }
    });
});

// HTTP API 用于查看在线用户（可选）
app.get('/api/users', (req, res) => {
    const users = [];
    clients.forEach((client, id) => {
        users.push({
            userId: id,
            username: client.username,
            online: true,
            hasLocation: !!client.location
        });
    });
    res.json(users);
});

// 简单的分享页面
app.get('/share/:userId', (req, res) => {
    const userId = req.params.userId;
    const client = clients.get(userId);
    
    if (client && client.location) {
        res.json({
            username: client.username,
            location: client.location,
            online: true
        });
    } else {
        res.json({
            online: false,
            message: '用户离线'
        });
    }
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 位置共享服务器已启动`);
    console.log(`📡 WebSocket: ws://0.0.0.0:${PORT}/ws`);
    console.log(`🌐 HTTP API: http://0.0.0.0:${PORT}`);
});
```

### 3. 运行服务器

```bash
node server.js
```

### 4. 在App中配置

1. 打开App设置
2. 点击"服务器地址"
3. 输入你的服务器地址，例如：`http://192.168.1.100:8080`
4. 保存设置
5. 打开"分享位置"开关

## Docker 部署

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 8080
CMD ["node", "server.js"]
```

```bash
docker build -t runshare-server .
docker run -d -p 8080:8080 runshare-server
```

## 服务器要求

- Node.js 16+
- 开放端口 8080（或自定义端口）
- 如果部署在公网，建议使用HTTPS/WSS

## 协议说明

### WebSocket 消息格式

**客户端 → 服务器:**

```json
// 认证
{ "type": "auth", "userId": "abc123", "username": "跑步者" }

// 位置更新
{
  "type": "location",
  "data": {
    "userId": "abc123",
    "username": "跑步者",
    "location": { "latitude": 39.9, "longitude": 116.4, ... },
    "isRunning": true,
    "distance": 1500.5,
    "duration": 600000
  }
}

// 订阅好友
{ "type": "subscribe", "friendId": "xyz789" }
```

**服务器 → 客户端:**

```json
// 好友位置更新
{
  "type": "location",
  "data": { ... }
}

// 好友离线
{ "type": "offline", "userId": "xyz789" }
```
