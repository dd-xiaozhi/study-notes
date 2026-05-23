# 第六章 FTP协议

## 概述

FTP（File Transfer Protocol，文件传输协议）是互联网上历史最悠久的应用层协议之一，设计于1971年RFC 114中，首次标准化在RFC 354（1983年）。FTP采用**客户端-服务器架构**，通过双重连接实现文件传输：**控制连接**（端口21）和**数据连接**（端口20）。

### FTP协议特点

- **双重连接设计**：控制连接用于传递命令和响应，数据连接用于传输实际文件数据
- **带外传输**：控制信息与数据分离传输
- **有状态协议**：服务器维护客户端会话状态
- **支持多种文件类型**：ASCII、二进制等
- **认证机制**：支持用户名/密码认证，也支持匿名访问

---

## FTP工作原理

### 连接建立流程

```mermaid
%%{ init: { "theme": "dark", "themeVariables": { "primaryColor": "#1a1a2e", "primaryTextColor": "#e0ffff", "primaryBorderColor": "#00d9ff", "lineColor": "#7b68ee", "secondaryColor": "#16213e", "tertiaryColor": "#0f3460" } } }%%
flowchart TB
    subgraph 客户端["<b style='color:#00d9ff'>FTP 客户端</b>"]
        A["<b style='color:#00ffff'>启动FTP客户端</b>"]
    end

    subgraph 服务器["<b style='color:#7b68ee'>FTP 服务器</b>"]
        B["<b style='color:#e0ffff'>监听端口21<br>控制连接监听</b>"]
        D["<b style='color:#e0ffff'>监听端口20<br>数据连接监听</b>"]
    end

    A --> C1{"<b style='color:#ffd700'>连接类型</b>"}
    C1 -->|控制连接| C2["<b style='color:#ffffff'>连接到服务器<br>端口21</b>"]
    C2 --> B
    B -->|发送欢迎信息| A

    style A fill:#0d1b2a,stroke:#00d9ff,color:#00ffff,stroke-width:2px
    style B fill:#1b2838,stroke:#7b68ee,color:#e0ffff,stroke-width:2px
    style D fill:#1b2838,stroke:#7b68ee,color:#e0ffff,stroke-width:2px
    style C1 fill:#2d1b4e,stroke:#ff6b9d,color:#ffd700,stroke-width:2px
    style C2 fill:#1a3a4a,stroke:#ffc107,color:#ffffff,stroke-width:2px
```

### 双重连接机制

```mermaid
%%{ init: { "theme": "dark", "themeVariables": { "primaryColor": "#1a1a2e", "primaryTextColor": "#e0ffff", "primaryBorderColor": "#00d9ff", "lineColor": "#7b68ee", "secondaryColor": "#16213e", "tertiaryColor": "#0f3460", "noteBkgColor": "#2d1b4e", "noteTextColor": "#ffd700", "noteBorderColor": "#ff6b9d", "actorBkg": "#0d1b2a", "actorBorder": "#00d9ff", "actorTextColor": "#e0ffff", "signalColor": "#00ffff", "signalTextColor": "#ffffff" } } }%%
sequenceDiagram
    participant C as "<b style='color:#00ffff'>FTP客户端</b>"
    participant S as "<b style='color:#7b68ee'>FTP服务器</b>"

    Note over C,S: <b style='color:#ffd700'>控制连接建立 (端口21)</b>
    C->>S: <b style='color:#00ffff'>TCP三次握手 - 端口21</b>
    S->>C: <b style='color:#3fb950'>220 Welcome (服务就绪)</b>

    Note over C,S: <b style='color:#ffd700'>用户认证</b>
    C->>S: <b style='color:#00ffff'>USER anonymous</b>
    S->>C: <b style='color:#ffa500'>331 User name okay, need password</b>
    C->>S: <b style='color:#00ffff'>PASS user@example.com</b>
    S->>C: <b style='color:#3fb950'>230 User logged in</b>

    Note over C,S: <b style='color:#ffd700'>数据连接建立 (按模式不同)</b>
    C->>S: <b style='color:#00ffff'>LIST /files</b>
    S->>C: <b style='color:#ffa500'>150 Opening data connection</b>

    Note over C,S: <b style='color:#ffd700'>数据传输</b>
    S-->>C: <b style='color:#ff6b9d'>数据连接传输文件列表</b>
    S->>C: <b style='color:#3fb950'>226 Transfer complete</b>

    Note over C,S: <b style='color:#ffd700'>控制连接保持</b>
    C->>S: <b style='color:#00ffff'>QUIT</b>
    S->>C: <b style='color:#3fb950'>221 Goodbye</b>
```

---

## FTP传输模式

### 主动模式 (PORT)

主动模式下，**客户端监听一个随机端口**，并通过控制连接告知服务器。服务器主动从**端口20**连接客户端。

```mermaid
%%{ init: { "theme": "dark", "themeVariables": { "primaryColor": "#1a1a2e", "primaryTextColor": "#e0ffff", "primaryBorderColor": "#00d9ff", "lineColor": "#7b68ee", "secondaryColor": "#16213e", "tertiaryColor": "#0f3460" } } }%%
flowchart LR
    subgraph Client["<b style='color:#00d9ff'>客户端防火墙场景</b>"]
        direction TB
        C1["<b style='color:#00ffff'>FTP客户端<br/>IP: 192.168.1.100</b>"]
        P1["<b style='color:#ffc107'>随机端口<br/>如: 54321</b>"]
    end

    subgraph Server["<b style='color:#7b68ee'>服务器端</b>"]
        C2["<b style='color:#00ffff'>FTP服务器<br/>IP: 10.0.0.50</b>"]
        P2["<b style='color:#ffc107'>数据端口<br/>20</b>"]
    end

    C1 -->|1. 控制连接<br/>PORT 54321| C2
    C2 -->|2. 确认| C1
    C2 -->|3. 从端口20<br/>连接54321| P1
    P1 -->|4. ACK| P2

    style C1 fill:#1a1a2e,stroke:#00d9ff,color:#00ffff,stroke-width:2px
    style P1 fill:#16213e,stroke:#ffc107,color:#ffc107,stroke-width:2px
    style C2 fill:#1a1a2e,stroke:#00d9ff,color:#00ffff,stroke-width:2px
    style P2 fill:#16213e,stroke:#ffc107,color:#ffc107,stroke-width:2px
```

**主动模式命令流程**：

```
ftp> open 10.0.0.50
Connected to 10.0.0.50.
220 FTP server ready.
ftp> user anonymous
331 User name okay, need password.
230 User logged in.
ftp> port 192,168,1,100,212,105
200 PORT command successful.
ftp> list
150 Opening data connection.
226 Transfer complete.
```

**主动模式问题**：如果客户端在NAT或防火墙后面，服务器无法主动连接客户端，因为客户端通常不会接受外部连接。

### 被动模式 (PASV)

被动模式下，**服务器监听一个随机端口**，客户端主动连接服务器。

```mermaid
%%{ init: { "theme": "dark", "themeVariables": { "primaryColor": "#1a1a2e", "primaryTextColor": "#e0ffff", "primaryBorderColor": "#00d9ff", "lineColor": "#7b68ee", "secondaryColor": "#16213e", "tertiaryColor": "#0f3460" } } }%%
flowchart LR
    subgraph Server["<b style='color:#7b68ee'>服务器端防火墙场景</b>"]
        direction TB
        C2["<b style='color:#00ffff'>FTP服务器<br/>IP: 10.0.0.50</b>"]
        P2["<b style='color:#ffc107'>随机数据端口<br/>如: 50000</b>"]
    end

    subgraph Client["<b style='color:#00d9ff'>客户端</b>"]
        C1["<b style='color:#00ffff'>FTP客户端<br/>IP: 192.168.1.100</b>"]
        P1["<b style='color:#ffc107'>随机端口<br/>如: 54321</b>"]
    end

    C1 -->|1. 控制连接<br/>端口21| C2
    C1 -->|2. PASV| C2
    C2 -->|3. 227 Entering<br/>Passive Mode<br/>10,0,0,50,195,80| C1
    P1 -->|4. 从54321连接<br/>50000端口| P2
    P2 -->|5. ACK| P1

    style C1 fill:#1a1a2e,stroke:#00d9ff,color:#00ffff,stroke-width:2px
    style P1 fill:#16213e,stroke:#ffc107,color:#ffc107,stroke-width:2px
    style C2 fill:#1a1a2e,stroke:#00d9ff,color:#00ffff,stroke-width:2px
    style P2 fill:#16213e,stroke:#ffc107,color:#ffc107,stroke-width:2px
```

**被动模式命令流程**：

```
ftp> open 10.0.0.50
Connected to 10.0.0.50.
220 FTP server ready.
ftp> user anonymous
331 User name okay, need password.
230 User logged in.
ftp> passive
Passive mode on.
ftp> list
227 Entering Passive Mode (10,0,0,50,195,80)
150 Opening data connection.
226 Transfer complete.
```

**被动模式问题**：服务器需要开放额外端口，服务器端防火墙配置更复杂。

### 两种模式对比

| 特性 | 主动模式 (PORT) | 被动模式 (PASV) |
|------|-----------------|-----------------|
| 数据连接方向 | 服务器→客户端 | 客户端→服务器 |
| 谁监听端口 | 客户端监听随机端口 | 服务器监听随机端口 |
| 防火墙问题 | 客户端难以接受外部连接 | 服务器需要开放多个端口 |
| NAT问题 | NAT后面的客户端难以工作 | 更适合NAT和防火墙环境 |
| 默认推荐 | 不推荐 | **推荐使用** |

---

## FTP命令详解

### 认证命令

| 命令 | 描述 | 示例 |
|------|------|------|
| `USER` | 指定用户名 | `USER anonymous` |
| `PASS` | 指定密码 | `PASS your@email.com` |
| `ACCT` | 指定账户 | `ACCT account-name` |
| `REIN` | 重新初始化会话 | `REIN` |
| `QUIT` | 断开连接 | `QUIT` |

### 文件操作命令

| 命令 | 描述 | 示例 |
|------|------|------|
| `LIST` | 列出文件/目录详情 | `LIST -la` |
| `NLST` | 列出文件/目录名称 | `NLST *.txt` |
| `RETR` | 下载文件 | `RETR report.pdf` |
| `STOR` | 上传文件（覆盖） | `STOR backup.zip` |
| `APPE` | 上传文件（追加） | `APPE log.txt` |
| `DELE` | 删除文件 | `DELE temp.tmp` |
| `RNFR` | 指定要重命名的文件 | `RNFR old.txt` |
| `RNTO` | 指定新名称 | `RNTO new.txt` |
| `MKD` | 创建目录 | `MKD newdir` |
| `RMD` | 删除目录 | `RMD olddir` |
| `PWD` | 显示当前目录 | `PWD` |
| `CWD` | 改变工作目录 | `CWD /public` |
| `CDUP` | 切换到父目录 | `CDUP` |

### 传输模式命令

| 命令 | 描述 | 示例 |
|------|------|------|
| `TYPE` | 设置传输类型 | `TYPE A` (ASCII)<br/>`TYPE I` (Binary) |
| `MODE` | 传输模式 | `MODE S` (Stream) |
| `STRU` | 文件结构 | `STRU F` (File) |

### 数据连接命令

| 命令 | 描述 | 示例 |
|------|------|------|
| `PORT` | 主动模式端口 | `PORT 192,168,1,100,212,105` |
| `PASV` | 进入被动模式 | `PASV` |
| `EPSV` | 扩展被动模式 | `EPSV` |
| `EPRT` | 扩展主动模式 | `EPRT |1|192.168.1.100|54321|` |

### 其他命令

| 命令 | 描述 | 示例 |
|------|------|------|
| `SYST` | 获取系统信息 | `SYST` |
| `FEAT` | 获取服务器特性 | `FEAT` |
| `SIZE` | 获取文件大小 | `SIZE largefile.iso` |
| `MDTM` | 获取文件修改时间 | `MDTM report.pdf` |
| `NOOP` | 空操作（保活） | `NOOP` |
| `HELP` | 获取帮助 | `HELP` |
| `STAT` | 获取状态信息 | `STAT` |

### 完整FTP会话示例

```
$ ftp -p 10.0.0.50
Connected to 10.0.0.50.
220 FTP server ready.
Name (10.0.0.50:user): anonymous
331 User name okay, need password.
Password:
230 User logged in.
Remote system type is UNIX.
Using binary mode to transfer files.

ftp> feat
211-Features:
 MDTM
 PASV
 EPSV
 TYPE L 8
211 End

ftp> pwd
257 "/home/user" is current directory.

ftp> list
200 PORT command successful.
150 Opening data connection.
drwxr-xr-x   3 user     staff         96 May 22 10:30 documents
-rw-r--r--   1 user     staff      10240 May 23 14:20 report.pdf
-rw-r--r--   1 user     staff     512000 May 23 09:15 backup.zip
226 Transfer complete.

ftp> type i
200 Type set to I.

ftp> retr report.pdf
200 PORT command successful.
150 Opening data connection.
226 Transfer complete.
10240 bytes received in 0.000547s (18.3 MB/s)

ftp> quit
221 Goodbye.
```

---

## FTP响应码

FTP服务器对每个命令返回三位数字响应码，客户端根据首位数字判断响应类型。

### 响应码分类

| 首位 | 类型 | 含义 |
|------|------|------|
| `1xx` | 肯定预备 | 命令已接收，正在处理 |
| `2xx` | 肯定完成 | 命令成功完成 |
| `3xx` | 肯定Intermediate | 命令已接收，需要更多信息 |
| `4xx` | 暂时否定 | 命令失败，客户端应重试 |
| `5xx` | 永久否定 | 命令无效或语法错误 |

### 常用响应码详解

| 响应码 | 含义 | 说明 |
|--------|------|------|
| `125` | 数据连接已打开，传输开始 | 确认数据连接建立成功 |
| `150` | 文件状态正常，即将打开数据连接 | 服务器准备传输文件 |
| `200` | 命令成功 | 通用成功响应 |
| `220` | 服务就绪 | 服务器欢迎信息 |
| `221` | 服务关闭控制连接 | 客户端退出 |
| `226` | 传输完成 | 文件传输成功结束 |
| `227` | 进入被动模式 | 返回服务器IP和端口 |
| `230` | 用户登录成功 | 认证成功 |
| `331` | 用户名正确，需要密码 | 等待密码输入 |
| `350` | 请求的文件操作需要更多信息 | RNFR后需要RNTO |
| `425` | 无法打开数据连接 | 数据连接建立失败 |
| `426` | 连接关闭，传输中断 | 数据连接异常 |
| `450` | 文件操作被拒绝 | 文件不可用 |
| `500` | 语法错误，命令无法识别 | 命令格式错误 |
| `501` | 参数语法错误 | 参数格式错误 |
| `530` | 未登录 | 需要先登录 |
| `550` | 操作被拒绝 | 文件不存在或权限不足 |

### 响应码解析示例

```
ftp> retr nonexistent.txt
550 nonexistent.txt: No such file or directory

ftp> dele report.pdf
250 Deleted report.pdf

ftp> stat
211-FTP server status:
     Connected to 192.168.1.100
     Logged in as anonymous
     TYPE: ASCII; TYPE: Image for binary files
     No pending commands
211 End of status
```

---

## ASCII vs Binary 传输模式

### ASCII模式

- 用于传输**纯文本文件**
- 自动转换换行符（CRLF ↔ LF）
- Windows使用CRLF行尾，Unix/Linux使用LF行尾
- 传输时自动转换，文本文件在不同系统间保持格式正确

```
ftp> type a
200 Type set to A (ASCII).

ftp> send readme.txt
200 PORT command successful.
150 Opening data connection.
226 Transfer complete.
```

### Binary模式

- 用于传输**二进制文件**（图片、视频、可执行文件、压缩包等）
- 逐字节传输，不做任何转换
- 所有字节原样保留

```
ftp> type i
200 Type set to I (Binary).

ftp> send image.png
200 PORT command successful.
150 Opening data connection.
226 Transfer complete.
```

### 模式选择建议

| 文件类型 | 传输模式 | 原因 |
|----------|----------|------|
| .txt, .csv, .json, .xml, .html, .css, .js | ASCII | 换行符自动转换 |
| .jpg, .png, .gif, .mp4, .zip, .tar, .pdf | Binary | 保持数据完整性 |
| .exe, .dll, .so, .dylib | Binary | 可执行文件精确传输 |

### 常见问题

**问题**：使用ASCII模式传输二进制文件会导致文件损坏。

```
# 错误示例 - ASCII模式传输图片
ftp> type a
ftp> send logo.png
# 接收方得到的文件已损坏！

# 正确示例 - Binary模式传输图片
ftp> type i
ftp> send logo.png
# 文件完整性保持
```

---

## FTPS vs SFTP vs FTP

这三种协议都用于文件传输，但实现和安全特性完全不同。

### 协议对比

| 特性 | FTP | FTPS (FTP over TLS) | SFTP (SSH File Transfer) |
|------|-----|---------------------|---------------------------|
| 底层协议 | TCP | TCP + TLS/SSL | SSH (TCP) |
| 默认端口 | 21 (控制) | 21 (控制) | 22 |
| 加密 | 无 | TLS/SSL加密 | SSH加密 |
| 证书 | 无 | 需要X.509证书 | SSH密钥 |
| 防火墙友好 | 一般 | 一般 | 是（单端口） |
| 传输效率 | 高 | 略低（加密开销） | 略低（加密开销） |
| 兼容性 | 极广 | 广泛 | 较广 |
| 命令兼容性 | 原始FTP | 原始FTP + TLS命令 | 完全不同的命令集 |

### FTP（明文）

```mermaid
%%{ init: { "theme": "dark", "themeVariables": { "primaryColor": "#1a1a2e", "primaryTextColor": "#e0ffff", "primaryBorderColor": "#ff4757", "lineColor": "#ff4757", "secondaryColor": "#2d1b4e", "tertiaryColor": "#0f3460" } } }%%
flowchart LR
    subgraph 不安全["<b style='color:#ff4757'>FTP (不安全)</b>"]
        C1["<b style='color:#ffffff'>FTP客户端</b>"] -->|"<b style='color:#ff6b6b'>明文数据</b>"| S1["<b style='color:#ffffff'>FTP服务器</b>"]
        C1 -->|"<b style='color:#ffa502'>明文 USER/PASS</b>"| S1
    end

    style C1 fill:#ff4757,stroke:#ff4757,color:#ffffff,stroke-width:2px
    style S1 fill:#ff4757,stroke:#ff4757,color:#ffffff,stroke-width:2px
```

### FTPS（TLS加密）

```mermaid
%%{ init: { "theme": "dark", "themeVariables": { "primaryColor": "#1a1a2e", "primaryTextColor": "#e0ffff", "primaryBorderColor": "#2ed573", "lineColor": "#3fb950", "secondaryColor": "#16213e", "tertiaryColor": "#0f3460" } } }%%
flowchart LR
    subgraph 安全["<b style='color:#3fb950'>FTPS (FTP over TLS)</b>"]
        C2["<b style='color:#ffffff'>FTP客户端</b>"] -->|"<b style='color:#2ed573'>TLS加密</b>"| S2["<b style='color:#ffffff'>FTP服务器</b>"]
        C2 -->|"<b style='color:#7bed9f'>TLS加密认证</b>"| S2
    end

    style C2 fill:#2ed573,stroke:#2ed573,color:#ffffff,stroke-width:2px
    style S2 fill:#2ed573,stroke:#2ed573,color:#ffffff,stroke-width:2px
```

### SFTP（SSH协议）

```mermaid
%%{ init: { "theme": "dark", "themeVariables": { "primaryColor": "#1a1a2e", "primaryTextColor": "#e0ffff", "primaryBorderColor": "#1e90ff", "lineColor": "#58a6ff", "secondaryColor": "#16213e", "tertiaryColor": "#0f3460" } } }%%
flowchart LR
    subgraph sftp["<b style='color:#58a6ff'>SFTP (SSH File Transfer)</b>"]
        C3["<b style='color:#ffffff'>SFTP客户端</b>"] -->|"<b style='color:#1e90ff'>SSH加密</b>"| S3["<b style='color:#ffffff'>SFTP服务器</b>"]
    end

    style C3 fill:#1e90ff,stroke:#1e90ff,color:#ffffff,stroke-width:2px
    style S3 fill:#1e90ff,stroke:#1e90ff,color:#ffffff,stroke-width:2px
```

### 安全性总结

```
FTP      →  ❌ 不推荐 - 用户名、密码、数据均为明文
FTPS     →  ⚠️  可用 - 隐式TLS (端口990) 或 显式TLS (端口21)
SFTP     →  ✅  推荐 - 基于SSH，端到端加密
```

---

## 工程实践

### 命令行工具

#### lftp - 强大的FTP客户端

lftp是一个功能丰富的FTP客户端，支持tab补全、断点续传、任务队列等高级功能。

**安装**：

```bash
# Debian/Ubuntu
sudo apt install lftp

# macOS
brew install lftp

# Windows (通过WSL)
sudo apt install lftp
```

**基本用法**：

```bash
# 连接FTP服务器
lftp ftp://anonymous@ftp.example.com

# 使用密码连接
lftp ftp://user:password@ftp.example.com

# 指定端口和被动模式
lftp -p 2121 -e "set ftp:passive-mode true" ftp.example.com
```

**常用命令**：

```bash
# 登录
lftp ftp.example.com -u username

# 切换目录
cd /public_html

# 列出文件
ls -la

# 下载单个文件
get report.pdf

# 下载多个文件 (通配符)
mget *.txt

# 上传单个文件
put backup.zip

# 上传多个文件
mput *.jpg

# 镜像下载 (同步到本地)
mirror documents/ ./downloads/

# 镜像上传 (同步到服务器)
mirror -R ./uploads/ /server/

# 断点续传
get -c largefile.iso

# 后台下载
pget -n 4 bigfile.tar.gz

# 并发上传
mput *.mp4 &

# 查看队列
jobs

# 删除文件
rm old.tmp

# 删除目录
rmdir olddir

# 设置传输模式
set ftp:passive-mode true
set ftp:charset utf8

# 设置binary模式
set binary on

# 查看设置
set -a

# 退出
quit
```

**高级用法 - 脚本文件**：

```bash
#!/bin/bash
# ftp_sync.sh - FTP同步脚本

HOST="ftp.example.com"
USER="username"
PASS="password"
REMOTE_DIR="/backups"
LOCAL_DIR="./backups"

lftp -c "
open ftp://${USER}:${PASS}@${HOST}
lcd ${LOCAL_DIR}
cd ${REMOTE_DIR}
mirror --verbose --parallel=4 --reverse
bye
"
```

**高级用法 - 定时同步**：

```bash
# crontab -e 添加定时任务
# 每天凌晨2点同步
0 2 * * * /home/user/ftp_sync.sh >> /var/log/ftp_sync.log 2>&1
```

#### 标准ftp命令

```bash
# 连接
ftp 10.0.0.50

# 被动模式
passive

# 登录
user anonymous

# 下载文件
get remote.txt local.txt

# 上传文件
put local.txt remote.txt

# 批量下载
mget *.pdf

# 批量上传
mput *.jpg

# 匿名登录常用配置
set ftp:anon-pass "anonymous@"
```

### Python ftplib

Python标准库提供`ftplib`模块，支持FTP协议编程。

#### 基本操作

```python
import ftplib
from ftplib import FTP

# 连接FTP服务器
ftp = FTP('ftp.example.com')
ftp.login(user='anonymous', passwd='anonymous@example.com')

# 打印欢迎信息
print(ftp.getwelcome())

# 获取当前目录
print(ftp.pwd())

# 切换目录
ftp.cwd('/public')

# 列出文件
ftp.retrlines('LIST')

# 下载文件 (ASCII模式)
with open('readme.txt', 'w') as f:
    ftp.retrlines('RETR readme.txt', f.write)

# 下载文件 (Binary模式)
with open('image.png', 'wb') as f:
    ftp.retrbinary('RETR image.png', f.write)

# 上传文件 (Binary模式)
with open('backup.zip', 'rb') as f:
    ftp.storbinary('STOR backup.zip', f)

# 上传文件 (ASCII模式)
with open('report.txt', 'r') as f:
    ftp.storlines('STOR report.txt', f)

# 获取文件大小
size = ftp.size('largefile.iso')
print(f"File size: {size} bytes")

# 重命名文件
ftp.rename('old.txt', 'new.txt')

# 删除文件
ftp.delete('temp.tmp')

# 创建目录
ftp.mkd('/newdir')

# 删除目录
ftp.rmd('/emptydir')

# 退出
ftp.quit()
```

#### 完整示例：文件同步工具

```python
#!/usr/bin/env python3
"""
FTP文件同步工具
功能：本地目录与FTP服务器目录同步
"""

import ftplib
import os
from datetime import datetime
from pathlib import Path

class FTPSync:
    def __init__(self, host, username, password, port=21):
        self.host = host
        self.username = username
        self.password = password
        self.port = port
        self.ftp = None

    def connect(self):
        """建立FTP连接"""
        self.ftp = ftplib.FTP()
        self.ftp.connect(self.host, self.port)
        self.ftp.login(self.username, self.password)
        self.ftp.encoding = 'utf-8'
        print(f"Connected to {self.host}")

    def download_file(self, remote_path, local_path):
        """下载单个文件"""
        try:
            # 确保本地目录存在
            Path(local_path).parent.mkdir(parents=True, exist_ok=True)

            # 获取远程文件大小
            remote_size = self.ftp.size(remote_path)

            # 检查本地文件是否存在
            local_exists = Path(local_path).exists()
            local_size = Path(local_path).stat().st_size if local_exists else 0

            # 断点续传
            if local_exists and local_size < remote_size:
                print(f"Resuming {remote_path} ({local_size}/{remote_size})")
                with open(local_path, 'ab') as f:
                    self.ftp.retrbinary(
                        f'RETR {remote_path}',
                        f.write,
                        rest=local_size
                    )
            else:
                print(f"Downloading {remote_path}")
                with open(local_path, 'wb') as f:
                    self.ftp.retrbinary(f'RETR {remote_path}', f.write)

            print(f"  -> Saved to {local_path}")
            return True

        except Exception as e:
            print(f"  Error downloading {remote_path}: {e}")
            return False

    def upload_file(self, local_path, remote_path):
        """上传单个文件"""
        try:
            print(f"Uploading {local_path}")
            with open(local_path, 'rb') as f:
                self.ftp.storbinary(f'STOR {remote_path}', f)
            print(f"  -> Saved to {remote_path}")
            return True

        except Exception as e:
            print(f"  Error uploading {local_path}: {e}")
            return False

    def sync_to_local(self, remote_dir, local_dir):
        """同步FTP目录到本地"""
        print(f"\nSyncing {remote_dir} -> {local_dir}")

        # 切换到远程目录
        try:
            self.ftp.cwd(remote_dir)
        except:
            print(f"Remote directory {remote_dir} not found")
            return

        # 获取远程文件列表
        files = self.ftp.nlst()

        for file in files:
            remote_path = f"{remote_dir}/{file}"
            local_path = os.path.join(local_dir, file)

            try:
                # 尝试切换目录（判断是否为目录）
                self.ftp.cwd(file)
                self.ftp.cwd('..')
                # 是目录，递归处理
                os.makedirs(local_path, exist_ok=True)
                self.sync_to_local(remote_path, local_path)

            except ftplib.error_perm:
                # 是文件，下载
                self.download_file(remote_path, local_path)

    def mirror_to_server(self, local_dir, remote_dir):
        """镜像本地目录到FTP服务器"""
        print(f"\nMirroring {local_dir} -> {remote_dir}")

        for root, dirs, files in os.walk(local_dir):
            # 计算相对路径
            rel_path = os.path.relpath(root, local_dir)
            if rel_path == '.':
                rel_path = ''

            remote_base = remote_dir if not rel_path else f"{remote_dir}/{rel_path}"

            # 创建远程目录
            try:
                self.ftp.cwd(remote_base)
            except ftplib.error_perm:
                try:
                    self.ftp.mkd(remote_base)
                    self.ftp.cwd(remote_base)
                except:
                    pass

            # 上传文件
            for file in files:
                local_path = os.path.join(root, file)
                remote_path = f"{remote_base}/{file}" if remote_base != remote_dir else f"{remote_dir}/{file}"
                self.upload_file(local_path, remote_path)

    def close(self):
        """关闭连接"""
        if self.ftp:
            self.ftp.quit()
            print("Disconnected")


def main():
    # 配置
    sync = FTPSync(
        host='ftp.example.com',
        username='username',
        password='password'
    )

    try:
        sync.connect()

        # 示例1: 下载整个目录
        sync.sync_to_local('/public/docs', './downloads/docs')

        # 示例2: 上传本地目录
        # sync.mirror_to_server('./uploads', '/backup')

    except Exception as e:
        print(f"Error: {e}")

    finally:
        sync.close()


if __name__ == '__main__':
    main()
```

#### FTP TLS 加密连接

```python
import ftplib
from pathlib import Path

class SecureFTP:
    """使用FTPS (FTP over TLS)"""

    def __init__(self, host, username, password, port=21):
        self.host = host
        self.username = username
        self.password = password
        self.port = port

    def connect(self, use_tls=True):
        """建立加密FTP连接"""
        if use_tls:
            self.ftp = ftplib.FTP_TLS()
        else:
            self.ftp = ftplib.FTP()

        self.ftp.connect(self.host, self.port)
        self.ftp.login(self.username, self.password)

        # TLS加密
        if use_tls:
            self.ftp.prot_p()  # 启用加密数据连接
            print("TLS/SSL encryption enabled")

        return self.ftp

    def download_file(self, remote_path, local_path):
        """下载文件"""
        Path(local_path).parent.mkdir(parents=True, exist_ok=True)
        with open(local_path, 'wb') as f:
            self.ftp.retrbinary(f'RETR {remote_path}', f.write)

    def close(self):
        """关闭连接"""
        self.ftp.quit()
```

#### 异步FTP操作 (asyncio)

```python
import asyncio
from ftplib import FTP
from concurrent.futures import ThreadPoolExecutor

async def async_download(ftp_host, ftp_user, ftp_pass, remote_file, local_file):
    """异步下载文件"""
    loop = asyncio.get_event_loop()

    def _download():
        ftp = FTP(ftp_host)
        ftp.login(ftp_user, ftp_pass)
        with open(local_file, 'wb') as f:
            ftp.retrbinary(f'RETR {remote_file}', f.write)
        ftp.quit()
        return local_file

    result = await loop.run_in_executor(None, _download)
    print(f"Downloaded: {result}")
    return result

async def main():
    tasks = [
        async_download('ftp.example.com', 'user', 'pass',
                       '/files/doc1.pdf', './doc1.pdf'),
        async_download('ftp.example.com', 'user', 'pass',
                       '/files/doc2.pdf', './doc2.pdf'),
        async_download('ftp.example.com', 'user', 'pass',
                       '/files/doc3.pdf', './doc3.pdf'),
    ]

    await asyncio.gather(*tasks)

if __name__ == '__main__':
    asyncio.run(main())
```

---

## FTP服务器软件

### vsftpd (Linux)

```bash
# Ubuntu/Debian 安装
sudo apt install vsftpd

# 配置文件
sudo nano /etc/vsftpd.conf

# 主要配置项
# 允许匿名访问
anonymous_enable=YES
# 允许本地用户
local_enable=YES
# 允许写入
write_enable=YES
# 被动模式端口范围
pasv_enable=YES
pasv_min_port=40000
pasv_max_port=40100
# 启用TLS
ssl_enable=YES
rsa_cert_file=/etc/ssl/certs/ssl-cert-snakeoil.pem
rsa_private_key_file=/etc/ssl/private/ssl-cert-snakeoil.key

# 启动服务
sudo systemctl start vsftpd
sudo systemctl enable vsftpd
```

### FileZilla Server (Windows)

```ini
# FileZilla Server 配置示例
# 监听端口
Port=14147

# 最大连接数
Max connections=100

# 被动模式设置
Passive mode port range=50000-50100

# 允许TLS
<ssl>TLS
```

---

## 常见问题排查

### 问题1：FTP连接超时

```
症状：连接时等待很久后超时

排查步骤：
1. 检查21端口是否可达: telnet ftp.example.com 21
2. 检查服务器FTP服务是否运行
3. 检查防火墙规则
4. 尝试被动模式: passive on
```

### 问题2：数据连接建立失败

```
症状：认证成功但LIST或传输失败

排查步骤：
1. 主动模式问题：检查客户端是否可接受外部连接
2. 被动模式问题：检查服务器数据端口范围是否开放
3. 服务器防火墙：开放端口20和被动模式端口范围
4. 客户端防火墙：允许FTP客户端出站连接
```

### 问题3：文件名乱码

```
症状：中文文件名显示为乱码

解决方案：
1. 设置UTF-8编码: ftp.encoding = 'utf-8'
2. 或使用支持UTF-8的客户端如lftp
3. 检查服务器和客户端编码设置一致
```

### 问题4：文件传输损坏

```
症状：下载的二进制文件无法打开

解决方案：
1. 确保使用Binary模式: type i
2. ASCII模式只能用于纯文本
3. 传输前检查文件大小是否一致
```

---

## 总结

FTP协议作为互联网历史悠久的文件传输协议，具有广泛的应用场景和良好的兼容性。理解其**双重连接机制**（控制连接21端口、数据连接20端口）、**主动/被动模式**的区别对于配置防火墙和排除连接问题至关重要。

现代文件传输场景中，**SFTP**因其基于SSH的安全性（单端口、端到端加密）已成为首选方案；**FTPS**则适用于需要与传统FTP系统兼容的场景。在实际工程中，推荐使用`lftp`等成熟工具或Python的`ftplib`库进行自动化文件传输操作。

---

## 参考资料

- RFC 959 - FILE TRANSFER PROTOCOL (FTP)
- RFC 2428 - FTP Extensions for IPv6 and NATs
- RFC 4217 - Securing FTP with TLS
- vsftpd文档: https://security.appspot.com/vsftpd.html
- lftp手册: https://lftp.yar.ru/lftp-man.html
- Python ftplib文档: https://docs.python.org/3/library/ftplib.html
