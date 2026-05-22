# redis的入门

## 1、简介

**认识NoSql**

![image-20220430140723120](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430140723120.png)



**认识Redis**

Redis诞生于2009年全称是Remote Dictionary Server，远程词典服务器，是一个基于内存的键值型NoSQL数据库。
特征：

-   键值（key-value）型，value支持多种不同数据结构，功能丰富
-   单线程，每个命令具备原子性
-   低延迟，速度快（基于内存、IO多路复用、良好的编码）。
-   支持数据持久化
-   支持主从集群、分片集群
-   支持多语言客户端

**官方网站**：https://redis.io/

**中文网**：https://www.redis.net.cn/

==**本次使用的是redis7**==



## 2、redis的安装

官网下载对应的包，然后上传到linux的opt目录下



### ①	准备工作

安装C语言编译环境

```sh
yum install -y centos-release-scl scl-utils-build
yum install -y devtoolset-8-toolchain
scl enable devtoolset-8 bash

# 安装完之后，测试gcc版本 
gcc --version
```



### ②	安装

```sh
# 1 进入到opt目录中解压redis
tar -zxvf redis-6.2.1.tar.gz 
# 2 cd进入到redis文件夹中
cd redis-6.2.1
# 3 进行编译和安装
make && make install
```



### ③	启动

**前台启动（不推荐）**

在/usr/local/bin中使用redis-server

**缺陷**：命令行窗口关闭了，服务就停止了

![image-20210918161735390](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/20210918161736.png)



**指定配置启动（推荐）**

```sh
# 1 拷贝一份redis.conf到其他目录
cp  /opt/redis-3.2.5/redis.conf  /etc/redis.conf

# 2 修改/etc/redis.conf文件
vim /etc/redis.conf
# 2.1 找到后台启动设置daemonize no改成yes

# 3 然后在/usr/local/bin目录中启动
[root@xiaozhi bin]# redis-server /etc/redis.conf

# 使用客户端来测试是否开启成功
[root@xiaozhi bin]# redis-cli
127.0.0.1:6379> ping
PONG

# 多个端口可以：redis-cli -p6379
```



**开机自启**

我们也可以通过配置来实现开机自启。

首先，新建一个系统服务文件：

```sh
vim /etc/systemd/system/redis.service
```

内容如下：

```sh
[Unit]
Description=redis-server
After=network.target

[Service]
Type=forking
ExecStart=/usr/local/bin/redis-server /etc/redis.conf
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```



然后重载系统服务：

```sh
systemctl daemon-reload
```



现在，我们可以用下面这组命令来操作redis了：

```sh
# 启动
systemctl start redis
# 停止
systemctl stop redis
# 重启
systemctl restart redis
# 查看状态
systemctl status redis
```



执行下面的命令，可以让redis开机自启：

```sh
systemctl enable redis
```





### ④	redis关闭

**单实例关闭**

```sh
[root@xiaozhi bin]# redis-cli shutdown

# 也可以进入终端再关
127.0.0.1:6379> shutdown
not connected> 
```



**多实例关闭**

指定端口关闭：**redis-cli -p 6379 shutdown**



### ⑤卸载

首先停掉redis

然后删除`/usr/local/bin`下所有的redis文件

```sh
rm -rf /usr/local/bin/redis-*
```





## 3	redis相关知识

端口 6379的来源Merz在手机上的位置

串行  vs  多线程+锁（**memcached**） vs  单线程+多路IO复用(**Redis**)

![image-20210918164438734](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/20210918164439.png)





