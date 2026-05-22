# 一、linux简介

Linux系统诞生于1991年,由芬兰大学生林纳斯-托瓦兹(Linux Torvalds)和后来陆续加入的众多爱好者共同开发完成.

Linux是开源软件,源代码开放的UNIX.目前存在着许多不同的Linux版本，但它们都使用了Linux内核。

Linux的特点:它是**多用户，多任务，丰富的网络功能，可靠的系统安全，良好的可移植性**，具有标准兼容性，良好的用户界面，出色的速度性能.

Linux可安装在各种计算机硬件设备中，比如手机、平板电脑、路由器、台式计算机

![image-20211011143937805](D:\学习笔记\img\20211011143939.png)



## linux的组成

-   **内核:**是系统的心脏,是运行程序管理硬件设备的核心程序.

    Linux内核网址:http://www.kernel.org

-   **Shell:**是系统的用户界面,提供了用户和内核进行交互操作的一种接口.它接收用户输入的命 

    令并把它送入内核去执行,是一个命令解释器.但它不仅只是命令解释器,而且还是高级编程语言,shell编程. 

-   **文件系统:**文件系统是操作系统用于明确存储设备（常见的是磁盘，也有基于NAND Flash的固 态硬盘）或分区上

    的文件的方法和数据结构；即在存储设备上组织文件的方法。

    Linux支持多种文件系统,如ext3,ext2,nfs,smb,iso9660等

-   **应用程序:**标准的Linux操作系统都会有一套应用程序                    **例如**X-windows,openoffice等



## CentOS Linux

-    **主流**：社会上主要应用于生产环境的linux是RedHat或者CentOS

-   **免费**：RedHat 和CentOS差别不大，Red Hat Linux 和Centos系统是免费的,但是RedHat

    ​			Linux 服务是收费的,比如免费版本不支持在线升级.Centos每个版本服务都是免费的. 

-   **更新方便**：CentOS独有的yum命令支持在线升级，可以即时更新系统，不像RedHat 那样需要花钱购买支持服务！



## linux目录结构

![image-20211011144617674](D:\学习笔记\img\20211011144618.png)

-   bin (binaries)存放二进制可执行文件
-   sbin (super user binaries)存放二进制可执行文件，只有root才能访问
-   etc (etcetera)存放系统配置文件
-   usr (unix shared resources)用于存放共享的系统资源
-   home 存放普通用户文件的根目录
-   root 超级用户目录
-   dev (devices)用于存放设备文件
-   lib (library)存放共享库及内核模块
-   mnt (mount)系统管理员安装临时文件系统的安装挂载点
-   boot 存放用于系统引导时使用的文件
-   tmp (temporary)用于存放各种临时文件
-   var (variable)用于存放运行时需要改变数据的文件





# 二、VI和VIM文本编辑器

## ①vi和vim常用的三种模式

![image-20211012194532871](D:\学习笔记\img\20211012194534.png)

### 1.正常模式

以vim 打开-一个档案就直接进入一般模式了(这是默认的模式)。在这个模式中， 你可以使用「上下左右」按键来
移动光标，你可以使用「删除字符」或| [删除整行」来处理档案内容，也可以使用 [复制、粘贴」来处理你的文件数
据。

### 2.插入模式

(按下i,I,o,O,a, A,r,R等任何一个字母之后才会进入编辑模式，- .般来说按i即可.



### 3.命令行模式 

输入esc再输入:在这个模式当中，可以提供你相关指令， 完成读取、存盘、替换、离开vim、显
示行号等的动作则是在此模式中达成的!



## ②插入命令

![image-20211012194815029](D:\学习笔记\img\20211012194816.png)



## ③定位命令

![image-20211012194901781](D:\学习笔记\img\20211012194903.png)



## ④删除命令

![image-20211012194916110](D:\学习笔记\img\20211012194917.png)



## ⑤复制与剪切命令

![image-20211012195048752](D:\学习笔记\img\20211012195049.png)



## ⑥替换和取消命令

![image-20211012195110086](D:\学习笔记\img\20211012195111.png)



## ⑦搜索和搜索替换命令

![image-20211012195148317](D:\学习笔记\img\20211012195149.png)



## ⑧保存和退出命令

![image-20211012195212500](D:\学习笔记\img\20211012195213.png)





## ⑨常用快捷键

![image-20211012195233182](D:\学习笔记\img\20211012195234.png)









# 三、关机、重启和用户登录注销

1) shutdown -h now                    立该进行关机
2) shudown  -h  1                        "1分钟后会关机了"
3) shutdown  -r  now                   现在重新启动计算机
4) halt                                         关机，作用和上面一样.
5) reboot                                     现在重新启动计算机
6) sync                                       把内存的数据同步到磁盘



**注意细节**

1.  不管是重启系统还是关闭系统，首先要运行sync命令，把内存中的数据写到磁盘中
2.  目 前的shutdown/reboot/halt 等命令均已经在关机前进行了syne ，老 韩提醒:小心驶得万年船

**用户登录和注销**
①基本介绍
1)登录时尽 量少用root帐号登录，因为它是系统管理员，最大的权限，避免操作失误。可以利用普通用户登录，登录
后再用”su-用户名’命令来切换成系统管理员身份.
2)在提示符下输入 logout即可注销用户



# 四、用户管理

## ①基本介绍

Linux系统是一个多用户多任务的操作系统，任何- -个要使用系统资源的用户，都必须首先向系统管理员申请-一个账号，然后以这个账号的身份进入系统

![image-20211011154628030](D:\学习笔记\img\20211011154629.png)

**一个标准用户有哪些配置文件?**

![image-20211012200438689](D:\学习笔记\img\20211012200439.png)

![image-20211012200459215](D:\学习笔记\img\20211012200500.png)

![image-20211012200508204](D:\学习笔记\img\20211012200509.png)

![image-20211012200520217](D:\学习笔记\img\20211012200521.png)

![image-20211012201022281](D:\学习笔记\img\20211012201023.png)

![image-20211012201045902](D:\学习笔记\img\20211012201047.png)

![image-20211012201124511](D:\学习笔记\img\20211012201125.png)





## ②用户管理

### 1.添加用户

-   useradd  [-选项]  用户名

**选项**

-   -u 指定用户ID（uid） 
-   -g 指定所属的组名（gid） 
-    -G 指定多个组，用逗号“，”分开（Groups） 
-    -c 用户描述（comment） 
-   -e 失效时间（expire date）

**细节说明**

1.  当创建用户成功后，会自动创建和用户同名的家目录
2.  也可以通过useradd -d 指定目录 新的用户名，给新创建的用户指定家目录的地址



### 2.指定/修改密码

-   passwd 用户名



### 3.修改用户

修改用户命令：usermod（user modify） 

-   -u 指定用户ID（uid） 
-    -g 指定所属的组名（gid） 
-   -G 指定多个组，用逗号“，”分开（Groups） 
-   -c 用户描述（comment） 
-   -e 失效时间（expire date）



### 4.删除用户

-   userdel 用户名	-->	删除用户但是保留主目录

-   userdel -r 用户名	-->	删除用户一级用户主目录

**建议**：保存用户主目录



### 5.查询用户信息

-   id 用户名



### 6.切换用户

-   su - 用户名



### 6.查看当前正在使用的用户

-   whoami/who am i



## ③用户组管理



### 1.新增组

-   groupadd 用户组
-   useradd -g 用户组 用户名 --> 增加用户时直接加上组



### 2.删除组

-   groupdel 用户组



### 3.修改用户的组

-   usermod -g 用户组 用户名



## ④用户和组相关文件

### 1./etc/passwd 文件

用户(user) 的配置文件，记录用户的各种信息
每行的含义:用户名:口令:用户标识号:组标识号:注释性描述:主目录:登录Shell



### 2./etc/shadow文件

**口令的配置文件**
每行的含义:登录名:加密口令:最后一次修改时间:最小时间间隔:最大时间间隔:警告时间:不活动时间:失效时间:标志



### 3./etc/group 文件

组(group)的配置文件，记录Linux包含的组的信息
每行含，义:组名:口令:组标识号:组内用户列表





# 五、常用指令

## ①指定运行级别

**运行级别说明**:
0:关机
1 :**单用户[找回丢失密码]**
2:多用户状态没有网络服务
3:多用户状态有网络服务
4:系统未使用保留给用户
5:图形界面
6:系统重启
常用运行级别是3和5，也可以指定默认运行级别

**命令**：

init [0123456]  ：填写哪一个就是执行对应的运行级别的功能



**CentOS7后运行级别说明**

在centos7以前，/etc/inittab 文件中
进行了简化，如下:

-   **multi-user.target**. analogous to runlevel 3
-   **graphical.target**. analogous to runlevel 5
-   **systemctl get-default**
-   **systemctl set-default TARGET.target**



## ②找回root密码





## ③帮助命令

-   main获取帮助信息
-   help指令



## ④文件目录类

### 1.pwd指令

查看当前所在路径



### 2.ls指令

基本语法: ls [选项] [目录或是文件]
**常用选项**
	-a : 显示当前目录所有的文件和目录，包括隐藏的。
	-l : 以列表的方式显示信息

**组合使用**：ls -al



### 3.cd指令

基本语法：cd [参数] 	切换到指定目录

-   cd 或者 cd~ 	回到家目录

-   cd.. 				 返回上级目录



### 4.mkdir指令

mkdir指令用于创建目录.
基本语法: mkdir [选项] 要 创建的目录
**常用选项**
-p :创建多级目录



### 5.rmdir指令

**基本语法**
	rmdir [选项] 要删除的空 目录
**案例**:删除-一个 目录/home/dog
**使用细节**
rmdir删除的是空目录，如果目录下有内容时无法删除的。
删除非空的目录使用rm -rf 目录名
**比如**:rm -rf /home/animal



### 6.touch指令

touch指令创建空文件

**基本语法**：touch 文件名称



### 7.cp指令

拷贝指定文件到指定位置

**基本语法**：cp [选项] 要赋值的文件路径 最终的文件路径

**常用选项**

-   -r：递归复制整个文件夹



### 8.rm指令

rm移除指定文件或者目录

**基本语法**：rm [选项] 要删除的文件或目录

**常用选项**：

-   r:递归删除整个文件夹
-   -f :强制删除不提示



### 9.mv指令

mv移动文件与目录或重命名，有两个路径的就是移动，只有一个的就是重命名
**基本语法** ：
mv  oldNameFile newNameFile(功能描述:重命名)



### 10.cat指令

cat查看文件内容

-   cat [选项]  要查看的文件

**常用选项**

-   -n：显示行号



### 11.more指令

more指令是一个基于VI编辑器的文本过滤器，它以全屏幕的方式按页显示文本文件的内容。more 指令中内置了若
干快捷键(交互的指令)，详见操作说明
**基本语法**

-   more要查看的文件

**操作说明**

![image-20211011183525246](D:\学习笔记\img\20211011183526.png)



### 12.less指令

less指令用来分屏查看文件内容，它的功能与more指令类似，但是比more指令更加强大，支持各种显示终端。less指令在显示文件内容时，并不是-次将整个文件加载之后才显示，而是根据显示需要加载内容，对于显示大型文件具有

**基本语法**

-   less 要查看的文件名

**操作说明**

![image-20211011184011074](D:\学习笔记\img\20211011184012.png)



### 13.echo指令

echo输出内容到控制台

**基本语法**

echo [选项] 输出的内容

可以使用echo输出环境变量，比如$PATH



### 14.head指令

head用于显示文件的开头部分内容，默认情况下head指令显示文件的前10行内容
**基本语法**

-   head文件    (功能描述: 查看文件头10行内容)
-   **head-n5**文件 (功能描述:查看文件头5行内容，5可以是任意行数)



### 15.tail指令

tail用于输出文件中尾部的内容，默认情况下tail指令显示文件的前10 行内容。
**基本语法**

-   **tail** 文件           (功能描述:查看文件尾10行内容)
-   **tail -n5** 文件    (功能描述:查看文件尾5行内容，5可以是任意行数)
-   **tail -f**    文件    (功能描述:实时追踪该文档的所有更新)



### 16.> 指令 和 >> 指令

">"表示重定向       ">>"表示重定向追加

**基本语法**

-   ls-l > 文件.             (功能描述:列表的内容写入文件a.txt中(覆盖写) )
-   ls-al >> 文件          (功能描述: 列表的内容追加到文件aa.txt的末尾)
-   cat文件1 > 文件2   (功能描述:将文件1的内容覆盖到文件2)
-   echo "内容" >> 文件（追加）



### 17.ln指令

软链接也称为符号链接，**类似于windows里的快捷方式**，主要存放了链接其他文件的路径

**基本语法**

In-s [原文件或目录] [软链接名] (功能描述: 给原文件创建-一个软链接)

-   **案例1**:在/home目录下创建一个软连接myroot, 连接到/root 目录

    In -s /root  /home/myroot

-   **案例2**:删除软连接
    rm /home/myroot

**细节说明**

当我们使用pwd指令查看目录时，仍然看到的是软链接所在目录。



### 18.history指令

查看已经执行过历史命令，也可以执行历史指令
**基本语法** .
history (功能描述: 查看已经执行过历史命令)

**应用实例**

-   案例1:显示所有的历史命令
    history
-   案例2:显示最近使用过的10个指令。
    history 10
-   案例3:执行历史编号为5的指令
    !5





## ⑤时间日期类

​	

### 1.date指令-显示当前日期

**基本语法**

1) date            (功能描述:显示当前时间)
2) date +%Y   (功能描述:显示当前年份)
3) date +%m   (功能描述:显示当前月份)
4) date +%d (功能描述:显示当前是哪- - 天)
5) date "+%Y-%m- %d %H:%M:%S" (功能描述:显示年月日时分秒)

### 2.date指令_设置日期

**基本语法**
date -S  字符串时间
**应用实例**
案例1:设置系统当前时间，比如设置成2020-1 1-03 20:02:10
date -s“2020- 11-03 20:02:10”



### 3.call命令

查看日历指令cal
**基本语法**
cal [选项]  (功能描述:不加选项，显示本月日历)
**应用实例**
案例1:显示当前日历：    cal
案例2:显示2020年日历:  cal 2020





## ⑥搜索查找类

### 1.find	

find指令将从指定目录向下递归地遍历其各个子目录，将满足条件的文件或者目录显示在终端。
**基本语法**
find [搜索 范围] [选项]

**选项说明**

![image-20211011190822416](D:\学习笔记\img\20211011190823.png)

**应用实例**

-   案例1:按文件名:根据名称查找/home目录下的hello.txt文件
    find /home -name hello.txt
-   案例2:按拥有者:查找/opt目录下，用户名称为nobody 的文件
    find /opt -user nobody
-   案例3:查找整个linux系统下大于200M的文件(+n大于-n小于 n等于， 单位有k,M,G)
    find / -size +200M



### 2.locate指令

locate指令可以快速定位文件路径。locate指令利用事先建立的系统中所有文件名称及路径的locate数据库实现快速定位给定的文件。Locate指令无需遍历整个文件系统，查询速度较快。为了保证查询结果的准确度，管理员必须定期更新locate时刻
**基本语法**
	locate 搜索文件
**特别说明**
由于locate 指令基于数据库进行查询，所以第一次运行前，必须使用updatedb 指令创建locate数据库。
**应用实例**

-   案例1:请使用locate指令快速定位hello.txt 文件所在目录

    ```sh
    [root@xiaozhi ~]# locate test.txt
    /root/test/test.txt
    /usr/local/share/doc/pcre/pcretest.txt
    /usr/local/src/nginx/pcre-8.35/doc/pcretest.txt
    /usr/local/src/nginx/pcre-8.35/doc/perltest.txt
    /usr/share/doc/pcre-devel-8.32/pcretest.txt
    /usr/share/doc/pcre-devel-8.32/perltest.txt
    ```

which指令，可以查看某个指令在哪个目录下，比如ls 指令在哪个目录
which ls .



### 3.grep指令和管道符|

grep过滤查找，管道符，“I”，表示将前--个命令的处理结果输出传递给后面的命令处理。
**基本语法**
grep [选项]  查找内容源文件

**常用选项**

![image-20211011191159183](D:\学习笔记\img\20211011191200.png)

**应用实例**
案例1:请在hello.txt 文件中，查找"yes"所在行， 并且显示行号
写法1: cat /home/hello.txt| grep "yes'
写法2: grep -n "yes" /home/hello.txt



### 4.wc指令

wc 统计文本的行数、字数、字符数（word count） 

-    -m 统计文本字符数
-    -w 统计文本字数
-    -l 统计文本行数





## ⑦压缩和解压类

### 1.gzip/gunzip指令

gzip用于压缩文件，gunzip 用于解压的
**基本语法**
gzip文件          ( 功能描述:压缩文件，只能将文件压缩为* .gz文件)
gunzip文件.gz ( 功能描述:解压缩文件命令)

**应用实例**

-   案例1: gzip压缩，将/home下的hello.txt 文件进行压缩

    gzip /home/hello.txt

-   案例2: gunzip压缩，将/home下的hello.txt.gz 文件进行解压缩
    gunzip /home/hello.txt.gz



### 2.zip/unzip指令

zip用于压缩文件，unzip用于解压的，这个在项目打包发布中很有用的

**基本语法**
zip [选项] XXX.zip  将要压缩的内容                  (功能描述:压缩文件和目录的命令)
unzip [选项] XXX.zip          (功能描述:解压缩文件)

**zip常用选项**

-   -r:递归压缩，即压缩目录

**unzip 的常用选项**

-   -d<目录> :指定解压后文件的存放目录

**应用实例**
案例1:将/home下的所有文件/文件夹进行压缩成myhome .zip
zip-rmyhomezip/home/[将home目录及其包含的文件和子文件夹都压缩]
案例2:将myhome.zip解压到/opt/tmp 目录下
unzip -d /opt/tmp /home/myhome.zip



### 3.tar指令

tar指令是打包指令，最后打包后的文件是.tar.gz 的文件。

**基本语法**
tar [选项] XXX tar.gz打包的内容            (功能描述:打包目录，压缩后的文件格式tar.gz)

**选项说明**

![image-20211011191824004](D:\学习笔记\img\20211011191825.png)

**应用实例**
案例1:压缩多个文件， 将/home/pig txt和/home/cat.txt 压缩成pc.tar.gz
tar -zcvf pc.tar.gz /home/pig. txt /home/cat.txt

案例2:将/home 的文件夹压缩成myhome tar .gz
tar -zcvf myhome.tar. gz /home/

案例3:将 pc.tar.gz 解压到当前目录
tar -zxvf pc.tar.gz

案例4:将myhome.tar.gz解压到/opt/tmp2 目录下(1) mkdir /opt/tmp2      (2) tar -zxvf /home/myhome.tar.gz _C /opt/tmp2



# 六、组管理和权限管理

## 5.1 linux组基本介绍

在linux中的每个用户必须属于一个组，不能独立于组外。在linux中每个文件
有所有者、所在组、其它组的概念。



## 5.2 文件目录所有者

### 1.查看文件所有者

**指令**：ls -ahl



### 2.修改文件所有者

**指令**：chown 用户名 文件名



## 5.3 组的创建

groupadd 组名





## 5.4 文件目录所在组

### 1.查看文件目录所在组

指令：ls -ahl



### 2.修改文件目录所在组

**基本指令**

chgrp 组名 文件名



## 5.5 其他组

除文件的所有者和所在组的用户外，系统的其它用户都是文件的其它组



## 5.6 改变用户所在组

在添加用户时，可以指定将该用户添加到哪个组中，同样的用root的管理权限可以改变某个用户所在的组。

-   usermod -g 新组名 用户名

-   usermod -d 目录名 用户名 改变该用户登录的初始目录     

    **特别说明**：用户需要有进入新目录的权限



## 5.7 权限的基本介绍

ls -I 中显示的内容如下:
-rwxrw-r-- 1 root root 1213 Feb 2 09:39 abc
**0-9位说明**

-   第0位确定文件类型(d,-,1,c,b)
    -   I是链接，相当于windows的快捷方式
    -   d是目录，相当于windows的文件夹
    -   c是字符设备文件，鼠标，键盘
    -   b是块设备，比如硬盘
-   第1-3位确定所有者(该文件的所有者)拥有该文件的权限。--User
-   第4-6位确定所属组(同用户组的)拥有该文件的权限，--Group
-   第7-9位确定其他用户拥有该文件的权限-_Other



## 5.8 rwx权限详解

### 1.rwx作用到文件

1) [r ]代表可读(read): 可以读取,查看
2) [ w ]代表可写(write);可以修改,但是不代表可以删除该文件,删除一个 文件的前提条件是对该文件所在的目录有写权限，才能删除该文件.
3) [ x ]代表可执行(execute):可以被执行



### 2.rwx作用到目录

1) [r ]代表可读(read): 可以读取，ls查看目录内容
2) [ w ]代表可写(write):可以修改，对目录内创建+删除+重命名目录
3) [ x ]代表可执行(execute):可以进入该目录



### 3.文件和目录实际案例

-rwxrw-r-- 1 root root 1213 Feb 2 09:39 abc

**10个字符确定不同用户能对文件干什么**

-   第一个字符代表文件类型: -1dcb
-   其余字符每3个- -组(rwx)读(r) 写(w)执行(x)
-   第一-组rwx:文件拥有者的权限是读、写和执行〉
-   第二组rw-:与文件拥有者同一组的用户的权限是读、写但不能执行
-   第三组r--:不与文件拥有者同组的其他用户的权限是读不能写和执行

可用数字表示为:r=4,w=2,x=1 因此rwx=4+2+1=7，数字可以进行组合

**其它说明**
1					文件:硬连接数或目录: 子目录数
root				用户
root				组

1213			  文件大小(字节)，如果是文件夹，显示4096 字节
Feb 209:39    最后修改日期
abc                文件名



## 5.9 修改权限

通过chmod指令，可以修改**文件或目录**的权限



### 1.第一种方式

**+、-、=变更权限**

u:所有者	g:所有组	o:其他人 	a:所有人(u、 g、o的总和)
1) chmod u=rwx,g=rx,0=x    文件/目录名
2) chmod  o+w    文件/目录名
3) chmod  a-X     文件/目录名

**案例演示**

1.  给abc文件的所有者读写执行的权限，给所在组读执行权限，给其它组读执行权限。
    chmod u=rwx g=rX,0=rX abc
2.  给abc文件的所有者除去执行的权限，增加组写的权限
    chmod u-x ,g+w abc
3.  给abc文件的所有用户添加读的权限
    chmod a+r abc



### 2.第二种方式

**通过数字变更权限**

r=4 w=2 x=l             rwx=4+2+1=7
chmod u=rwx,g=rX,0=x    文件目录名
相当于chmod     751         文件/目录名

**案例演示**

**要求**：将/home/abc.txt文件的权限修改成rWxr-xr-x, 使用给数字的方式实现:

chmod  755  /home/abc.txt



## 5.10 修改文件所有者



















# 七、RPM软件包管理

RPM是RedHat Package Manager（RedHat软件包管理工具）的缩写



## RPM命令使用

rpm的常用参数

-   i：安装应用程序（install） 
-   e：卸载应用程序（erase） 
-   vh：显示安装进度；（verbose hash） 
-   U：升级软件包；（update） 
-   qa: 显示所有已安装软件包（query all） 
-    结合grep命令使用

**例子**：rmp -e gcc-c++-4.4.7-3.el6.x86_64.rpm



## YUM命令

Yum（全称为 Yellow dog Updater, Modified）是一个在Fedora和RedHat以 及SUSE、CentOS中的Shell前端软件包管理器。

**例子（需要上网，没有网络可以建本地源）**：

• yum install gcc-c++

• yum remove gcc-c++

• yum update gcc-c++



### 配置本地YUM源

-   步骤1：在Vmware中，确保iso镜像已经正常连接到系统上。
-   步骤2：建立挂载点，在root用户下mkdir  /mnt/cdrom来创建目录
-   步骤3：输入mount -t iso9660 /dev/cdrom /mnt/cdrom将光驱或者iso文件挂载到 /mnt/cdrom目录下，使用df -h可以看到已经成功挂载。
-   步骤4：进入/etc/yum.repos.d目录，ls查看当前目录下的yum源配置文件，新建bak目录， 将除CentOS-Media.repo之外的repo文件移动到bak文件夹备份。
-   步骤5：使用vi编辑文件CentOS-Media.repo（文件内容见备注）
-   步骤6：输入“yum clean all”命令清理。
-   步骤7：yum makecache/yum repolist 进行加载yum源



## 安装Mysql

-   rpm -qa|grep mysql –查看是否安装了mysql
-   rpm -e --nodeps mysql-libs -el6.x86_64 --卸载指定的包
-   chmod u+x mysql57 - community -release-el7-10.noarch.rpm --修改执行权限
-   rpm -ivh mysql57- community -release-el7-10.noarch.rpm --安装官方的yum源
-   yum - y install mysql- community - server --安装MySQL服务器
-   systemctl start/status mysqld.service --启动/查看mysql服务
-   grep "password" /var/log/mysqld.log --查看mysql初始化密码
-   mysql> ALTER USER 'root'@'localhost' IDENTIFIED BY '!Laoyang123456'; -- 登录后,修改密码信息
-   mysql> grant all privileges on *.* to 'root'@'%' identified by '!Laoyang123456' with grant option; --更新允许远程登录
-   mysql> f lush privileges; --刷新配置信息





# 其他

## 网络配置

在VMWare中配置静态网络

![image-20230727194547314](D:\学习笔记\img\img01\image-20230727194547314.png) 

![image-20230727194554132](D:\学习笔记\img\img01\image-20230727194554132.png) 

![image-20230727194602047](D:\学习笔记\img\img01\image-20230727194602047.png) 

修改网卡配置

```sh
[root@localhost ~]# vim /etc/sysconfig/network-scripts/ifcfg-ens32
DEVICE=ens32
TYPE="Ethernet"
PROXY_METHOD="none"
BROWSER_ONLY="no"
BOOTPROTO="static"
DEFROUTE="yes"
IPV4_FAILURE_FATAL="no"
IPV6INIT="yes"
IPV6_AUTOCONF="yes"
IPV6_DEFROUTE="yes"
IPV6_FAILURE_FATAL="no"
IPV6_ADDR_GEN_MODE="stable-privacy"
NAME="ens32"
UUID="5437c774-f435-47e7-baf8-742d795abda6"
DEVICE="ens32"
ONBOOT="yes"

# 下面的ip地址要在之前设定的ip地址范围内
IPADDR=192.168.10.102
PREFIX=24
GATEWAY=192.168.10.2
DNS1=192.168.10.2
```

复制替换重新启动网卡即可







## linux下环境搭建

### Java环境搭建

#### 1、解压jdk包

#### 2、配置环境

vim /etc/profile

```shell
JAVA_HOME=/opt/env/jdk1.8.0_261
PATH=$PATH:$JAVA_HOME/bin
CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export JAVA_HOME CLASSPATH PATH
```

然后刷新一下环境

```shell
source /etc/profile
```



#### 3、测试

```sh
# 输入 java -version 或者 java --version 查看是否安装成功
```











## 快捷键

![image-20211012190142612](D:\学习笔记\img\20211012190144.png)



ctrl + alt + F3 命令行界面模式
ctrl + alt + F1 图形界面模式

