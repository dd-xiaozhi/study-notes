# redis事物

## 1、概述

redis事物是一个单独的隔离操作，事物中的命令都会被序列化，按顺序执行。事物在执行的过程中，不会被其他客户端发来的命令打断

Redis事物的主要作用就是串联多个命令，防止命令之间插队



**事物的两个阶段**

![image-20220508100023529](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220508100023529.png)

组队阶段：通过Multi命令开启组队，客户端发送过来的命令就会放到队列中，并不会执行

执行阶段：通过Exec命令执行队列中的命令，按顺序执行，如果在执行阶段之前使用了discard命令，事物结束





## 2、使用事物

-   multi：开启事物，进行组队
-   discard：撤销组队
-   exec：执行命令

![image-20220508101107026](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220508101107026.png)



## 3、事物中出现异常

**情况一**：在组队阶段有异常，执行失败，所有命令不执行

![image-20220508101345926](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220508101345926.png)



**情况二**：执行阶段有异常，有异常的那条数据执行失败，其他的成功执行

![image-20220508101600032](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220508101600032.png)



**总结**

-   语法出现错误就会导致所有的操作一起失败
-   语法没有错误，但是它的使用不合法，那么在提交事务的时候就是不合法的那个执行失败，其他执行成功





## 4、事物冲突 - 锁

### ①悲观锁

**悲观锁**：每一次访问都会给对应的key上锁，直到访问结束，其他客户端才能获取到这个key的值

这种方式虽然保证了数据安全性，但是它损失了效率，Redis作为缓存时需要很快的响应，所以这种方式一般不采用

![image-20220508101919065](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220508101919065.png)



### ②乐观锁(推荐使用)

**乐观锁**：通过版本号来解决冲突，在进行事物更新时会看一下版本号是否有发生改变，如果发生改变，就要获取最新版本的数据，然后对其进行操作

-   **watch key [key ...]命令**：在multi执行之前，先执行watch key命令可以监视一个或多个key，如果在事物执行之前这个（或这些）key被其他命令改动，那么事物也被打断
-   **UNWATCH**：取消监听，当我们发现有人修改了值我们可以手动取消监听

![image-20220508102911839](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220508102911839.png)

![image-20220508102933297](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220508102933297.png)

**说明**：客户端1将key进行修改exec执行，那么其他客户端得到的为null，多个客户端同时访问，只能有一个成功









