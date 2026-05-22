# Docker概述

## Docker为什么出现？

我们知道产品的上线分为开发和运维，我们开发好的一个项目让运维部署上去。

但是环境的配置是十分的麻烦的，我们开发的时候在本机上搭建好的环境到了上线环境又要部署一次，这就会消耗非常多的精力。

docker的出现解决了这个问题，它将我们的项目和环境一起打包，然后进行上线。省去很多的步骤和精力

Docker的思想来自于集装箱，我们在部署环境的时候多个应用可能会造成端口冲突，docker避免了这种情况，集装箱的思想就是隔离，他们是相互隔离开的，互相不影响。





## Docker能干什么？

### 虚拟化技术

我们开发的时候，项目部署是在虚拟机上部署的，当我们需要搭建集群的时候就会非常的麻烦，我们需要的虚拟机就会变多，我们的虚拟机占用的资源非常多，对性能不好的电脑不友好，启动也是非常的慢

![image-20210413213540786](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210413213540786.png)

我们可以看到虚拟机的应用是搭建在内核之上的



### 容器化技术

![image-20210413213841000](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210413213841000.png)

**比较Docker和虚拟机技术的不同**：

-   传统虚拟机，虚拟出一条硬件，运行一个完整的操作系统，然后在这个系统上安装和运行软件
-   容器内的应用直接运行在宿主机的内容，容器是没有自己的内核的，也没有虚拟我们的硬件，所以就轻便了
-   每个容器间是互相隔离，每个容器内都有一个属于自己的文件系统，互不影响

### docker的名词解释

**镜像**：是一种文件存储形式，是冗余的一种类型，一个磁盘上的数据在另一个磁盘上存在一个完全相同的副本即为镜像

**容器**：可以理解成是一个简单的linux系统，我们可以独立运行一个或一组应用

**仓库**：仓库就是用来存储镜像的地方，相当于是一个商店，我们可以从仓库中获取镜像

# Docker的安装

官网：https://www.docker.com/

官方文档：https://docs.docker.com/

官方仓库：https://registry.hub.docker.com/

因为docker的官方仓库是在外国的，网速会比较慢，所以我们一般都是使用国内的，这里使用阿里云仓库



## 安装

1	查看内核版本，必须是3以上的才能使用最新版的docker

```shell
[root@2019040740 ~]# uname -r
4.18.0-147.5.1.el8_1.x86_64
```

2	环境查看

```shell
[root@2019040740 ~]# cat /etc/os-release 
NAME="CentOS Linux"
VERSION="8"
ID="centos"
ID_LIKE="rhel fedora"
VERSION_ID="8"
PLATFORM_ID="platform:el8"
PRETTY_NAME="CentOS Linux 8"
ANSI_COLOR="0;31"
CPE_NAME="cpe:/o:centos:centos:8"
HOME_URL="https://centos.org/"
BUG_REPORT_URL="https://bugs.centos.org/"
CENTOS_MANTISBT_PROJECT="CentOS-8"
CENTOS_MANTISBT_PROJECT_VERSION="8"
```

3	根据官方文档来进行安装

1.  卸载旧版本

    ```shell
    yum remove docker \
                      docker-client \
                      docker-client-latest \
                      docker-common \
                      docker-latest \
                      docker-latest-logrotate \
                      docker-logrotate \
                      docker-engine
    ```

2.  需要的安装包

    ```shell
    yum install -y yum-utils
    ```

3.  设置镜像仓库

    ```shell
    # 官方的仓库（不推荐使用，慢）
    yum-config-manager \
        --add-repo \
        https://download.docker.com/linux/centos/docker-ce.repo
    ```

    ```shell
    # 阿里云的镜像仓库（推荐）
    yum-config-manager \
        --add-repo \
        http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
    ```

4.  更新软件包索引

    ```shell
    yum makecache fast
    ```

5.  安装docker

    ```shell
    # docker-ce是社区版的 docker-ee是企业版的
    yum install docker-ce docker-ce-cli containerd.io -y
    ```

6.  启动docker

    ```shell
    systemctl start docker
    ```

7.  测试docker是否安装成功

    ```shell
    docker version
    ```

8.  测试hello-world

    ```shell
    docker run hello-world
    ```

    ![image-20210414205331685](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210414205331685.png)

    ```shell
    # 查看我们拉下来的镜像
    docker images
    ```



## 卸载docker

```shell
# 卸载依赖
yum remove docker-ce docker-ce-cli containerd.io
# 2、删除资源
rm -rf /var/lib/docker
rm -rf /var/lib/containerd
# /var/lib/docker 是docker的默认工作路径
```



## 镜像加速

阿里云镜像加速

1	登录阿里云到后台找到容器镜像加速

![image-20210414213047641](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210414213047641.png)

2	找到镜像加速器

![image-20210414213131491](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210414213131491.png)

3	配置使用

```shell
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://ws52ti5a.mirror.aliyuncs.com"]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```





## helloword的运行原理

![image-20210414214411161](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210414214411161.png)



## 底层原理

**docker是怎么工作的？**

Docker是一个CS结构的系统，docker的守护进程运行在主机上。通过socker客户端访问！DockerServer接收到Docker-Client的指令就会执行这个命令

![image-20210414214848212](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210414214848212.png)



**为什么Docker会比虚拟机快？**

-   (1)docker有着比虚拟机更少的抽象层

    由于docker不需要Hypervisor(虚拟机)实现硬件资源虚拟化,运行在docker容器上的程序直接使用的都是实际物理机的硬件资源。因此在CPU、内存利用率上docker将会在效率上有明显优势。

-   (2)docker利用的是宿主机的内核,而不需要加载操作系统OS内核

    当新建一个容器时,docker不需要和虚拟机一样重新加载一个操作系统内核。进而避免引寻、加载操作系统内核返回等比较费时费资源的过程,当新建一个虚拟机时,虚拟机软件需要加载OS,返回新建过程是分钟级别的。而docker由于直接利用宿主机的操作系统,则省略了返回过程,因此新建一个docker容器只需要几秒钟。







# Docker的常用命令

## 帮助启动命令

```sh
启动docker： systemctl start docker
停止docker： systemctl stop docker
重启docker： systemctl restart docker
查看docker状态： systemctl status docker
开机启动： systemctl enable docker
```

```shell
docker version		# docker的版本信息
docker info 		# 显示docker的系统信息，包括镜像和容器的数量
docker 命令 --help   # 帮助命令
```

命令文档地址：https://docs.docker.com/reference/





## 镜像命令

### **查看所有本地主机上的镜像**

```shell
[root@2019040740 nginx]# docker images
REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
nginx        latest    62d49f9bab67   40 hours ago   133MB

# 解释
REPOSITORY	 镜像的仓库源
TAG		     镜像的标签
IMAGE ID	 镜像的id
CREATED		 镜像的创建时间
SIZE		 镜像的大小

# 可选项
  -a, --all             # 列出所有镜像
  -q, --quiet           # 只显示镜像的id
```



### **docker搜索镜像**

```shell
[root@2019040740 nginx]# docker search mysql
NAME                              DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
mysql                             MySQL is a widely used, open-source relation…   10748     [OK]       
mariadb                           MariaDB Server is a high performing open sou…   4047      [OK]       

# 可选项，通过收藏量来进行过滤
--filter=STARS=3000	 # 搜索出来的镜像就是STARS大于3000的

[root@2019040740 nginx]# docker search mysql --filter=STARS=3000
NAME      DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
mysql     MySQL is a widely used, open-source relation…   10748     [OK]       
mariadb   MariaDB Server is a high performing open sou…   4047      [OK]
```



### 查看镜像/容器/数据卷所占的空间

```sh
[root@localhost ~]# docker system df
TYPE            TOTAL     ACTIVE    SIZE      RECLAIMABLE
Images          1         0         231.3MB   231.3MB (100%)
Containers      0         0         0B        0B
Local Volumes   0         0         0B        0B
Build Cache     0         0         0B        0B
```





### **docker pull**  下载镜像

```shell
# 下载镜像 docker pull 镜像名[:tag]
[root@2019040740 nginx]# docker pull mysql
Using default tag: latest	# 如果不写tag，默认就是下载最新的版本
latest: Pulling from library/mysql
f7ec5a41d630: Already exists 	# 分层下载，docker image的核心	联合文件系统
9444bb562699: Already exists 
6a4207b96940: Already exists 
181cefd361ce: Already exists 
8a2090759d8a: Already exists 
15f235e0d7ee: Already exists 
d870539cd9db: Already exists 
5726073179b6: Pull complete 
eadfac8b2520: Pull complete 
f5936a8c3f2b: Pull complete 
cca8ee89e625: Pull complete 
6c79df02586a: Pull complete 
Digest: sha256:6e0014cdd88092545557dee5e9eb7e1a3c84c9a14ad2418d5f2231e930967a38 # 签名
Status: Downloaded newer image for mysql:latest
docker.io/library/mysql:latest	# 真实地址，和docker pull mysql是等价的
```

说明：也可以指定版本下载，但是得是docker仓库中有才能下载到本地



### **docker rmi 删除镜像**

```shell
docker rmi -f 容器ID				  # 删除指定ID的容器
docker rmi -f 容器ID 容器ID 容器ID 	# 删除多个容器
docker rmi -f $(docker images -aq) 	 # 删除全部的容器
```





## 容器命令

**说明**：有了镜像才能创建容器，linux，下载一个centos镜像来测试练习

```shell
docker pull centos
```

### **新建容器并启动**

```shell
docker run [可选参数] image

# 参数说明
--name="Name" 		 容器名字	不同的名字区分容器
-d			   	    后台方式运行
-it			   	    使用交互方式运行，进入容器中查看内容
-P(大)			   指定容器端口	-p	8080:8080
	-p	ip:主机端口:容器端口
	-p	主机端口:容器端口
	-p  容器端口
-p(小)			   随机指定端口

# 测试，启动进入容器
[root@2019040740 nginx]# docker run -it centos /bin/bash
[root@2e2b6e982a77 /]# ls
bin  etc   lib	  lost+found  mnt  proc  run   srv  tmp  var
dev  home  lib64  media       opt  root  sbin  sys  usr

# 从容器中退出
exit
```



### **列出所有的运行的容器**

```shell
# docker ps命令
-n		# 显示最近n个创建的容器。
-a		# 列出当前正在运行的容器和历史运行过的容器
-n=?	# 显示最近创建的容器，比如？为1，那么就会显示最近创建的1条数据
-q		# 只显示容器的编号，可以和-a组合使用
```



### **退出容器**

```shell
exit		 # 直接容器停止并退出
ctrl + P + Q  # 容器不停止退出
```



### **删除容器**

```shell
docker rm 容器ID				# 删除指定的容器，不能删除正在运行的容器，如果要删除，可以加上-f参数强制删除
docker rm -f $(docker ps -aq)  # 删除所有的容器
docker ps -a -q | xargs docker rm 	# 删除所有的容器 
```



### **启动和停止容器的操作**

```shell
docker start 容器ID		# 启动容器
docker restart 容器ID		# 重启容器
docker stop	容器ID	    # 停止当前正在运行的容器
docker kill 容器ID		# 停止当前正在运行的容器
```





## 常用的其他命令

### **后台启动容器**

```shell
# 命令  docker run -d 镜像名
[root@2019040740 ~]# docker run -d centos

# 问题docker ps，发现centos停止了

# 常见的坑：docker容器使用后台运行，就必须要有一个前台程序，docker发现没有前台或者对外应用，就会自动停止
```



### **查看日志**

```shell
docker logs -f -t --tail 容器

# 没有日志时，我们可以自己编写一段shell脚本
"while true;dp echo xiaozhi;sleep 1;done"

[root@2019040740 /]# docker run -d centos /bin/sh -c "while true;do echo xiaozhi;sleep 1;done"
12c4a0570f8e18688ab2dad9c9eeff947f00c72c582d5f919ed1a3ba236c44e9

# 显示日志
-tf			   # 显示日志
--tail	number	# 要显示日志条数

[root@2019040740 /]# docker logs -tf --tail 10 b9e46c5d7788
2021-04-15T12:35:49.994327916Z xiaozhi
2021-04-15T12:35:50.996513785Z xiaozhi
2021-04-15T12:35:51.998962898Z xiaozhi
...
```



### **查看容器中进程信息**

```shell
# docker top 容器ID
[root@2019040740 /]# docker top 12c4a0570f8e
UID                 PID                 PPID                C                   STIME               TTY                 TIME                CMD
root                33217               33196               0                   20:35              
root                33635               33217               0                   20:38  
```



### **查看镜像的原数据**

```shell
# docker inspect 镜像ID

[root@2019040740 /]# docker inspect 12c4a0570f8e
[
    {
        "Id": "12c4a0570f8e18688ab2dad9c9eeff947f00c72c582d5f919ed1a3ba236c44e9",
        "Created": "2021-04-15T12:35:36.270875412Z",
        "Path": "/bin/sh",
        "Args": [
            "-c",
            "while true;do echo xiaozhi;sleep 1;done"
        ],
        "State": {
            "Status": "running",
            "Running": true,
            "Paused": false,
            "Restarting": false,
            "OOMKilled": false,
            "Dead": false,
            "Pid": 33217,
            "ExitCode": 0,
            "Error": "",
            "StartedAt": "2021-04-15T12:35:36.928765067Z",
            "FinishedAt": "0001-01-01T00:00:00Z"
......
```



### 进入当前运行的容器

```shell
# 我们通常使用后台运行的方式运行的，需要进入容器，修改一些配置

# 命令
docker exec -it 容器ID /bin/bash

[root@2019040740 /]# docker exec -it 12c4a0570f8e /bin/bash

# 方式二
docker attach 容器ID  

[root@2019040740 ~]# docker attach ca915a27f353

# 两种方式的区别：
# exec相当于是开启一个新的终端
# attach进入当正在运行终端
```



### 从容器内拷贝文件到主机上

```shell
docker cp 容器ID:容器内路径	目的的主机路径
docker cp 主机路径	容器ID:容器内的路径			# 主机文件考别到容器中

# 拷贝是一个手动过程，未来我们可以使用-v卷技术，实现同步
```





### 导入和导出容器

-   export 导出容器的内容留作为一个tar归档文件[对应import命令]
-   import 从tar包中的内容创建一个新的文件系统再导入为镜像[对应export]





## 常用命令

![image-20230613141708315](E:\学习笔记\img\img01\image-20230613141708315.png) 

-   attach   Attach to a running container         # 当前 shell 下 attach 连接指定运行镜像
-   build   Build an image from a Dockerfile        # 通过 Dockerfile 定制镜像
-   commit   Create a new image from a container changes  # 提交当前容器为新的镜像
-   cp     Copy files/folders from the containers filesystem to the host path  #从容器中拷贝指定文件或者目录到宿主机中
-   create   Create a new container             # 创建一个新的容器，同 run，但不启动容器
-   diff    Inspect changes on a container's filesystem  # 查看 docker 容器变化
-   events   Get real time events from the server      # 从 docker 服务获取容器实时事件
-   exec    Run a command in an existing container     # 在已存在的容器上运行命令
-   export   Stream the contents of a container as a tar archive  # 导出容器的内容流作为一个 tar 归档文件[对应 import ]
-   history  Show the history of an image          # 展示一个镜像形成历史
-   images   List images                  # 列出系统当前镜像
-   import   Create a new filesystem image from the contents of a tarball # 从tar包中的内容创建一个新的文件系统映像[对应export]
-   info    Display system-wide information        # 显示系统相关信息
-   inspect  Return low-level information on a container  # 查看容器详细信息
-   kill    Kill a running container            # kill 指定 docker 容器
-   load    Load an image from a tar archive        # 从一个 tar 包中加载一个镜像[对应 save]
-   login   Register or Login to the docker registry server   # 注册或者登陆一个 docker 源服务器
-   logout   Log out from a Docker registry server      # 从当前 Docker registry 退出
-   logs    Fetch the logs of a container         # 输出当前容器日志信息
-   port    Lookup the public-facing port which is NAT-ed to PRIVATE_PORT   # 查看映射端口对应的容器内部源端口
-   pause   Pause all processes within a container     # 暂停容器
-   ps     List containers                # 列出容器列表
-   pull    Pull an image or a repository from the docker registry server  # 从docker镜像源服务器拉取指定镜像或者库镜像
-   push    Push an image or a repository to the docker registry server   # 推送指定镜像或者库镜像至docker源服务器
-   restart  Restart a running container          # 重启运行的容器
-   rm     Remove one or more containers         # 移除一个或者多个容器
-   rmi    Remove one or more images    # 移除一个或多个镜像[无容器使用该镜像才可删除，否则需删除相关容器才可继续或 -f 强制删除]
-   run    Run a command in a new container        # 创建一个新的容器并运行一个命令
-   save    Save an image to a tar archive         # 保存一个镜像为一个 tar 包[对应 load]
-   search   Search for an image on the Docker Hub     # 在 docker hub 中搜索镜像
-   start   Start a stopped containers           # 启动容器
-   stop    Stop a running containers           # 停止容器
-   tag    Tag an image into a repository         # 给源中镜像打标签
-   top    Lookup the running processes of a container  # 查看容器中运行的进程信息
-   unpause  Unpause a paused container           # 取消暂停容器
-   version  Show the docker version information      # 查看 docker 版本号
-   wait    Block until a container stops, then print its exit code  # 截取容器停止时的退出状态值





# 部署练习

## nginx

```shell
# 拉取镜像
[root@2019040740 ~]# docker pull nginx
# 运行测试
[root@2019040740 ~]# docker run -d -p 3344:80 --name nginx01 nginx
# 进入容器
[root@2019040740 ~]# docker exec -it 64abeaed0623 /bin/bash
root@64abeaed0623:/# ls
bin   docker-entrypoint.d   home   media  proc	sbin  tmp
boot  docker-entrypoint.sh  lib    mnt	  root	srv   usr
dev   etc		    lib64  opt	  run	sys   var
```



## tomcat

```shell
# 进入到容器中
[root@2019040740 ~]# docker run -d -p 8088:8088 tomcat:9.0
9028f6117cce28f4167397e1b0178b542d7413149ae8e57261449fa03fb953cd
[root@2019040740 ~]# docker exec -it 9028f6117cce /bin/bash
root@9028f6117cce:/usr/local/tomcat# ls
BUILDING.txt	 README.md	conf		temp
CONTRIBUTING.md  RELEASE-NOTES	lib		webapps
LICENSE		 RUNNING.txt	logs		webapps.dist
NOTICE		 bin		native-jni-lib	work
# 把webapps.dist下的所有文件拷贝到webapps目录下
root@9028f6117cce:/usr/local/tomcat# cp -r webapps.dist/* webapps
root@9028f6117cce:/usr/local/tomcat# cd webapps
root@9028f6117cce:/usr/local/tomcat/webapps# ls
ROOT  docs  examples  host-manager  manager
root@9028f6117cce:/usr/local/tomcat/webapps# 
```

**进入到容器中发现问题**：1、linux命令少了		2、webapps为空

**解释**：这是因为阿里云的原因，它默认是最小的镜像，所有不必要的都要提出掉，保证最小运行环境

**解决**：把webapps.dist下的所有文件拷贝到webapps目录下即可，我们的网页就可以正常进行访问了



## elasticsearch

问题：①es暴露的端口多	②es比较耗内存	③es数据一般需要放到安全目录

配置比较低的机子如果没有限制的情况下启动是会很卡的，因为内存不太够

```shell
# 拉取镜像
docker pull elasticsearch:7.6.2
# 运行
docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.6.2
# 测试是否启动
[root@2019040740 ~]# curl localhost:9200
{
  "name" : "9e1796320495",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "GfAXnIHtSXGG5wRfhMVcAQ",
  "version" : {
    "number" : "7.6.2",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "ef48eb35cf30adf4db14086e8aabd07ef6fb113f",
    "build_date" : "2020-03-26T06:34:37.794943Z",
    "build_snapshot" : false,
    "lucene_version" : "8.4.0",
    "minimum_wire_compatibility_version" : "6.8.0",
    "minimum_index_compatibility_version" : "6.0.0-beta1"
  },
  "tagline" : "You Know, for Search"
}
# 启动成功，我们查看一下内存使用情况
[root@2019040740 ~]# docker stats
```

我么可以看到内存的消耗非常大，所以我们要限制一下它的内存使用量

```shell
# 先停止之前开启的
[root@2019040740 ~]# docker stop 9e1796320495
# 删除镜像
[root@2019040740 ~]# docker rm 9e1796320495
# 启动增加使用限制的elasticsearch，修改配置文件
# -e 环境配置修改
# --net somenetwork 网络配置
docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e ES_JAVA_OPTS="-Xms64m -Xmx512m" elasticsearch:7.6.2
# 再次查看内存使用情况
```

![image-20210421171911380](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421171911380.png)

我们可以看到消耗变得很小了





## docker图形化管理工具

**Portainer 可视化面板安装**

-   portainer(目前阶段先使用这个)

    ```shell
    docker run -d -p 8080:9000 \
    --restart=always -v /var/run/docker.sock:/var/run/docker.sock --privileged=true portainer/portainer
    ```

-   Rancher(CI/CD再用)

提供一个后台面板给我们进行操作

```shell
# 安装命令
[root@iz2zeak7sgj6i7hrb2g862z ~]# docker run -d -p 8080:9000 \
> --restart=always -v /var/run/docker.sock:/var/run/docker.sock --privileged=true portainer/portainer
```

然后输入http://IP:8080/进行访问即可

①注册

②选择本地的

![image-20210421172954195](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421172954195.png)





# 镜像原理

## 文件联合系统

### 镜像是什么

镜像是一种轻量级、可执行的独立软件保，用来打包软件运行环境和基于运行环境开发的软件，他包含运行某个软件所需的所有内容，包括代码、运行时库、环境变量和配置文件。

**获取镜像**：①远程仓库下载	②别人拷贝给你	③自己制作镜像



## Docker镜像加载原理

**UnionFs （联合文件系统）**

Docker镜像加载原理

Union文件系统（UnionFs）是一种分层、轻量级并且高性能的文件系统，他支持对文件系统的修改作为一次提交来一层层的叠加，同时可以将不同目录挂载到同一个虚拟文件系统下（ unite several directories into a single virtual filesystem)。Union文件系统是 Docker镜像的基础。镜像可以通过分层来进行继承，基于基础镜像（没有父镜像），可以制作各种具体的应用镜像
特性：一次同时加载多个文件系统，但从外面看起来，只能看到一个文件系统，联合加载会把各层文件系统叠加起来，这样最终的文件系统会包含所有底层的文件和目录。



### **Docker镜像加载原理**

docker的镜像实际上由一层一层的文件系统组成，这种层级的文件系统UnionFS。
boots(boot file system）主要包含 **bootloader**和 **Kernel**, **bootloader**主要是引导加 kernel, Linux刚启动时会加bootfs文件系统，在 Docker镜像的最底层是 boots。这一层与我们典型的Linux/Unix系统是一样的，包含boot加載器和内核。当boot加载完成之后整个内核就都在内存中了，此时内存的使用权已由 bootfs转交给内核，此时系统也会卸载bootfs。
rootfs（root file system),在 bootfs之上。包含的就是典型 Linux系统中的/dev,/proc,/bin,/etc等标准目录和文件。 rootfs就是各种不同的操作系统发行版，比如 Ubuntu, Centos等等。

### **为什么我们的容器这么小**

对于个精简的OS,rootfs可以很小，只需要包合最基本的命令，工具和程序库就可以了，因为底层直接用Host的kernel，自己只需要提供rootfs就可以了。由此可见对于不同的Linux发行版， boots基本是一致的， rootfs会有差別，因此不同的发行版可以公用bootfs.



**说明**：linux的启动页是引导加载bootfs，加载完成之后就会存在内存中，然后对bootfs进行卸载，那么内存的使用权就交到了我们的各大操作系统来进行操作，比如centos、ubuntu等等，所以docker底层使用的就是主机的内核，所以容器可以秒级启动



### 分层理解

我们从仓库上下载镜像是一层层下载到本地的，那么为什么要采取这种方式呢，其实是为了资源共享，如果有相同的层我们就可以进行复用，减少数据的冗余。

**查看镜像分层的方式可以通过docker image inspect 命令**

```shell
[root@2019040740 ~]# docker image inspect centos
[
    {
        "Id": "sha256:300e315adb2f96afe5f0b2780b87f28ae95231fe3bdd1e16b9ba606307728f55",
        "RepoTags": [
            "centos:latest"
        ],
        "RepoDigests": [
            "centos@sha256:5528e8b1b1719d34604c87e11dcd1c0a20bedf46e83b5632cdeac91b8c04efc1"
        ],
        "Parent": "",
        "Comment": "",
        "Created": "2020-12-08T00:22:53.076477777Z",
        "Container": "395e0bfa7301f73bc994efe15099ea56b8836c608dd32614ac5ae279976d33e4",
        "ContainerConfig": {
            "Hostname": "395e0bfa7301",
            "Domainname": "",
            "User": "",
            "AttachStdin": false,
            "AttachStdout": false,
            "AttachStderr": false,
            "Tty": false,
            "OpenStdin": false,
            "StdinOnce": false,
            "Env": [
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            ],
            "Cmd": [
                "/bin/sh",
                "-c",
                "#(nop) ",
                "CMD [\"/bin/bash\"]"
            ],
            "Image": "sha256:6de05bdfbf9a9d403458d10de9e088b6d93d971dd5d48d18b4b6758f4554f451",
            "Volumes": null,
            "WorkingDir": "",
            "Entrypoint": null,
            "OnBuild": null,
            "Labels": {
                "org.label-schema.build-date": "20201204",
                "org.label-schema.license": "GPLv2",
                "org.label-schema.name": "CentOS Base Image",
                "org.label-schema.schema-version": "1.0",
                "org.label-schema.vendor": "CentOS"
            }
        },
        "DockerVersion": "19.03.12",
        "Author": "",
        "Config": {
            "Hostname": "",
            "Domainname": "",
            "User": "",
            "AttachStdin": false,
            "AttachStdout": false,
            "AttachStderr": false,
            "Tty": false,
            "OpenStdin": false,
            "StdinOnce": false,
            "Env": [
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            ],
            "Cmd": [
                "/bin/bash"
            ],
            "Image": "sha256:6de05bdfbf9a9d403458d10de9e088b6d93d971dd5d48d18b4b6758f4554f451",
            "Volumes": null,
            "WorkingDir": "",
            "Entrypoint": null,
            "OnBuild": null,
            "Labels": {
                "org.label-schema.build-date": "20201204",
                "org.label-schema.license": "GPLv2",
                "org.label-schema.name": "CentOS Base Image",
                "org.label-schema.schema-version": "1.0",
                "org.label-schema.vendor": "CentOS"
            }
        },
        "Architecture": "amd64",
        "Os": "linux",
        "Size": 209348104,
        "VirtualSize": 209348104,
        "GraphDriver": {
            "Data": {
                "MergedDir": "/var/lib/docker/overlay2/6e4a57fbb0deb402b63d13e2f64cd75374311762cd65ba7990cca3b563751fcb/merged",
                "UpperDir": "/var/lib/docker/overlay2/6e4a57fbb0deb402b63d13e2f64cd75374311762cd65ba7990cca3b563751fcb/diff",
                "WorkDir": "/var/lib/docker/overlay2/6e4a57fbb0deb402b63d13e2f64cd75374311762cd65ba7990cca3b563751fcb/work"
            },
            "Name": "overlay2"
        },
        "RootFS": {
            "Type": "layers",
            "Layers": [
                "sha256:2653d992f4ef2bfd27f94db643815aa567240c37732cae1405ad1c1309ee9859"
            ]
        },
        "Metadata": {
            "LastTagTime": "0001-01-01T00:00:00Z"
        }
    }
]

```



**说明**：所有的Docker镜像都起始于一个基础镜像，当进行修改或者增加新的内容时，就会在当前镜像层上，创建新的镜像层来保存本次的修改和新增，这就类似于快照了。在添加额外镜像层的同时，镜像始终保持是当前所有镜像的组合.

![img](https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fimages2017.cnblogs.com%2Fblog%2F1190037%2F201802%2F1190037-20180203180342031-1621628053.png&refer=http%3A%2F%2Fimages2017.cnblogs.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621601143&t=336befe0ebc9adb61006ba40a9a861a2)

这个就比较形象的说明了分层的概念

**注意**：镜像是基于文件系统实现的，如果有旧版本和新版本两个版本，旧版本中如果存在新版本已经更新的文件，那么新版的文件会覆盖掉旧版的文件

​	![image-20210421210140304](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421210140304.png)

Docker通过存储引擎（新版本采用快照机制）的方式来实现镜像层堆栈，并保证多镜像层对外展示为统一的文件系统

下图展示了与系统显示相同的三层镜像。所有镜像层堆并合井，对外提供统一的视图。

![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2NoZW5nY29kZXgvY2xvdWRpbWcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDUxNTE2NTU1NzgwNy5wbmc?x-oss-process=image/format,png)



**启动镜像**

![image-20210421205241878](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421205241878.png)





## commit镜像

```shell
# docker commit：提交容器成为一个新的副本
# 命令和git的原理类似
docker commit -m="描述信息" -a="作者" 容器id 目标镜像名:[版本TAG]
```

实战测试

```shell
# 1、启动一个默认的tomcat

[root@iz2zeak7sgj6i7hrb2g862z ~]# docker run -d -p 8080:8080 tomcat
de57d0ace5716d27d0e3a7341503d07ed4695ffc266aef78e0a855b270c4064e

# 2、发现这个默认的tomcat 是没有webapps应用，官方的镜像默认webapps下面是没有文件的！

#docker exec -it 容器id /bin/bash
[root@iz2zeak7sgj6i7hrb2g862z ~]# docker exec -it de57d0ace571 /bin/bash
root@de57d0ace571:/usr/local/tomcat# 

# 3、从webapps.dist拷贝文件进去webapp

root@de57d0ace571:/usr/local/tomcat# cp -r webapps.dist/* webapps
root@de57d0ace571:/usr/local/tomcat# cd webapps
root@de57d0ace571:/usr/local/tomcat/webapps# ls
ROOT  docs  examples  host-manager  manager

# 4、将操作过的容器通过commit调教为一个镜像！我们以后就使用我们修改过的镜像即可，而不需要每次都重新拷贝webapps.dist下的文件到webapps了，这就是我们自己的一个修改的镜像。

docker commit -m="描述信息" -a="作者" 容器id 目标镜像名:[TAG]
docker commit -a="kuangshen" -m="add webapps app" 容器id tomcat02:1.0

[root@iz2zeak7sgj6i7hrb2g862z ~]# docker commit -a="csp提交的" -m="add webapps app" de57d0ace571 tomcat02.1.0
sha256:d5f28a0bb0d0b6522fdcb56f100d11298377b2b7c51b9a9e621379b01cf1487e

[root@iz2zeak7sgj6i7hrb2g862z ~]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED             SIZE
tomcat02.1.0          latest              d5f28a0bb0d0        14 seconds ago      652MB
tomcat                latest              1b6b1fe7261e        5 days ago          647MB
nginx                 latest              9beeba249f3e        5 days ago          127MB
mysql                 5.7                 b84d68d0a7db        5 days ago          448MB
elasticsearch         7.6.2               f29a1ee41030        8 weeks ago         791MB
portainer/portainer   latest              2869fc110bf7        2 months ago        78.6MB
centos                latest              470671670cac        4 months ago        237MB
hello-world           latest              bf756fb1ae65        4 months ago        13.3kB
```


如果你想要保存当前容器的状态，就可以通过commit来提交，获得一个镜像，就好比我们我们使用虚拟机的快照。


如果你想要保存当前容器的状态，就可以通过commit来提交，获得一个镜像，就好比我们我们使用虚拟机的快照。







## 本地镜像推送到阿里云

上一节我们自己制作了一个镜像，使用 commit 命令提交到了本地，接下我们将自制的镜像推送到阿里云镜像仓库



### 推送镜像

首先我们需要在阿里云中常见一个镜像仓库

1.  在阿里云中开通 `容器镜像服务` 

2.  进入个人实例

    ![image-20230613174051800](E:\学习笔记\img\img01\image-20230613174051800.png) 

3.  创建一个命名空间

    ![image-20230613174155296](E:\学习笔记\img\img01\image-20230613174155296.png) 

4.  接着创建镜像仓库

    ![image-20230613174646743](E:\学习笔记\img\img01\image-20230613174646743.png) 

    ![image-20230613174657073](E:\学习笔记\img\img01\image-20230613174657073.png) 

仓库创建完成就可以按照文档来操作了~~~

![image-20230613175532195](E:\学习笔记\img\img01\image-20230613175532195.png)

点击管理按钮就可以看到对应的操作步骤了

```sh
1. 登录阿里云Docker Registry
$ docker login --username=xiaozhi_oss registry.cn-hangzhou.aliyuncs.com
用于登录的用户名为阿里云账号全名，密码为开通服务时设置的密码。

您可以在访问凭证页面修改凭证密码。

2. 从Registry中拉取镜像
$ docker pull registry.cn-hangzhou.aliyuncs.com/xiaozhi_oss/demo:[镜像版本号]
3. 将镜像推送到Registry
$ docker login --username=xiaozhi_oss registry.cn-hangzhou.aliyuncs.com
$ docker tag [ImageId] registry.cn-hangzhou.aliyuncs.com/xiaozhi_oss/demo:[镜像版本号]
$ docker push registry.cn-hangzhou.aliyuncs.com/xiaozhi_oss/demo:[镜像版本号]
请根据实际镜像信息替换示例中的[ImageId]和[镜像版本号]参数。

4. 选择合适的镜像仓库地址
从ECS推送镜像时，可以选择使用镜像仓库内网地址。推送速度将得到提升并且将不会损耗您的公网流量。

如果您使用的机器位于VPC网络，请使用 registry-vpc.cn-hangzhou.aliyuncs.com 作为Registry的域名登录。

5. 示例
使用"docker tag"命令重命名镜像，并将它通过专有网络地址推送至Registry。

$ docker images
REPOSITORY                                                         TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
registry.aliyuncs.com/acs/agent                                    0.7-dfb6816         37bb9c63c8b2        7 days ago          37.89 MB
$ docker tag 37bb9c63c8b2 registry-vpc.cn-hangzhou.aliyuncs.com/acs/agent:0.7-dfb6816
使用 "docker push" 命令将该镜像推送至远程。

$ docker push registry-vpc.cn-hangzhou.aliyuncs.com/acs/agent:0.7-dfb6816
```









# 容器数据卷

## 什么是容器数据卷

如果我们的数据保存在容器中，我们的容器删除了，数据也就删除了，这样是不行的，所以我们要将我们的数据保存在宿主机中，删除了容器，我们的数据也不会删除。那么容器数据卷就是用来进行数据同步的，将容器的目录挂在到宿主机上

**总结一句话：容器的持久化和同步操作！容器间也是可以数据共享的！**

![image-20210421211202048](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421211202048.png)



## 使用数据卷

**方式一**：直接使用命令挂载 -v

```shell
# docker run -it -v 主机目录:容器内目录  -p 主机端口:容器内端口
[root@2019040740 ~]# docker run -it -v /home/test:/home centos /bin/bash
# 宿主机同步的文件创建一个文件
[root@2019040740 test]# vim test.txt
# 可以看到
[root@e173ab1a4895 home]# ls
test.txt
```

也可以通过docker inspect 容器id 挂载的目录情况

![image-20210421211852639](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421211852639.png)



**再来测试！**

1、停止容器

2、宿主机修改文件

3、启动容器

4、容器内的数据依旧是同步的





## 实战：安装MySQL

-d 后台运行

 -p 端口映射

-P 随机端口

 -v 卷挂载 

-e 环境配置 

-- name 容器名字

```shell
# 启动镜像，挂载对应到对应的文件夹
docker run -d -p 331:3306 -v /home/mysql/conf:/etc/mysql/conf.d -v /home/mysql/data:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=123456 --name mysql mysql:5.7
```

使用工具连接

![image-20210421212305970](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421212305970.png)

我们在SQLyog上面创建test数据库，在容器中也会进行创建



```shell
# 删除我们的mysql容器，文件依然存在
[root@2019040740 ~]# docker rm $(docker ps -aq)
448da80a0e5f
e173ab1a4895
[root@2019040740 ~]# cd /home/mysql/data/
[root@2019040740 data]# ls
auto.cnf         client-key.pem  ib_logfile1         public_key.pem   test
ca-key.pem       ib_buffer_pool  mysql               server-cert.pem
ca.pem           ibdata1         performance_schema  server-key.pem
client-cert.pem  ib_logfile0     private_key.pem     sys
```





## 具名和匿名挂载

```shell
# 匿名挂载
-v 容器内路径!
$ docker run -d -P --name nginx01 -v /etc/nginx nginx

# 查看所有的volume(卷)的情况
$ docker volume ls    
DRIVER              VOLUME NAME # 容器内的卷名(匿名卷挂载)
local               21159a8518abd468728cdbe8594a75b204a10c26be6c36090cde1ee88965f0d0
local               b17f52d38f528893dd5720899f555caf22b31bf50b0680e7c6d5431dbda2802c	# 这就是匿名挂载
         
# 这里发现，这种就是匿名挂载，我们在 -v只写了容器内的路径，没有写容器外的路径！

# 具名挂载
[root@2019040740 ~]# docker run -d -P --name nginx02 -v juming-nginx:/etc/nginx nginx
5621d628aae4b68a6f18a139871582b300c00387fcb9a6abfdbd2da622fb9079

# 查看所有的volume(卷)的情况
$ docker volume ls                  
DRIVER              VOLUME NAME
local               21159a8518abd468728cdbe8594a75b204a10c26be6c36090cde1ee88965f0d0
local               b17f52d38f528893dd5720899f555caf22b31bf50b0680e7c6d5431dbda2802c
local               juming-nginx #多了一个名字

# 通过 -v 卷名：查看容器内路径
# 查看一下这个卷
$ docker volume inspect juming-nginx
[
    {
        "CreatedAt": "2020-05-23T13:55:34+08:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/juming-nginx/_data", #默认目录
        "Name": "juming-nginx",
        "Options": null,
        "Scope": "local"
    }
]

# 总结：匿名挂载就是一串代码，具名挂载就是由名字的
```

![image-20210421213741606](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210421213741606.png)

所有的docker容器内的卷，没有指定目录的情况下都是在**/var/lib/docker/volumes/自定义的卷名/_data（具名挂载）**下，**如果指定了目录（匿名挂在），docker volume ls 是查看不到的**。







## 拓展

```shell
# 通过 -v 容器内路径： ro rw 改变读写权限
ro #readonly 只读
rw #readwrite 可读可写
$ docker run -d -P --name nginx05 -v juming:/etc/nginx:ro nginx
$ docker run -d -P --name nginx05 -v juming:/etc/nginx:rw nginx

# ro 只要看到ro就说明这个路径只能通过宿主机来操作，容器内部是无法操作！
```





# 数据卷容器

## 多个容器实现同步

命名的容器挂载数据卷

![image-20210422111839814](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210422111839814.png)

```shell
# 启动三个容器进行测试
[root@2019040740 ~]# docker run -it --name centos01 xiaozhi/centos
[root@2316242a43b7 /]# ls
bin  home   lost+found	opt   run   sys  var
dev  lib    media	proc  sbin  tmp  volume01
etc  lib64  mnt		root  srv   usr  volume02

# 其余的两个容器继承这个父容器
# 这里要注意带上版本号
docker run -it --name centos02 --volumes-from centos01 xiaozhi/centos:latest
# 尝试使用不同的容器继承，发现也是可以的
docker run -it --name centos03 --volumes-from centos01 centos:latest
# 我们进入到父容器挂载的目录中创建一个文件
# 然后进入我们两个子容器中查看发现实现了同步
# 我们把父容器删除掉，发现我们挂在的卷依然还在
```

**结论**：**容器之间的配置信息的传递，数据卷容器的生命周期一直持续到没有容器使用为止**。

​			**但是一旦你持久化到了本地，这个时候，本地的数据是不会删除的**！



## MySQL实现同步

```shell
$ docker run -d -p 3306:3306 -v /home/mysql/conf:/etc/mysql/conf.d -v /home/mysql/data:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=123456 --name mysql01 mysql:5.7

$ docker run -d -p 3310:3306 -e MYSQL_ROOT_PASSWORD=123456 --name mysql02 --volumes-from mysql01  mysql:5.7
```









# Dockerfile

## Dockerfile是什么？

**Dockerfile 就是用来构建docker镜像的构建文件**！命令脚本！

通过这个**脚本可以生成镜像**，镜像是一层一层的，脚本是一个个的命令，每个命令都是一层！

**说明**：①创建一个Dockerfile文件，名字建议为Dockerfile

​		   ②文件中的内容： 指令(大写) + 参数

```shell
[root@2019040740 test]# vim dockerfile
FROM centos                     # 当前这个镜像是以centos为基础的
VOLUNE ["volume01","volume02"]  # 挂载卷的卷目录列表(多个目录)
CMD echo "----end---"           # 输出一下用于测试
CMD /bin/bash                   # 默认走bash控制台

# 构建出这个镜像 
-f dockerfile1 			# f代表file，指这个当前文件的地址(这里是当前目录下的dockerfile1)
-t caoshipeng/centos 	# t就代表target，指目标目录(注意caoshipeng镜像名前不能加斜杠‘/’)
. 						# 表示生成在当前目录下
[root@2019040740 ~]# docker build -f /home/test/dockerfile -t xiaozhi/centos .
Sending build context to Docker daemon  207.3MB
Step 1/4 : FROM centos
 ---> 300e315adb2f
Step 2/4 : VOLUME ["volume01","volume02"]
 ---> Running in 28978ae0c644
Removing intermediate container 28978ae0c644
 ---> 53244cf902ef
Step 3/4 : CMD echo "-----end-----"
 ---> Running in 99975a4f4c39
Removing intermediate container 99975a4f4c39
 ---> ea6beda5b0da
Step 4/4 : CMD /bin/bash
 ---> Running in aaed47168333
Removing intermediate container aaed47168333
 ---> d123138fb7ce
Successfully built d123138fb7ce
Successfully tagged xiaozhi/centos:latest
```

![image-20210422104348102](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210422104348102.png)

启动我们自己的容器可以看到挂载的两个目录

![image-20210422105113957](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210422105113957.png)

使用**docker inspect 容器ID** 查看卷挂载

![image-20210422105338700](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210422105338700.png)

我们测试一下这个目录是否同步

```shell
# 进入同步的目录
[root@2019040740 volumes]# cd /var/lib/docker/volumes/e519cf34150242cfc969cc89c54b7a83bf4514fbd43e78f0177e560cc522af40/_data
[root@2019040740 _data]# ls
[root@2019040740 _data]# touch test.txt
[root@2019040740 _data]# ls
test.txt
# 进入容器内部的目录进行查看
[root@e66a3bc22b8d /]# ls
bin  home   lost+found	opt   run   sys  var
dev  lib    media	proc  sbin  tmp  volume01
etc  lib64  mnt		root  srv   usr  volume02
[root@e66a3bc22b8d /]# cd volume01
[root@e66a3bc22b8d volume01]# ls
test.txt
```







## 构建和发布镜像

1、 编写一个dockerfile文件

2、 docker build 构建成为一个镜像

3、 docker run运行镜像

4、 docker push发布镜像（DockerHub 、阿里云仓库)

![img](https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fimg2020.cnblogs.com%2Fblog%2F1869289%2F202005%2F1869289-20200529090814461-1122968296.png&refer=http%3A%2F%2Fimg2020.cnblogs.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621656518&t=6c68026746ba4dc85f4d3e87926e7c33)

```shell
FROM				# from:基础镜像，一切从这里开始构建
MAINTAINER			# maintainer:镜像是谁写的， 姓名+邮箱
RUN					# run:镜像构建的时候需要运行的命令
	shell格式		RUN <命令行命令>
	exec格式		RUN ["可执行文件", "参数1", "参数2", "....参数n"]
ADD					# add:步骤，tomcat镜像，这个tomcat压缩包！添加内容 添加同目录
USER 				# 指定该镜像以什么样的用户去执行，如果都不指定，默认是root
WORKDIR				# workdir:镜像的工作目录
VOLUME				# volume:挂载的目录
EXPOSE				# expose:保留端口配置
CMD					# cmd:指定这个容器启动的时候要运行的命令，只有最后一个会生效，可被替代
ENTRYPOINT			# entrypoint:指定这个容器启动的时候要运行的命令，可以追加命令
ONBUILD				# onbuild:当构建一个被继承DockerFile这个时候就会运行onbuild的指令，触发指令
COPY				# copy:类似ADD，将我们文件拷贝到镜像中
ENV					# env:构建的时候设置环境变量！
```

![img](https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fimg2018.cnblogs.com%2Fcommon%2F874963%2F202002%2F874963-20200209160957298-684710888.png&refer=http%3A%2F%2Fimg2018.cnblogs.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621656877&t=cbee6bdcccc936fdd85ff50e9914840a)



### CMD 和 ENTRYPOINT区别

```shell
CMD					# 指定这个容器启动的时候要运行的命令，只有最后一个会生效，可被替代。
ENTRYPOINT			# 指定这个容器启动的时候要运行的命令，可以追加命令
```







## 实战测试

### 1	构建和启动镜像

我们可以模仿官方的dockerfile来构建自己的镜像

![image-20210422160728677](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210422160728677.png)

```shell
# 1、/home目录下创建dockerfile目录
[root@2019040740 home]# mkdir dockerfile

# 2、创建dockerfile-centos文件并编辑文件
FROM centos								# centos作为基础镜像
MAINTAINER xiaozhi<test@qq.com>	   # 作者信息

ENV MYPATH /usr/local					# 定义环境变量
WORKDIR $MYPATH		 					# 工作路径

RUN yum -y install vim					# 给官方原生的centos 增加 vim指令
RUN yum -y install net-tools			# 给官方原生的centos 增加 ifconfig命令
	
EXPOSE 80							# 对外暴露80端口

CMD echo $MYPATH					# 输出MYPATH路径
CMD echo "---end---"				
CMD /bin/bash						# 启动进入/bin/bash

# 3、通过这个文件构建镜像
[root@2019040740 dockerfile]# docker build -f dockerfile-centos -t mycentos:0.1 .

# 查看是否创建成功
[root@2019040740 dockerfile]# docker images
REPOSITORY            TAG          IMAGE ID       CREATED          SIZE
mycentos              0.1          8afe846e4650   59 seconds ago   291MB
```

**说明**：工作路径就是进入容器所在的目录

我们运行一下我们创建好的镜像，测试一下加入的功能是否可用

```shell
[root@2019040740 dockerfile]# docker run -it --name mycentos01 mycentos:0.1 
[root@be86aa009477 local]# pwd
/usr/local
[root@be86aa009477 local]# vim test.txt
[root@be86aa009477 local]# ifconfig
eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 172.17.0.5  netmask 255.255.0.0  broadcast 172.17.255.255
        ether 02:42:ac:11:00:05  txqueuelen 0  (Ethernet)
        RX packets 13  bytes 1126 (1.0 KiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        loop  txqueuelen 1000  (Local Loopback)
        RX packets 0  bytes 0 (0.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
# 我们发现我们加入的功能是可用的
```

通过docker history 镜像ID或者镜像名:[tag]查看镜像的构建信息

```shell
[root@2019040740 dockerfile]# docker history mycentos:0.1
IMAGE          CREATED         CREATED BY                                      SIZE      COMMENT
8afe846e4650   7 minutes ago   /bin/sh -c #(nop)  CMD ["/bin/sh" "-c" "/bin…   0B        
ab694a00c882   7 minutes ago   /bin/sh -c #(nop)  CMD ["/bin/sh" "-c" "echo…   0B        
adf724b4bcbf   7 minutes ago   /bin/sh -c #(nop)  CMD ["/bin/sh" "-c" "echo…   0B        
1eb8751e4580   7 minutes ago   /bin/sh -c #(nop)  EXPOSE 80                    0B        
c568ab7341b5   7 minutes ago   /bin/sh -c yum -y install net-tools             23.3MB    
b3de467c0b96   7 minutes ago   /bin/sh -c yum -y install vim                   58MB      
4940b65cb4cd   7 minutes ago   /bin/sh -c #(nop) WORKDIR /usr/local            0B        
abae7c6e1a8b   7 minutes ago   /bin/sh -c #(nop)  ENV MYPATH=/usr/local        0B        
e2cff74c14a2   7 minutes ago   /bin/sh -c #(nop)  MAINTAINER xiaozhi<159697…   0B        
300e315adb2f   4 months ago    /bin/sh -c #(nop)  CMD ["/bin/bash"]            0B        
<missing>      4 months ago    /bin/sh -c #(nop)  LABEL org.label-schema.sc…   0B        
<missing>      4 months ago    /bin/sh -c #(nop) ADD file:bd7a2aed6ede423b7…   209MB     
```

还可以通过docker tag 镜像ID或者镜像名:[tag] 新版本镜像名:[tag]  修改镜像的版本

如果没有指定版本，那么就会标注为是最新版

![image-20210422163244984](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210422163244984.png)











### 2	发布自己的镜像

```shell
# 1、先进行登录
docker login -u 你的用户名 -p

[root@2019040740 ~]# docker login -uxiaozhi123 
Password: 
WARNING! Your password will be stored unencrypted in /root/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store
Login Succeeded

# 2、提交镜像
docker push 容器ID或者容器名[tag]
```

**报错**：denied: requested access to the resource is denied

我们push容器的前面要跟我们的用户名

```shell
[root@2019040740 ~]# docker push xiaozhi123/mycentos:0.1	# 加上用户名才能push
```



### 阿里云仓库

使用官方的仓库比较慢，我们使用阿里云的仓库来push我们的镜像

登录阿里云，找到镜像仓库，按照官方文档的步骤来就可以了

```shell
docker login --username=xiaozhi_oss registry.cn-shenzhen.aliyuncs.com		# 登录阿里云
# 修改镜像信息
docker tag [ImageId] registry.cn-shenzhen.aliyuncs.com/xiaozhi_oss/xiaozhi:[镜像版本号]
[root@2019040740 ~]# docker tag bed8e6955e2b registry.cn-shenzhen.aliyuncs.com/xiaozhi_oss/mycentos:0.1

# 推送到仓库
docker push registry.cn-shenzhen.aliyuncs.com/xiaozhi_oss/xiaozhi:[镜像版本号]
[root@2019040740 ~]# docker push registry.cn-shenzhen.aliyuncs.com/xiaozhi_oss/mycentos:0.1
```





## tomcat实战

1	准备tomcat和jdk到当前目录下

2	编写dockerfile文件

```shell
FROM centos 										# 基础镜像centos
MAINTAINER cao<1165680007@qq.com>					# 作者
COPY README /usr/local/README 						# 复制README文件
ADD jdk-8u231-linux-x64.tar.gz /usr/local/ 			# 添加jdk，ADD 命令会自动解压
ADD apache-tomcat-9.0.35.tar.gz /usr/local/ 		# 添加tomcat，ADD 命令会自动解压
RUN yum -y install vim								# 安装 vim 命令
ENV MYPATH /usr/local 								# 环境变量设置 工作目录
WORKDIR $MYPATH

ENV JAVA_HOME /usr/local/jdk1.8.0_121 				# 环境变量： JAVA_HOME环境变量
ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar

ENV CATALINA_HOME /usr/local/apache-tomcat-9.0.45 	# 环境变量： tomcat环境变量
ENV CATALINA_BASH /usr/local/apache-tomcat-9.0.45

# 设置环境变量 分隔符是：
ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/lib:$CATALINA_HOME/bin 	

EXPOSE 8080 										# 设置暴露的端口

CMD /usr/local/apache-tomcat-9.0.35/bin/startup.sh 
&& tail -F /usr/local/apache-tomcat-9.0.45/logs/catalina.out # 设置默认命令
```

3	构建镜像

```shell
# 因为dockerfile命名使用默认命名 因此不用使用-f 指定文件
docker build -t mytomcat:0.1

[root@2019040740 docker-tomcat]# docker build -t mytomcat:0.1 .
Sending build context to Docker daemon  179.2MB
Step 1/15 : FROM centos
 ---> 300e315adb2f
Step 2/15 : MAINTAINER xiaozhi<1596971466@qq.com>
 ---> Running in 76927fb1d21e
Removing intermediate container 76927fb1d21e
 ---> 8cd6660a0d24
Step 3/15 : COPY README /usr/local/README
 ---> b1431a9fb369
Step 4/15 : ADD jdk-8u261-linux-x64.tar.gz /usr/local
 ---> 86e452cc0465
Step 5/15 : ADD apache-tomcat-9.0.45.tar.gz /usr/local
......

[root@2019040740 docker-tomcat]# docker images
REPOSITORY   TAG       IMAGE ID       CREATED          SIZE
mytomcat     0.1       c98b8ace4ddf   15 seconds ago   636MB
```

4	启动构建好的镜像测试

```shell
[root@2019040740 docker-tomcat]# docker run -d -p 8080:8080 --name tomcat03 -v /home/docker-tomcat/test:/usr/local/apache-tomcat-9.0.45/webapps/test -v /home/docker-tomcat/tomcatlogs/:/usr/local/apache-tomcat-9.0.45/logs mytomcat:0.3

# 本地测试
[root@1459d909b0dc logs]# curl localhost:8080
```

外网进行测试

![image-20210425225257819](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210425225257819.png)



**小结**：

![image-20210425215909645](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210425215909645.png)







# Docker网络

## 基础学习

**问题**：docker容器之间是如何进行访问的

```shell
# 测试  运行一个tomcat
$ docker run -d -P --name tomcat01 tomcat

# 查看容器内部网络地址
$ docker exec -it 容器id ip addr

# 发现容器启动的时候会得到一个 eth0@if91 ip地址，docker分配！
$ ip addr
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
261: eth0@if91: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default 
    link/ether 02:42:ac:12:00:02 brd ff:ff:ff:ff:ff:ff link-netnsid 0
    inet 172.18.0.2/16 brd 172.18.255.255 scope global eth0
       valid_lft forever preferred_lft forever

       
# 思考？ linux能不能ping通容器内部！ 可以 容器内部可以ping通外界吗？ 可以！
$ ping 172.18.0.2
PING 172.18.0.2 (172.18.0.2) 56(84) bytes of data.
64 bytes from 172.18.0.2: icmp_seq=1 ttl=64 time=0.069 ms
64 bytes from 172.18.0.2: icmp_seq=2 ttl=64 time=0.074 ms
```



**原理**：我们每启动一个docker容器，docker就会给docker容器分配一个IP，这个分配IP的分配器就是docker0，它是基于veth-pair技术实现的。

**再次进行测试ip addr**

![image-20210426183815459](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426183815459.png)



再启动一个容器测试，我们会发现又会多出一个网络

![image-20210426183919721](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426183919721.png)



**发现**

我们发现这个容器带来网卡，都是一对对的，veth-pair 就是一对的虚拟设备接口，他们都是成对出现的，一端连着协议，一端彼此相连，正因为有这个特性 veth-pair 充当一个桥梁，连接各种虚拟网络设备的，OpenStac,Docker容器之间的连接，OVS的连接，都是使用evth-pair技术



**接下来测试tomcat01和tomcat02是否可以ping通**

```shell
# 获取tomcat01的ip 172.17.0.2
$ docker-tomcat docker exec -it tomcat01 ip addr  
550: eth0@if551: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default 
    link/ether 02:42:ac:11:00:02 brd ff:ff:ff:ff:ff:ff link-netnsid 0
    inet 172.17.0.2/16 brd 172.17.255.255 scope global eth0
       valid_lft forever preferred_lft forever
       
# 让tomcat02 ping tomcat01       
$ docker-tomcat docker exec -it tomcat02 ping 172.17.0.2
PING 172.17.0.2 (172.17.0.2) 56(84) bytes of data.
64 bytes from 172.17.0.2: icmp_seq=1 ttl=64 time=0.098 ms
64 bytes from 172.17.0.2: icmp_seq=2 ttl=64 time=0.071 ms

# 结论：容器和容器之间是可以互相ping通
```



**网络模型图**

![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2NoZW5nY29kZXgvY2xvdWRpbWcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDUxNjE3NDI0ODYyNi5wbmc?x-oss-process=image/format,png)



**结论**：tomcat01和tomcat02公用一个路由器，docker0,所有的容器不指定网络的情况下，都是docker0路由的，docker会给我们的容器分配一个默认的可用ip。



## --link

**思考一个场景：我们编写了一个微服务，database url=ip: 项目不重启，数据ip换了，我们希望可以处理这个问题，可以通过名字来进行访问容器**？

```shell
$ docker exec -it tomcat02 ping tomca01   # ping不通
ping: tomca01: Name or service not known

# 运行一个tomcat03 --link tomcat02 
$ docker run -d -P --name tomcat03 --link tomcat02 tomcat
5f9331566980a9e92bc54681caaac14e9fc993f14ad13d98534026c08c0a9aef

# 3连接2
# 用tomcat03 ping tomcat02 可以ping通
$ docker exec -it tomcat03 ping tomcat02
PING tomcat02 (172.17.0.3) 56(84) bytes of data.
64 bytes from tomcat02 (172.17.0.3): icmp_seq=1 ttl=64 time=0.115 ms
64 bytes from tomcat02 (172.17.0.3): icmp_seq=2 ttl=64 time=0.080 ms

# 2连接3
# 用tomcat02 ping tomcat03 ping不通
```



**探究：**

docker network ls 		查看有多少个网络

docker network inspect 网络id		可以看到网段是相同的

![image-20210426185743153](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426185743153.png)



**docker inspect tomcat03**

![image-20210426190056944](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426190056944.png)



**查看tomcat03里面的/etc/hosts发现有tomcat02的配置**

![image-20210426190218369](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426190218369.png)

–link 本质就是在hosts配置中添加映射

现在使用Docker已经不建议使用–link了！

自定义网络，不适用docker0！

docker0问题：不支持容器名连接访问！





## 自定义网络

```shell
$ docker network --help
connect     -- Connect a container to a network
create      -- Creates a new network with a name specified by the
disconnect  -- Disconnects a container from a network
inspect     -- Displays detailed information on a network
ls          -- Lists all the networks created by the user
prune       -- Remove all unused networks
rm          -- Deletes one or more networks
```

**查看所有的docker网络**

![image-20210426190359446](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426190359446.png)



**网络模式**

bridge ：桥接 docker（默认，自己创建也是用bridge模式）

none ：不配置网络，一般不用

host ：和所主机共享网络

container ：容器网络连通（用得少！局限很大）



**测试**

```shell
# 我们直接启动的命令 --net bridge,而这个就是我们得docker0
# bridge就是docker0
$ docker run -d -P --name tomcat01 tomcat
等价于 => docker run -d -P --name tomcat01 --net bridge tomcat

# docker0，特点：默认，域名不能访问。 --link可以打通连接，但是很麻烦！
# 我们可以 自定义一个网络
$ docker network create --driver bridge --subnet 192.168.0.0/16 --gateway 192.168.0.1 mynet
```

![image-20210426190537677](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426190537677.png)

**在自定义的网络下，服务可以互相ping通，不用使用–link**

创建两个tomcat并使用自定义的网络

```shell
[root@2019040740 ~]# docker run -d -P --name tomcat-net-01 --net mynet tomcat:9.0
3544ea2913bab21edf9e43d44cee8d4f4a12b6bf0389749cc87bd39471561e94
[root@2019040740 ~]# docker run -d -P --name tomcat-net-02 --net mynet tomcat:9.0
970be812407582e014e74b674a7cd787759f4b47388767f4cb10bf46d707c5a6
```

![image-20210426191401317](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426191401317.png)

使用容器名也是可以ping通的





## 网络连通

加入网络

![image-20210426190805805](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210426190805805.png)



**测试**

创建一个tomcat使用默认的网络，既docker0

```shell
[root@2019040740 ~]# docker run -d -P --name tomcat01 tomcat:9.0

# ping不同网络的容器
[root@2019040740 ~]# docker exec -it tomcat01 ping tomcat-net-01
ping: tomcat-net-01: Name or service not known
```

发现是ping不通的



要将tomcat01连通tomcat-net-01，就是要将tomcat01加到mynet网络，一个容器两个id

**测试**

```shell
[root@2019040740 ~]# docker network connect mynet tomcat01
[root@2019040740 ~]# docker exec -it tomcat01 ping tomcat-net-01
PING tomcat-net-01 (192.168.0.2) 56(84) bytes of data.
64 bytes from tomcat-net-01.mynet (192.168.0.2): icmp_seq=1 ttl=64 time=0.119 ms
64 bytes from tomcat-net-01.mynet (192.168.0.2): icmp_seq=2 ttl=64 time=0.107 ms
64 bytes from tomcat-net-01.mynet (192.168.0.2): icmp_seq=3 ttl=64 time=0.101 ms
64 bytes from tomcat-net-01.mynet (192.168.0.2): icmp_seq=4 ttl=64 time=0.102 ms
......
```

**结论**：假设要跨网络操作别人，就需要使用docker network connect 连通！





## 实战：部署Redis集群









## SpringBoot打包Docker镜像

1、构建SpringBoot项目

2、打包运行

mvn package

3、编写dockerfile

```shell
FROM java:8
COPY *.jar /app.jar
CMD ["--server.port=8080"]
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```


4、构建镜像

```shell
# 1.复制jar和DockerFIle到服务器
# 2.构建镜像
$ docker build -t xxxxx:xx  .
```

5、发布运行

```shell
[root@2019040740 springBoot]# docker run -d -P --name project xiaozhi:0.1

# 进行测试
[root@2019040740 springBoot]# curl localhost:49167
欢迎来到编程的世界
```







# Docker Compose

我们之前运行容器是一个个来进行启动的，如果我们的微服务有100个模块要部署的话，一个一个来运行就会很耗时间和精力，那么DockerCompose就是来解决这个问题的，它可以批量运行容器，可以高效的管理我们的容器的编排



## 概述和安装

### 概述

Compose is a tool for defining and running multi-container Docker applications. With Compose, you use a YAML file to configure your application’s services. Then, with a single command, you create and start all the services from your configuration. To learn more about all the features of Compose, see [the list of features](https://docs.docker.com/compose/#features).

**所有环境都可以使用compose**

Compose works in all environments: production, staging, development, testing, as well as CI workflows. You can learn more about each case in [Common Use Cases](https://docs.docker.com/compose/#common-use-cases).

**三步骤**

Using Compose is basically a three-step process:

1.  Define your app’s environment with a **`Dockerfile`** so it can be reproduced anywhere.
    -   Dockerfile保证我们在任何地方都可以运行
2.  Define the services that make up your app in **`docker-compose.yml`** so they can be run together in an isolated environment.
    -   service		什么是服务
    -   docker-compose.yml这个文件怎么写
3.  Run `docker compose up` and the [Docker compose command](https://docs.docker.com/compose/cli-command/) starts and runs your entire app. You can alternatively run **`docker-compose up`** using the docker-compose binary.
    -   启动项目



### 安装

官方文档地址：https://docs.docker.com/compose/install/

```shell
# 下载安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

[root@2019040740 ~]# docker-compose --version
docker-compose version 1.29.1, build c34c88b2
# 出现版本就是成功了
```



### 实例

#### 1	创建文件

```shell
 mkdir composetest
 cd composetest
 vim app.py
```

app.py文件内容

```python
import time

import redis
from flask import Flask

app = Flask(__name__)
cache = redis.Redis(host='redis', port=6379)

def get_hit_count():
    retries = 5
    while True:
        try:
            return cache.incr('hits')
        except redis.exceptions.ConnectionError as exc:
            if retries == 0:
                raise exc
            retries -= 1
            time.sleep(0.5)

@app.route('/')
def hello():
    count = get_hit_count()
    return 'Hello World! I have been seen {} times.\n'.format(count)
```

```shell
# 创建文件
[root@2019040740 composetest]# vim requirements.txt
flask
redis
```



#### 2	创建Dockerfile文件

```shell
[root@2019040740 composetest]# vim Dockerfile
FROM python:3.7-alpine
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories 
WORKDIR /code
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
RUN apk add --no-cache gcc musl-dev linux-headers
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
EXPOSE 5000
COPY . .
CMD ["flask", "run"]

```



#### 3	在compose file中定义服务 

```shell
[root@2019040740 composetest]# vim docker-compose.yml
version: "3.9"
services:
  web:
    build: .
    ports:
      - "5000:5000"
  redis:
    image: "redis:alpine"
```



#### 4	构建和运行compose

```shell
[root@2019040740 composetest]# docker-compose up
Building web
Sending build context to Docker daemon  5.632kB
Step 1/10 : FROM python:3.7-alpine
3.7-alpine: Pulling from library/python
540db60ca938: Pull complete 
a7ad1a75a999: Pull complete 
37ce6546d5dd: Pull complete 
ec9e91bed5a2: Pull complete 
c629b5f73da8: Pull complete
......

# 测试
[root@2019040740 ~]# curl localhost:5000
Hello World! I have been seen 1 times.
# 正常的返回了消息，安装没有问题
```



#### 5	停止

```shell
ctrl + c
docker-compose down
```























**开发到部署一套流水线**

![img](https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fimg-blog.csdnimg.cn%2F2019032819485411.png%3Fx-oss-process%3Dimage%2Fwatermark%2Ctype_ZmFuZ3poZW5naGVpdGk%2Cshadow_10%2Ctext_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NDIyMTYxMw%3D%3D%2Csize_16%2Ccolor_FFFFFF%2Ct_70&refer=http%3A%2F%2Fimg-blog.csdnimg.cn&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621652875&t=ddacceb20666243f453ec6c63bffdbae)

