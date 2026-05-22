#  Nginx概念

## Nginx简介

*Nginx* (engine x) 是一个高性能的[HTTP](https://baike.baidu.com/item/HTTP)和[反向代理](https://baike.baidu.com/item/反向代理/7793488)web服务器，同时也提供了IMAP/POP3/SMTP，能够支持高达 50,000 个并发连接数的响应

![image-20210319212731807](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319212731807.png)

**说明**：我们在并发量不高的时候就是这样来进行访问资源，当我们的访问量大的时候，服务器处理请求的速度就会变慢，那么为了解决这个问题有两个方案，一种是横向扩展，就是我们扩展服务器的硬件，另外一种就是纵向扩展，也就是我们在客户端和服务端中间加一个代理服务器，由我们的代理服务器将请求分发到几台服务器上，从而减轻单机的压力





## 正向代理

是某台电脑通过一台服务器来上Internet网的这种方式，，其中这台电脑叫客户机，这台服务器叫正向代理服务器，也是通常所说的代理服务器。一般情况下，客户机必须制定代理服务器（IE浏览器可在工具→Internet选项→连接→局域网设置→代理服务器中设置）。它还是访问的是服务器的端口，只是由代理服务器给它进行转发

![image-20210319213730852](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319213730852.png)



## 反向代理

![image-20210319213545689](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319213545689.png)



## 正向代理和反向代理的区别

正向代理我们需要给用户暴露我们的Web服务的数据库等系统的IP地址和端口号等敏感息，所以容易遭受到恶意攻击，还有的就是正向代理需要客户动手设置浏览器，麻烦。反向代理可以隐藏我们的额Web应用服务，如数据库的IP地址、端口号等信息，提高了系统的安全性，所以我们一般都是使用反向代理。一句话，安全



## 负载均衡

将请求平均的分发给服务器，也可以手动设置服务器的权重来给服务器多一点请求或者少一点请求

![image-20210319214606792](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319214606792.png)





## 动静分离

动态资源和静态资源放到两台不同的服务器上，由Nginx（代理服务器）来进行访问

![image-20210319214844813](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319214844813.png)





# 配置实例

## 环境搭建

### 一	安装编译工具及库文件

```sh
yum -y install make zlib zlib-devel gcc-c++ libtool  openssl openssl-devel
```



### 二	安装 PCRE

1	下载 PCRE 安装包，下载地址： http://downloads.sourceforge.net/project/pcre/pcre/8.35/pcre-8.35.tar.gz

```shell
[root@xiaozhi src]# cd /usr/local/src/
[root@xiaozhi src]# wget http://downloads.sourceforge.net/project/pcre/pcre/8.35/pcre-8.35.tar.gz
```

2	解压

```shell
[root@xiaozhi src]# tar zxvf pcre-8.35.tar.gz
```

3	进入安装包目录

```sh
[root@xiaozhi src]# cd pcre-8.35
```

4	编译安装 

```shell
[root@xiaozhi pcre-8.35]# ./configure
[root@xiaozhi pcre-8.35]# make && make install
```

5	查看pcre版本

```shell
[root@xiaozhi pcre-8.35]# pcre-config --version
```



### 三、安装Nginx

1、下载 Nginx，下载地址：https://nginx.org/en/download.html

```sh
[root@xiaozhi src]# cd /usr/local/src/
[root@xiaozhi src]# wget http://nginx.org/download/nginx-1.6.2.tar.gz
```

 2、解压安装包

```shell
[root@xiaozhi src]# tar zxvf nginx-1.6.2.tar.gz
```

3、进入安装包目录

```shell
[root@xiaozhi src]# cd nginx-1.6.2
```

4、编译安装

```shell
[root@xiaozhi nginx-1.6.2]# ./configure --prefix=/usr/local/webserver/nginx --with-http_stub_status_module --with-http_ssl_module --with-pcre=/usr/local/src/pcre-8.35
[root@xiaozhi nginx-1.6.2]# make && make install
```

5、查看nginx版本

```shell
[root@xiaozhi nginx-1.6.2]# /usr/local/webserver/nginx/sbin/nginx -v
```



### 四	通过ip访问nginx

出现下面那个页面就说明nginx可以正常访问

![image-20210319223802365](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319223802365.png)

==**注意**：要开放对应的端口，或者关闭防火墙==



## 常用命令

```sh
/usr/local/webserver/nginx/sbin/nginx		              # 开启Nginx
/usr/local/webserver/nginx/sbin/nginx -s reload            # 重新载入配置文件
/usr/local/webserver/nginx/sbin/nginx -s reopen            # 重启 Nginx
/usr/local/webserver/nginx/sbin/nginx -s stop              # 停止 Nginx
/usr/local/webserver/nginx/sbin/nginx -v               	   # 查看版本号
```









## Nginx的配置文件

### **配置文件所在位置**

```sh
/usr/local/nginx/conf/nginx.conf
```



### **配置文件中的分布**

==**注意**：进入配置文件的时候有可能是因为分页功能导致我们只看到一部分的配置，用键盘向下滑动就可以了==

#### **第一部分：全局块**

从配置文件到开始events块的内容，主要配置一些影响nginx服务器整体运行的配置指令

比如配置文件中的第一行		`worker_processes  1;`

这是nginx服务器并发处理服务的相关配置，work_processses值越大，可以支持的并发处理量越多，但是会受到硬件、软件等设备的制约



#### **第二部分：events块**

这里涉及到的指令主要影响nginx服务器与用户的网络连接

比如		`worker_connections  1024;`支持最大的连接数



#### **第三部分：http块**

nginx配置最频繁的地方

**①http全局块**

http全局块配置的指令包括文件引入、MIME-TYPE定义、归志自定义、连接超时时间、单链接请求数上限等。。



**②全局Server块**

这块和虚拟主机有密切关系,虚拟主机从用户角度看,和一台独立的硬件主机是完全一样的,该技术的产生是为了节省互联网服务器硬件成本。

每个http块可以包括多个server块,而每个server块就相当于一一个虚拟主机。

而每个服务器块也分为全局服务器块，以及可以同时包含多个locaton块

**1、全局server块**

最常见的配置是本虚拟机主机的监听配置和本虚拟主机的名称或IP配置。

**2、localtion块**

一个server块可以配置多个location块

这块的主要作用是基于Nginx服务器接收到的请求字符串(例如server name/uri-string ) , 对虚拟主机名称，(也可以是IP别名)之外的字符串(例如前面的/uri-string )进行匹配,对特定的请求进行处理。地址定向、数据缓存和应答控制等功能,还有许多第三方模块的配置也在这里进行。



## 反向代理

1、实现效果

浏览器输入www.123.com跳转到我们的tomcat服务器中

![image-20210319231655002](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319231655002.png)



**实现步骤**

1	安装tomcat服务器

2	在window访问页面，修改hosts文件，在文件中添加下面的内容

默认在	C:\Windows\System32\drivers\etc

```txt
IP地址	www.123.com		
```

3	现在我们就可以通过www.123.com:8080访问到服务器了，接下来我们用Nginx来进行请求转发

4	配置nginx，修改配置文件

![image-20210319232651414](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210319232651414.png)

5	现在我们直接打开浏览器输入www.123.com就可以访问到我们的服务器了，不用输入端口号，因为现在暴露的是nginx服务器的端口，它会给我们转发请求给服务器



**注意**：

```sh
nginx: [emerg] invalid number of arguments in "proxy_pass" directive in /usr/local/nginx/conf/nginx.conf:46
# 这个错误是因为没分号结尾，要注意这个问题
```



## 反向代理2

**1	实验效果**

访问http://IP:9001/edu/	直接跳转到127.0.0.1:8080

访问http://IP.1:9001/vod/	直接跳转到127.0.0.1:8081

**2	准备工作**

①准备两天tomcat服务器，一个8080端口，一个8081端口

②创建文件夹和测试页面

**3	具体配置**

①找到nginx配置文件，进行反向代理

![image-20210320105800876](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320105800876.png)

**4	测试**

![image-20210320110328122](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320110328122.png)

![image-20210320110341604](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320110341604.png)

**没有问题！！**



## 负载均衡

**1	实现效果**

在浏览器访问http://IP/edu/a.html，负载均衡效果，平均分到8080,8081端口号中

**2	准备工作**

①两台tomcat服务器，一台8080，一台8081

②两台服务器分别在webapps文件夹中创建edu文件夹，然后创建a.html文件，用于测试

**3	具体配置**

![image-20210320113006709](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320113006709.png)



### 策略

**1、轮询（默认）**

每个请求按时间顺序逐-分配到不同的后端服务器，如果后端服务器down掉，能自动剔除

**2、weight**

权重，就是分发的请求的数量，权重大的分到的请求就越多

![image-20210320113703526](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320113703526.png)

**3、IP_hash**

每个请求按访问ipe.的hash结果分配,这样每个访客固定访问一个后端服务器,可以解诀session的问题

**4、fair（第三方）**

按后端，服务器的响应时间来分配请求，响应时间短的优先分配。



![image-20210320113736359](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320113736359.png)

**说明**：策略都是在这个块下定义的



## 动静分离

**1	两种方案**

①将静态文件单独放在一个服务器上，动态和静态的请求分开，这是比较推荐的做法

②动态资源和静态资源放在同一台服务器上，通过localtion指定不同的后缀名实现不同的请求转发。然后缓存到客户端中，第二次请求的时候就不用请求服务器了，当然这个是有缓存时间的，当过了这个缓存时间，就会被清理掉。这种方式适合不经常变动的资源。如果资源修改频繁的话我们也可以通过Expires定义缓存过期时间，它会和服务器文件最后更新的时间做一个对比，如果没有变化，就会返回304状态码，如果有修改，则直接从服务器重新下载，返回状态码200

**2	准备工作**

在root目录下创建data文件夹，在data中创建两个文件夹存放静态资源，分别是www和images

**3	具体配置**



这个就是autoindex on的作用了，列出当前文件夹的内容

![image-20210320130336937](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320130336937.png)





## 高可用

为了防止Ningx宕机，我们可以准备一台备用的Nginx服务器，这样就算主服务器宕机了，工作也是照常执行，这就是高可用性

![image-20210320163627476](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320163627476.png)

**为什么需要虚拟ip呢**：可以想一下，如果主机挂了，要想转到备机，我们是不是需要访问备机的ip，由于我们之前对外暴露的是主机的ip，那么这时候请求的还是主机的ip，就会发生找不到的情况，所以需要一个虚拟ip，由这个ip来对我们实际的ip进行映射



**1	准备工作**

①需要两台服务器

②在两台服务器上安装nginx

③在两台服务器中安装keepalived

keepalived负责检查nginx服务器是否存活

**通过命令行来进行安装**

官方地址 [keepalived 下载地址](https://links.jianshu.com/go?to=https%3A%2F%2Fwww.keepalived.org%2Fdownload.html)，选择指定的版本后我们就开始下载

```shell
wget https://www.keepalived.org/software/keepalived-2.2.2.tar.gz
```

解压缩

```shell
tar -zxvf keepalived-2.2.2.tar.gz
```

进行configure配置：

```shell
./configure
```

编译安装

```shell
make && make install
```

使用yum install keepalived -y

**2	配置keepalived**

我们安装完就可以在/etc/keepalived目录下看到keepalived的配置文件了

![image-20210320215455054](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210320215455054.png)

在src目录下创建一个脚本文件nginx_check.sh

```shell
#!/bin/bash
A=`ps -C nginx -no-header |wc -1`
if [ $A -eq 0 ];then
    /usr/local/nginx/sbin/nginx
    sleep 2
    if [ `ps -C nginx --no-header |wc -1` -eq 0 ];then
        killall keepalived
    fi  
fi
```

**3	然后通过systemctl start keepalived启动keepalived**

启动nginx

然后通过我们定义好的虚拟ip访问，结果显示是可以的，我们就配置好了









# nginx 的原理与配置  

**1	master&worker**  

有两个进程，一个是mester，一个是worker，mester主要负责分配和管理，worker做任务，worker会有多个，他们之间是互相竞争的关系，他们会争抢做任务

![image-20210321001110199](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210321001110199.png)



**2	master-workers 的机制的好处**  

首先， 对于每个 worker 进程来说， 独立的进程， 不需要加锁， 所以省掉了锁带来的开销， 同时在编程以及问
题查找时， 也会方便很多。
其次， 采用独立的进程， 可以让互相之间不会影响， 一个进程退出后， 其它进程还在工作， 服务不会中断， master
进程则很快启动新的 worker 进程。 当然， worker 进程的异常退出， 肯定是程序有 bug 了， 异常退出， 会导致当前
worker 上的所有请求失败， 不过不会影响到所有请求， 所以降低了风险。  

**3	需要设置多少个 worker**
Nginx 同 redis 类似都采用了 io 多路复用机制， 每个 worker 都是一个独立的进程， 但每个进程里只有一个主线
程， 通过异步非阻塞的方式来处理请求， 即使是千上万个请求也不在话下。 每个 worker 的线程可以把一个 cpu
的性能发挥到极致。
所以 worker 数和服务器的 cpu 数相等是最为适宜的。 设少了会浪费 cpu， 设多了会造成 cpu 频繁切换上下文带
来的损耗。

**4	连接数 worker_connection**
这个值是表示每个 worker 进程所能建立连接的最大值， 所以， 一个 nginx 能建立的最大连接数， 应该是
worker_connections * worker_processes。 当然， 这里说的是最大连接数， 对于 HTTP 请求本地资源来说， 能够支持的
最大并发数量是 worker_connections * worker_processes， 如果是支持 http1.1 的浏览器每次访问要占两个连接， 所以
普通的静态访问最大并发数是： worker_connections * worker_processes /2， 而如果是 HTTP 作为反向代理来说， 最
大并发数量应该是 worker_connections * worker_processes/4。 因为作为反向代理服务器， 每个并发会建立与客户端
的连接和与后端服务的连接， 会占用两个连接  







