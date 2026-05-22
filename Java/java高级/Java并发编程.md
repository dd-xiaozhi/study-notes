# 一、概述

## 1、进程与线程

### 1.1 进程

程序由指令和数据组成，数据要进行读写，就必须将指令加载到 CPU ，数据记载到内存中，在指令的运行过程中还需要用到磁盘、网络等设备。这些加载指令、管理内存和管理 IO 的操作 由进程来完成

程序运行从磁盘中加载代码到内存中，这就开启了一个进程，进程可以视为程序的一个实例，有些程序只能由一个实例，有些程序可以有多个实力，看这个程序是否支持多实例





### 1.2 线程

一个进程中可以拥有多个线程，通过线程来执行不同的任务。线程就是一个指令容器，将一条条的指令按照一定顺序交给 CPU 读取和执行。它本身是不具备执行的能力的。

![image-20231011212713711](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011212713711.png) 

在Java中，线程是最小的调度单位，进程作为资源分配的最小单位。在 windows 中进程是不活动的，它只是充当了线程容器的身份



### 1.3 两者对比

- 进程基本相对独立，线程在进程内，是进程的一个子集
- 进程内的线程资源共享
- 进程间通信较为复杂
    - 同一台计算机的进程通信称为 IPC(inter-process-communication)
    - 不同计算机之间的进程间通信，需要通过网络，并遵守共同的协议，比如HTTP协议
- 线程通信相对简单，他们共享进程内的内存
- 线程更加轻量，线程上下文切换成本一般要比进程上下文切换低





## 2、并行和并发

### 2.1 并发

单核 CPU 下，线程实际是串行运行的，操作系统中有一个组件叫做任务调度器，将 cpu 的时间片（windows 下时间片最小约为 15 毫秒）分给不同的程序使用，只是由于 CPU 在线程间的切换非常快，人类感觉是 `同时运行的`。

微观串行，宏观并行。一般会将这种 `线程轮流使用 CPU 时间片`的做法称为并发，concurrent

![image-20231011214932416](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011214932416.png) 

如上图所示，任务调度器在多个线程间来回切换，这个行为就是并发



### 2.2 并行

多个核心同时执行任务的行为就是并行，现代计算器的 CPU 普遍拥有多个核心，那也就意味着多个核心可以同时执行任务，大大提高运行效率，但是这也会出现同个资源被多个核心抢夺的问题。

![image-20231011214941634](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011214941634.png) 

如上图，每个 `核（core）` 都可以调度运行线程，这时候线程可以是并行的。

> 引用 `Rob Pike(golang创始人之一)` 的一段描述：
>
> 并发（concurrent）是同一时间应对（dealing with）多件事情的能力
>
> 并行（parallel）是同一时间动手做（doing）多件事情的能力





# 二、Java线程

## 1、创建和运行线程

### 1.1 方式一：Thread

直接创建 线程的方式，通过继承 Thread 类重写 run 方法的方式

```java
// 方式一
new Thread("方式一") {
    @Override
    public void run() {
        log.debug("方式一执行任务...");
    }
}.start();
```

![image-20231011221918765](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011221918765.png) 



### 1.2 方式二：Runnable

这种方式需要配合 Thread 使用

```java
// 方式二：
new Thread(new Runnable() {
    @Override
    public void run() {
        log.debug("方式二执行任务...");
    }
}, "方式二").start();
```

![image-20231011222004853](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011222004853.png) 



### 1.3 方式三：FutureTask

`FutureTask` 的顶级父类是 `Runnable`，它能够接受 Callable 类型的参数，用来处理由返回结果的任务

```java
// 方式三
FutureTask<Integer> task = new FutureTask<>(() -> {
    log.debug("方式三执行任务...");
    return 100;
});
new Thread(task, "方式三").start();
Integer result = task.get();
log.debug("结果是：{}", result);
```

![image-20231011222229768](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011222229768.png) 





### 1.4、Thread 和 Runnable 的关系（原理）

`Runnable` 需要借助 `Thread` 来创建线程，需要以参数的形式传递给 Thread，我们看一下参数传给谁了

![image-20231011223255614](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011223255614.png) 

![image-20231011223307612](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011223307612.png) 

最后 init 方法中它将 Runnable 对象赋值给了 Thread 的属性 

![image-20231011223331864](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011223331864.png) 

查看 Thread 的 run 方法

![image-20231011223445321](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011223445321.png) 

可以发现调用的是 Runnable 对象中的 run 方法，前提是对象不为空



**总结**

- 方法1 就是将线程和任务合并在一起，方法2 把线程和任务分开了，方法3 是将任务进行细分
- 用 Runnable 的方式更加容易与线程池等高级 API 配合
- 用 Runnable 让任务脱离了 Thread 继承体系，更加灵活





## 2、查看进城线程的方法

### 2.1 windows

任务管理器可以查看进程和线程数，也可以用来杀死进程

- tasklist 查看进程

- taskkill 杀死进程



### 2.2 Linux

- ps -fe 查看所有进程
- `ps -fT -p <PID>` 查看某个进程（PID）的所有线程
- kill 杀死进程
- top 按大写 H 切换是否显示线程
- `top -H -p <PID>` 查看某个进程（PID）的所有线程



### 3.2 Java自带工具

- jps 命令查看所有 Java 进程
- `jstack <PID>` 查看某个 Java 进程（PID）的所有线程状态
- jconsole 来查看某个 Java 进程中线程的运行情况（图形界面）





## 3、线程运行原理

![image-20231011231220721](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231011231220721.png) 

上图是线程运行的内存原理图



### 3.1 栈和栈帧 

Java Virtual Machine Stacks （Java 虚拟机栈），Java中的每个线程都有一个独立的栈，每个方法是一个栈帧（Frame），栈帧中有局部变量表、返回地址、锁记录、操作数栈



### 3.2 线程上下文切换（Thread Context Switch）

因为以下一些原因导致 CPU 不在执行当前线程，转而执行另一个线程就会发生上下文切换

- 线程的 cpu 时间片用完
- 垃圾回收
- 有更高优先级的线程需要运行
- 线程自己调用了 sleep、yield、wait、join、park、synchronized、lock 等方法

当切换线程时，需要由操作系统保存当前线程的状态，然后恢复将要执行线程的状态。

Java 虚拟机中有一个组件叫程序计数器，它的作用是记录下一条 jvm 指令的执行地址，是线程私有的。

- 状态包括程序计数器、虚拟机栈中每个栈帧的信息，如局部变量、操作数栈、返回地址等
- Context Switch 频繁发生会影响性能





## 4、常见方法

| 方法名           | static | 功能说明                                                     | 注意                                                         |
| ---------------- | ------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| start()          |        | 启动一个新线程，在新的线程运行 run 方法中的代码              | start 方法只是让线程进入就绪，里面代码不一定立刻运行（CPU 的时间片还没分给它）。每个线程对象的start方法只能调用一次，如果调用了多次会出现`IllegalThreadStateException` |
| run()            |        | 新线程启动后会调用的方法                                     | 如果在构造 Thread 对象时传递了 Runnable 参数，则线程启动后会调用 Runnable 中的 run 方法，否则默认不执行任何操作。但可以创建 Thread 的子类对象，来覆盖默认行为 |
| join()           |        | 等待线程运行结束                                             |                                                              |
| join(long)       |        | 等待线程运行结束,最多等待 n 毫秒                             |                                                              |
| getId()          |        | 获取线程长整型的 id                                          | 每个线程都有唯一ID                                           |
| getName()        |        | 获取线程名                                                   |                                                              |
| setName(String)  |        | 修改线程名                                                   |                                                              |
| getPriority()    |        | 获取线程优先级                                               |                                                              |
| setPriority(int) |        | 修改线程优先级                                               | java中规定线程优先级是1~10 的整数，较大的优先级能提高该线程被 CPU 调度的机率，但是 CPU 空闲时则此时优先级不起作用。 |
| getState()       |        | 获取线程状态                                                 | Java 中线程状态是用 6 个 enum 表示，分别为：`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED` |
| interrupt()      |        | 打断线程                                                     | 如果被打断线程正在 sleep，wait，join 会导致被打断的线程抛出 InterruptedException，并清除 打断标记 ；如果打断的正在运行的线程，则会设置 打断标记 ；park 的线程被打断，也会设置 打断标记 |
| isInterrupted()  |        | 判断线程是否被打断                                           | 不会清除 打断标记                                            |
| interrupted()    | static | 判断当前线程是否被打断                                       | 会清除 打断标记，它和`isInterrupted()`最大的区别就是它是静态的而且它会清除打断标记 |
| isAlive()        |        | 线程是否存活（还没有运行完毕）                               |                                                              |
| currentThread()  | static | 获取当前正在执行的线程                                       |                                                              |
| sleep(long)      | static | 让当前执行的线程休眠n毫秒，休眠时让出 cpu 的时间片给其它线程 |                                                              |
| yield()          | static | 提示线程调度器让出当前线程对CPU的使用                        | 主要是为了测试和调试                                         |

![img](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1RoZVdpbmRPZlNvbg==,size_16,color_FFFFFF,t_70.png)



### 4.1 start和run

```java
@Slf4j(topic = "startAndRun.test")
public class StartAmdRunTest {

    public static void main(String[] args) {
        // run();
        start();
    }

    public static void start() {
        System.out.println("============= start ===========");
        Thread t1 = new Thread(() -> {
            log.debug("线程名为：{}", Thread.currentThread().getName());
            // 读取文件操作
            FileReader.read();
        }, "t1");
        t1.start();
    }

    public static void run() {
        System.out.println("============= run ===========");
        Thread t2 = new Thread(() -> {
            log.debug("线程名为：{}", Thread.currentThread().getName());
            // 读取文件操作
            FileReader.read();
        }, "t2");
        t2.run();
    }
}
```

测试结果如下：

![image-20231014172303752](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231014172303752.png) 

![image-20231014172329903](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231014172329903.png) 

**小结**：

- run 方法并没有创建新的线程，而是在 main 主线程中执行的同步操作
- start 方法则是创建了一个新的线程，在新线程中调用了 run 方法，执行异步操作





### 4.2 join

获取线程执行权。可以理解为插队现象，线程A执行了线程B的join方法，那么就要等线程B完事后线程A才会继续往下走。也就是说我们将异步的操作变成了同步的。

- join() 等待线程执行完成才往下执行
- join(long) 等待指定时间，时间一到则往下执行

```java
@Slf4j
public class JoinTest {

    static int r1 = 0;
    static int r2 = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            log.debug("========== t1 start ==========");
            TimeUtil.sleep(1);      // 休眠一秒
            r1 = 10;
            log.debug("========== t1 end ==========");
        }, "t1");
        Thread t2 = new Thread(() -> {
            log.debug("========== t2 start ==========");
            TimeUtil.sleep(1);      // 休眠一秒
            r2 = 20;
            log.debug("========== t2 end ==========");
        }, "t2");
        t1.start();
        t2.start();
        // t1.join();
        // t2.join();
        log.debug("r1的值为：{}", r1);
        log.debug("r2的值为：{}", r2);
    }
}
```

在上述例子中，结果是异步的，`r1` 和 `r2` 的值都为 `0`

![image-20231014202612285](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231014202612285.png) 

使用 `join()` 之后

![image-20231014202755821](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231014202755821.png) 

可以是执行完两个线程，结果也是获取到了线程设置的值。

**注意**：在这里的两个线程是异步的，因为我们是在 main方法 中使用 join 方法，所以是对 main 线程有效果，其他线程中没效果，除非是在其他线程中也执行了 join 方法。



**拓展一下**

这里我想使用 join 方法完成 奇数和偶数交替输出

```java
private static void test2() throws InterruptedException {
    t1 = new Thread(() -> {
        for (int i = 1; i <= 100; i++) {
            if (i % 2 == 0) {
                try {
                    log.debug(String.valueOf(i));
                    t2.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }, "t1");
    t2 = new Thread(() -> {
        for (int i = 1; i <= 100; i++) {
            if (i % 2 != 0) {
                try {
                    log.debug(String.valueOf(i));
                    t1.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }, "t2");
    t1.start();
    t2.start();
    t1.join();
}
```

上述代码执行会出现死锁现象，原因是 t1 在等 t2，t2 在等 t1 ，因此发生了死锁





### 4.3 interrupt、interrupted、isInterrupted

- interrupt：打断线程
- interrupted：判断线程是否被打断，会清除标记，方法为 static
- isInterrupted：判断线程是否被打断吗，不会清除标记



#### 4.3.1 打断 wait、sleep、join 的线程

这几个方法会是线程进入阻塞状态，打断 sleep 状态会清空打断标记

```java
public static void BreakSleep() {
    Thread t1 = new Thread(() -> {
        TimeUtil.sleep(2);
    }, "t1");
    t1.start();
    TimeUtil.sleep(1);	// 休眠1秒是因为 t1 是异步执行的，所以要防止main线程执行完t1线程还没启动
    t1.interrupt();
    log.debug("打断状态：{}", t1.isInterrupted());
} 
```

![image-20231014210027944](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231014210027944.png)



#### 4.3.2 打断正常运行的线程

```java
public static void BreakNormalThread() {
    Thread t2 = new Thread(() -> {
        while (true) {
            Thread current = Thread.currentThread();
            boolean interrupted = current.isInterrupted();
            if (interrupted) {
                log.debug("(isInterrupted) 打断状态：{}", interrupted);
                log.debug("(interrupted) 打断状态：{}", Thread.interrupted());
                break;
            }
        }
    }, "t2");
    t2.start();
    TimeUtil.sleep(1);  // 休眠1秒是因为 t1 是异步执行的，所以要防止main线程执行完t1线程还没启动
    t2.interrupt();
}
```

![image-20231014212128297](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231014212128297.png)  



#### 4.3.3 两阶段终止模式

**为什么需要这个模式?**

程序发生异常被迫停止，在停止之前我们需要记录一下最后的日志，此时以下两种方式就不适用：

- stop()方法，此方法会发生死锁的情况。它会真正的杀死线程，但是如果这个时候线程锁住了共享资源，就会导致共享资源一直被锁着，其他线程永远无法获取到共享资源。
- System.exit(0)，此方法会将整个程序直接停掉

所以我们需要使用 `interrupt打断线程` 的方式来实现



**如何实现?**

![image-20231017165425417](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231017165425417.png) 

上图描述的是此模式的具体流程，可以看到我们是在料理后事之后才结束的循环，符合我们的要求



**代码实现**

```java
/**
 * @author xiaozhi
 *
 * 两阶段终止模式
 */
public class TestTwoPhaseTermination {

    public static void main(String[] args) {
        TwoPhaseTermination twoPhaseTermination = new TwoPhaseTermination();
        twoPhaseTermination.start();

        TimeUtil.sleep(3);
        twoPhaseTermination.stop();

    }
}

@Slf4j(topic = "TwoPhaseTermination")
class TwoPhaseTermination {

    private Thread monitor;     // 监控线程

    // 启动线程
    public void start() {
        monitor = new Thread(() -> {
            while (true) {
                Thread currentThread = Thread.currentThread();
                if (currentThread.isInterrupted()) {
                    log.debug("料理后事...");
                    break;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                    log.debug("监控中...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    currentThread.interrupt();      // 重新设置打印标记
                }
            }
        }, "monitor");
        monitor.start();
    }

    // 停止线程
    public void stop() {
        monitor.interrupt();
    }
}
```

![image-20231017172812590](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231017172812590.png) 

可以看到结果是和我们设想的一样.😁



#### 4.3.4 打断pack线程

- 打断 park 线程, 不会清空打断状态
- 如果打断标记已经是true，park会失效。

```java
/**
 * @author xiaozhi
 *
 * 打断 park 线程
 */
@Slf4j(topic = "BreakParkThread")
public class BreakParkThread {

    public static void main(String[] args) {
        // test();
        test2();
    }

    public static void test() {
        Thread t1 = new Thread(() -> {
            log.debug("park...");
            LockSupport.park();
            log.debug("unpark...");
            log.debug("打断状态：{}", Thread.currentThread().isInterrupted());
        }, "t1");
        t1.start();

        TimeUtil.sleep(1);
        t1.interrupt();
    }

    public static void test2() {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                log.debug("park...");
                LockSupport.park();     // 多次循环没有重置标记，park 失效
                log.debug("打断状态：{}", Thread.currentThread().isInterrupted());
            }
        }, "t1");
        t1.start();

        TimeUtil.sleep(1);
        t1.interrupt();
    }
}
```





### 4.4  sleep 与 yield

**sleep**

1. 调用 sleep 会让当前线程从 *Running* 进入 *Timed Waiting* 状态（阻塞）
2. 其它线程可以使用 interrupt 方法打断正在睡眠的线程，这时 sleep 方法会抛出 InterruptedException
3. 睡眠结束后的线程未必会立刻得到执行
4. 建议用 TimeUnit 的 sleep 代替 Thread 的 sleep 来获得更好的可读性

**yield**

1. 调用 yield 会让当前线程从 *Running* 进入 *Runnable* 就绪状态，然后调度执行其它线程
2. 具体的实现依赖于操作系统的任务调度器

这里我们做一个实验，使用 yield 来让奇数和偶数交替输出

```java
public static void testYield() {
    new Thread(() -> {
        for (int i = 0; i < 100; i++) {
            if (i % 2 == 0) {
                log.debug("t1 输出：{}", i);
                Thread.yield();
            }
        }
    }, "t1").start();
    new Thread(() -> {
        for (int i = 0; i < 100; i++) {
            if (i % 2 != 0) {
                log.debug("t2 输出：{}", i);
                Thread.yield();
            }
        }
    }, "t2").start();
}
```

![image-20231014212953242](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231014212953242.png) 

结果显示，它们并没有按顺序交替输出，也就是说 yield 虽然让出了 CPU时间片，==但是任务调度器下一次还是有可能选中它==



### 4.5 线程优先级

- 线程优先级会提示（hint）调度器优先调度该线程，但它仅仅是一个提示，==调度器可以忽略它==
- 如果 cpu 比较忙，那么优先级高的线程会获得更多的时间片，但 cpu 闲时，优先级几乎没作用





## 5、同步模式-两阶段终止模式

它和直接停掉程序不同，它只会关闭当前线程并且可以料理后事再进行停止。

```java
/**
 * @author xiaozhi
 *
 * 两阶段终止模式
 */
public class TestTwoPhaseTermination {

    public static void main(String[] args) {
        TwoPhaseTermination twoPhaseTermination = new TwoPhaseTermination();
        twoPhaseTermination.start();

        TimeUtil.sleep(3);
        twoPhaseTermination.stop();

    }
}

@Slf4j(topic = "TwoPhaseTermination")
class TwoPhaseTermination {

    private Thread monitor;     // 监控线程

    // 启动线程
    public void start() {
        monitor = new Thread(() -> {
            while (true) {
                Thread currentThread = Thread.currentThread();
                if (currentThread.isInterrupted()) {
                    log.debug("料理后事...");
                    break;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                    log.debug("监控中...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    currentThread.interrupt();      // 重新设置打印标记
                }
            }
        }, "monitor");
        monitor.start();
    }

    // 停止线程
    public void stop() {
        monitor.interrupt();
    }
}
```





## 6、不推荐方法

这些方法已过时，容易破坏同步代码块，造成线程死锁

| 方法名    | **功能说明**         |
| --------- | -------------------- |
| stop()    | 停止线程运行         |
| suspend() | 挂起（暂停）线程运行 |
| resume()  | 恢复线程运行         |





## 7、主线程与守护线程

在 Java 中 Main 方法就是主线程，我们自己创建的线程为辅助线程或者工作线程，工作线程可以通过 `Thread#setDomain()` 方法设置成守护线程，==当所有非守护线程停止，那么守护线程也会停止==，即使还有任务未执行完毕。

- 垃圾回收器线程就是一种守护线程
- Tomcat 中的 Acceptor 和 Poller 线程都是守护线程，所以 Tomcat 接收到 shutdown 命令后，不会等待它们处理完当前请求

**案例**：创建两个线程，t2为守护线程

```java
@Slf4j(topic = "SetDaemon")
public class SetDaemon {

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            log.debug("t1 start...");
            TimeUtil.sleep(2);
            log.debug("线程 {} end...", Thread.currentThread().getName());
        }, "t1");
        Thread t2 = new Thread(() -> {
            log.debug("t2 start...");
            TimeUtil.sleep(3);
            log.debug("线程 {} end...", Thread.currentThread().getName());
        }, "t2");
        t1.start();
        t2.setDaemon(true);
        t2.start();
        log.debug("线程 {} end...", Thread.currentThread().getName());
    }
}
```

![image-20231019203133328](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231019203133328.png) 

从结果可以看出，t2 代码尚未执行结束就停止了。





## 8、线程状态

### 8.1 五种状态

从操作系统层面描述的五种线程状态

![image-20231020185533975](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231020185533975.png) 

- 【初始状态】仅是在语言层面创建了线程对象，还未与操作系统线程关联

- 【可运行状态】（就绪状态）指该线程已经被创建（与操作系统线程关联），可以由 CPU 调度执行

- 【运行状态】指获取了 CPU 时间片运行中的状态

    - 当 CPU 时间片用完，会从【运行状态】转换至【可运行状态】，会导致线程的上下文切换

- 【阻塞状态】

    - 如果调用了阻塞 API，如 BIO 读写文件，这时该线程实际不会用到 CPU，会导致线程上下文切换，进入【阻塞状态】
    - 等 BIO 操作完毕，会由操作系统唤醒阻塞的线程，转换至【可运行状态】与【可运行状态】的区别是，对【阻塞状态】的线程来说只要它们一直不唤醒，调度器就一直不会考虑调度它们

- 【终止状态】表示线程已经执行完毕，生命周期已经结束，不会再转换为其它状态

    

    

### 8.2 六种状态

从 Java API 层面描述。根据 Thread.State 枚举，分为六种状态

![image-20231020185709791](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231020185709791.png) 

- `NEW` 线程刚被创建，但是还没有调用 start() 方法
- `RUNNABLE` 当调用了 start() 方法之后，注意，**Java API** 层面的 RUNNABLE 状态涵盖了 **操作系统** 层面的【可运行状态】、【运行状态】和【阻塞状态】（由于 BIO 导致的线程阻塞，在 Java 里无法区分，仍然认为是可运行）
- `BLOCKED` ， `WAITING` ， `TIMED_WAITING` 都是 **Java API** 层面对【阻塞状态】的细分，后面会在状态转换一节详述
- `TERMINATED` 当线程代码运行结束



**代码实现这六种状态**

```java
/**
 * @author xiaozhi
 */
@Slf4j(topic = "ThreadState")
public class ThreadState {

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            log.debug("t1...");
        });

        Thread t2 = new Thread(() -> {
            while (true) {

            }
        });
        t2.start();

        Thread t3 = new Thread(() -> {
            try {
                t2.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t3.start();

        Thread t4 = new Thread(() -> {
            synchronized (ThreadState.class) {
                TimeUtil.sleep(10000000);
            }
        });
        t4.start();

        Thread t5 = new Thread(() -> {
            synchronized (ThreadState.class) {
                TimeUtil.sleep(10000000);
            }
        });
        t5.start();

        Thread t6 = new Thread(() -> {
            log.debug("welcome...");
        });
        t6.start();

        log.debug("六种线程状态如下：");
        log.debug(String.valueOf(t1.getState()));
        log.debug(String.valueOf(t2.getState()));
        log.debug(String.valueOf(t3.getState()));
        log.debug(String.valueOf(t4.getState()));
        log.debug(String.valueOf(t5.getState()));
        log.debug(String.valueOf(t6.getState()));
    }
}
```

![image-20231020191224408](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231020191224408.png) 







# 三、共享模型之管程

## 1、共享带来的问题

### 加减例子

有两个线程：

- t1 线程对变量 count 进行 5000 次加加
- t2 线程对变量 count 进行 5000 次减减

预想的结果输出最后 count 为 0

```java
@Slf4j(topic = "Test01")
public class Test01 {

    static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5000; i++) {
                count++;
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5000; i++) {
                count--;
            }
        }, "t2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        log.info("count={}", count);
    }
}
```

结果显示：

![image-20231021171652736](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231021171652736.png) 

这个结果和我们设想的不一样，这是因为两个线程增强共享资源导致的问题



### 问题解析

我们的 `i++` 和 `i--` 操作的 `JVM字节码指令` 如下

```java
// i++
getstatic count // 获取静态变量i的值
iconst_1 	    // 准备常量1
iadd 		   // 自增
putstatic i 	// 将修改后的值存入静态变量i
```

```java
// i--
getstatic count  // 获取静态变量i的值
iconst_1 		// 准备常量1
isub 			// 自减
putstatic count  // 将修改后的值存入静态变量i
```

可以看到指令有四条，所以加加减减操作并不是原子操作。

如果在单线程中执行 `i++` 和 `i--` 操作是没有问题的，他们会按照顺序依次往下执行。

但是在多线程中执行就会出现共享资源争抢的问题

![image-20231021174418600](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231021174418600.png) 

上图是两个线程的时序图，可以看到最后的结果是负数，这是因为执行的操作不是原子性的，导致在最后写回的时候强制切换，那么当再次切换回来的时候还是执行写入操作，而这个时候的共享资源 i 已经被另外一个线程重新赋值，所以当前线程提交就会覆盖之前的线程修改的值，也就会导致我们的结果出现问题。

因此最后的结果也可能会出现是正数的情况，和我们设想的结果依然不同。



### 临界区 Critical Section 和 竞态条件 Race Condition

**临界区**

多个线程访问同一个共享变量时会发生指令交错，导致结果出现偏差。

一段代码内如果存在对共享资源访问的多线程读写操作，称这段代码快为临界区

举例

```java
static int counter = 0;
static void increment() 
// 临界区
{ 
 counter++;
}
static void decrement() 
// 临界区
{ 
 counter--;
}
```

counter 是共享资源，多个线程对它进行读写操作，加加减减就是读写操作。



**竞态条件**

多个线程在临界区内执行，由于代码的**执行序列不同**而导致结果无法预测，称之为发生了**竞态条件**





## 2、synchronized解决方案

### 概述

为了避免临界区的竞态条件发生，我们可以有以下方案：

- 阻塞式的解决方案：`synchronized`、`Lock`
- 非阻塞式的解决方案：原子变量

本次我们使用 `synchronized` 解决上述问题，俗称的【对象锁】，它采用互斥的方式让同一时刻至多有一个线程持有【对象锁】，其他线程如果想要获取这个【对象锁】就要阻塞等待持有【对象锁】的线程释放锁，这样来保证拥有锁的线程可以完整执行完临界区的代码，不用担心线程上下文切换到出现的问题。

> **注意**
>
> 虽然 java 中互斥和同步都可以采用 synchronized 关键字来完成，但它们还是有区别的：
>
> - 互斥是保证临界区的竞态条件发生，同一时刻只能有一个线程执行临界区代码
> - 同步是由于线程执行的先后、顺序不同、需要一个线程等待其它线程运行到某个点

**语法**

```java
synchronized(对象) // 线程1， 线程2(blocked)
{
 临界区代码
}
```





### 使用

接下来我们使用 `synchronized` 来解决之前的问题

![image-20231021224648733](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231021224648733.png) 

给临界区的代码都加上同步代码块，接下来我们看一下结果是不是我们预想的

![image-20231021224725541](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231021224725541.png) 

count = 0，结果和我们设想的一样，nice~~~



### 面向对象改进

```java
@Slf4j(topic = "test02")
public class Test02 {

    public static void main(String[] args) throws InterruptedException {
        Option option = new Option();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5000; i++) {
                option.increase();
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5000; i++) {
                option.decrease();
            }
        }, "t2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        log.debug("count={}", option.getCount());
    }
}

class Option {
    private int count;

    public void increase() {
        synchronized (this) {
            count++;
        }
    }

    public void decrease() {
        synchronized (this) {
            count--;
        }
    }

    public int getCount() {
        return count;
    }
}
```





## 3、方法上的 synchronized

```java
public void test01() {

}
// 相当于
public void test01() {
    synchronized (this) {

    }
}
```

```java
public static void test02() {

}
// 相当于
public static void test02() {
    synchronized (Test03.class) {

    }
}
```

可以看到成员方法和静态方法锁住的对象是不同的：

成员方法：this

静态方法：类

**要注意这两者的区别，锁的东西不一样，那么获取的时候也要是这样东西才行**。





### 习题：“线程八锁”

"线程八锁"是出题人起的名字



**情况1**：12 或 21

```java
@Slf4j(topic = "c.Number")
class Number{
    public synchronized void a() {
    	log.debug("1");
	}
	public synchronized void b() {
 		log.debug("2");
 	}
}
public static void main(String[] args) {
    Number n1 = new Number();
    new Thread(()->{ n1.a(); }).start();
    new Thread(()->{ n1.b(); }).start();
}
```

**情况2**：1s后12，或 2 1s后 1

```java
@Slf4j(topic = "c.Number")
class Number{
     public synchronized void a() {
         sleep(1);
         log.debug("1");
     }
     public synchronized void b() {
         log.debug("2");
     }
}
public static void main(String[] args) {
     Number n1 = new Number();
     new Thread(()->{ n1.a(); }).start();
     new Thread(()->{ n1.b(); }).start();
}
```

**情况3**：3 1s 12 或 23 1s 1 或 32 1s 1

```java
@Slf4j(topic = "c.Number")
class Number{
    public synchronized void a() {
        sleep(1);
        log.debug("1");
    }
    public synchronized void b() {
        log.debug("2");
    }
    public void c() {
        log.debug("3");
    }
}

public static void main(String[] args) {
    Number n1 = new Number();
    new Thread(()->{ n1.a(); }).start();
    new Thread(()->{ n1.b(); }).start();
    new Thread(()->{ n1.c(); }).start();
}
```

**情况4**：2 1s 后 1

```java
@Slf4j(topic = "c.Number")
class Number{
    public synchronized void a() {
     sleep(1);
     log.debug("1");
    }
    public synchronized void b() {
     log.debug("2");
    }
}
public static void main(String[] args) {
    Number n1 = new Number();
    Number n2 = new Number();
    new Thread(()->{ n1.a(); }).start();
    new Thread(()->{ n2.b(); }).start();
}
```

**情况5**：2 1s 后 1

```java
@Slf4j(topic = "c.Number")
class Number{
    public static synchronized void a() {
     sleep(1);
     log.debug("1");
    }
    public synchronized void b() {
     log.debug("2");
    }
}
public static void main(String[] args) {
    Number n1 = new Number();
    new Thread(()->{ n1.a(); }).start();
    new Thread(()->{ n1.b(); }).start();
}
```

**情况6**：1s 后12， 或 2 1s后 1

```java
@Slf4j(topic = "c.Number")
class Number{
    public static synchronized void a() {
        sleep(1);
        log.debug("1");
    }
    public static synchronized void b() {
        log.debug("2");
    }
}
public static void main(String[] args) {
    Number n1 = new Number();
    new Thread(()->{ n1.a(); }).start();
    new Thread(()->{ n1.b(); }).start();
}
```

**情况7**：2 1s 后 1

```java
@Slf4j(topic = "c.Number")
class Number{
    public static synchronized void a() {
        sleep(1);
        log.debug("1");
    }
    public synchronized void b() {
        log.debug("2");
    }
}
public static void main(String[] args) {
    Number n1 = new Number();
    Number n2 = new Number();
    new Thread(()->{ n1.a(); }).start();
    new Thread(()->{ n2.b(); }).start();
}
```

**情况8**：1s 后12， 或 2 1s后 1

```java
@Slf4j(topic = "c.Number")
class Number{
    public static synchronized void a() {
        sleep(1);
        log.debug("1");
    }
    public static synchronized void b() {
        log.debug("2");
    }
}
public static void main(String[] args) {
    Number n1 = new Number();
    Number n2 = new Number();
    new Thread(()->{ n1.a(); }).start();
    new Thread(()->{ n2.b(); }).start();
}
```





## 4、变量线程安全分析

**局部变量线程安全分析**

- 如果它们没有共享，则线程安全
- 如果它们被共享了，根据它们的状态是否能够改变，又分两种情况
    - 如果只有读操作，则线程安全
    - 如果有读写操作，则这段代码是临界区，需要考虑线程安全



**局部变量线程安全分析**

- 局部变量是线程安全的

- 但局部变量引用的对象则未必

    - 如果该对象没有逃离方法的作用访问，它是线程安全的

        比如在一个方法内使用的局部变量的值不是在方法内的，而是存在于外部，那么这个时候就会有线程安全问题

    - 如果该对象逃离方法的作用范围，需要考虑线程安全



### 局部变量线程安全分析

#### 基本数据类型

```java
public static void test1() {
    int i = 10;
    i++;
}
```

每个线程调用 test1 方法时，局部变量会在每个线程的栈帧内存中创建多份，因此不存在共享

```java
// JVM 字节码文件
public static void test1();
    descriptor: ()V
     flags: ACC_PUBLIC, ACC_STATIC
    Code:
        stack=1, locals=1, args_size=0
        0: bipush 10
        2: istore_0
        3: iinc 0, 1
        6: return
    LineNumberTable:
        line 10: 0
        line 11: 3
        line 12: 6
    LocalVariableTable:
        Start Length Slot Name Signature
        3 	  4 	 0 	 i 	  I
```



#### 引用数据类型

它是局部变量但是它的引用是外部的，这就会造成线程安全

```java
@Slf4j
public class Test04 {
    static final int THREAD_NUMBER = 2;
    static final int LOOP_NUMBER = 200;

    public static void main(String[] args) {
        ThreadUnsafe unsafe = new ThreadUnsafe();
        for (int i = 0; i < THREAD_NUMBER; i++) {
            new Thread(() -> {
                unsafe.method01(LOOP_NUMBER);
            }, "t" + i);
        }
    }
}

class ThreadUnsafe {

    List<String> list = new ArrayList<>();
    public void method01(int loopNum) {
        for (int i = 0; i < loopNum; i++) {
            method02();
            method03();
        }
    }
    private void method02() {
        list.add("1");
    }
    private void method03() {
        list.remove(0);
    }
}
```

在这段代码中就会出现 线程1 未 add，然后线程2 remove 掉了，就会报错

它们两个线程共用一个变量就会产生竞态条件从而产生安全问题。

**解决方式**：

1. 给临界区的代码上锁
2. 将 list 变成局部变量，这样每个线程都有一个独立的 list ，因此也不会产生临界区和竞态条件。



#### 子类安全

我们在之前的基础上将 `ThreadUnsafe类` 的 `private` 方法修改成 `public` ，然后创建一个它的子类，在子类中覆盖 `method2` 和 `method3` 方法

```java
class ThreadSafeSubClass extends ThreadUnsafe {

    @Override
    public void method03() {
        new Thread(() -> {
            list.remove(0);
        }).start();
    }
}
```

 此时我们将  `ThreadUnsafe类` 的实现类换成它的子类。此时还是会产生安全问题。因为它在子类的方法中又创建了一个线程，如果此时我们没有加锁，那么还是会出现线程安全问题，所以如果我们不希望子类来继承破坏我们的方法我们可以使用 `private` 或者将类设为 `final类`，这样就可以保证我们子类的安全。



### 常见线程安全类

- String
- Integer
- StringBuffer
- Random
- Vector
- Hashtable
- java.util.concurrent 包下的类

这里说它们是线程安全的是指，多个线程调用它们同一个实例的某个方法时，是线程安全的。

- 单个方法是原子的
- 多个方法的组合就不是原子的



#### 线程安全方法的组合

分析下面代码是否线程安全？

```java
Map<String, String> map = new Hashtable<>();

public void test(String value) {
    if (map.get("key") == null) {
        map.put("key", value);
    }
}
```

![image-20231023152731635](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023152731635.png) 

它是线程不安全的，单独的方法是原子的，但是他们的组合不是。



#### 不可变类线程安全性

String、Integer 等都是不可变类，因为其内部的状态不可以改变，因此它们的方法都是线程安全的

String 有 replace，substring 等方法【可以】改变值啊，那么这些方法又是如何保证线程安全的呢？

这是因为 String 它会重新生成一个 String 对象进行返回，所以它是线程安全的





## 5、习题

### 卖票

```java
/**
 * @author xiaozhi
 *
 * 卖票
 */
@Slf4j(topic = "Ticket")
public class ExerciseSell {

    public static void main(String[] args) throws InterruptedException {
        TicketWindow window = new TicketWindow(2000);
        List<Thread> threads = new ArrayList<>();
        List<Integer> sellCount = new Vector<>();
        for (int i = 0; i < 2000; i++) {
            Thread thread = new Thread(() -> {
                int count = window.sell(randomCount());
                sellCount.add(count);
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        log.debug("售出票：{}", sellCount.stream().mapToInt(i -> i).sum());
        log.debug("余票：{}", window.getCount());
    }

    public static Random random = new Random();
    public static int randomCount() {
        return random.nextInt(5) + 1;   // 生成1-5
    }
}

class TicketWindow {

    private int count;      // 总票数

    public TicketWindow(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    /**
     * 购票
     * @param amount    票数
     * @return          购买的票数
     */
    public int sell(int amount) {
        if (this.count >= amount) {  // 如果有票
            this.count -= amount;
            return amount;
        }
        return 0;
    }
}
```

![image-20231023154718421](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023154718421.png) 

结果显示错误，这是因为我们没有加锁的原因。此时的临界区是在 `sell` 方法处，所以我们可以对它加锁

```java
public synchronized int sell(int amount) {
    if (this.count >= amount) {  // 如果有票
        this.count -= amount;
        return amount;
    }
    return 0;
}
```

这样就没问题了





### 转账

```java
/**
 * @author xiaozhi
 *
 * 转账
 */
@Slf4j(topic = "TransferMoney")
public class ExerciseTransfer {

    public static void main(String[] args) throws InterruptedException {
        Account a = new Account(2000);
        Account b = new Account(2000);
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                a.transfer(b, randomAmount());
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                b.transfer(a, randomAmount());
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        log.debug("转账前后总金额：{}", a.getMoney() + b.getMoney());
    }

    // Random 为线程安全
    static Random random = new Random();
    // 随机 1~100
    public static int randomAmount() {
        return random.nextInt(100) + 1;
    }
}

class Account {

    private int money;

    public Account(int money) {
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    /**
     * 转账
     * @param target    转账账户
     * @param money     金额
     */
    public void transfer(Account target, int money) {
        if (this.money >= money) {
            this.setMoney(this.getMoney() - money);
            target.setMoney(target.getMoney() + money);
        }
    }
}
```

![image-20231023155730725](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023155730725.png) 

这钱怎么会越来越多呢，所以我们需要给临界区的代码上锁，防止线程不安全的现象发生

我们从代码中可以看出来临界区还是 `Account#transfer` 方法，所以我们给这个方法解锁可以吗？

![image-20231023160100532](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023160100532.png) 

![image-20231023160109203](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023160109203.png) 

结果显示还是不对，这是为什么？

**分析**：

在方法上加 synchronized 锁住的是 this 对象，但是在方法中我们还有另外一个 Account 对象，也就是说这两外一个对象没有锁住所以导致问题的发生。

**解决方法**：

我们锁住的不能只是 this，Account 对象也要锁住，他们两个都是 Account 类，所以锁住 Account.class 才是正解

![image-20231023160436720](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023160436720.png) 

![image-20231023160442212](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023160442212.png) 

结果显示是正确的，nice~~~





## 6、Monitor

### Java对象头

以32位虚拟机为例

**普通对象**

![image-20231023203904118](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023203904118.png) 

**数组对象**

![image-20231023203913752](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023203913752.png) 

**说明**：

- Mark Word（标记字）：主要用来表示对象的线程锁状态，另外还可以用来配合GC、存放该对象的hashCode；
- Klass Word：是一个指向方法区中Class信息的指针，意味着该对象可随时知道自己是哪个Class的实例；
- 还有一些在这里不列举....
- 对象还有对象体和对齐字节，这里我们不展开

其中 MarkWord 结果为

![image-20231023203932565](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023203932565.png) 

64 位虚拟机 MarkWord

![image-20231023203947925](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023203947925.png) 

**注意**：不是整个表表示一个 `Mark word`，这是例举了会出现的 `Mark Word`，没一行都是代表不同的状态

可以看到表中有五种状态：

1. Normal 是正常状态，就是没上锁的状态
2. Biased 是偏向锁
3. Lightweight Locked 是轻量级锁
4. Heavyweight Locked 是重量级锁
5. GC标记

对应不同的状态会有不同的功能，下面我们就对中间四种状态进行讲解



### 原理之 Monitor（锁）

Monitor 翻译成 监视器或管程

每个Java 对象都可以关联一个 Monitor 对象，如果使用 `synchronized` 给对象上锁（重量级锁）之后，该对象的 `Mrak Word` 中就会被设置指向 Monitor 对象的指针

**注意**：

- 没有使用 `synchronized` 是不会关联 Monitor 对象的
- 只有 `synchronized` 锁住的同一个对象才能被 Monitor 进行管理

 Monitor 结构如下：

![image-20231023210615351](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023210615351.png)  

**说明**：

- 每个对象都可以关联一个 `Monitor`
- `Monitor` 中的 `Owner` 刚开始是为 null 的
- 当 Thread-2 执行 `synchronized(obj)` 就会将 `Owner` 设置为 `Thread-2` ，Monitor 只有一个 Owner
- 当 Owner 不为 null，此时后面来的线程就要进入到 EntryList 中阻塞等待 Owner 再次为空
- 当 Thread-2 执行完 synchronized 中的代码时，就会唤醒 EntryList 中的线程进行竞争，竞争的时是非公平的
- 图中的 Thread-0 和 Thread-1 是之前获过锁的，但条件不满足所以进入 WAITING 状态的线程，后面讲到 wait-notify 会分析





### 原理之 synchronized

我们可以从字节码层面来看一下 `synchronized` 做了什么操作

```java
public class Test06 {

    static final Object lock = new Object();
    static int count = 0;
    public static void main(String[] args) {
        synchronized (lock) {
            count++;
        }
    }
}
```

```java
javac Test06.java		// 编译
javap -c Test06.class 	// 查看字节码文件
```

```java
public class com.xiaozhi.sharedmodel03.Test06 {
  static final java.lang.Object lock;

  static int count;

  public com.xiaozhi.sharedmodel03.Test06();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: getstatic     #2                  // <- lock引用 （synchronized开始）
       3: dup
       4: astore_1						  // lock引用 ->  slot 1
       5: monitorenter					  // 将 lock 对象 Mark Word 置为Monitor指针，这个是由c去做的
       6: getstatic     #3                  // <- i
       9: iconst_1						  // 准备常数1
      10: iadd							  // +1
      11: putstatic     #3                  // -> i
      14: aload_1						  // <- lock引用 
      15: monitorexit					  // 将 lock 对象 Mark Word 重置，唤醒 EntryList
      16: goto          24
      19: astore_2						  // e -> slot 2
      20: aload_1						  // <- lock引用
      21: monitorexit					  // 将 lock对象 MarkWord 重置, 唤醒 EntryList
      22: aload_2						  //  <- slot 2 (e)
      23: athrow						  // throw e
      24: return
    Exception table:					   // 异常表：6 - 16 行的位置如果发生错误就跳到 19 行执行
       from    to  target type			    // 19 行开始是释放锁的操作，所以发生异常之后也还是会释放锁的
           6    16    19   any
          19    22    19   any

  static {};
    Code:
       0: new           #4                  // class java/lang/Object
       3: dup
       4: invokespecial #1                  // Method java/lang/Object."<init>":()V
       7: putstatic     #2                  // Field lock:Ljava/lang/Object;
      10: iconst_0
      11: putstatic     #3                  // Field count:I
      14: return
}

```





### 原理之 synchronized进阶

#### 小故事

故事角色

- 老王 - JVM
- 小南 - 线程
- 小女 - 线程
- 房间 - 对象
- 房间门上 - 防盗锁 - Monitor
- 房间门上 - 小南书包 - 轻量级锁
- 房间门上 - 刻上小南大名 - 偏向锁
- 批量重刻名 - 一个类的偏向锁撤销到达 20 阈值
- 不能刻名字 - 批量撤销该类对象的偏向锁，设置该类不可偏向

小南要使用房间保证计算不被其它人干扰（原子性），最初，他用的是防盗锁，当上下文切换时，锁住门。这样，

即使他离开了，别人也进不了门，他的工作就是安全的。

但是，很多情况下没人跟他来竞争房间的使用权。小女是要用房间，但使用的时间上是错开的，小南白天用，小女

晚上用。每次上锁太麻烦了，有没有更简单的办法呢？

小南和小女商量了一下，约定不锁门了，而是谁用房间，谁把自己的书包挂在门口，但他们的书包样式都一样，因

此每次进门前得翻翻书包，看课本是谁的，如果是自己的，那么就可以进门，这样省的上锁解锁了。万一书包不是

自己的，那么就在门外等，并通知对方下次用锁门的方式。

后来，小女回老家了，很长一段时间都不会用这个房间。小南每次还是挂书包，翻书包，虽然比锁门省事了，但仍

然觉得麻烦。

于是，小南干脆在门上刻上了自己的名字：【小南专属房间，其它人勿用】，下次来用房间时，只要名字还在，那

么说明没人打扰，还是可以安全地使用房间。如果这期间有其它人要用这个房间，那么由使用者将小南刻的名字擦

掉，升级为挂书包的方式。

同学们都放假回老家了，小南就膨胀了，在 20 个房间刻上了自己的名字，想进哪个进哪个。后来他自己放假回老

家了，这时小女回来了（她也要用这些房间），结果就是得一个个地擦掉小南刻的名字，升级为挂书包的方式。老

王觉得这成本有点高，提出了一种批量重刻名的方法，他让小女不用挂书包了，可以直接在门上刻上自己的名字

后来，刻名的现象越来越频繁，老王受不了了：算了，这些房间都不能刻名了，只能挂书包





#### 1.轻量级锁

轻量级锁的使用场景：如果一个对象虽然有多个线程访问，但是每个线程不访问的时间不重叠（也就是没有竞争），那么可以使用轻量锁来优化

在Java中，轻量锁对于使用者是透明的，它会自动变成轻量锁，它的语法依然是 `synchronized`

假设有两个方法同步块，利用同一个对象加锁

```java
static final Object obj = new Object();
public static void method() {
	synchronized(obj) {
        // 同步块
    }
}
public static void method2() {
	synchronized(obj) {
        // 同步块B
    }
}
```

加锁的执行流程如下：

1.创建锁记录（Lock Record）对象，每个线程的栈帧都会包含一个锁记录的结构，内部可以存储锁定对象的`Mark Word`

![image-20231024160329889](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024160329889.png) 

2.接着让锁记录中 `Object reference` 指向锁对象，并尝试用 cas 替换 Object 的 `Mark Word`，将 `Mark Word` 的值存入 Lock Record中，将 `lock recode 地址 00` 存入到 Ojbect 中，完成一个替换

![image-20231024160708393](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024160708393.png) 

3.cas 替换会有成功和失败两种情况

cas 替换成功，对象头存储了 `锁记录地址和状态 00`，表示由该线程给对象加上了 轻量级锁，这时图表示如下：

![image-20231024160947324](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024160947324.png) 

cas 替换失败，有两种情况：

- 其他线程拥有该对象的轻量级锁，这时表明有竞争，进入锁膨胀过程

- 自己执行了 synchronized 锁重入，那么再添加一条 Lock Record 作为重入的计数

    ![image-20231024161237023](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024161237023.png) 

4.当退出 synchronized 代码块时（解锁时）如果有取值为 null 的锁记录，表示是重入的，这时重置锁记录，表示重入计数减一

![image-20231024160947324](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024160947324.png) 

5.当退出 synchronized 代码快（解锁时）所记录的值不为 null，这时使用 cas 将 Mark Word 的值恢复给对象头

- 成功，则解锁成功
- 失败，说明轻量级锁进行了锁膨胀升级为了重量级锁，进入重量级锁的解锁流程



#### 2.锁膨胀

如果在尝试加轻量锁的过程中，cas 操作无法成功，这时一种情况就是其他线程参入竞争想要给对象加上轻量级锁，这时需要进行锁膨胀，将轻量级锁变为重量级锁。

```java
static final Object obj = new Object();
public static void method() {
	synchronized(obj) {
        // 同步块
    }
}
```

当 Thread-1 进行轻量级加锁时，Thread-0 已经给该对象加上了轻量级锁

![image-20231024162017989](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024162017989.png) 

此时 Thread-1 加轻量级锁失败，进入锁膨胀流程

- 既为对象申请 Monitor 锁，让 Object 指向重量级锁地址
- 然后自己进入 Monitor 的 EntryList 进行 BLOCKED

![image-20231024162449107](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024162449107.png) 

当 Thread-0 退出同步代码块解锁时，使用 cas 将 Mark Word 的值回复给对象头，失败。这时会进入重量级锁流程，就是按照 Monitor 地址找到 Monitor 对象，设置 Owner 为 null，唤醒 EntryList 中 BLOCKED 的线程



#### 3.自旋优化

重量级锁竞争的时候，还可以使用自旋来进行优化，自旋就是循环获取锁，获取到就不需要唤醒直接上位。如果当前线程自旋成功（即这时候持锁的县城已经退出了同步块，释放了锁），这时当前线程就可以避免阻塞。如果自旋失败那么就会进入阻塞状态。

**注意**：

- 自旋会占用 CPU 时间，单核 CPU 自旋就是浪费，多核 CPU 自旋才能发挥优势
- Java6 之后自旋是自适应的，比如对象刚刚的依次自旋操作成功过，那么认为这次自旋成功的可能性就会高，就会多自旋几次；反之，就会少自旋或者干脆不自旋，比较智能化
- Java7 之后不能控制是否开启自旋功能

**自选重试成功的情况**：

![image-20231024172433301](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024172433301.png) 

**自旋重试失败的情况**：
![image-20231024172449039](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024172449039.png) 



#### 4.偏向锁

##### 概述

轻量锁在没有竞争时（就自己这个线程），每次重入任需要执行 CAS 操作

![image-20231024161237023](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024161237023.png) 

上图是重入一次的 CAS 操作，这样的操作比较耗资源，所以要进行优化。

Java6 中引入了偏向所来做进一步优化：

只有线程第一次使用 CAS 将线程 ID 设置到对象的 Mark Word 头，之后发现如果还是这个线程 ID，不用重新 CAS。以后只要不发生竞争，那么这个对象就归该线程所有。

**下面是一个优化的例子：轻量锁 -> 偏向锁**

```java
static final Object obj = new Object();
public static void m1() {
    synchronized( obj ) {
        // 同步块 A
        m2();
    }
}
public static void m2() {
    synchronized( obj ) {
        // 同步块 B
        m3();
    }
}
public static void m3() {
    synchronized( obj ) {
        // 同步块 C
    }
}
```

![image-20231024173257250](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024173257250.png) 

![image-20231024173304671](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024173304671.png) 



##### 偏向状态

回忆一下对象头格式

![image-20231024173417288](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024173417288.png) 

一个对象创建时：

- 如果开启了偏向锁（默认开启），那么创建对象后，Mark Word 值为 0x05，即最后三位为 101 ，此时它的 thraed、epoch、age 都为 0
- 偏向锁默认是延时的，不会在程序启动的时立即生效，我们可以使用 `sleep方法` 来等待它加载，也可以加 VM参数 `-XX:BiasedLockingStartupDelay=0` 来禁用延迟
-  如果没有开启偏向锁，那么对象创建后，Mark Word 值为 0x01 即最后三位为 001，此时它的hashcode、age都为0，第一次使用 hashcode 时才会赋值

**测试偏向锁**

这里使用第三方工具 jol 来查看对象头信息

```xml
<dependency>
    <groupId>org.openjdk.jol</groupId>
    <artifactId>jol-core</artifactId>
    <version>0.10-TEST</version>
</dependency>
```

```java
// 添加虚拟机参数 -XX:BiasedLockingStartupDelay=0 
public static void main(String[] args) throws IOException {
    Dog d = new Dog();
    ClassLayout classLayout = ClassLayout.parseInstance(d);
    new Thread(() -> {
        log.debug("synchronized 前");
        System.out.println(classLayout.toPrintableSimple(true));
        synchronized (d) {
        	log.debug("synchronized 中");
        	System.out.println(classLayout.toPrintableSimple(true));
    	}
        log.debug("synchronized 后");
    	System.out.println(classLayout.toPrintableSimple(true));
    }, "t1").start();
}
```

输出结果：

```java
11:08:58.117 c.TestBiased [t1] - synchronized 前
00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000101 
11:08:58.121 c.TestBiased [t1] - synchronized 中
00000000 00000000 00000000 00000000 00011111 11101011 11010000 00000101 
11:08:58.121 c.TestBiased [t1] - synchronized 后
00000000 00000000 00000000 00000000 00011111 11101011 11010000 00000101
```

从结果可以看出来 默认是加的偏向锁



**测试禁用偏向锁**

在上面测试代码运行时在添加 VM 参数 -XX:-UseBiasedLocking 禁用偏向锁

输出

```java
11:13:10.018 c.TestBiased [t1] - synchronized 前
00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
11:13:10.021 c.TestBiased [t1] - synchronized 中
00000000 00000000 00000000 00000000 00100000 00010100 11110011 10001000 
11:13:10.021 c.TestBiased [t1] - synchronized 后
00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001
```

禁用之后上锁加的是轻量级锁



##### 撤销偏向锁情况

###### 1、对象调用 hashCode

调用了对象的 hashCode，但偏向锁的对象 MarkWord 中存储的是线程 id，如果调用 hashCode 会导致偏向锁被撤销

- 轻量级锁会在锁记录中记录 hashCode
- 重量级锁会在 Monitor 中记录 hashCode

结果输出：

```java
11:22:10.386 c.TestBiased [main] - 调用 hashCode:1778535015 
11:22:10.391 c.TestBiased [t1] - synchronized 前
00000000 00000000 00000000 01101010 00000010 01001010 01100111 00000001 
11:22:10.393 c.TestBiased [t1] - synchronized 中
00000000 00000000 00000000 00000000 00100000 11000011 11110011 01101000 
11:22:10.393 c.TestBiased [t1] - synchronized 后
00000000 00000000 00000000 01101010 00000010 01001010 01100111 00000001
```

结果可以看出来，当对象调用 hashCode ，就会导致偏向锁被撤销



###### 2、其它线程使用对象

当有其它线程使用偏向锁对象时，**会将偏向锁升级为轻量级锁**

```java
private static void test2() throws InterruptedException {
    Dog d = new Dog();
    Thread t1 = new Thread(() -> {
        synchronized (d) {
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple(true));
        }
        synchronized (TestBiased.class) {
            TestBiased.class.notify();
        }
        // 如果不用 wait/notify 使用 join 必须打开下面的注释
        // 因为：t1 线程不能结束，否则底层线程可能被 jvm 重用作为 t2 线程，底层线程 id 是一样的
         /*try {
            System.in.read();
         } catch (IOException e) {
         e.printStackTrace();
         }*/
    }, "t1");
    t1.start();
    Thread t2 = new Thread(() -> {
        synchronized (TestBiased.class) {
            try {
                TestBiased.class.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.debug(ClassLayout.parseInstance(d).toPrintableSimple(true));
        synchronized (d) {
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple(true));
        }
        log.debug(ClassLayout.parseInstance(d).toPrintableSimple(true));
    }, "t2");
    t2.start();
}
```

结果输出

```java
[t1] - 00000000 00000000 00000000 00000000 00011111 01000001 00010000 00000101 
[t2] - 00000000 00000000 00000000 00000000 00011111 01000001 00010000 00000101 
[t2] - 00000000 00000000 00000000 00000000 00011111 10110101 11110000 01000000 
[t2] - 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001
```



###### 3、调用 wait/notify

它会让偏向锁变成重量级锁

```java
public static void main(String[] args) throws InterruptedException {
    Dog d = new Dog();
    Thread t1 = new Thread(() -> {
        log.debug(ClassLayout.parseInstance(d).toPrintableSimple(true));
        synchronized (d) {
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple(true));
            try {
                d.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple(true));
        }
    }, "t1");
    t1.start();
    new Thread(() -> {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (d) {
            log.debug("notify");
            d.notify();
        }
    }, "t2").start();
}
```

结果输出

```java
[t1] - 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000101 
[t1] - 00000000 00000000 00000000 00000000 00011111 10110011 11111000 00000101 
[t2] - notify 
[t1] - 00000000 00000000 00000000 00000000 00011100 11010100 00001101 11001010
```



##### 批量重偏向

如果对象虽然被多个线程访问，但没有竞争，这时偏向了线程 t1 的对象任有机会重新偏向 t2，重偏向会重置对象的 Thread ID

当撤销偏向锁阈值超过 20 次后，JVM会觉得是否是偏向错了，于是会给这些对象加锁时重新偏向至加锁线程

```java
private static void test3() throws InterruptedException {
    Vector<Dog> list = new Vector<>();
    Thread t1 = new Thread(() -> {
        for (int i = 0; i < 30; i++) {
            Dog d = new Dog();
            list.add(d);
            synchronized (d) {
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            }
        }
        synchronized (list) {
            list.notify();
        }
    }, "t1");
    t1.start();

    Thread t2 = new Thread(() -> {
        synchronized (list) {
            try {
                list.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.debug("===============> ");
        for (int i = 0; i < 30; i++) {
            Dog d = list.get(i);
            log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            synchronized (d) {
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            }
            log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
        }
    }, "t2");
    t2.start();
}
```

运行输出

```
[t1] - 0 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 1 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 2 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 3 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 4 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 5 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 6 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 7 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 8 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 9 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 10 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 11 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 12 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 13 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 14 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 15 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 16 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 17 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 18 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 19 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 20 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 21 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 22 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 23 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 24 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 25 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 26 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 27 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 28 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t1] - 29 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - ===============> 
[t2] - 0 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 0 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 0 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 1 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 1 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 1 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 2 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 2 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 2 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 3 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 3 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 3 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 4 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 4 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 4 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 5 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 5 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 5 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 6 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 6 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 6 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 7 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 7 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 7 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 8 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 8 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 8 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 9 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 9 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 9 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 10 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 10 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 10 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 11 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 11 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 11 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 12 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 12 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 12 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 13 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 13 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 13 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 14 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 14 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 14 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 15 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 15 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 15 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 16 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 16 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 16 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 17 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 17 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 17 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 18 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 18 00000000 00000000 00000000 00000000 00100000 01011000 11110111 00000000 
[t2] - 18 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 
[t2] - 19 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 19 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 19 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 20 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 20 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 20 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 21 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 21 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 21 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 22 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 22 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 22 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 23 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 23 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 23 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 24 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 24 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 24 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 25 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 25 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 25 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 26 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 26 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 26 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 27 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 27 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 27 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 28 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 28 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 28 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 29 00000000 00000000 00000000 00000000 00011111 11110011 11100000 00000101 
[t2] - 29 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101 
[t2] - 29 00000000 00000000 00000000 00000000 00011111 11110011 11110001 00000101
```

从结果我们可以看出来在 t2 线程从 19 开始之后就是处于偏向锁的状态，没有变成轻量级锁，所以 JVM 它重新偏向了 t2 



##### 批量撤销

当撤销偏向锁阈值超过 40 次后，JVM就会觉得自己偏向错了。于是就会将整个类的所有对象都设置成不可偏向，新建的对象也是不可偏向

```java
static Thread t1,t2,t3;
private static void test4() throws InterruptedException {
    Vector<Dog> list = new Vector<>();
    int loopNumber = 39;
    t1 = new Thread(() -> {
        for (int i = 0; i < loopNumber; i++) {
            Dog d = new Dog();
            list.add(d);
            synchronized (d) {
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            }
        }
        LockSupport.unpark(t2);
    }, "t1");
    t1.start();
    t2 = new Thread(() -> {
        LockSupport.park();
        log.debug("===============> ");
        for (int i = 0; i < loopNumber; i++) {
            Dog d = list.get(i);
            log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            synchronized (d) {
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            }
            log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
        }
        LockSupport.unpark(t3);
    }, "t2");
    t3 = new Thread(() -> {
        LockSupport.park();
        log.debug("===============> ");
        for (int i = 0; i < loopNumber; i++) {
            Dog d = list.get(i);
            log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            synchronized (d) {
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
            }
            log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple(true));
        }
    }, "t3");
    t3.start();
    t3.join();
    log.debug(ClassLayout.parseInstance(new Dog()).toPrintableSimple(true));
}
```



#### 5.锁消除

下面一段代码来测试上锁和没上锁的性能差别

```java
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations=3)
@Measurement(iterations=5)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MyBenchmark {
    static int x = 0;
    @Benchmark
    public void a() throws Exception {
        x++;
    }
    @Benchmark
    public void b() throws Exception {
        Object o = new Object();
        synchronized (o) {
            x++;
        }
    }
}
```

打成 Jar 包

java -jar benchmarks.jar

```java
Benchmark			Mode 	Samples 	Score 	Score error 	Units 
c.i.MyBenchmark.a 	 avgt 	 5 			 1.542 		  0.056 	 ns/op 
c.i.MyBenchmark.b 	 avgt 	 5 			 1.518 		  0.091 	 ns/op
```

结果显示性能基本相近，这是因为 JVM 默认会进行锁消除

JVM 有一个 JIT 编译器，它会将热点代码进行优化，在我们的代码中 b方法 锁住的对象是局部变量，它不会被共享，也就没有安全问题，所以 JIT 编译器就会将这段代码优化直接将锁去掉，所以我们看到的结果性能几乎一样

`-XX:-EliminateLocks` 参数是取消锁消除优化

java -XX:-EliminateLocks -jar benchmarks.jar

```java
Benchmark 			Mode 	Samples Score Score error Units 
c.i.MyBenchmark.a 	 avgt 		5 	1.507 		0.108 ns/op 
c.i.MyBenchmark.b 	 avgt 		5 	16.976 		1.572 ns/op
```

这次我们没有取消锁，可以看到结果相差还是很大的。这也就证明了锁消除的好处。





## 7、wait 和 notify

### 小故事 - 为什么需要wait

由于条件不满足，小南不能继续进行计算

但小南如果一直占用着锁，其它人就得一直阻塞，效率太低

![image-20231024233447429](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024233447429.png) 

于是老王单开了一间休息室（调用 wait 方法），让小南到休息室（WaitSet）等着去了，但这时锁释放开，

其它人可以由老王随机安排进屋

直到小M将烟送来，大叫一声 [ 你的烟到了 ] （调用 notify 方法）

![image-20231024233500102](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024233500102.png) 

小南于是可以离开休息室，重新进入竞争锁的队列

![image-20231024233509519](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024233509519.png) 





### API 介绍

| 方法名       | static | 描述                                              |
| ------------ | ------ | ------------------------------------------------- |
| wait()       |        | 让进入 object 监视器的线程到 waitSet 等待         |
| wait(long n) |        | 有限时间的等待，n毫秒结束等待，或者被 notify 唤醒 |
| notify()     |        | 在 object 上正在 waitSet 等待的线程中挑一个唤醒   |
| notifyAll()  |        | 让 object 上正在 waitSet 等待的线程全部唤醒       |

它们都是线程之间进行协作的手段，都属于 Object 对象的方法。必须获得此对象的锁，才能调用这几个方法

也就是说单独使用不行的，必须和 synchronized 一起使用才可以，不然会报错

```java
Exception in thread "main" java.lang.IllegalMonitorStateException
```

下面做一个案例体会一下三个方法的用处

```java
@Slf4j(topic = "TestWaitAndNotify")
public class TestWaitAndNotify {

    final static Object lock = new Object();

    public static void main(String[] args) {
        new Thread(() -> {
            log.debug("执行...");
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.debug("其他代码...");
        }, "t1").start();
        new Thread(() -> {
            log.debug("执行...");
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.debug("其他代码...");
        }, "t2").start();
        TimeUtil.sleep(1);
        synchronized (lock) {
            lock.notify();          // 随机唤醒一个线程
            // lock.notifyAll();    // 唤醒全部线程
        }
    }
}
```

结果：

```java
23:42:47.220 [t1] DEBUG TestWaitAndNotify - 执行...
23:42:47.220 [t2] DEBUG TestWaitAndNotify - 执行...
23:42:48.221 [t1] DEBUG TestWaitAndNotify - 其他代码...
```

可以看到 t2 线程没有被唤醒

我们使用 `Object#notify` 方法看一下如何

![image-20231024234439125](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231024234439125.png) 



### wati / notify 的正确使用姿势

#### sleep(n) 和 wait(n) 的区别

1. sleep 是 Thread 方法，wait 是 Object 的方法
2. sleep 不需要强制和 synchronized 配合使用，但是wait 需要
3. sleep 在睡眠的同时，不会释放对象锁，但 wait 等待的时候释放对象锁
4. 他们的状态都是 TIME_WAITING
5. 

#### **案例**：体会一下两者的区别

##### setp1：sleepd等待

```java
@Slf4j(topic = "SleepAndWaitTest")
public class SleepAndWaitTest {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    TimeUtil.sleep(2);
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                synchronized (room) {
                    log.debug("其他人可以干活了");
                }
            }, "其他人").start();
        }
        TimeUtil.sleep(1);
        new Thread(() -> {
            hasCigarette = true;
            log.debug("烟到了噢！");
        }, "送烟的").start();
    }
}
```

输出

```java
23:55:42.344 [小南] DEBUG SleepAndWaitTest - 有烟没？[false]
23:55:42.349 [小南] DEBUG SleepAndWaitTest - 没烟，先歇会！
23:55:43.358 [送烟的] DEBUG SleepAndWaitTest - 烟到了噢！
23:55:44.355 [小南] DEBUG SleepAndWaitTest - 有烟没？[true]
23:55:44.355 [小南] DEBUG SleepAndWaitTest - 可以开始干活了
23:55:44.355 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
23:55:44.355 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
23:55:44.355 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
23:55:44.356 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
23:55:44.356 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
```

**分析**：

- 其他干活的线程，要等小南执行完才能执行，一直阻塞，效率太低
- 小南要休眠 2 秒，烟提前到了也无法立刻醒来，效率低
- 加了 synchronized(room) 后，就好比小南在里面反锁了门睡觉，烟根本没法送进门

**解决方案**：使用 wait - notify 机制



##### setp2：wait - notify 机制

将 sleep 替换成 wait ，然后在送烟线程中唤醒它

```java
@Slf4j(topic = "SleepAndWaitTest")
public class SleepAndWaitTest {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                synchronized (room) {
                    log.debug("其他人可以干活了");
                }
            }, "其他人").start();
        }
        TimeUtil.sleep(1);
        new Thread(() -> {
            hasCigarette = true;
            log.debug("烟到了噢！");
            synchronized (room) {
                room.notify();   
            }
        }, "送烟的").start();
    }
}
```

```java
00:05:30.324 [小南] DEBUG SleepAndWaitTest - 有烟没？[false]
00:05:30.331 [小南] DEBUG SleepAndWaitTest - 没烟，先歇会！
00:05:30.331 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
00:05:30.331 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
00:05:30.331 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
00:05:30.331 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
00:05:30.331 [其他人] DEBUG SleepAndWaitTest - 其他人可以干活了
00:05:31.329 [送烟的] DEBUG SleepAndWaitTest - 烟到了噢！
00:05:31.329 [小南] DEBUG SleepAndWaitTest - 有烟没？[true]
00:05:31.329 [小南] DEBUG SleepAndWaitTest - 可以开始干活了
```

**分析**：解决了其它干活的线程阻塞的问题，但如果有其它线程也在等待条件呢？



##### setp3：notifyAll()

我们继续添加一个送外卖的线程

```java
@Slf4j(topic = "SleepAndWaitTest")
public class SleepAndWaitTest {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();
        new Thread(() -> {
            synchronized (room) {
                log.debug("有外卖没？[{}]", hasTakeout);
                if (!hasCigarette) {
                    log.debug("没外卖，先歇会！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                log.debug("有外卖没？[{}]", hasTakeout);
                if (hasCigarette) {
                    log.debug("可以开始送外卖了...");
                }
            }
        }, "小女").start();
        TimeUtil.sleep(1);
        new Thread(() -> {
            synchronized (room) {
                hasTakeout = true;
                log.debug("外卖到了");
                room.notify();
            }
        }, "送外卖的").start();
    }
}
```

```java
00:12:12.054 [小南] DEBUG SleepAndWaitTest - 有烟没？[false]
00:12:12.058 [小南] DEBUG SleepAndWaitTest - 没烟，先歇会！
00:12:12.058 [小女] DEBUG SleepAndWaitTest - 有外卖没？[false]
00:12:12.058 [小女] DEBUG SleepAndWaitTest - 没外卖，先歇会！
00:12:13.059 [送外卖的] DEBUG SleepAndWaitTest - 外卖到了
00:12:13.059 [小女] DEBUG SleepAndWaitTest - 有外卖没？[true]
00:12:13.059 [小南] DEBUG SleepAndWaitTest - 有烟没？[false]
```

**分析**：

-  notifyAll 唤醒全部线程
- 可以看到线程中的 if 中只有一次判断，一旦条件不成立，就没有重新判断的机会了



##### setp4：解决虚假等待 

解决只有一次机会的问题，使用 while 来解决

```java
if (!hasCigarette) {
    log.debug("没烟，先歇会！");
    try {
        room.wait();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
}
log.debug("有烟没？[{}]", hasCigarette);
if (hasCigarette) {
    log.debug("可以开始干活了");
}
```

将上述代码进行修改

```java
while (!hasCigarette) {
    log.debug("没烟，先歇会！");
    try {
        room.wait();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
}
log.debug("有烟没？[{}]", hasCigarette);
log.debug("可以开始干活了");
```

这样我们就能确保它有多次机会

**小总结**：

下面这是一套模式，它可以确保执行指定条件下的代码

```java
synchronized(lcok) {
	while (条件不成立) {
		lock.wati();
	}
    // 干活
}

// 另外一个线程
synchronized(lock) {
    lock.notifyAll();
}
```





### 原理之wati / notify

![image-20231023210615351](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231023210615351.png) 

- Owner 线程发现条件不满足，调用 wait 方法，即可进入 WaitSet 变为 WAITING 状态
- BLOCKED 和 WAITING 的线程都处于阻塞状态，不占用 CPU 时间片
- BLOCKED 线程会在 Owner 线程释放锁时唤醒
- WAITING 线程会在 Owner 线程调用 notify 或 notifyAll 时唤醒，但唤醒后并不意味着立刻获得锁，仍需进入 EntryList 重新竞争





### 同步模式之保护性暂停

#### 1. 定义

即 Guarded Suspension，用在一个线程等待另一个线程的执行结果

**要点**：

- 如果有一个结果需要从一个线程传递给另一个线程，可以让他们关联一个 `GuardedObject` 对象
- 如果有结果不断从一个线程到另一个线程，那么可以使用消息队列（见生产者消费者章节）
- JDK中，join 的实现，Future 的实现，采用的就是此模式
- 此模式是一个线程等待另外一个线程，所以归类为同步模式

![image-20231025134058284](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231025134058284.png) 

1. t1 循环等待 t2 赋值
2. t2 给 response 赋值之后唤醒 t1
3. t1 获取到值



#### 2. 实现

```java
class GuardedObject {

    private Object response;

    public Object get() {
    	synchronized (this) {
             // 不满足条件需要等待
            while (response == null) {

                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return this.response;
    }

    public void complete(Object response) {
        synchronized (this) {
            this.response = response;
            this.notify();
        }
    }
}
```

**测试**

```java
@Slf4j(topic = "ProtectivePause")
public class ProtectivePause {

    public static void main(String[] args) {
        GuardedObject go = new GuardedObject();
        new Thread(() -> {
            log.debug("start...");
            Object res = go.get();
            log.debug("结果：{}", res);
        }, "t1").start();
        new Thread(() -> {
            log.debug("start...");
            go.complete("我是个靓仔");
            log.debug("结果已传递...");
        }, "t2").start();
    }
}
```

结果显示

```java
13:30:37.070 [t2] DEBUG ProtectivePause - start...
13:30:37.070 [t1] DEBUG ProtectivePause - start...
13:30:37.074 [t2] DEBUG ProtectivePause - 结果已传递...
13:30:37.074 [t1] DEBUG ProtectivePause - 结果：我是个靓仔
```



#### 3. 超时版 GuardedObject

不让 t1 一直等待，我们给它设定一个时间，超过这个时间我们就不等待了

方法改造：complete 方法不变，get 方法发生变化

```java
public Object get(long timeout) {
    synchronized (this) {
        // 开始时间
        long begin = System.currentTimeMillis();
        // 已经经历时间
        long timePassed = 0;
        // 不满足条件需要等待
        while (response == null) {
            // 剩余等待的时间 = 总共等待时间 - 已经经历时间
            long waitTime = timeout - timePassed;
            // 如果剩余等待时间为 0 则退出循环
            if (waitTime <= 0) {
                log.debug("退出循环...");
                break;
            }
            try {
                // 只需要等待剩余的等待时间即可
                this.wait(waitTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 已经经历时间
            timePassed = System.currentTimeMillis() -  begin;
        }
    }
    return this.response;
}
```

**测试**：我们让 t1 等待 2000ms，t2睡眠 3 秒，查看结果如何

```java
public static void main(String[] args) {
    GuardedObject go = new GuardedObject();
    new Thread(() -> {
        log.debug("start...");
        Object res = go.get(2000);
        log.debug("结果：{}", res);
    }, "t1").start();
    new Thread(() -> {
        log.debug("start...");
        TimeUtil.sleep(3);
        go.complete("我是个靓仔");
        log.debug("结果已传递...");
    }, "t2").start();
}
```

```java
13:54:29.144 [t1] DEBUG ProtectivePause - start...
13:54:29.144 [t2] DEBUG ProtectivePause - start...
13:54:31.168 [t1] DEBUG GuardedObject - 退出循环...
13:54:31.168 [t1] DEBUG ProtectivePause - 结果：null
13:54:32.165 [t2] DEBUG ProtectivePause - 结果已传递...
```

结果显示我们提前结束了等待，获取结果为 null



#### 4. 原理之join()

join() 是 Thread 的方法，我们去 Thread 类中查看它的实现，join() 实现的原理其实就是 保护性暂停模式。

```java
public final void join() throws InterruptedException {
    join(0);
}
```

join() 里面调用的是 join(long n) 方法

```java
public final synchronized void join(long millis) throws InterruptedException {
    // 获取开始时间
    long base = System.currentTimeMillis();
    // 已经经历的时间
    long now = 0;
	// 参数校验
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }
	// 如果等待时间为0
    if (millis == 0) {
        // 循环判断线程是否存活，如果存活就执行等待方法
        while (isAlive()) {
            wait(0);
        }
    } else {	// 不为0
        while (isAlive()) {
            // 获取剩余等待的时间 = 总等待时间 - 已经经历的等待时间
            long delay = millis - now;
            // 小于 0 就退出等待
            if (delay <= 0) {
                break;
            }
            // 等待剩余等待时间
            wait(delay);
            // 经历的时间 = 当前时间 - 开始时间
            now = System.currentTimeMillis() - base;
        }
    }
}
```

还有一个 join(long, int) 方法，这个方法它的第二个参数是 纳秒，但是它的具体实现是不会精确到纳秒的，也是毫秒级

```java
if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
    millis++;
}
```

可以看到它如果是大于 500000 就加一毫秒



#### 5. 多任务版 GuardedObject

![image-20231025152753105](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231025152753105.png) 

图中 Futures 中有多个 GuardedObject ，每个 GuardedObject 都有对应的 id，这是为了让 线程能够和对应的 GuardedObject 进行绑定，多个GuardedObject 来完成多任务

**案例**：模拟邮递员送邮件

Furures 就相当于是我们的邮箱，t4、t5、t6 就是我们的邮递员，将信放入到我们的邮箱中，然后由 t1、t2、t3 获取信件

由于我们的 GuradedObject 对象做参数传递不方便，所以我们使用一个中间类来进行解耦，解耦出 【结果等待者】和【结果生产者】，还能同时支持多个任务的管理

**具体实现**：

**给 GuradedObject 对象新增 id、构造器和get方法**

```java
// GuradedObject 
private int id;

public GuardedObject(int id) {
    this.id = id;
}

public int getId() {
    return id;
}
```

**构造中间类**

```java
class MailBoxes {

    private static Map<Integer, GuardedObject> BOXES = new Hashtable<>();

    private static int id = 1;

    // 获取唯一id
    public static synchronized int generateId() {
        return id++;
    }

    // 通过 ID 获取 GO
    public static GuardedObject getGuardedObject(int id) {
        return BOXES.remove(id);
    }
    public static GuardedObject createGO() {
        GuardedObject go = new GuardedObject(generateId());
        BOXES.put(go.getId(), go);
        return go;
    }
    public static Set<Integer> getIds() {
        return BOXES.keySet();
    }
}
```

**业务类构建**：People 和 Postman 类，一个接受邮件，一个送邮件的

```java
@Slf4j(topic = "People")
class People extends Thread{
    @Override
    public void run() {
        GuardedObject go = MailBoxes.createGO();
        log.debug("开始收信 id:{}", go.getId());
        Object mail = go.get(5000);
        log.debug("收到信 id：{}，内容；{}", go.getId(), mail);
    }
}
@Slf4j(topic = "Postman")
class Postman extends Thread {
    private int id;
    private Object mail;

    public Postman(int id, Object mail) {
        this.id = id;
        this.mail = mail;
    }
    @Override
    public void run() {
        GuardedObject go = MailBoxes.getGuardedObject(this.id);
        log.debug("送信 id:{}, 内容:{}", id, mail);
        go.complete(mail);
    }
}
```

**测试**

```java
public class FuturesTest {
    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            new People().start();
        }
        TimeUtil.sleep(1);
        for (Integer id : MailBoxes.getIds()) {
            new Postman(id, "内容：" + id).start();
        }
    }
}
```

**结果输出**

```
16:02:09.243 [Thread-2] DEBUG People - 开始收信 id:3
16:02:09.243 [Thread-0] DEBUG People - 开始收信 id:1
16:02:09.243 [Thread-1] DEBUG People - 开始收信 id:2
16:02:10.250 [Thread-3] DEBUG Postman - 送信 id:3, 内容:内容：3
16:02:10.250 [Thread-4] DEBUG Postman - 送信 id:2, 内容:内容：2
16:02:10.250 [Thread-5] DEBUG Postman - 送信 id:1, 内容:内容：1
16:02:10.250 [Thread-1] DEBUG People - 收到信 id：2，内容；内容：2
16:02:10.250 [Thread-0] DEBUG People - 收到信 id：1，内容；内容：1
16:02:10.250 [Thread-2] DEBUG People - 收到信 id：3，内容；内容：3
```

完美~~~



### 异步模式之生产者消费者

#### 1.概述

![image-20231025170344602](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231025170344602.png) 

**要点**：

- 与前面的 保护性暂停的 GuradedObject 不同，它不需要生产者和消费者一一对应
- 消费队列可以用来平衡生产和消费的线程资源
- 生产者负责产生结果数据，不关心消息如何被处理，而消费者专心处理数据
- 消息队列是有容量的，因为资源是有限的，必须限制它的大小，不然就会导致内存满了服务被停止
- JDK 各种阻塞队列，采用的就是这种模式



#### 2.实现

```java
@Slf4j(topic = "MessageQueue")
class MessageQueue {
    private final LinkedList<Message> LIST = new LinkedList<>();
    private int capacity;

    public MessageQueue(int capacity) {
        this.capacity = capacity;
    }

    public Message take() {
        synchronized (LIST) {
            // 如果队列为空，那么就等待
            while (LIST.isEmpty()) {
                try {
                    log.debug("队列为空，等待中...");
                    LIST.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            Message message = LIST.removeFirst();
            LIST.notifyAll();
            return message;
        }
    }

    public void put(Message message) {
        synchronized (LIST) {
            // 队列的长度达到设置的阈值，等待被消费
            while (LIST.size() >= this.capacity) {
                try {
                    log.debug("队列已满，等待中...");
                    LIST.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            LIST.push(message);
            LIST.notifyAll();
        }
    }
}

class Message {
    private int id;
    private Object content;

    public Message(int id, Object content) {
        this.id = id;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public Object getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", content=" + content +
                '}';
    }
}
```



#### 3.应用

创建三个生产者线程，然后创建一个消费者线程进行消费

```java
@Slf4j(topic = "ProducesAndConsumer")
public class ProducesAndConsumer {

    public static void main(String[] args) {
        MessageQueue messageQueue = new MessageQueue(2);
        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(() -> {
                Message message = new Message(id, "我是靓仔：" + id);
                messageQueue.put(message);
                log.debug("生产一条消息");
            }, "生产者" + i).start();
        }
        new Thread(() -> {
            while (true) {
                Message msg = messageQueue.take();
                log.debug("消费内容：{}", msg.getContent());
            }
        }, "消费者").start();
    }
}
```

输出结果

```java
17:30:56.720 [消费者] DEBUG MessageQueue - 队列为空，等待中...
17:30:56.723 [生产者1] DEBUG ProducesAndConsumer - 生产一条消息
17:30:56.723 [生产者2] DEBUG ProducesAndConsumer - 生产一条消息
17:30:56.723 [生产者0] DEBUG MessageQueue - 队列已满，等待中...
17:30:56.723 [生产者0] DEBUG ProducesAndConsumer - 生产一条消息
17:30:56.723 [消费者] DEBUG ProducesAndConsumer - 消费内容：我是靓仔：2
17:30:56.726 [消费者] DEBUG ProducesAndConsumer - 消费内容：我是靓仔：1
17:30:56.726 [消费者] DEBUG ProducesAndConsumer - 消费内容：我是靓仔：0
17:30:56.726 [消费者] DEBUG MessageQueue - 队列为空，等待中...
```

可以看到生产者生产到第二条的时候就满了就停止生产，然后消费者消费之后它再继续生产，最后消费完成，队列为空



## 8、park & unpark

### API介绍

park & unpark 是 LockSupport 类的方法

| 方法名         | static | 描述               |
| -------------- | ------ | ------------------ |
| park           | true   | 让当前线程暂停执行 |
| unpark(thread) | true   | 恢复某个线程的执行 |



### 应用

**先 park 再 unpark**

```java
public static void test01() {
    Thread t1 = new Thread(() -> {
        log.debug("start...");
        TimeUtil.sleep(1);
        LockSupport.park();
        log.debug("继续执行...");
    }, "t1");
    t1.start();
    TimeUtil.sleep(2);
    log.debug("unpark...");
    LockSupport.unpark(t1);
}
```

输出

```java
22:17:16.387 [t1] DEBUG ParkAndUnpark - start...
22:17:18.391 [main] DEBUG ParkAndUnpark - unpark...
22:17:18.391 [t1] DEBUG ParkAndUnpark - 继续执行...
```



**先 unpark 再 park**

```java
public static void test02() {
    Thread t1 = new Thread(() -> {
        log.debug("start...");
        TimeUtil.sleep(2);
        LockSupport.park();
        log.debug("继续执行...");
    }, "t1");
    t1.start();
    TimeUtil.sleep(1);
    log.debug("unpark...");
    LockSupport.unpark(t1);
}
```

输出

```java
22:18:01.142 [t1] DEBUG ParkAndUnpark - start...
22:18:02.151 [main] DEBUG ParkAndUnpark - unpark...
22:18:03.151 [t1] DEBUG ParkAndUnpark - 继续执行...
```

诶？这怎么 unpark 提前执行了 t1 还会继续执行呢？下面就会讲解它的原理。



### 特点

它和 Object 的 wait & notify 有什么区别吗？

- wait & notify 必须和 Monitor 一起使用，park & unpark 不需要
- park & unpark 刚明确，它可以将指定对应的线程唤醒。而 wait & notify 不行，要么唤醒一个，要么使用 notifyAll 唤醒全部线程，它没有 park & unpark 那么明确
- park & unpark 可以先 unpark，wait & notify 不可以先 notify，notify 必须在 wait 后面才有效



### 原理

每个线程都有一个 Parker 对象，由 _counter、_cond 和 _mutex 三部分组成

![image-20231025223949109](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231025223949109.png) 

1. 当前线程调用 park 方法
2. 检查 _counter，此时它的值为0，获得 mutex 互斥锁
3. 线程进入 _cond条件变量阻塞
4. 设置 _counter=0

![image-20231025224008637](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231025224008637.png) 

1. 调用 Unsafe.unpark 方法，设置 _counter 为 1
2. 唤醒 _cond 条件变量中的线程
3. 线程恢复运行
4. 设置 _counter 为 0

**先 unpark 后 park 还是能执行这是为什么？**

1. 调用 Unsafe.unpark 方法，设置 _counter 为 1
2. 调用 Unsafe.unpark 方法，检查 _counter ，此时值 为1
3. 不进入阻塞状态，继续执行
4. 设置 _counter 为 0





## 9、线程状态转换

![image-20231025224342151](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231025224342151.png) 

假设有线程 t



### 情况1：`NEW -> RUNABLE`

当调用 start() 时，状态由 `NEW -> RUNABLE`



### 情况2：`RUNNABLE <--> WAITING`

1. wait & notify
2. join
3. park & unpark



### 情况3：`RUNNABLE <--> TIMED_WAITING`

1. wait(n) 
2. join(long n) 
3. sleep(long n) 



### 情况4：`RUNNABLE <--> BLOCKED`

- 多个线程获取对象锁，获得锁的线程从 `BLOCKED -> RUNNABLE`
- 其余竞争失败的线程：`RUNNABLE <--> BLOCKED`



### 情况5：`RUNNABLE <--> TERMINATED`

线程执行完成就会从 `RUNNABLE <--> TERMINATED`



## 11、活跃性

### 多把锁

不同业务的线程锁定同一个对象，那么如果多个线程同时执行，就会产生竞争，多个线程只能有一个可以运行，其他线程进入 BLOCKED 状态，那么对于本身没有业务冲突的多个线程我们可以将他们的锁对象分开，更细粒度化

**案例**：现在有一个 Room 对象，里面有两个方法，一个是学习的一个是玩耍的，他们两个方法没有任何的业务交集

```java
public class Test07 {

    public static void main(String[] args) {
        Room room = new Room();
        new Thread(room::study, "t1").start();
        new Thread(room::play, "t2").start();
    }
}

@Slf4j(topic = "room")
class Room {
    private final Object LOCK = new Object();
    public void study() {
        synchronized (LOCK) {
            log.debug("开始学习...");
            TimeUtil.sleep(1);
            log.debug("结束学习...");
        }
    }
    public void play() {
        synchronized (LOCK) {
            log.debug("开始玩耍...");
            TimeUtil.sleep(1);
            log.debug("结束玩耍...");
        }
    }
}
```

输出

```java
16:36:06.700 [t1] DEBUG room - 开始学习...
16:36:07.711 [t1] DEBUG room - 结束学习...
16:36:07.711 [t2] DEBUG room - 开始玩耍...
16:36:08.723 [t2] DEBUG room - 结束玩耍...
```

可以看到学习之后才到玩耍，这是因为 t1 一开始获得了锁，后面才到 t2

这两个方法没有任何关联，所以我们应该给不同业务的方法上不同的锁对象

修改之前的代码：将 LOCK 换成 this

```java
public void play() {
    synchronized (this) {
        log.debug("开始玩耍...");
        TimeUtil.sleep(1);
        log.debug("结束玩耍...");
    }
}
```

再次运行：

```java
16:39:14.066 [t2] DEBUG room - 开始玩耍...
16:39:14.066 [t1] DEBUG room - 开始学习...
16:39:15.073 [t1] DEBUG room - 结束学习...
16:39:15.073 [t2] DEBUG room - 结束玩耍...
```

可以看到两个线程同时执行，它们两个没有竞争，细粒度化有利于程序的运行



### 死锁

#### 什么是死锁

- t1 线程获取了 A对象锁，它现在要获取 B对象锁
- t2 线程获取了 B对象锁，它现在要获取 A对象锁

两个线程都在等待对方释放锁，因此就导致程序一直在等待，这 **你等待我我等待你** 的现象就是死锁

**案例**：

```java
@Slf4j(topic = "DeadLockTest")
public class DeadLockTest {

    private final static Object A = new Object();
    private final static Object B = new Object();

    public static void main(String[] args) {

        new Thread(() -> {
            synchronized (A) {
                log.debug("获取到A锁");
                TimeUtil.sleep(1);
                synchronized (B) {
                    log.debug("获取到B锁");
                }
            }
        }, "t1").start();
        new Thread(() -> {
            synchronized (B) {
                log.debug("获取到B锁");
                TimeUtil.sleep(2);
                synchronized (A) {
                    log.debug("获取到A锁");
                }
            }
        }, "t2").start();
    }
}
```

![image-20231026164644953](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231026164644953.png) 

结果显示程序一直在等待



#### 定位死锁

出现死锁的情况我们应该怎么找哪里出现问题了呢？

- jconsole
- jstack

##### jconsole

1. 输出命令

    ```sh
    jconsole
    ```

    ![image-20231026165800779](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231026165800779.png) 

2. 连接到我们出现问题的程序

    ![image-20231026165844480](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231026165844480.png) 

3. 找到 `线程` 菜单按钮，然后进入点开检查死锁

    ![image-20231026170058188](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231026170058188.png) 

4. 检测出死锁后我们可以点击对应的线程查看线程状态，它里面也告诉了我们那里出现了问题

 

##### jstack

1. 使用 jps 查看 JVM 进程

    ```java
    // jps命令
    36596 Jps
    31656 RemoteMavenServer36
    8280 Launcher
    27340 DeadLockTest			// 这是我们产生死锁的程序
    7420 
    ```

2. 使用 jstack PID 查看进程的线程状态

    ```java
    "t2" #13 prio=5 os_prio=0 tid=0x000002c20a42a000 nid=0x2fd4 waiting for monitor entry [0x000000dca25ff000]
       java.lang.Thread.State: BLOCKED (on object monitor)
            at com.xiaozhi.sharedmodel03.DeadLockTest.lambda$main$1(DeadLockTest.java:33)
            - waiting to lock <0x000000076bee01e0> (a java.lang.Object)
            - locked <0x000000076bee01f0> (a java.lang.Object)
            at com.xiaozhi.sharedmodel03.DeadLockTest$$Lambda$2/1685538367.run(Unknown Source)
            at java.lang.Thread.run(Thread.java:750)
    
    "t1" #12 prio=5 os_prio=0 tid=0x000002c20a550800 nid=0x8240 waiting for monitor entry [0x000000dca24ff000]
       java.lang.Thread.State: BLOCKED (on object monitor)
            at com.xiaozhi.sharedmodel03.DeadLockTest.lambda$main$0(DeadLockTest.java:24)
            - waiting to lock <0x000000076bee01f0> (a java.lang.Object)
            - locked <0x000000076bee01e0> (a java.lang.Object)
            at com.xiaozhi.sharedmodel03.DeadLockTest$$Lambda$1/186276003.run(Unknown Source)
            at java.lang.Thread.run(Thread.java:750)
            
            
    其他内容......
    
    Found one Java-level deadlock:
    =============================
    "t2":
      waiting to lock monitor 0x000002c27fb69748 (object 0x000000076bee01e0, a java.lang.Object),
      which is held by "t1"
            - locked <0x000000076bee01e0> (a java.lang.Object)
            at com.xiaozhi.sharedmodel03.DeadLockTest$$Lambda$1/186276003.run(Unknown Source)
            at java.lang.Thread.run(Thread.java:750)
    
    Found 1 deadlock.
    ```

在输出的信息最后我们看到检测出了死锁，可以看到出现死锁的是 t1 和 t2 线程导致的，在前面我们可以看到 t1 和 t2 线程的状态，它们出现了死锁，其中告诉了我们 t1 和 t2 线程出现死锁的代码在哪里，通过这个我们就可以找到对应的行然后进行修改



#### 案例：哲学家就餐问题

有五个哲学家和五双筷子，每个哲学家需要一双筷子才能吃饭，最后每个哲学家都拥有一支筷子，然后他们都在等待别人放下筷子，因此就出现了死锁

![image-20231026170524512](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231026170524512.png)、

**代码实现**

筷子类

```java
@Slf4j(topic = "筷子")
class Chopstick {
}
```

哲学家类

```java
@Slf4j(topic = "哲学家")
class Philosopher extends Thread{

    private Chopstick left;
    private Chopstick right;
    private String name;

    public Philosopher(String name, Chopstick left, Chopstick right) {
        this.name = name;
        this.left = left;
        this.right = right;
    }

    @Override
    public void run() {
        while (true) {
            // 拿到左边筷子
            synchronized (left) {
                // 拿到右边筷子
                synchronized (right) {
                    eat();
                }
            }
        }
    }

    public void eat() {
        log.debug("{} 吃上了饭", this.name);
        TimeUtil.sleep(1);
    }
}
```

测试

```java
public static void main(String[] args) {
    Chopstick c1 = new Chopstick();
    Chopstick c2 = new Chopstick();
    Chopstick c3 = new Chopstick();
    Chopstick c4 = new Chopstick();
    Chopstick c5 = new Chopstick();
    new Philosopher("苏格拉底", c1, c2).start();
    new Philosopher("柏拉图", c2, c3).start();
    new Philosopher("亚里士多德", c3, c4).start();
    new Philosopher("赫拉克利特", c4, c5).start();
    new Philosopher("阿基米德", c5, c1).start();
}
```

结果输出

![image-20231026171739839](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231026171739839.png) 

可以看到执行几次后死锁了

使用 jconsole 检查死锁

![image-20231026171911272](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231026171911272.png) 

可以看到五个线程阻塞住了



### 活锁

循环的程序需要停止执行就需要有结束条件，那么如果我线程之间一直改变对应线程的结束条件，那么这个程序就会一直执行不结束，这种修改结束条件导致程序无法结束的现象叫活锁

它和死锁不同，死锁它的线程状态是 BLOCKED，活锁的状态是 RUNNABLE

**解决方案**：分时间段去执行

**案例**：

```java
@Slf4j(topic = "LiveLockTest")
public class LiveLockTest {

    static volatile Integer COUNT = 10;

    public static void decrease() {
        while (COUNT > 0) {
            TimeUtil.sleep(0.2);
            COUNT--;
            log.debug("当前 count={}", COUNT);
        }
    }

    public static void increase() {
        while (COUNT <= 20) {
            TimeUtil.sleep(0.2);
            COUNT++;
            log.debug("当前 count={}", COUNT);
        }
    }

    public static void main(String[] args) {
        new Thread(LiveLockTest::decrease, "t1").start();
        new Thread(LiveLockTest::increase, "t2").start();
    }
}
```

这个程序会不断的执行下去



### 饥饿

哲学家就餐问题导致的死锁我们可以按照顺序来加锁解决这个死锁的问题。

但是它会导致某些线程的执行次数很多，而其他线程执行的次数很少，这种现象就是饥饿（狂执行哈哈~~~）

**修改之前哲学家就餐获取筷子的顺序**

没修改的

```java
public static void main(String[] args) {
    Chopstick c1 = new Chopstick();
    Chopstick c2 = new Chopstick();
    Chopstick c3 = new Chopstick();
    Chopstick c4 = new Chopstick();
    Chopstick c5 = new Chopstick();
    new Philosopher("苏格拉底", c1, c2).start();
    new Philosopher("柏拉图", c2, c3).start();
    new Philosopher("亚里士多德", c3, c4).start();
    new Philosopher("赫拉克利特", c4, c5).start();
    new Philosopher("阿基米德", c5, c1).start();
}
```

修改

```java
new Philosopher("阿基米德", c1, c5).start();
```

本次执行结果显示：

一开始都有执行，然后后面基本是 赫拉克利特 和 亚里士多德 在执行......





## 12、ReentrantLock

### 概述

**语法**

- ReentrantLock#lock()：获取锁，==它不是将自己锁住了，而是获取执行的锁对象==
- ReentrantLock#unlock()：释放锁对象，给其他需要获取锁对象的线程使用

**注意**：每次上锁都要释放锁，不然线程会阻塞。可以在 try-finally 中 的 finally 块执行 unlock()

**特点**：

- 和 synchonized 一样可重入
- 可中断
- 可以设置 公平锁
- 可以 多个条件变量，synchonized  只能由一个条件变量



### 1.可重入

可以多次上锁不阻塞，如果线程持有锁可以再次加锁，这个就是可重入

**例子**：一个线程多次上锁

```java
@Slf4j(topic = "ReentrantTest")
public class ReentrantTest {
    static Object lock = new Object();
    public static void main(String[] args) {
        synchronized (lock) {
            log.debug("main 加锁");
            m1();
        }
    }

    private static void m1() {
        synchronized (lock) {
            log.debug("m1 加锁");
            m2();
        }
    }

    private static void m2() {
        synchronized (lock) {
            log.debug("m2 加锁");
        }
    }
}
```

输出：

```java
22:44:00.545 [main] DEBUG ReentrantTest - main 加锁
22:44:00.547 [main] DEBUG ReentrantTest - m1 加锁
22:44:00.548 [main] DEBUG ReentrantTest - m2 加锁
```

结果显示线程没有被阻塞



### 2.可打断

ReentranLock#lockInterruptibly()：

- 没有竞争，获取锁
- 有竞争，进入阻塞队列，可以被其他线程调用当前线程的 interrupt 方法打断

这个方法的好处就是防止无休止的等待，可以直接去打断它来结束

**注意**：lock不能被打断...

```java
@Slf4j(topic = "LockInterruptiblyTest")
public class LockInterruptiblyTest{
    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        Thread t1 = new Thread(() -> {
            log.debug("启动...");
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.debug("等锁的过程中被打断...");
                return;
            }
            log.debug("执行其他代码...");
        }, "t1");
        lock.lock();
        log.debug("获取锁...");
        t1.start();
        // TimeUtil.sleep(0.5);
        // lock.unlock();
        try {
            TimeUtil.sleep(1);
            t1.interrupt();
            log.debug("执行打断...");
        } finally {
            lock.unlock();
        }
    }
}
```

输出

```java
23:07:18.310 [main] DEBUG LockInterruptiblyTest - 获取锁...
23:07:18.313 [t1] DEBUG LockInterruptiblyTest - 启动...
23:07:19.323 [main] DEBUG LockInterruptiblyTest - 执行打断...
23:07:19.323 [t1] DEBUG LockInterruptiblyTest - 等锁的过程中被打断...
java.lang.InterruptedException
	at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireInterruptibly(AbstractQueuedSynchronizer.java:900)
	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireInterruptibly(AbstractQueuedSynchronizer.java:1225)
	at java.util.concurrent.locks.ReentrantLock.lockInterruptibly(ReentrantLock.java:340)
	at com.xiaozhi.sharedmodel03.reentranlock.LockInterruptiblyTest.lambda$main$0(LockInterruptiblyTest.java:18)
```

从结果可以看出来被打断之后就会报错，我们打断后要给它释放锁



### 3.锁超时

#### 尝试获取锁

ReentranLock#tryLock

- tryLock()：尝试获取锁，成功返回 true，失败返回 false
- tryLock(long, TimeUnit)：指定时间内循环获取锁，成功返回 true，失败返回 false

**tryLock()案例**

```java
@Slf4j(topic = "TryLockTest")
public class TryLockTest {

    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        Thread t1 = new Thread(() -> {
            log.debug("启动...");
            if (!lock.tryLock()) {
                log.debug("立刻获取失败...");
                return;
            }
            try {
                log.debug("获取了锁...");
            } finally {
                lock.unlock();
            }
        }, "t1");
        lock.lock();
        log.debug("获得了锁");
        t1.start();
        try {
            TimeUtil.sleep(1);
        } finally {
            lock.unlock();
        }
    }
}
```

输出

```java
23:25:23.040 [main] DEBUG TryLockTest - 获得了锁
23:25:23.043 [t1] DEBUG TryLockTest - 启动...
23:25:23.043 [t1] DEBUG TryLockTest - 立刻获取失败...
```



**tryLock(n, m)**

我们让它的等待时间比释放锁的线程更长

```java
// 修改 Thread 内部执行的代码
Thread t1 = new Thread(() -> {
    log.debug("启动...");
    try {
        if (!lock.tryLock(2, TimeUnit.SECONDS)) {
            log.debug("立刻获取失败...");
            return;
        }
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    try {
        log.debug("t1 获取了锁...");
    } finally {
        lock.unlock();
    }
}, "t1");
```

```java
3:29:13.542 [main] DEBUG TryLockTest - main 获得了锁
23:29:13.546 [t1] DEBUG TryLockTest - 启动...
23:29:14.558 [t1] DEBUG TryLockTest - t1 获取了锁...
```

可以看到一秒后 t1 获取到了锁



#### 解决哲学家就餐问题

我们可以使用 tryLock 来解决这个问题，获取不了锁我们就让出去

```java
/**
 * @author xiaozhi
 *
 * 解决哲学家问题
 */
public class SolvePhilosopherEatQuestion {

    public static void main(String[] args) {
        Chopstick c1 = new Chopstick();
        Chopstick c2 = new Chopstick();
        Chopstick c3 = new Chopstick();
        Chopstick c4 = new Chopstick();
        Chopstick c5 = new Chopstick();
        new Philosopher("苏格拉底", c1, c2).start();
        new Philosopher("柏拉图", c2, c3).start();
        new Philosopher("亚里士多德", c3, c4).start();
        new Philosopher("赫拉克利特", c4, c5).start();
        new Philosopher("阿基米德", c5, c1).start();
    }
}

@Slf4j(topic = "筷子")
class Chopstick extends ReentrantLock{
}

@Slf4j(topic = "哲学家")
class Philosopher extends Thread{

    private Chopstick left;
    private Chopstick right;
    private String name;

    public Philosopher(String name, Chopstick left, Chopstick right) {
        this.name = name;
        this.left = left;
        this.right = right;
    }

    @Override
    public void run() {
        while (true) {
            // 拿到左边筷子
            if (left.tryLock()) {
                try {
                    if (right.tryLock()) {
                        try {
                            eat();
                        } finally {
                            right.unlock();		// 注意释放锁
                        }
                    }
                } finally {
                    left.unlock();	// 注意释放锁
                }
            }
        }
    }

    public void eat() {
        log.debug("{} 吃上了饭", this.name);
        TimeUtil.sleep(1);
    }
}
```



### 4.公平锁

ReentranLock 默认是不公平的，我们可以通过它的构造器设置它为公平的

```JAVA
ReentranLock lock = new ReentranLock(true);		// 设置为公平锁
```

**公平锁**：多个线程按照申请锁的顺序进行排队，排在前面的优先获取锁，后面的线程阻塞等待

- 优点：所有的线程都能获取到锁，不会饿死在等待队列中
- 缺点：吞吐量会下降很多，队里面除了第一个线程运行，其他线程阻塞，cpu按序唤醒线程阻塞线程的开销很大

**非公平锁**：每个线程获取锁的顺序是随机的，不会遵循顺序去获取锁，所有的线程都会竞争获取锁

- 优点：执行效率高，谁先获取到锁，锁就是谁的
- 缺点：获取锁的顺序是随机的，可能会出现线程饿死的情况。

**案例**：

非公平锁

```java
@Slf4j(topic = "FairLockTest")
public class FairLockTest {

    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock(false);
        lock.lock();
        for (int i = 0; i <  500; i++) {
            new Thread(() -> {
                lock.lock();    // 获取锁
                try {
                    log.debug("{} running...", Thread.currentThread().getName());
                } finally {
                    lock.unlock();
                }
            }, "t" + i).start();
        }
        TimeUtil.sleep(1);
        new Thread(() -> {
            log.debug("{} start...", Thread.currentThread().getName());
            lock.lock();
            try {
                log.debug("{} running...", Thread.currentThread().getName());
            } finally {
                lock.unlock();
            }
        }, "强制插入").start();
        lock.unlock();
    }
}
```

![image-20231028005750497](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231028005750497.png) 

 **公平锁**

```java
ReentrantLock lock = new ReentrantLock(true);
```

![image-20231028010003534](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231028010003534.png) 

结果显示它在最后才获取到锁，然后才执行输出



### 5.可多个条件变量

synchronized 中的 waitSet 就是一个条件变量

ReentranLock 支持多个条件变量，可以根据条件变量来唤醒，更加细粒度话进行管理

#### 方法

```java
ReentranLock lock = new ReentranLock();
Condtion c1 = lock.newCondition();
```

Condtion 方法：

- await()：线程等待
- signal()：随机唤醒被等待线程
- signalAll()：唤醒所有等待线程

**要点**：

- await 前需要获取锁，==signal 释放锁的时候也需要获取锁，否则会报错==
- await 执行后释放锁，进入 conditionObject 等待
- await 的线程被唤醒（或打断、或超时）时重新竞争 lock 锁
- 竞争锁成功，从 await 后面开始继续执行

**注意**：因为可以有多个条件变量，线程等待和唤醒的对象要是同一个



#### 例子：买烟和买早餐

```java
@Slf4j(topic = "AwaitTest")
public class AwaitTest {

    static ReentrantLock lock = new ReentrantLock();
    static Condition waitCigaretteQueue = lock.newCondition();
    static Condition waitBreakfastQueue = lock.newCondition();
    static boolean hasCigarette = false;
    static boolean hasBreakfast = false;

    public static void main(String[] args) {
        new Thread(() -> {
            lock.lock();
            try {
                while (!hasCigarette) {
                    waitCigaretteQueue.await();
                }
                log.debug("烟送到了...");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }, "t1").start();
        new Thread(() -> {
            lock.lock();
            try {
                while (!hasBreakfast) {
                    waitBreakfastQueue.await();
                }
                log.debug("早餐送到了...");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }, "t2").start();
        TimeUtil.sleep(1);
        sendCigarette();
        TimeUtil.sleep(1);
        sendBreakfast();
    }

    private static void sendBreakfast() {
        lock.lock();
        try {
            hasCigarette = true;
            waitCigaretteQueue.signalAll();
            log.debug("烟送出去了...");
        } finally {
            lock.unlock();
        }
    }

    private static void sendCigarette() {
        lock.lock();
        try {
            hasBreakfast = true;
            waitBreakfastQueue.signalAll();
            log.debug("早餐送出去了...");
        } finally {
            lock.unlock();
        }
    }
}
```

```java
01:29:00.901 [main] DEBUG AwaitTest - 烟送出去了...
01:29:00.904 [t1] DEBUG AwaitTest - 烟送到了...
01:29:01.906 [main] DEBUG AwaitTest - 早餐送出去了...
01:29:01.906 [t2] DEBUG AwaitTest - 早餐送到了...
```

可以看到我们使用 ReentranLcok 可以各自唤醒对于条件变量的线程，而如果使用 wait & notify 的话防止虚假等待就需要唤醒所有的线程，但是 【烟】比 【早餐】更早送出去，所以就会造成提前唤醒导致早餐获取为空，所以多个条件变量就可以解决这个问题，更加灵活的处理不同的业务。



## 同步模式之顺序控制

### 顺序执行

#### wait & notify 版

```java
/**
 * @author xiaozhi
 *
 * 顺序执行之 wait & notify
 */
@Slf4j(topic = "SequentialExecutionForWaitNotify")
public class SequentialExecutionForWaitNotify {

    static final Object LOCK = new Object();
    static boolean t2Run = false;
    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (LOCK) {
                while (!t2Run) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                log.debug("t1 执行...");
            }
        }).start();
        new Thread(() -> {
            synchronized (LOCK) {
                log.debug("t2 执行...");
                t2Run = true;
                LOCK.notifyAll();       // 唤醒其他线程
            }
        }).start();
    }
}
```



#### park & unpark 版

```java
/**
 * @author xiaozhi
 *
 * 顺序执行之 park & unpark
 */
@Slf4j(topic = "SequentialExecutionForParkUnpark")
public class SequentialExecutionForParkUnpark {

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            LockSupport.park();
            log.debug("t1 执行");
        }, "t1");
        t1.start();
        new Thread(() -> {
            log.debug("t2 执行");
            LockSupport.unpark(t1);
        }, "t2").start();
    }
}
```



### 交替输出

#### wait & notify 版

线程通过设置标记来判断是否该它执行，如果不是我们对应的标记，那么就等待

```java
public class AlternatingOutputWaitNotify {

    public static void main(String[] args) {
        SyncWaitNotify notify = new SyncWaitNotify(1, 5);
        new Thread(() -> { notify.print("a", 1, 2); }).start();
        new Thread(() -> { notify.print("b", 2, 3); }).start();
        new Thread(() -> { notify.print("c", 3, 1); }).start();
    }
}

class SyncWaitNotify {
    private int flag;
    private final int loopNum;

    public SyncWaitNotify(int flag, int loopNum) {
        this.flag = flag;
        this.loopNum = loopNum;
    }

    public void print(String str, int waitFlag, int nextFlag) {
        for (int i = 0; i < loopNum; i++) {
            synchronized (this) {
                while (flag != waitFlag) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.print(str);
                flag = nextFlag;
                this.notifyAll();
            }
        }
    }
}
```

```
abcabcabcabcabc
```



#### park & unpark 版

```java
public class AlternatingOutputForPark {
    static Thread t1 = null;
    static Thread t2 = null;
    static Thread t3 = null;

    public static void main(String[] args) {
        SyncPark syncPark = new SyncPark(5, 1);
        t1 = new Thread(() -> { syncPark.print("a", 1, 2, t2); });
        t2 = new Thread(() -> { syncPark.print("b", 2, 3, t3); });
        t3 = new Thread(() -> { syncPark.print("c", 3, 1, t1); });
        t1.start();
        t2.start();
        t3.start();
        LockSupport.unpark(t1);
    }
}

class SyncPark {
    private int loopNum;
    private int flag;

    public SyncPark(int loopNum, int flag) {
        this.loopNum = loopNum;
        this.flag = flag;
    }
    public void print(String str, int flag, int nextFlag,Thread nextThread) {
        for (int i = 0; i < loopNum; i++) {
            while (this.flag != flag) {
                LockSupport.park();
            }
            System.out.print(str);
            this.flag = nextFlag;
            LockSupport.unpark(nextThread);
        }
    }
}
```



#### ReentranLock版

```java
public class AlternatingOutputForReentranLock {

    public static void main(String[] args) {
        AwaitSignal awaitSignal = new AwaitSignal(5);
        Condition c1 = awaitSignal.newCondition();
        Condition c2 = awaitSignal.newCondition();
        Condition c3 = awaitSignal.newCondition();
        new Thread(() -> { awaitSignal.print("a", c1, c2); }).start();
        new Thread(() -> { awaitSignal.print("b", c2, c3); }).start();
        new Thread(() -> { awaitSignal.print("c", c3, c1); }).start();
        awaitSignal.start(c1);      // 唤醒c1
    }
}

class AwaitSignal extends ReentrantLock {

    private final int loopNum;

    public AwaitSignal(int loopNum) {
        this.loopNum = loopNum;
    }
    // 一开始多个线程都再等待，所以需要先唤醒一个线程才能往下执行
    public void start(Condition condition) {
        lock();
        try {
            condition.signalAll();
        } finally {
            lock();
        }
    }

    public void print(String str, Condition cur, Condition next) {
        lock();     // 获取锁
        try {
            cur.await();
            System.out.print(str);
            next.signalAll();       // 唤醒下一个条件变量的线程
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock();
        }
    }
}
```

```
abcabcabcabcabc
```

**错误想法**：

- 我在想要不要用一个标记来实现这个功能，但其实并不需要，我们是通过条件变量唤醒，只需要传入对应需要唤醒的条件变量即可，我们不需要设置等待条件





## 本章小结

重点掌握：

- 分析线程访问共享资源时，那些代码属于临界区
- 使用 synchronized 解决临界区的线程安全问题
    - 掌握 synchronized 锁对象语法
    - 掌握 synchronized 加载成员变量方法和静态方法
    - 掌握 wait/notify 同步方法
- 使用 lock 互斥解决临界区线程安全问题
    - 掌握 lock 的使用细节：可重入、可打断、锁超时、公平锁、条件变量
- 学会分析变量的线程安全性，掌握常见线程安全类的使用
- 了解线程活跃性问题：死锁、活锁、饥饿

**应用方面**：

- 互斥：使用 synchronized 和 Lock 实现资源共享互斥效果
- 同步：使用 wait / notify 或 Lock 条件变量来达到线程通信效果

**原理方面**：

- monitor、synchonized、wait / notify 原理
- synchronized 进阶原理
- park & unpark 原理

**模式方面**：

- 同步模式之保护性暂停
    - 
- 异步模式之生产者消费者
- 同步模式之顺序控制
    - 顺序执行
    - 交替执行





# 四、共享模型之内存

## Java 内存模型

全称 Java Memory model，简称 JMM，Java对主寸和工作内存进行了抽象，底层对应 CPU寄存器、缓存、硬件内存、CPU指令优化等等，它让程序员不必关注于底层的内存管理，通过 Java 提供的关键字来控制

JMM 体现在以下几个方面

- 原子性：保证指令不受线程上下文切换的影响
- 可见性：保证指令不会受 CPU 缓存的影响
- 有序性：保证指令不会受 CPU 指令并行优化影响



## 可见性

### 可见性问题

我们首先看一段程序，然后我们分析一下为什么会出现循环一直不退出的情况

```java
public class LoopDoesNotExit {
    static boolean isStop = false;
    public static void main(String[] args) {
        new Thread(() -> {
            while (!isStop) {

            }
        }).start();
        System.out.println("停止...");
        TimeUtil.sleep(1);
        isStop = true;
    }
}
```

![image-20231029202952853](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231029202952853.png) 

结果显示我们修改了退出条件但是程序不停止



### 问题分析

这个是因为我们的内存可见性导致的，JIT 编译器会对频繁从主存中取值的值缓存到当前执行的线程的工作内存的高速缓存中，减少对主存中的值的访问，提高效率

![image-20231029203853480](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231029203853480.png) 

所以当主线程修改主存中的值的时候 `线程t1` 它还是访问的高速缓存，导致主存的修改`线程t1` 看不到，因此导致循环无法退出



### 解决问题

可以使用 volatile 关键字解决主存不可见问题

volatile 翻译是异变的意思， 它可以用来修饰成员变量或静态变量，局部变量在工作内存中，无需 volatile 修饰。它可以避免从工作缓存中获取值，而是直接从主存中获取值，这样就可以解决可见性的问题。

synchonized 也可以解决此问题，但是它是重锁，性能较低，所以能用 volatile 解决就不使用它。

**程序改进**

```java
static volatile boolean isStop = false;
```

再次运行

![image-20231029204128059](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231029204128059.png) 

可以看到循环结束了



### 终止模式之两阶段终止模式

回顾我们之前的两阶段终止模式，使用 打断程序的方式 来终止运行

```java
@Slf4j(topic = "TwoPhaseTermination")
class TwoPhaseTermination {

    private Thread monitor;

    public void start() {
        monitor = new Thread(() -> {
            while (true) {
                Thread thread = Thread.currentThread();
                if (thread.isInterrupted()) {
                    log.debug("料理后事...");
                    break;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                    log.debug("记录监控日志...");
                } catch (InterruptedException e) {
                    thread.interrupt();     // 重新设置打印标记
                }
            }
        }, "monitor");
        monitor.start();
    }
    public void stop() {
        monitor.interrupt();
    }
}
```

观察发现我们在打断程序的时候需要给它重新设置一下打断标记，如果它没有正确设置，那么就会导致我们的程序无法料理后事，所以我们可以设置我们自己的标记，然后在 stop 方法中设置一下，当满足我们的退出条件就结束循环

**改进代码**：使用 标记 的方式来终止

```java
@Slf4j(topic = "TwoPhaseTermination")
class TwoPhaseTermination {

    private Thread monitor;
    private volatile boolean isStop = false;

    public void start() {
        monitor = new Thread(() -> {
            while (true) {
                if (isStop) {
                    log.debug("料理后事...");
                    break;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                    log.debug("记录监控日志...");
                } catch (InterruptedException e) {
                    log.debug("程序打断...");
                }
            }
        }, "monitor");
        monitor.start();
    }
    public void stop() {
        this.isStop = true;
        monitor.interrupt();
    }
}
```

```java
21:08:28.749 [monitor] DEBUG TwoPhaseTermination - 记录监控日志...
21:08:29.755 [monitor] DEBUG TwoPhaseTermination - 记录监控日志...
21:08:30.764 [monitor] DEBUG TwoPhaseTermination - 记录监控日志...
21:08:31.259 [monitor] DEBUG TwoPhaseTermination - 程序打断...
21:08:31.259 [monitor] DEBUG TwoPhaseTermination - 料理后事...
```

我们需要使用 volatile 来修饰 `isStop` 变量，因为它被多个程序使用，所以需要保证它的内存可见性。



### 同步模式之 Balking

Balking（犹豫）模式是用于一个线程发现另外一个线程已经做过某一件相同的事，那么本线程无需重复操作，直接返回即可



#### 监控程序实现

![image-20231029213953985](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231029213953985.png)

当我们点击开始按钮就启动我们的监控线程，停止就是将监控线程停止

但是如果我们多次点击开始，我们只需要使用一个标记来记录当前线程是否已经启动，如果已经启动那么就 break，没有启动就需要 new 一个线程

**下面是我们的监控线程启动方法**

```java
public void start() {
    // 缩小同步范围，提升性能
    synchronized (this) {
        log.info("该监控线程已启动?({})", starting);
        if (starting) {
            return;
        }
        starting = true;
    }
    // 创建线程执行监控任务
    ...
}
```

这块使用的就是我们的 Blaking 模式，它首先判断是否已经启动，如果启动了就 return，否则就创建线程



#### 线程安全的单例

```java
class Person {
    private Person person = null;
    private Person() {}
    public synchronized Person getPerson() {
        if (this.person == null) {
            this.person = new Person();
        }
        return this.person;
    }
}
```





## 有序性

### 指令重排序

#### 1.什么是指令重排序

指令重排序是指编译器或 CPU 为了优化程序的执行性能而对指令进行重新排序的一种手段， 重新排序会带来可见性问题，所以在多线程开发中要规避和关注重排序。

**举个例子**：我们需要泡茶喝，有以下步骤：`洗茶壶-洗杯子-煮开水-洗茶-泡茶`，其中煮开水的时间是最长的，在这个时间内我们可以将其他的步骤先做好，那么此时我们可以修改一下它的顺序为：`煮开水-洗茶壶-洗杯子-洗茶-泡茶`，这个就和我们的指令重排比较相近了，在一段时间内完成多个指令操作，以此来优化指令的执行速度

**三种重排序的场景**：

1. 编译器重排序

    编译器可以在不改变单线程代码执行结果的前提下，可以对代码执行顺序进行重排序

2. CPU指令重排序

    这个是针对 CPU 指令来说的，处理器采用指令集并行技术将许多条指令重叠执行，如果不存在数据依赖性，处理器可以改变语句对应的机器指令执行顺序

3. 内存重排序

    因为CPU缓存使用 缓冲区的方式(Store Buffere )进行延迟写入，这个过程会造成多个CPU缓存可见性的问题，这种可见性的问题导致结果的对于指令的先后执行显示不一致，从表面结果上来看好像指令的顺序被改变了，内存重排序其实是造成可见性问题的主要原因所



#### 2.指令重排的规则

**as-if-serial语义**

as-if-serial 表示所有程序指令都可以重排序，前提是在单线程环境下执行结果不变的情况下改变它的指令执行顺序

举个编译器层面的优化：

```java
int a = 1;
int b = 2;
a = a + 2;
```

那么这个时候我们可以将它修改如下：

```java
int a = 1;
a = a + 2;
int b = 2;
```

相比于一开始的，a 在赋值完成后 不用再从 主存中给 b 赋值，直接就是放入寄存器中，接着执行 + 2 操作，这样省下了放入内存中然后再从内存中获取的情况



#### 3.重排序对多线程的影响

以下一段代码，分析它在多线程环境下会有什么问题

```java
private static int value;
private static boolean flag;

public void init() {
    this.value = 2;
    this.flag = true;
}
public void getValue() {
    if (this.flag) {
        System.out.println(this.value);
    }
}
```

**分析**：

如果是代码都是按照允许执行，那么 getValue() 打印值就为8，但是如果对 init() 进行指令重排序，那么此时的顺序如下：

```java
this.flag = true;
this.value = 2;
```

这个时候两个线程同时调用 getValue() 和 init() 就会导致结果为 0

![image-20231030005224974](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231030005224974.png) 





### 诡异的结果

我们借助 java 并发压测工具 jcstress【https://wiki.openjdk.java.net/display/CodeTools/jcstress】 来测试

创建 maven 工程，提供如下测试类

```java
@JCStressTest
@Outcome(id = {"1", "4"}, expect = Expect.ACCEPTABLE, desc = "ok")
@Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "!!!!")
@State
public class ConcurrencyTest {

    int num = 0;
    boolean ready = false;
    @Actor
    public void actor1(I_Result r) {
        if(ready) {
            r.r1 = num + num;
        } else {
            r.r1 = 1;
        }
    }

    @Actor
    public void actor2(I_Result r) {
        num = 2;
        ready = true;
    }
}
```

打成 jar 包执行

```java
mvn clean install 
java -jar target/jcstress.jar
```

**执行结果**：

![image-20231030173413890](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231030173413890.png) 

可以看到，出现结果为 0 的情况有 1652 次，虽然次数相对很少，但毕竟是出现了。





### 禁止指令重排

使用 volatile 来解决指令重排的问题，volatile 修饰的变量，可以禁用指令重排

```java
volatile boolean ready = false;
```

再次测试结果为：

```java
*** INTERESTING tests 
Some interesting behaviors observed. This is for the plain curiosity. 
0 matching test results.
```

没有问题



### CPU 缓存结构原理

暂时不学





### Volatile原理

volatile 的底层实现原理是内存屏障，Memory Barrier（Memory Fence）

- 对 volatile 变量的写指令后会加入写屏障
- 对 volatile 变量的读指令前会加入读屏障



#### 1、如何保证可见性

写屏障它保证了对共享变量的修改都会同步到主存中

读屏障保证在该凭证后，对共享变量的读取，加载的是主存中最新的数据



#### 2、如何保证有序性

写屏障会确保指令重排序时，不会将写屏障之前的代码排在写屏障之后

读屏障会确保指令重排序时，不会将读屏障之后的代码排在读屏障之前



#### 3、double-checked locking 问题

以著名的 double-checked locking 单例模式为例 





#### 4、double-checked locking 解决







# 五、共享模型之无锁(CAS)

## CAS概述

### 无锁(逻辑锁)

#### 问题提出

我们写一个存钱获取取钱的案例

```java
public class AccountTest {

    public static void main(String[] args) {
        AccountUnsafe account = new AccountUnsafe(10000);
        Account.demo(account);
    }
}

class AccountUnsafe implements Account{
    private Integer balance;
    public AccountUnsafe(Integer balance) {
        this.balance = balance;
    }
    @Override
    public int getBalance() {
        return this.balance;
    }

    @Override
    public void withdraw(Integer amount) {
        this.balance -= amount;
    }
}

interface Account {
    // 获取余额
    int getBalance();
    // 取钱
    void withdraw(Integer amount);

    static public void demo(Account account) {
        List<Thread> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            Thread thread = new Thread(() -> {
                account.withdraw(10);
            });
            list.add(thread);
            thread.start();
        }
        list.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.currentTimeMillis();
        System.out.println("剩余金额：" + account.getBalance()
                + " coat：" + (end - start) + "ms");
    }
}
```

结果

```java
剩余金额：180 coat：137ms
```



#### 解题难题 - 有锁

上面的案例中，withdraw 方法是一个临界区

```java
@Override
public void withdraw(Integer amount) {
    this.balance -= amount;
}
```

`this.balance -= amount;` 这样代码进行了读写操作

我们可以通过 synchronized 和 ReentranLock 来解决这个问题，这里我使用 synchronized

```java
@Override
public synchronized void withdraw(Integer amount) {
    this.balance -= amount;
}
```

接着我们再次远行

```java
剩余金额：0 coat：140ms
```

完美~~~



#### 解决问题 - 无锁

那我们可以使用无锁的方式去做共享资源的保护吗？可以的，使用 CAS 的方式

我们使用 `AtomicInteger` 修改一下我们的代码

```java
class AccountUnsafe implements Account{
    private AtomicInteger balance;
    public AccountUnsafe(int balance) {
        this.balance = new AtomicInteger(balance);
    }
    @Override
    public int getBalance() {
        return balance.get();
    }

    @Override
    public void withdraw(Integer amount) {
        while (true) {
            int prev = this.balance.get();
            int next = prev - amount;
            if (this.balance.compareAndSet(prev, next)) {
                break;
            }
        }
    }
}
```

```java
剩余金额：0 coat：121ms
```

此时我们的 withdraw 并没有用 synchronized 修饰，也就是说我们没有加锁，那么为什么不加锁也可以保证线程安全性呢？





### CAS 与 Volatile

#### CAS是什么

CAS 就是通过比较上个版本来判断共享资源是否被其他线程改变了，如果改变了那么此次执行失败，接着它的循环去改变这个共享变量，直到它比较成功。CAS是原子操作。

> **注意**
>
> - 其实 CAS 的底层是 **lock cmpxchg** 指令（X86 架构），在单核 CPU 和多核 CPU 下都能够保证【比较-交换】的原子性。
> - 在多核状态下，某个核执行到带 lock 的指令时，CPU 会让总线锁住，当这个核把此指令执行完毕，再开启总线。这个过程中不会被线程的调度机制所打断，保证了多个线程对内存操作的准确性，是原子的。

接下来哦们看一下上一章节的 withdraw 方法做了什么

```java
@Override
public void withdraw(Integer amount) {
    // 不断重试
    while (true) {
        // 获取没修改之前的值
        int prev = this.balance.get();
        // 获取修改之后的值
        int next = prev - amount;
        // 比较之前没修改的值，如果和现在获取的值一致，那么就将 next 设置为最新的值
        if (this.balance.compareAndSet(prev, next)) {
            break;
        }
    }
}
```

其中的 compareAndSet 方法，它的简称就是 CAS （也有 compareAndSwap的说法），翻译 “比较和设置”，通过比较上一次的值来判断值是否被改变了，如果被改变了，那么当前的操作就会失败返回 false，所以我们这里使用 `while(true)` 来不断重试

它的执行序列图如下：

![image-20231102152958932](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231102152958932.png) 

可以看到我们的线程一执行了两次操作才成功的减去了 10





#### volatile

获取共享变量时，为了保证该变量的可见性，我们可以使用 volatile 来修饰变量

> **注意**
>
> volatile 仅仅保证了共享变量的可见性，让其它线程能够看到最新值，但不能解决指令交错问题（不能保证原子性）

那么我们使用的 AtomicInteger 类需要使用 volatile 修饰吗？

不需要，因为它所维护的变量已经用 volatile 修饰过了

```java
// 来自 AtomicInteger 类源码
private volatile int value;
```



#### 为什么无锁效率高呢？

- 无锁清况下即使它判断失败了，它还是一直处在 Runnable 状态，并没有 Blocked ，而 Synchronized 它在切换线程的时候进行上下文切换，线程会被 Blocked，一直跑的赛车和一直重新启动再加速的赛车那个更快呢？很显然是一直跑的那个
- 无锁它需要多个 CPU 核心支持，因为它是一直运行的，如果是单核心，它还是会上下文切换，所以建议使用无锁的时候它的线程数不要超过 CPU 的核心数





## CAS类和方法

### 原子整数

#### 三种类型

原子整数类有以下三种类型：

- AtomicInteger
- AtomicLong
- AtomicBoolean

#### 方法

我们这里使用 AtomicInteger 做示例，其他的都是类似的操作

```java
public class AtomicIntegerTest {

    public static void main(String[] args) {
        AtomicInteger a = new AtomicInteger(0);
        System.out.println("========= 获取值 =======");
        System.out.println(a.get());

        System.out.println("========= i加加 和 加加i OP =======");
        System.out.println("incrementAndGet：" + a.incrementAndGet());
        System.out.println("getAndIncrement：" + a.getAndIncrement());

        System.out.println("========= 加法减法 OP =======");
        System.out.println("addAndGet(5)：" + a.addAndGet(5));
        System.out.println("getAndAdd(5)：" + a.getAndAdd(5));

        System.out.println("========= CAS OP =======");
        System.out.println("compareAndSet(2, 5)：" + a.compareAndSet(2, 5));
        System.out.println("get：" + a.get());

        System.out.println("========= 更新和获取值（lambda表达式） =====");
        System.out.println("更新并获取：" + a.updateAndGet(x -> x * 2));
        System.out.println("先获取再更新：" + a.getAndUpdate(x -> x / 2));

        System.out.println("======== 计算和获取值 ========");
        System.out.println("accumulateAndGet：" + a.accumulateAndGet(2, (p, x) -> p + x));
        System.out.println("getAndAccumulate：" + a.getAndAccumulate(-2, (p, x) ->  p + x));   // p 是还未修改前的值，x是第一个参数
        System.out.println("get：" + a.get());
    }
}
```

结果：

```java
========= 获取值 =======
0
========= i加加 和 加加i OP =======
incrementAndGet：1
getAndIncrement：1
========= 加法减法 OP =======
addAndGet(5)：7
getAndAdd(5)：7
========= CAS OP =======
compareAndSet(2, 5)：false
get：12
========= 更新和获取值（lambda表达式） =====
更新并获取：24
先获取再更新：24
======== 计算和获取值 ========
accumulateAndGet：14
getAndAccumulate：14
get：12
```



#### updateAndGet原理

下面是这个方法的源码

```java
public final int updateAndGet(IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        // 未修改的值
        prev = get();
        // 修改后的值
        next = updateFunction.applyAsInt(prev);
        // 进行 CAS 比较，不满足继续循环
    } while (!compareAndSet(prev, next));
    return next;
}
```

这个不就是我们之前使用  AtomicInteger 解决问题时用的方式吗，它这里做了一些代码简化，也就是说我们不使用它的方法我们自己也可以用我们自己的





### 原子引用

- AtomicReference
- AtomicMarkableReference
- AtomicStampedReference



#### AtomicReference

##### 修改 Account 的案例

我们修改之前的代码，将 int 类型改成 BigDecimal 类型

```java
class UnsafeAccount implements Account{
    private BigDecimal balance;

    public UnsafeAccount(int balance) {
        this.balance = BigDecimal.valueOf(balance);
    }

    @Override
    public BigDecimal getBalance() {
        return this.balance;
    }

    @Override
    public void withdraw(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}
interface Account {

    BigDecimal getBalance();

    void withdraw(BigDecimal amount);

    static void demo(Account account) {
        List<Thread> threads = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            Thread thread = new Thread(() -> {
                account.withdraw(new BigDecimal(10));
            });
            threads.add(thread);
            thread.start();
        }
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        long end = System.currentTimeMillis();
        System.out.println("balance：" + account.getBalance()
                + ", time：" + (end - start) + "ms");
    }
}
```

结果：

```sh
balance：410, time：166ms
```

我们之前用的整数类型，现在我使用引用类型，那么我们就不能使用原子整数类来解决了，我们使用原子引用类来解决



##### 解决问题

```java
class UnsafeAccount implements Account{
    // private BigDecimal balance;
    private AtomicReference<BigDecimal> ref;    // 使用原子引用解决资源共享问题

    public UnsafeAccount(BigDecimal balance) {
        this.ref = new AtomicReference<>(balance);
    }
    @Override
    public BigDecimal getBalance() {
        return this.ref.get();
    }

    @Override
    public void withdraw(BigDecimal amount) {
        this.ref.updateAndGet(balance -> balance.subtract(amount));
    }
}
```

withdraw 中也可以不使用 AtomicReference 的方法，我们也可以编写我们自己的逻辑

```java
@Override
public void withdraw(BigDecimal amount) {
    BigDecimal prev;
    BigDecimal next;
    do {
        prev = ref.get();
        next = prev.subtract(amount);
    } while (!ref.compareAndSet(prev, next));
}
```



#### AtomicStampedReference

##### ABA问题

```java
@Slf4j(topic = "ABAQuestion")
public class ABAQuestion {
    static AtomicReference<String> CODE = new AtomicReference<>("A");

    public static void main(String[] args) {
        String prev = CODE.get();
        other();
        TimeUtil.sleep(1);
        log.debug("A -> C：{}", CODE.compareAndSet(prev, "C"));
    }

    private static void other() {
        new Thread(() -> {
            log.debug("A -> B：{}", CODE.compareAndSet(CODE.get(), "B"));
        }, "t1").start();
        TimeUtil.sleep(0.5);
        new Thread(() -> {
            log.debug("B -> A：{}", CODE.compareAndSet(CODE.get(), "A"));
        }, "t2").start();
    }
}
```

```java
21:49:22.763 [t1] DEBUG ABAQuestion - A -> B：true
21:49:23.262 [t1] DEBUG ABAQuestion - B -> A：true
21:49:24.269 [main] DEBUG ABAQuestion - A -> C：true
```

**说明**：两个线程先 A -> B，然后 B -> A，最后是 main线程 A -> C，那么在这个过程中发生了 A -> B -> A 的过程，这个过程我们无法感知，最后的结果是返回 true，因为比对的结果没问题；

从逻辑上看两个线程的操作相当于是没有做修改，但是实际上他们修改了两次，只是结果和第一次的相同；

那么我们如何知道这个变量已经被修改了呢？



##### 版本号

我们可以给共享变量一个版本，当我们修改了这个共享变量，那么此时它的版本号发生变化，当其他线程要修改的时候，获取最新的版本号进行对比，如果发现版本号不一致，那么修改不成功，相反则成功。

**AtomicStampedReferenc*** 它就是使用这种机制来保证共享资源的安全

**方法**：

```java
// AtomicStampedReferenc
boolean compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
```

- expectedReference：旧引用
- newReference：新引用
- expectedStamp：旧印记
- newStamp：新印记



##### 解决ABA问题

```java
@Slf4j(topic = "ABAQuestion")
public class ABAQuestionSolution {
    static AtomicStampedReference<String> CODE = new AtomicStampedReference<>("A", 0);

    public static void main(String[] args) {
        int stamp = CODE.getStamp();
        other();
        TimeUtil.sleep(1);
        log.debug("更新版本为：{}", stamp);
        log.debug("A -> C：{}", CODE.compareAndSet(CODE.getReference(), "C", stamp, stamp + 1));
    }

    private static void other() {
        new Thread(() -> {
            int stamp = CODE.getStamp();
            log.debug("更新版本为：{}", stamp);
            log.debug("A -> B：{}", CODE.compareAndSet(CODE.getReference(), "B", stamp, stamp + 1));
        }, "t1").start();
        TimeUtil.sleep(0.5);
        new Thread(() -> {
            int stamp = CODE.getStamp();
            log.debug("更新版本为：{}", stamp);
            log.debug("B -> A：{}", CODE.compareAndSet(CODE.getReference(), "A", stamp, stamp + 1));
        }, "t2").start();
    }
}
```

```java
22:06:41.313 [t1] DEBUG ABAQuestion - 更新版本为：0
22:06:41.319 [t1] DEBUG ABAQuestion - A -> B：true
22:06:41.813 [t2] DEBUG ABAQuestion - 更新版本为：1
22:06:41.814 [t2] DEBUG ABAQuestion - B -> A：true
22:06:42.822 [main] DEBUG ABAQuestion - 更新版本为：0
22:06:42.822 [main] DEBUG ABAQuestion - A -> C：false
```

可以看到因为我们是以版本号为基准判断是否已经被修改，所以 main 修改不成功，因为版本号已经发生改变



#### AtomicMarkableReference

有时候我们不关注它修改了多少次，我们关心的是它是否被修改了没，所以就有了 AtomicMarkableReference，它只有两个标记分别是 true 和 false，通过这两个标记来做对应的业务，为true干什么，为 false 又干什么

**案例**

这里我们做一个 【吃饭拉屎】🤣 的案例，我们有两个线程，一个是肚子和屁股，肚子饿了要吃，吃饱了就拉

```java
public class AtomicMarkableReferenceTest {
    public static void main(String[] args) {
        Person person = new Person(true);
        AtomicMarkableReference<Person> ref = new AtomicMarkableReference<Person>(person, true);
        new Thread(() -> {
            // 是 true 的就让设置为 false
            if (ref.isMarked()) {
                person.setAbdomen(false);
                ref.compareAndSet(person, person, true, false);
            }
        }, "eat").start();
        new Thread(() -> {
            if (!ref.isMarked()) {
                person.setAbdomen(true);
                ref.compareAndSet(person, person, false, true);
            }
        }, "la").start();
        TimeUtil.sleep(0.5);
        System.out.println(person);
    }
}

class Person {
    private boolean abdomen;
    public Person(boolean abdomen) {
        this.abdomen = abdomen;
    }
    public void setAbdomen(boolean abdomen) {
        this.abdomen = abdomen;
    }

    @Override
    public String toString() {
        if (abdomen) {
            return "肚子好饿";
        } else {
            return "吃饱拉出";
        }
    }
}
```

```java
23:15:44.781 [eat] DEBUG AtomicMarkableReferenceTest - 它饿了，吃了一顿，肚子好饱...
23:15:45.291 [la] DEBUG AtomicMarkableReferenceTest - 它拉了，又饿了...
23:15:45.794 [main] DEBUG AtomicMarkableReferenceTest - 最后它肚子好饿
```

**说明**：

- Person 对象中有一个肚子属性 abdomen，为 true 就是肚子空了，false 肚子饱了
- 创建两个线程，饿了就吃，吃了就拉（设 abdomen 为空）
- 最后肚子空了



### 原子数组

访问数组元素的时候它并不是线程安全的，所以 JDK 提供以下类来保证访问数据安全：

- AtomicIntegerArray
- AtomicLongArray
- AtomicReferenceArray

**案例**

对数组中的元素加加 1000_0 次

封装一个万能的 demo 方法，方便测试使用原子数组前后的对比

```java
/**
 * demo
 * @param arraySupplier     提供数组
 * @param countFun          获取数组元素个数
 * @param putConsumer       元素自增方法，参数 (array, index)
 * @param printConsumer     打印数组方法
 */
public static <T> void demo(Supplier<T> arraySupplier,
                            Function<T, Integer> countFun,
                            BiConsumer<T, Integer> putConsumer,
                            Consumer<T> printConsumer) {
    List<Thread> threads = new ArrayList<>();   // 保存 Thread
    T array = arraySupplier.get();  // 获取数组
    Integer count = countFun.apply(array);
    for (int i = 0; i < count; i++) {
        Thread thread = new Thread(() -> {
            for (int j = 0; j < 1000_0; j++) {
                // 这里 index 不能使用 i，以为lambda 中的外部变量必须要 final 修饰，i最后的值1000_0
                putConsumer.accept(array, j % count);
            }
        });
        threads.add(thread);
        thread.start();
    }
    threads.forEach(thread -> {
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    });
    printConsumer.accept(array);
}
```

**不安全实现**

```java
public static void main(String[] args) {
    demo(
            () -> new int[10],
            array -> array.length,
            (array, index) -> array[index]++,
            array -> System.out.println("res：" + Arrays.toString(array))
    );
}
```

```java
res：[8835, 8814, 8828, 8815, 8793]
```

结果显示错误

**使用引用数组的安全实现**

```java
demo(
        () -> new AtomicIntegerArray(5),
        AtomicIntegerArray::length,
        AtomicIntegerArray::incrementAndGet,
        System.out::println
);
```

```java
[10000, 10000, 10000, 10000, 10000]
```



### 原子累加器

- LongAdder
- DoubleAdder



#### 累加器性能比较

编写一个测试方法，让它累加到 20万

```java
public static <T> void demo(Supplier<T> adderSupplier, Consumer<T> consumer) {
    T adder = adderSupplier.get();
    long start = System.nanoTime();
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
        threads.add(new Thread(() -> {
            for (int j = 0; j < 5000_00; j++) {
                consumer.accept(adder);
            }
        }));
    }
    threads.forEach(Thread::start);
    threads.forEach(t -> {
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    });
    long end = System.nanoTime();
    System.out.println(adder + " 耗时：" + (end - start) / 1000_000 + "ns");
}
```

```java
public static void main(String[] args) {
    for (int i = 0; i < 5; i++) {
        demo(
                () -> new AtomicInteger(0),
                AtomicInteger::getAndIncrement
        );
    }
    System.out.println("=============================");
    for (int i = 0; i < 5; i++) {
        demo(
                LongAdder::new,
                LongAdder::increment
        );
    }
}
```

```java
2000000 耗时：42ns
2000000 耗时：40ns
2000000 耗时：31ns
2000000 耗时：34ns
2000000 耗时：29ns
=============================
2000000 耗时：15ns
2000000 耗时：6ns
2000000 耗时：6ns
2000000 耗时：6ns
2000000 耗时：6ns
```

可以看到是累加器性能更好一点~~~



#### LongAdder源码解析

LongAdder 是并发大师 @author Doug Lea （大哥李）的作品，设计的非常精巧

LongAdder 它的原理就是每个线程都有对应的累加单元 Cell，在最后将每个线程的 Cell 进行累加，最终得出总结果





##### 重要域

LongAdder 中的几个重要属性，准确的来说不是 LongAdder 中的， 而是 Striped64 类中的，但是 LongAdder 继承了 Striped64，所以也可以这么说

```java
// 累加载单元数组，懒加载
transient volatile Cell[] cells;

// 基础值，如果没有竞争就使用这个来累加
transient volatile long base;

// 在 cells 创建或扩容时，置为1，表示加锁 
transient volatile int cellsBusy;
```

> **补充**：transient 关键字的作用就是不进行序列化



##### 原理之伪共享

Striped64的内部类 Cell

```java
@sun.misc.Contended 
static final class Cell {
    volatile long value;
    ......
}
```

它使用 `@sun.misc.Contended`  注解进行修饰，这个注解有什么作业呢？

**CPU缓存结果解析**

![image-20231103205928681](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231103205928681.png)

CPU 它有三级缓存分别是 L1、L2、L3，通过缓存取值可以缩短执行时间，因为从内存中取值相对于缓存更慢

他们的速度比较如下：

| **从** **cpu** **到** | **大约需要的时钟周期**           |
| --------------------- | -------------------------------- |
| 寄存器                | 1 cycle (4GHz 的 CPU 约为0.25ns) |
| L1                    | 3~4 cycle                        |
| L2                    | 10~20 cycle                      |
| L3                    | 40~45 cycle                      |
| 内存                  | 120~240 cycle                    |

缓存以缓存行为单位，每个缓存行对应着一块内存，一般是 **64 byte（8 个 long）**

缓存的加入会造成副本的产生，一份数据会被缓存到不同核心的缓存中

CPU要保证缓存一致性，一旦某个核心更改了数据，其它 CPU 核心对应的整个缓存行必须失效

![image-20231103210303644](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231103210303644.png) 

LongAdder 中的 Cell 是以数组的形式存在的，数组是一个连续的内存结构。一个 Cell 为 24 个字节（对象头为16字节，属性 vlue 为 8字节），那么一个缓存块最多 2 个 cell对象，假设我们的操作如下：

- Core1 修改 Cell[0]
- Core2 修改 Cell[1]

那么 Core1 和 Core2 的缓存快中都有 Cell[0]、Cell[1]，此时我们的 Core1 修改 Cell[0] 就会导致 Core2 的缓存行失效需要重新加载数据，这个过程的时间就被浪费了，我们能不能将我们需要修改的 Cell 分开呢？

可以的，我们可以给 Cell 后面填充 padding，让我们一整行的缓存快填满，那么这个时候我们两个 Core 加载的就是不一样的数据了

![image-20231103212252096](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231103212252096.png) 

这样我们其中的 Core 更改数据就不会对其他的 Core 造成影响，性能就提升了

==而 `@sun.misc.Contended` 就是来做内存填充的==



##### 原理之 increment()

这是 LongAdder 的自增方法，我们看它到底做了什么

```java
public void increment() {
    add(1L);
}
```

```java
public void add(long x) {
    // as为累积单元数组，b为基础值，x为累加值
    Cell[] as; long b, v; int m; Cell a;
    // 进入 if 的条件
    // as 不为空，表示发生竞争  ||  cas失败表示有竞争	
    if ((as = cells) != null || !casBase(b = base, b + x)) {
        boolean uncontended = true;
        // as 还没创建
        if (as == null || (m = as.length - 1) < 0 ||
            // 当前对应的 Cell 还没有
            (a = as[getProbe() & m]) == null ||
            // cas 给当前线程的 cell 累加失败
            !(uncontended = a.cas(v = a.value, v + x)))
            // 创建 Cell 数组
            longAccumulate(x, null, uncontended);
    }
}
```

我们看一下 `longAccumulate(x, null, uncontended);` 如何创建数组的

```java
// 值为别是 x, null, ture
final void longAccumulate(long x, LongBinaryOperator fn,
                          boolean wasUncontended) {
    int h;
    // 当前线程还没有对应的 cell, 需要随机生成一个 h 值用来将当前线程绑定到 cell
    if ((h = getProbe()) == 0) {
        // 初始化 probe
        ThreadLocalRandom.current(); 
        // h 对应新的 probe 值, 用来对应 cell
        h = getProbe();
        wasUncontended = true;
    }
    // collide 为 true 表示需要扩容
    boolean collide = false;              
    for (;;) {
        Cell[] as; Cell a; int n; long v;
        // 已经创建了 cells
        if ((as = cells) != null && (n = as.length) > 0) {
            // 当前线程还没有对应的 Cell 对象
            if ((a = as[(n - 1) & h]) == null) {
                // 没有加锁
                if (cellsBusy == 0) {       
                    Cell r = new Cell(x);   
                    // 再次判断是否有加锁
                    if (cellsBusy == 0 && casCellsBusy()) {
                        // 是否已经创建，这个标记是退出使用的
                        boolean created = false;
                        try {             
                            // 下面就是将创建好的 Cell 放入数组中
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            // 这里做一个解锁
                            cellsBusy = 0;
                        }
                        // 创建不成功就继续创建，成功就退出
                        if (created)
                            break;
                        continue;          
                    }
                }
                collide = false;
            }
            // 有竞争, 改变线程对应的 cell 来重试 cas
            else if (!wasUncontended)     
                wasUncontended = true;   
            // fn为空个，使用 LongAdder 进行 CAS
            else if (a.cas(v = a.value, ((fn == null) ? v + x :
                                         fn.applyAsLong(v, x))))
                break;
            //  如果 cells 长度已经超过了最大核心数, 或者已经扩容, 改变线程对应的 cell 来重试 cas
            else if (n >= NCPU || cells != as)
                collide = false;       
            // 产生冲突，需要扩容 
            else if (!collide)
                collide = true;
            // 加锁
            else if (cellsBusy == 0 && casCellsBusy()) {
                // 加锁成功，扩容 x 2
                try {
                    if (cells == as) {    
                        Cell[] rs = new Cell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;              
            }
            // 改变线程对应的 cell
            h = advanceProbe(h);
        }
        // 还没有 cells, 尝试给 cellsBusy 加锁
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            boolean init = false;
            // 加锁成功，初始化 cells，长度为 2，并填充一个 null
            try {                       
                if (cells == as) {
                    Cell[] rs = new Cell[2];
                    rs[h & 1] = new Cell(x);
                    cells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            // 初始化成功 break
            if (init)
                break;
        }
        // 上面两种情况失败，尝试给 base 累加  
        else if (casBase(v = base, ((fn == null) ? v + x :
                                    fn.applyAsLong(v, x))))
            break;
    }
}
```





## Unsafe

### 概述

原子类的底层调用的是 Unsafe 的方法，它提供了底层操作内存、线程的方法

Unsafe 不能直接获取，可以通过反射获得或者通过系统加载

cas方法

- compareAndSwapInt
- compareAndSwapLong
- compareAndSwapObject



### 使用

```java
public class UnsafeTest {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        // 获取 unsafe 对象，它封装了一个获取方法
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(null);

        // 1.获取域(属性)的偏移地址
        Field id = Student.class.getDeclaredField("id");
        Field name = Student.class.getDeclaredField("name");
        long idOffset = unsafe.objectFieldOffset(id);
        long nameOffset = unsafe.objectFieldOffset(name);

        // 2.执行 cas 操作
        Student student = new Student();
        unsafe.compareAndSwapObject(student, idOffset, 0, 1);
        unsafe.compareAndSwapObject(student, nameOffset, null, "张三");

        // 3.验证
        System.out.println(student);
    }
}
@Data
class Student {
    private int id;
    private String name;
}
```



### 我自己的原子类

封装一下 Unsafe 

```java
public class UnsafeAccessor {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}
```

这里我们写一个自己的原子类继承 Account 来实现存钱取钱操作

Account类

```java
public interface Account {
    // 获取余额
    int getBalance();
    // 取钱
    void withdraw(Integer amount);

    static public void demo(Account account) {
        List<Thread> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            Thread thread = new Thread(() -> {
                account.withdraw(10);
            });
            list.add(thread);
            thread.start();
        }
        list.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.currentTimeMillis();
        System.out.println("剩余金额：" + account.getBalance()
                + " coat：" + (end - start) + "ms");
    }
}
```

```java
public class MyAtomicInteger implements Account {

    private static final Unsafe UNSAFE;
    // 记得添加 valatile 让它可见
    private volatile int value;
    private static final long VALUE_OFFSET;

    static {
        UNSAFE = UnsafeAccessor.getUnsafe();
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(MyAtomicInteger.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public MyAtomicInteger(int value) {
        this.value = value;
    }

    @Override
    public int getBalance() {
        return this.value;
    }

    @Override
    public void withdraw(Integer amount) {
        int prev, next;
        do {
            prev = this.value;
            next = this.value - amount;
        } while (!UNSAFE.compareAndSwapInt(this, VALUE_OFFSET, prev, next));
    }

    public static void main(String[] args) {
        Account.demo(new MyAtomicInteger(1000_0));
    }
}
```

```java
剩余金额：0 coat：139ms
```

完美~~~





# 六、共享模型之不可变

## 可变的安全性

### 1、案例：格式化时间

```java
public static void simpleDateFormatTest() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            try {
                Date date = dateFormat.parse("2023-01-01");
                log.debug("date：{}", date);
            } catch (ParseException e) {
                log.error("{}", e.toString());
            }
        }).start();
    }
}
```

因为 SimpleDateFormat  不是线程安全的，运行上述会出现以下错误：

```java
java.lang.NumberFormatException: multiple points
	at sun.misc.FloatingDecimal.readJavaFormatString(FloatingDecimal.java:1890)
	at sun.misc.FloatingDecimal.parseDouble(FloatingDecimal.java:110)
	at java.lang.Double.parseDouble(Double.java:538)
	at java.text.DigitList.getDouble(DigitList.java:169)
	at java.text.DecimalFormat.parse(DecimalFormat.java:2089)
	at java.text.SimpleDateFormat.subParse(SimpleDateFormat.java:1869)
	at java.text.SimpleDateFormat.parse(SimpleDateFormat.java:1514)
	at java.text.DateFormat.parse(DateFormat.java:364)
```



### 2、解决思路 - 加锁

```java
public static void simpleDateFormatTest() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            synchronized (Date.class) {
                try {
                    Date date = dateFormat.parse("2023-01-01");
                    log.debug("date：{}", date);
                } catch (ParseException e) {
                    log.error("{}", e.toString());
                }
            }
        }).start();
    }
}
```

虽然这样可以解决问题，但是会带来性能上的损耗



### 3、解决思路 - 使用 DateTimeFormatter

```java
public static void dateTimeFormatterTest() {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            LocalDate date = dateFormatter.parse("2023-10-10", LocalDate::from);
            log.debug("date：{}", date);
        }).start();
    }
}
```

```java
22:01:58.427 [Thread-4] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-1] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-0] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-9] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-5] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-6] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-8] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-2] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-3] DEBUG Test01 - date：2023-10-10
22:01:58.427 [Thread-7] DEBUG Test01 - date：2023-10-10
```

没问题~~~



### 4、分析

分析一下 DateTimeFormatter 为什么没有像 SimpleDateFormat 一样出现了异常

我们看一下它的类

```java
public final class DateTimeFormatter {
    private final CompositePrinterParser printerParser;
    private final Locale locale;
    private final DecimalStyle decimalStyle;
    private final ResolverStyle resolverStyle;
    private final Set<TemporalField> resolverFields;
    private final Chronology chrono;
    private final ZoneId zone;
    
    ......
}
```

可以看到我们的 DateTimeFormatter 类中所有变量都是 private + final 修饰的，就连类都是 final 修饰的，妥妥的安全，那么为什么加 final 可以保证它的线程安全性呢？





## final 原理





## 不可变设计

下面我们用 String 类来做示例，看一下 JDK 如何设计不可变类



### final 使用

- 属性使用了 final 来修饰，属性不能被修改
- 类使用 final 修饰，让类不能被继承重写，保证了方法的不可变安全

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    private final char value[];
    private int hash; // Default to 0
    private static final long serialVersionUID = -6849794470754667710L;
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];
```



### 保护性拷贝

观察 String 类方法发现它里面也有很多修改属性的方法，我们拿 substring 来举例

```java
public String substring(int beginIndex) {
    if (beginIndex < 0) {
        throw new StringIndexOutOfBoundsException(beginIndex);
    }
    int subLen = value.length - beginIndex;
    if (subLen < 0) {
        throw new StringIndexOutOfBoundsException(subLen);
    }
    return (beginIndex == 0) ? this : new String(value, beginIndex, subLen);
}
```

上面的代码做的是边界检查，重点是下面这句

```java
return (beginIndex == 0) ? this : new String(value, beginIndex, subLen);
```

如果 beginIndex == 0 则返回当前对象，否则创建一个新对象返回

我们在看一下 String 的构造方法中做了什么

```java
// 上面的都是在做边界检查
this.value = Arrays.copyOfRange(value, offset, offset+count);
```

它将之前的 value 做了切割然后复制一份，==这种通过创建副本对象来避免共享的手段叫【保护性拷贝】==



### 无状态类

没有任何成员变量的类是线程安全的，这就是我们的无状态类







## 设计模式 - 享元模式

### 定义

我们之前讲过 【保护性拷贝】，它让我们避免了变量共享，但是出现了新问题就是对象不断创建会导致内存撑爆，不利于程序的运行，所以我们有没有什么办法优化一下呢？

有的，我们可以对相同的对象进行重用，这样就能不能重复创建独享，这种需要重用数量有限的同一类对象的模式就是享元模式。



### 体现

在JDK中 Boolean，Byte，Short，Integer，Long，Character 等包装类提供了 valueOf 方法，以 Long 为例

```java
public static Long valueOf(long l) {
    final int offset = 128;
    if (l >= -128 && l <= 127) { // will cache
        return LongCache.cache[(int)l + offset];
    }
    return new Long(l);
}
```

我们可以看到如果值在 -128 ~ 127 时，它会从缓存中获取奖值返回，如果超过了缓存范围它就会创建新的对象返回，这个就是享元模式

> **注意：**
>
> - Byte, Short, Long 缓存的范围都是 -128~127
> - Character 缓存的范围是 0~127
> - Integer的默认范围是 -128~127
> - 最小值不能变
> - 但最大值可以通过调整虚拟机参数 ` 
> - -Djava.lang.Integer.IntegerCache.high` 来改变
> - Boolean 缓存了 TRUE 和 FALSE



### DIY-数据库连接池

#### 简化版

```java
/**
 * @author xiaozhi
 *
 * DIY 连接池
 */
@Slf4j(topic = "DIYPool")
public class DIYPool {

    public static void main(String[] args) {
        Pool pool = new Pool(2);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                String name = Thread.currentThread().getName();
                MockConnection conn = pool.getConnection();
                log.debug("{} 获取连接 {}", name, conn.getName());
                TimeUtil.sleep(1);
                pool.free(conn);
                log.debug("{} 释放连接 {}", name, conn.getName());
            }, "t" + i).start();
        };
    }
}

@Slf4j(topic = "Pool")
class Pool {

    private final int size;
    private final MockConnection[] connections;
    // 连接状态数组，空闲-0，繁忙-1
    private final AtomicIntegerArray states;

    public Pool(int size) {
        this.size = size;
        states = new AtomicIntegerArray(size);
        this.connections = new MockConnection[size];
        log.debug("============== 创建连接 ===============");
        for (int i = 0; i < size; i++) {
            connections[i] = new MockConnection("连接" + i);
        }
    }

    // 获取连接
    public MockConnection getConnection() {
        // 遍历判断是否有空闲的连接
        while (true) {
            for (int i = 0; i < size; i++) {
                if (states.get(i) == 0 && states.compareAndSet(i, 0, 1)) {
                    return connections[i];
                }
            }
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // 归还连接
    public void free(Connection conn) {
        for (int i = 0; i < connections.length; i++) {
            if (conn == connections[i]) {
                // 设置成空闲状态，它不用进行 cas 因为它是此时是获取锁的，所以没有其他线程竞争
                states.set(i, 0);
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }
    }
}

class MockConnection implements Connection {

    private String name;

    public MockConnection(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
	// 实现忽略...
}
```

```java
00:32:53.224 [main] DEBUG Pool - ============== 创建连接 ===============
00:32:53.274 [t0] DEBUG DIYPool - t0 获取连接 连接0
00:32:53.274 [t1] DEBUG DIYPool - t1 获取连接 连接1
00:32:54.291 [t0] DEBUG DIYPool - t0 释放连接 连接0
00:32:54.291 [t1] DEBUG DIYPool - t1 释放连接 连接1
00:32:54.291 [t3] DEBUG DIYPool - t3 获取连接 连接1
00:32:54.291 [t4] DEBUG DIYPool - t4 获取连接 连接0
00:32:55.305 [t3] DEBUG DIYPool - t3 释放连接 连接1
00:32:55.305 [t4] DEBUG DIYPool - t4 释放连接 连接0
00:32:55.305 [t2] DEBUG DIYPool - t2 获取连接 连接1
00:32:56.319 [t2] DEBUG DIYPool - t2 释放连接 连接1
```

结果没出题，释放连接后才可以获取连接

> 这是参考了 Tomcat 的 JDBC连接池实现源码，建议可以读一下它的源码
>
> 拓展：不建议线上自己去写连接池，可以使用 c3p0、Druid等成熟的开源框架



#### 改进版

改进的地方

- 连接的增长和收缩，比如最小连接数和最大连接数
- 超市等待，如果连接池的连接一直被占用，那么就直接让它超时结束
- 连接保话（可用性检测），通过发送 SQL 语句来检测活性，如果一定时间内没有返回那么就是连接超时
- 分布式 hash，一个连接池维护**多个数据库**的多个连接，我们可以通过计算hash来获取对应位置的连接

```java

```







# 七、工具之线程池

本章内容

- 





## 自定义线程池

之前我们讲过享元模式，它可以减少重复对象的创建，节省内存提高了性能。我们的线程池也是如此防止线程频繁的创建，我们可以复用之前创建的线程来执行我们的任务，提高我们的内存和提高性能。

我们先试着自己写一个线程池，后面我们会讲成熟的线程池

![image-20231107164310936](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231107164310936.png) 

上图是我们的架构图，它的主要部件：

- BlockerQueue：阻塞队列，当我们的线程池中的线程全部繁忙，那么就让他们在阻塞队列中等待线程执行结束，线程执行完其他任务之后就会从阻塞队列中获取 task 来执行，这里使用的是我们的生产者消费者模式。生产方一般是多余消费方，所以我们进行阻塞等待执行完毕。
- ThreadPool：线程池，存放我们创建的线程，从 BlockedQueue 中获取 task 来执行



### 1.创建BlockedQueue 阻塞队列

首先我们需要创建一个 BlockedQueue 对象来保存我们的 task

- addTask：添加 task，一直等待
- timeOutAddTask：添加 task，超过一定时间就不等待了
- getTask：获取 task，一直等待
- timeOutGetTask：获取task，超过一定时间就不等待

```java
/**
 * 阻塞队列
 */
@Slf4j(topic = "ThreadPoolTest")
class BlockedQueue<T> {
    // 队列保存 task
    private Deque<T> tasks = new ArrayDeque<>(); // 大多数情况下，它的性能比 LinkedList 好

    // 锁
    private ReentrantLock lock = new ReentrantLock();

    // 生产者条件变量，如果它满了那么就让它进行等待
    private Condition fullWaitSet = lock.newCondition();

    // 消费者条件变量
    private Condition idleWaitSet = lock.newCondition();

    // 阻塞队列最大容量
    private int capcity;

    public BlockedQueue(int capcity) {
        this.capcity = capcity;
    }

    // 获取当前队列的数量
    public int getSize() {
        lock.lock();
        try {
            return tasks.size();
        } finally {
            lock.unlock();
        }
    }

    // 阻塞添加
    public void addTask(T task) {
        lock.lock();
        try {
            // 如果队列满了那么就一直等待
            while (tasks.size() == capcity) {
                try {
                    log.debug("阻塞队列已满...");
                    fullWaitSet.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tasks.addLast(task);
            log.debug("添加任务：{}", task);
            idleWaitSet.signal();       // 唤醒空闲的线程
        } finally {
            lock.unlock();
        }
    }

    // 超时等待阻塞添加
    public boolean timeOutAddTask(T task, long time, TimeUnit unit) {
        lock.lock();
        try {
            long nanos = unit.toNanos(time);
            // 如果队列满了那么就一直等待
            while (tasks.size() == capcity) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    // awaitNanos 方法返回等待的纳秒数
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tasks.add(task);
            idleWaitSet.signal();       // 唤醒空闲的线程
            return true;
        } finally {
            lock.unlock();
        }
    }

    // 阻塞获取
    public T getTask() {
        lock.lock();
        try {
            // 如果它为空，那么让它等待
            while (tasks.isEmpty()) {
                try {
                    idleWaitSet.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            T task = tasks.removeFirst();
            log.debug("添加任务 {}", task);
            fullWaitSet.signal();       // 此时队列中有位置了，所以唤醒生产者
            return task;
        } finally {
            lock.unlock();
        }
    }

    // 超时等待阻塞获取
    public T timeOutGetTask(long time, TimeUnit unit) {
        lock.lock();
        try {
            long nanos = unit.toNanos(time);
            // 如果它为空，那么让它等待
            while (tasks.isEmpty()) {
                try {
                    // 记得要进行退出
                    if (nanos <= 0) {
                        return null;
                    }
                    log.debug("阻塞等待...");
                    nanos =  idleWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            T task = tasks.removeFirst();
            fullWaitSet.signal();       // 此时队列中有位置了，所以唤醒生产者
            return task;
        } finally {
            lock.unlock();
        }
    }
}
```



### 2.创建线程池

```java
/**
 * 自定义线程池
 */
@Slf4j(topic = "ThreadPoolTest")
class MyPool {
    // 阻塞队列
    private BlockedQueue<Runnable> taskQueue;

    // 线程集合 - 保存线程
    private HashSet<Worker> workers;

    // 核心线程数
    private int coreSize;

    // 超时时间
    private long timeout;
    // 时间单位
    private TimeUnit unit;

    public MyPool(int coreSize, int capcity, long timeout, TimeUnit unit) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.unit = unit;
        workers = new HashSet<>(this.coreSize);
        taskQueue = new BlockedQueue<>(capcity);
    }

    public void execute(Runnable task) {
        synchronized (workers) {
            // 线程没满就创建线程运行，如果线程池满了，则添加任务到阻塞队列中
            if (workers.size() < coreSize) {
                Worker worker = new Worker(task);
                log.debug("新增 worker {}", worker);
                workers.add(worker);
                worker.start();
            } else {
                taskQueue.timeOutAddTask(task, timeout, unit);
            }
        }
    }

    class Worker extends Thread {
        private Runnable task;
        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            // 执行条件：当前 task 不为空，队列中有 task
            while (task != null || (task = taskQueue.timeOutGetTask(timeout, unit)) != null) {
                try {
                    log.debug("执行任务：{}", task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }
            // 如果没有任务了就移除
            synchronized (workers) {
                workers.remove(this);
            }
        }
    }
}
```



### 3.测试

```java
@Slf4j(topic = "ThreadPoolTest")
public class ThreadPoolTest {
    public static void main(String[] args) {
        MyPool myPool = new MyPool(1, 2, 5, TimeUnit.SECONDS);
        for (int i = 0; i < 5; i++) {
            int j = i;
            myPool.execute(() -> {
                TimeUtil.sleep(2);
                log.debug("{}", j);
            });
        }
    }
}
```

```java
20:11:39.959 [main] DEBUG ThreadPoolTest - 阻塞队列已满...
20:11:39.959 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$1/186276003@3a9f7f17
20:11:39.962 [Thread-0] DEBUG ThreadPoolTest - 0
20:11:41.967 [main] DEBUG ThreadPoolTest - 阻塞队列已满...
20:11:41.967 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$1/186276003@4ea86b5d
20:11:41.967 [Thread-0] DEBUG ThreadPoolTest - 1
20:11:43.974 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$1/186276003@f0ed258
20:11:43.974 [Thread-0] DEBUG ThreadPoolTest - 2
20:11:45.986 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$1/186276003@1fb2096f
20:11:45.986 [Thread-0] DEBUG ThreadPoolTest - 3
20:11:47.987 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$1/186276003@609096d0
20:11:47.987 [Thread-0] DEBUG ThreadPoolTest - 4
20:11:49.987 [Thread-0] DEBUG ThreadPoolTest - 阻塞等待...
```

**分析**：从结果中我们可以知道队列满了两次

- 一开始执行了 task0，task1 和 task2 放入阻塞队列中，task3, task4阻塞，日志输出队列已满
- 接着 task0 结束，task1 上位，队列空一个，task2 加入
- 此时 task4 再加入就会被阻塞等待，日志输出队列已满
- 最后 task1 执行完毕，task2上位，task4加入队列中
- 因为后面没有其他 task 了，所以没有输出队列已满



### 4、情况：阻塞队列满了

#### 描述

我们的做法是如果当前没有空闲的线程去执行 task，就将任务放入到 阻塞队列中，那么如果此时阻塞队列满了，这些 task该如何处理呢？

列举一些处理方式：

- 一直等待
- 带超时等待
- 让调用者放弃任务执行
- 让调用者抛出异常
- 让调用者自己去执行任务

但是这么多的选项我们要一个个去写方法供调用者选择吗？当然不是，我们可以交给调用者自己来定义当阻塞队列满时该如何操作，因为我们提供的这些选择也可能不是人家想要的，所以让调用者自定义操作是比较稳妥的。

所以，这里我们用上 **设计模式及 - 策略模式** 来做这个改进



#### 代码改进

首先我们这里需要定义一个接口，我们定义规范，然后让调用者来做实现。我们这里只提供一个抽象方法，所以我们选择函数式接口

```java
@FunctionalInterface
interface RejectPolicy<T> {
    void reject(BlockedQueue<T> queue, T task);
}
```

因为是队列的功能实现，所以我们需要再 BlockedQueue 中添加一个方法，这个方法来做我们的自定义操作

```java
public void tryAddTask(RejectPolicy<T> rejectPolicy, T task) {
    lock.lock();
    try {
        // 判断队列中是否已满
        if (tasks.size() == capcity) {
            rejectPolicy.reject(this, task);
        } else {
            log.debug("加入任务队列： {}", task);
            tasks.addLast(task);
            idleWaitSet.signal();
        }
    } finally {
        lock.unlock();
    }
}
```

接着我们需要使用这个自定义的方法，修改 execute 方法

```java
public void execute(Runnable task) {
    synchronized (workers) {
        // 线程没满就创建线程运行，如果线程池满了，则添加任务到阻塞队列中
        if (workers.size() < coreSize) {
            Worker worker = new Worker(task);
            workers.add(worker);
            worker.start();
        } else {
            // taskQueue.timeOutAddTask(task, timeout, unit);
            // rejectPolicy 通过构造器传入进来
            taskQueue.tryAddTask(rejectPolicy, task);
        }
    }
}
```



#### 测试

```java
public static void main(String[] args) {
    MyPool myPool = new MyPool(1, 2, 5, TimeUnit.SECONDS,
            ((queue, task) -> {
                // 一直等待
                // queue.addTask(task);
                // 带超时等待
                // queue.timeOutAddTask(task, 2, TimeUnit.SECONDS);
                // 让调用者放弃任务执行
                // log.debug("放弃任务：{}", task);
                // 让调用者抛出异常
                // throw new RuntimeException("任务执行失败：" + task);
                // 让调用者自己去执行任务
                task.run();
            }));
    for (int i = 0; i < 5; i++) {
        int j = i;
        myPool.execute(() -> {
            log.debug("{}", j);
            TimeUtil.sleep(2);
        });
    }
}
```

```java
20:55:53.319 [main] DEBUG ThreadPoolTest - 加入任务队列： com.xiaozhi.tool07.ThreadPoolTest$$Lambda$2/485815673@5e8c92f4
20:55:53.319 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$2/485815673@6c02e750
20:55:53.323 [Thread-0] DEBUG ThreadPoolTest - 0
20:55:53.323 [main] DEBUG ThreadPoolTest - 加入任务队列： com.xiaozhi.tool07.ThreadPoolTest$$Lambda$2/485815673@3581c5f3
20:55:53.323 [main] DEBUG ThreadPoolTest - 3
20:55:55.333 [main] DEBUG ThreadPoolTest - 4
20:55:57.343 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$2/485815673@5e8c92f4
20:55:57.343 [Thread-0] DEBUG ThreadPoolTest - 1
20:55:59.354 [Thread-0] DEBUG ThreadPoolTest - 执行任务：com.xiaozhi.tool07.ThreadPoolTest$$Lambda$2/485815673@3581c5f3
20:55:59.354 [Thread-0] DEBUG ThreadPoolTest - 2
20:56:01.355 [Thread-0] DEBUG ThreadPoolTest - 阻塞队列为空，等待中...
```

可以看到有些任务是由调用者(main) 去执行的~~~





## ThreadPoolExecutor

![image-20231108160124123](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231108160124123.png) 



### ThreadPoolExecutor 类分析

#### 线程池状态表示

```java
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;
```

在 ThreadPoolExecutor 声明了五种状态

| 状态名     | 高3位 | 接收新任务 | 处理阻塞队列中的任务 | 说明                                               |
| :--------- | :---: | :--------: | :------------------: | :------------------------------------------------- |
| RUNNING    |  111  |     Y      |          Y           | 运行状态                                           |
| SHUTDOWN   |  000  |     N      |          Y           | 不会再接收新的任务，但是还是会执行阻塞队列中的任务 |
| STOP       |  001  |     N      |          N           | 不会接受新任务，也不会执行阻塞队列中的任务         |
| TIDYING    |  010  |     -      |          -           | 任务全执行完毕，活动线程为0即进入终结状态          |
| TERMINATED |  011  |     -      |          -           | 终结状态                                           |

ThreadPoolExecutor 使用 int 类型的高三位表示线程池的状态，剩下的 29 位表示线程的数量

```java
// ThreadPoolExecutor 类
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
// 线程数量
private static final int COUNT_BITS = Integer.SIZE - 3;

......

private static int ctlOf(int rs, int wc) { return rs | wc; }
```

状态 和 线程数量存放在一个 原子变量 ctl 中，目的是将他们两个合二为一，减少一次 cas 操作，可以看到上面的代码中我们的 ctl 是一个原子整数。ctlOf 方法的功能就是使用异或将两个数合在一起。而状态的表示在上面我们已经看到过了



#### 构造方法

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler)
```

**说明**：

- corePoolSize：核心线程数（最多保留的线程数）
- maximumPoolSize：最大线程数
- keepAliveTime：急救线程生存时间
- unit：急救线程生存时间单位
- workQueue：阻塞队列
- threadFactory：线程工厂，自定义线程的创建
- handler：拒绝策略，当阻塞队列满时做出的策略

**工作方式**：

**核心线程**：

- 线程池一开始是没有线程的，当有任务提交了，线程池才会创建爱一个线程来执行任务
- 当线程数达到 corePoolSize 后，任务会被放到阻塞队列中，等待空闲的线程来获取执行

**急救线程**：

- 如果选择的是有界阻塞队列时，当队列满时，就会创建  **maximumPoolSize - corePoolSize** 数量的急救线程来处理溢出的任务
- 当高峰期过后，超过 corePoolSize 的急救线程在一段时间内没有任务做，那么它就会被销毁，这个时间是由 **keepAliveTime 和 unit** 来控制 

**拒绝策略**：如果核心线程 + 急救线程的数量达到 maximumPoolSize 时仍有任务这时就会执行拒绝策略。拒绝策略 JDK 提供了 4 种实现，其他著名的框架也提供了不同的实现

- **JDK**：

    RejectedExecutionHandler 的实现类如下：

    ![image-20231109002220187](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231109002220187.png) 

    - AbortPolicy：让调用者抛出 RejectedExecutionException 异常，这是默认策略

    - CallerRunsPolicy：让调用者运行任务

    - DiscardPolicy：放弃本次任务

    - DiscardOldestPolicy：放弃队列中最早的任务，本任务取而代之 

- **其他框架**：

    - Dubbo 的实现，在抛出 RejectedExecutionException 异常之前会记录日志，并 dump 线程栈信息，方便定位问题

    - Netty 的实现，是创建一个新线程来执行任务

    - ActiveMQ 的实现，带超时等待（60s）尝试放入队列，类似我们之前自定义的拒绝策略

    - PinPoint 的实现，它使用了一个拒绝策略链，会逐一尝试策略链中每种拒绝策略



### 创建线程池

JDK 提供一个 Executors 类通过众多工厂方法创建各种用途的线程池，不需要我们手动去 new

#### newFixedThreadPool

特点：

- 它可以创建只有核心线程的固定数量线程池
- 阻塞队列是无界的，可以放置任意数量的 task

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  // 没有救急线程，所以秒数为 0
                                  0L, TimeUnit.MILLISECONDS,
                                  // 无边界的阻塞队列
                                  new LinkedBlockingQueue<Runnable>());
}
```

**使用**

ExecutorService#execute(Runnable)：接收一个task，然后执行

```java
public static void method01() {
    ExecutorService executorService = Executors.newFixedThreadPool(5);
    for (int i = 0; i < 10; i++) {
        int finalI = i;
        executorService.execute(() -> {
            TimeUtil.sleep(1);
            log.debug("执行任务：{}", finalI);
        });
    }
}
```

```
00:33:26.861 [pool-1-thread-4] DEBUG ExecutorsTest - 执行任务：3
00:33:26.861 [pool-1-thread-5] DEBUG ExecutorsTest - 执行任务：4
00:33:26.861 [pool-1-thread-1] DEBUG ExecutorsTest - 执行任务：0
00:33:26.861 [pool-1-thread-2] DEBUG ExecutorsTest - 执行任务：1
00:33:26.861 [pool-1-thread-3] DEBUG ExecutorsTest - 执行任务：2
00:33:27.875 [pool-1-thread-3] DEBUG ExecutorsTest - 执行任务：9
00:33:27.875 [pool-1-thread-1] DEBUG ExecutorsTest - 执行任务：7
00:33:27.875 [pool-1-thread-4] DEBUG ExecutorsTest - 执行任务：5
00:33:27.875 [pool-1-thread-5] DEBUG ExecutorsTest - 执行任务：6
00:33:27.875 [pool-1-thread-2] DEBUG ExecutorsTest - 执行任务：8
```

后面五个一秒后被执行了

==**注意**：此时的程序还未结束，核心线程是不会消失的，所以它们会一直在等待，因此程序不会结束==



#### newCachedThreadPool

```java
public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>());
    }
```

**特点**：

- 核心线程为 0，最大线程数是 Integer.MAX_VALUE，也就是说全部都是 急用线程
- 急用线程的存活是见是 60s，最多创建 Integer.MAX_VALUE 个急用线程
- SynchronousQueue 队列没有容量，只有当线程来取的时候才能放进任务（一手交钱一手交货）

> **评价** 整个线程池表现为线程数会根据任务量不断增长，没有上限，当任务执行完毕，空闲 1分钟后释放线程。 适合任务数比较密集，但每个任务执行时间较短的情况

```java
private static void method02() {
    SynchronousQueue<Integer> synchronousQueue = new SynchronousQueue<>();
    new Thread(() -> {
        try {
            synchronousQueue.put(1);
            log.debug("putted {}", 1);

            synchronousQueue.put(2);
            log.debug("putted {}", 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }, "t1").start();
    new Thread(() -> {
        try {
            Integer take = synchronousQueue.take();
            log.debug("taking {}", take);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }, "t2").start();
    new Thread(() -> {
        try {
            Integer take = synchronousQueue.take();
            log.debug("taking {}", take);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }, "t3").start();
}
```

```java
00:56:44.499 [t2] DEBUG ExecutorsTest - taking 1
00:56:44.499 [t1] DEBUG ExecutorsTest - putted 1
00:56:44.502 [t1] DEBUG ExecutorsTest - putted 2
00:56:44.502 [t3] DEBUG ExecutorsTest - taking 2
```

t2 先 take，然后 t3 等待 t2 获取，所以 putted 2输出了，最后它才获取了 take



**newSingleThreadExecutor**

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}
```

**特点**：只有一个核心线程和一个无界的阻塞队列

**使用场景**：希望任务一个一个按顺序执行

**手动创建和它区别**：

- 手动创建任务失败了没有补救措施，而线程池会保证有一个线程存在

- 线程池的个数不能被修改，FinalizableDelegatedExecutorService 使用装饰器模式，对外暴露了 ExecutorService 接口，因此不能调用修改方法来修改它的个数

    ```java
    static class FinalizableDelegatedExecutorService
        extends DelegatedExecutorService {
        FinalizableDelegatedExecutorService(ExecutorService executor) {
            super(executor);
        }
        protected void finalize() {
            super.shutdown();
        }
    }
    ```

**使用**

```java
private static void method03() {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    for (int i = 0; i < 5; i++) {
        int finalI = i;
        executorService.execute(() -> {
            log.debug("输出：{}", finalI);
        });
    }
}
```

```java
01:08:06.297 [pool-1-thread-1] DEBUG ExecutorsTest - 输出：0
01:08:06.302 [pool-1-thread-1] DEBUG ExecutorsTest - 输出：1
01:08:06.302 [pool-1-thread-1] DEBUG ExecutorsTest - 输出：2
01:08:06.302 [pool-1-thread-1] DEBUG ExecutorsTest - 输出：3
01:08:06.302 [pool-1-thread-1] DEBUG ExecutorsTest - 输出：4
```

结果有序输出~~~



### 提交任务

等待任务的执行结果返回才往下执行

#### ExecutorService方法

```java
// 提交任务 task，用返回值 Future 获得任务执行结果
<T> Future<T> submit(Callable<T> task);

// 提交 tasks 中所有任务，所有任务都完成才能提交
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
 throws InterruptedException;

// 提交 tasks 中所有任务，所有任务都完成才能提交，带超时时间
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
 long timeout, TimeUnit unit)
 throws InterruptedException;

// 提交 tasks 中所有任务，哪个任务先成功执行完毕，返回此任务执行结果，其它任务取消
<T> T invokeAny(Collection<? extends Callable<T>> tasks)
 throws InterruptedException, ExecutionException;

// 提交 tasks 中所有任务，哪个任务先成功执行完毕，返回此任务执行结果，其它任务取消，带超时时间
<T> T invokeAny(Collection<? extends Callable<T>> tasks,
 long timeout, TimeUnit unit)
 throws InterruptedException, ExecutionException, TimeoutException;
```



#### 使用

```java
@Slf4j(topic = "SubmitTaskTest")
public class SubmitTaskTest {
    static ExecutorService threadPool = Executors.newFixedThreadPool(5);
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // submitTest();
        // invokeAllTest();
        // invokeAnyTest();
    }

    private static void invokeAllTest() throws InterruptedException {
        List<Future<Object>> futures = threadPool.invokeAll(Arrays.asList(
                () -> {
                    TimeUtil.sleep(1);
                    log.debug("执行完毕 1");
                    return "str1";
                },
                () -> {
                    TimeUtil.sleep(3);
                    log.debug("执行完毕 2");
                    return "str2";
                },
                () -> {
                    TimeUtil.sleep(2);
                    log.debug("执行完毕 3");
                    return "str3";
                }
        ));
        futures.forEach(f -> {
            try {
                log.debug("result：{}", f.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        /*
           结果输出：
                01:41:12.051 [pool-1-thread-1] DEBUG SubmitTaskTest - 执行完毕 1
                01:41:13.058 [pool-1-thread-3] DEBUG SubmitTaskTest - 执行完毕 3
                01:41:14.059 [pool-1-thread-2] DEBUG SubmitTaskTest - 执行完毕 2
                01:41:14.059 [main] DEBUG SubmitTaskTest - result：str1
                01:41:14.060 [main] DEBUG SubmitTaskTest - result：str2
                01:41:14.060 [main] DEBUG SubmitTaskTest - result：str3
                // 全部执行完了才会提交结果
         */
    }

    private static void invokeAnyTest() throws InterruptedException, ExecutionException {
        String res = threadPool.invokeAny(Arrays.asList(
                () ->  {
                    TimeUtil.sleep(1);
                    return "str1";
                },
                () -> {
                    TimeUtil.sleep(2);
                    return "str2";
                },
                () -> {
                    TimeUtil.sleep(3);
                    return "str3";
                }
        ));
        log.debug("result：{}", res);    // result：str1，它的执行时间最短
    }

    private static void submitTest() throws InterruptedException, ExecutionException {
        Future<?> future = threadPool.submit(() -> {
            log.debug("start...");
            TimeUtil.sleep(1);
            return "test";
        });
        String str = (String) future.get();
        log.debug("result：{}", str);    // result：test
    }
}
```





### 关闭线程池

#### shutdown

```java
/*
	线程池状态变为 SHUTDOWN
		- 不会接收新任务
 		- 会完成已加入阻塞队列中的任务
 		- 次方法不会阻塞调用线程的执行
 */
void shutdown();
```

ThreadPoolExecutor 中的实现

```java
public void shutdown() {
    // 创建锁
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 修改线程池状态
        advanceRunState(SHUTDOWN);
        // 仅会打断空闲线程
        interruptIdleWorkers();
        onShutdown();	// 扩展点 ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    // 尝试终结
    tryTerminate();
}
```

```java
private static void shutdownTest() {
    ExecutorService pool = Executors.newFixedThreadPool(5);
    for (int i = 0; i < 5; i++) {
        int finalI = i;
        pool.execute(() -> {
            TimeUtil.sleep(2);
            log.debug("执行任务：{}", finalI);
        });
    }
    log.debug("执行关闭操作...");
    pool.shutdown();
}
```

```java
15:45:47.937 [main] DEBUG ShutdownTest - 执行关闭操作...
15:45:49.945 [pool-1-thread-4] DEBUG ShutdownTest - 执行任务：3
15:45:49.945 [pool-1-thread-3] DEBUG ShutdownTest - 执行任务：2
15:45:49.945 [pool-1-thread-5] DEBUG ShutdownTest - 执行任务：4
15:45:49.945 [pool-1-thread-1] DEBUG ShutdownTest - 执行任务：0
15:45:49.945 [pool-1-thread-2] DEBUG ShutdownTest - 执行任务：1
```

可以看到我们是先关闭，然后才执行的~~



#### shutdownNow

```java
/*
	线程池状态变为 STOP
		- 不会接收新任务
		- 不会处理阻塞队列中的任务
		- 使用 interupt 的方式中断正在执行任务的线程
 */
List<Runnable> shutdownNow();
```

```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 修改线程状态
        advanceRunState(STOP);
        // 打断所有线程
        interruptWorkers();
        // 获取队列中剩余的任务
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    // 尝试关闭
    tryTerminate();
    return tasks;
}
```



#### 使用效果

```java
@Slf4j(topic = "ShutdownTest")
public class ShutdownTest {

    static ExecutorService pool = Executors.newCachedThreadPool();
    static ExecutorService pool2 = Executors.newCachedThreadPool();

    public static void demo(ExecutorService pool, Consumer<ExecutorService> consumer) {
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            pool.execute(() -> {
                TimeUtil.sleep(1);
                log.debug("执行任务：{}", finalI);
            });
        }
        consumer.accept(pool);
    }
    public static void main(String[] args) {
        log.debug("========== shutdown =========");
        demo(pool , ExecutorService::shutdown);
        TimeUtil.sleep(3);
        log.debug("========== shutdownNow =========");
        demo(pool2, ExecutorService::shutdownNow);
    }

}
```

```java
16:07:15.007 [main] DEBUG ShutdownTest - ========== shutdown =========
16:07:15.066 [main] DEBUG ShutdownTest - 执行关闭...
// 关闭后还是将队列中的任务执行完毕了
16:07:16.067 [pool-1-thread-4] DEBUG ShutdownTest - 执行任务：3
16:07:16.067 [pool-1-thread-5] DEBUG ShutdownTest - 执行任务：4
16:07:16.067 [pool-1-thread-1] DEBUG ShutdownTest - 执行任务：0
16:07:16.067 [pool-1-thread-2] DEBUG ShutdownTest - 执行任务：1
16:07:16.082 [pool-1-thread-3] DEBUG ShutdownTest - 执行任务：2
16:07:18.078 [main] DEBUG ShutdownTest - ========== shutdownNow =========
16:07:18.079 [main] DEBUG ShutdownTest - 执行关闭...
// shutdownNow 它调用了打断线程的方法，所以 sleep 被打断就会报错
Exception in thread "pool-2-thread-5" Exception in thread "pool-2-thread-1" Exception in thread "pool-2-thread-2" Exception in thread "pool-2-thread-4" Exception in thread "pool-2-thread-3" java.lang.RuntimeException: java.lang.InterruptedException: sleep interrupted
	at com.xiaozhi.utils.TimeUtil.sleep(TimeUtil.java:14)
	at com.xiaozhi.tool07.ShutdownTest.lambda$demo$0(ShutdownTest.java:23)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:750)
```



### 其他方法

```java
// 不在 RUNNING 状态的线程池，此方法就返回 true
boolean isShutdown();

// 线程池状态是否是 TERMINATED
boolean isTerminated();

// 调用 shutdown 后，由于调用线程并不会等待所有任务运行结束，因此如果它想在线程池 TERMINATED 后做些事情，可以利用此方法等待
boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
```

```java
public static void main(String[] args) {
    ExecutorService threadPool = Executors.newFixedThreadPool(5);
    threadPool.execute(() -> { System.out.println("aaa"); });
    threadPool.shutdown();
    System.out.println(threadPool.isShutdown());
    TimeUtil.sleep(2);
    System.out.println(threadPool.isTerminated());
}
```





### 任务调度线程池

#### Timer(不推荐)

在任务调度线程池功能加入之前，可以使用 java.util.Timer 来做任务调度，但是由于它是单线程调度的，因此所有的任务都是串行的，同一时间内只有一个任务可以被执行，**前一个任务的延迟或异常会影响后面的任务**

```java
public static void main(String[] args) {
    Timer timer = new Timer();
    log.debug("start...");
    TimerTask task1 = new TimerTask() {
        @Override
        public void run() {
            log.debug("task1");
            TimeUtil.sleep(1);
        }
    };
    TimerTask task2 = new TimerTask() {
        @Override
        public void run() {
            log.debug("task2");
        }
    };
    // 两个任务延迟1秒后执行
    timer.schedule(task1, 1000);
    timer.schedule(task2, 1000);
}
```

```java
17:13:52.239 [main] DEBUG ScheduledTasksTest - start...
17:13:53.251 [Timer-0] DEBUG ScheduledTasksTest - task1
17:13:54.264 [Timer-0] DEBUG ScheduledTasksTest - task2
```

我们设置的是程序在一秒后执行，task1 在一秒后输出了，但是 task2 是在启动后两秒才输出的，这就是因为 task1 睡眠了一秒影响了后面 task2 的执行



#### ScheduledExecutorService

它可以设置它的线程核心数，相比于固定大小的 Timer 更好，前面的任务不会影响后面任务的执行，即使前面出现异常也不会影响。但是如果核心线程数设置为1，那么它的效果和 Timer 一样。

```java
public static void main(String[] args) {
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
    log.debug("start...");
    service.schedule(() -> {
        log.debug("task1");
        TimeUtil.sleep(1);
    }, 1, TimeUnit.SECONDS);
    service.schedule(() -> {
        log.debug("task2");
    }, 1, TimeUnit.SECONDS);
}
```

```java
17:19:44.212 [main] DEBUG ScheduledTasksTest - start...
17:19:45.263 [pool-1-thread-2] DEBUG ScheduledTasksTest - task2
17:19:45.263 [pool-1-thread-1] DEBUG ScheduledTasksTest - task1
```

task1 和 task2 一同输出，因为有两个线程，所以可以并发处理任务



#### 定时任务线程池

**scheduleAtFixedRate**

```java
public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                              long initialDelay,
                                              long period,
                                              TimeUnit unit);
```

- Runnable command：Runnable 对象
- long initialDelay：隔多久时间触发
- long period：距离上次任务结束后多久触发一次
- TimeUnit unit：时间单位

**案例**：

```java
private static void method03() {
    ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    log.debug("start...");
    service.scheduleAtFixedRate(() -> {
        log.debug("running...");
    }, 1, 1, TimeUnit.SECONDS);
}
```

```java
19:37:26.692 [main] DEBUG ScheduledTasksTest - start...
19:37:27.748 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
19:37:28.749 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
19:37:29.740 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
```

我们让任务执行久一点，睡眠 2 s，再次启动测试一下

![image-20231113194209749](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231113194209749.png) 

```java
19:39:08.806 [main] DEBUG ScheduledTasksTest - start...
19:39:09.858 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
19:39:11.863 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
19:39:13.874 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
```

我们可以看到从原来的间隔 1s 变成了现在的间隔 2s，也就是说任务执行时间一旦超过了我们设定的时间，那么它就不会等待间隔时间，而是马上执行



**scheduleWithFixedDelay**

这个和 scheduleAtFixedRate 功能一样，但是它是会等够设定的间隔时间才会执行下一次任务

```java
public static void main(String[] args) {
    ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    log.debug("start...");
    service.scheduleWithFixedDelay(() -> {
        log.debug("running...");
        TimeUtil.sleep(2);
    }, 1, 1, TimeUnit.SECONDS);
}
```

```java
19:45:11.196 [main] DEBUG ScheduledTasksTest - start...
19:45:12.246 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
19:45:15.268 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
19:45:18.290 [pool-1-thread-1] DEBUG ScheduledTasksTest - running...
```

可以看到往下执行的每一次间隔了 3s，任务2s +  1s 间隔时间



#### 应用：每星期周四6:00执行任务

```java
/**
 * 每周四 6:00 执行任务
 */
private static void method5() {
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
    // 获取当前时间
    LocalDateTime now = LocalDateTime.now();
    // 设定开始时间
    LocalDateTime nextDate = now.with(DayOfWeek.THURSDAY).withHour(18).withMinute(0).withSecond(0).withNano(0);
    // 这里需要判断这周的星期四是否已经过去了
    nextDate = now.isBefore(nextDate) ? nextDate : nextDate.plusWeeks(1);
    System.out.println(nextDate);
    // 计算当前时间到开始时间的时间差
    long initialDelay = Duration.between(now, nextDate).toMillis();
    // 间隔时间
    long delayTime = 1000 * 60 * 60 * 24 * 7;
    log.debug("{} start...", new Date());
    service.scheduleWithFixedDelay(() -> {
        log.debug("{} running...", new Date());
    }, initialDelay, delayTime, TimeUnit.MILLISECONDS);
}
```

**测试**：

我们将时间设置为目标时间差一分钟的时候，看一下执行效果

![image-20231116175933579](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231116175933579.png) 

```java
17:59:14.016 [main] DEBUG ScheduledTasksTest - Thu Nov 16 17:59:14 GMT+08:00 2023 start...
18:00:00.085 [pool-1-thread-1] DEBUG ScheduledTasksTest - Thu Nov 16 18:00:00 GMT+08:00 2023 running...
```





### 创建多少个线程合适？

- 过小会导致程序不能充分地利用系统资源、容易导致饥饿
- 过大会导致更多的线程上下文切换，占用更多内存

**CPU** **密集型运算**

通常采用 cpu 核数 + 1 能够实现最优的 CPU 利用率，+1 是保证当线程由于页缺失故障（操作系统）或其它原因导致暂停时，额外的这个线程就能顶上去，保证 CPU 时钟周期不被浪费

**I/O** **密集型运算**

CPU 不总是处于繁忙状态，例如，当你执行业务计算时，这时候会使用 CPU 资源，但当你执行 I/O 操作时、远程RPC 调用时，包括进行数据库操作时，这时候 CPU 就闲下来了，你可以利用多线程提高它的利用率。

**经验公式如下**

线程数 = 核数 * 期望 CPU 利用率 * 总时间(CPU计算时间+等待时间) / CPU 计算时间

例如 4 核 CPU 计算时间是 50% ，其它等待时间是 50%，期望 cpu 被 100% 利用，套用公式

```java
4 * 100% * 100% / 50% = 8
```

例如 4 核 CPU 计算时间是 10% ，其它等待时间是 90%，期望 cpu 被 100% 利用，套用公式

```java
4 * 100% * 100% / 10% = 40
```





### Tomcat 线程池源码分析

Tomcat 在哪里用到了线程池？

- LimitLatch 用来限流，可以控制最大连接个数，类似 J.U.C 中的 Semaphore 后面再讲
- Acceptor 只负责【接收新的 socket 连接】
- Poller 只负责监听 socket channel 是否有【可读的 I/O 事件】
- 一旦可读，封装一个任务对象（socketProcessor），提交给 Executor 线程池处理
- Executor 线程池中的工作线程最终负责【处理请求】

Tomcat 的线程池扩展了 ThreadPoolExecutor

```java
public class ThreadPoolExecutor extends AbstractExecutorService {
```

它的行为和 JDK 的稍有不同，如果总线程数达到 maximumPoolSize 

- 这个时候不会立刻抛 RejectedExecutionException 异常，而是再次判断
- 再次尝试放入阻塞队列中，如果还是失败，那么它就会抛出异常

```java
@Deprecated
public void execute(Runnable command, long timeout, TimeUnit unit) {
    submittedCount.incrementAndGet();
    try {
        executeInternal(command);
    } catch (RejectedExecutionException rx) {
        if (getQueue() instanceof TaskQueue) {
            // If the Executor is close to maximum pool size, concurrent
            // calls to execute() may result (due to Tomcat's use of
            // TaskQueue) in some tasks being rejected rather than queued.
            // If this happens, add them to the queue.
            final TaskQueue queue = (TaskQueue) getQueue();
            try {
                if (!queue.force(command, timeout, unit)) {
                    submittedCount.decrementAndGet();
                    throw new RejectedExecutionException(sm.getString("threadPoolExecutor.queueFull"));
                }
            } catch (InterruptedException x) {
                submittedCount.decrementAndGet();
                throw new RejectedExecutionException(x);
            }
        } else {
            submittedCount.decrementAndGet();
            throw rx;
        }
    }
}
```





## 异步模式之 Worker Thread

### 定义

让有限的工作线程（Worker Thread）来轮流异步执行无限多的任务。也可以归类为分工模式，它的典型模式就是线程池，也体现了经典设计模式中的享元模式



### 饥饿现象

比如我们现在的业务是综合了多种类型任务的业务，在固定数量的线程池中，当所有的线程去执行同一种类型的任务，那么就会导致另外的业务没有线程来处理，这个僵持局面就是饥饿现象，我们看一个例子：

```java
@Slf4j(topic = "HungerPhenomenonTest")
public class HungerPhenomenonTest {

    static final List<String> MENU = Arrays.asList("猪脚饭", "木桶饭", "小鸡炖蘑菇");
    static final Random RANDOM = new Random();
    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            threadPool.execute(() -> {
                log.debug("处理点餐...");
                Future<?> future = threadPool.submit(() -> {
                    log.debug("做菜...");
                    return MENU.get(RANDOM.nextInt(MENU.size()));
                });
                try {
                    log.debug("上菜：{}", future.get());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
```

**说明**：

- 创建了 两个线程的线程池
- 执行两次任务，它执行的任务分为以下两部分
    - 前台：点餐和上菜
    - 厨房：做菜

**执行结果**：

![image-20231112233508354](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231112233508354.png) 

我们的程序在这里卡住了，因为线程池中一共就只有两个线程，我们的前台任务将这两个线程使用了，接下来到我们厨房线程的时候线程池中无线程可用就会导致我们的程序一直处于等待对方先释放的状态，所以程序僵持住了



### 解决饥饿问题

我们可以增加线程来解决这个问题，但是这不能解决根本问题。将同类型的任务交给不同的线程池来处理是比较合理的，使用分而治之的思想。

使用不同的线程池来改进我们的程序

```java
public static void main(String[] args) {
    // 前台线程池
    ExecutorService receptionPool = Executors.newFixedThreadPool(2);
    // 厨房线程池
    ExecutorService cookPool = Executors.newFixedThreadPool(2);
    for (int i = 0; i < 2; i++) {
        receptionPool.execute(() -> {
            log.debug("处理点餐...");
            Future<?> future = cookPool.submit(() -> {
                log.debug("做菜...");
                return MENU.get(RANDOM.nextInt(MENU.size()));
            });
            try {
                log.debug("上菜：{}", future.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

```java
23:44:09.833 [pool-1-thread-1] DEBUG HungerPhenomenonTest - 处理点餐...
23:44:09.833 [pool-1-thread-2] DEBUG HungerPhenomenonTest - 处理点餐...
23:44:09.837 [pool-2-thread-1] DEBUG HungerPhenomenonTest - 做菜...
23:44:09.837 [pool-2-thread-2] DEBUG HungerPhenomenonTest - 做菜...
23:44:09.837 [pool-1-thread-2] DEBUG HungerPhenomenonTest - 上菜：小鸡炖蘑菇
23:44:09.837 [pool-1-thread-1] DEBUG HungerPhenomenonTest - 上菜：猪脚饭
```

此时就没什么问题了





## Fork/Join

### 概述

Fork/Join 是 JDK 1.7 加入的新的线程池实现，体现的是分而治之的思想，将一个任务拆分成多个小任务给多个线程去执行，最后汇总结果，**适用于能够进行任务拆分的CPU密集型计算任务**



### 使用

创建 ForkJoinPool 对象

```java
// 创建CPU核心个数的核心线程
ForkJoinPool pool = new ForkJoinPool();
// 创建指定个数的核心线程
ForkJoinPool pool = new ForkJoinPool(2);
```

```java
// 执行方法
public <T> T invoke(ForkJoinTask<T> task)
```



### 案例

计算从 1 加到 n

```java
@Slf4j(topic = "ForkJoinPoolTest")
public class ForkJoinPoolTest {

    public static void main(String[] args) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        log.debug("结果输出：{}", forkJoinPool.invoke(new Task1(5)));
    }

}

@Slf4j(topic = "Task1")
class Task1 extends RecursiveTask<Integer> {
    private int n;
    public Task1(int n) {
        this.n = n;
    }
    @Override
    public String toString() {
        return "{" + n + "}";
    }
    @Override
    protected Integer compute() {
        // 程序出口
        if (n == 1) {
            log.debug("join() {}", n);
            return n;
        }
        // 将任务拆分
        Task1 task1 = new Task1(n - 1);
        task1.fork();
        log.debug("fork() {} + {}", n, task1);

        int result = n + task1.join();
        log.debug("join() {} + {} = {}", n, task1, result);
        return result;
    }
}
```

说明：fork() 的作用就是调用一个线程来执行 compute() 方法，我们通过构造方法来改变推出条件，知道我们程序到最后面满足条件然后返回结果，最后通过逐层返回得到最终结果，递归的思想

```java
00:21:06.973 [ForkJoinPool-1-worker-0] DEBUG Task1 - fork() 2 + {1}
00:21:06.973 [ForkJoinPool-1-worker-1] DEBUG Task1 - fork() 5 + {4}
00:21:06.973 [ForkJoinPool-1-worker-3] DEBUG Task1 - fork() 3 + {2}
00:21:06.973 [ForkJoinPool-1-worker-2] DEBUG Task1 - fork() 4 + {3}
00:21:06.979 [ForkJoinPool-1-worker-0] DEBUG Task1 - join() 1
00:21:06.980 [ForkJoinPool-1-worker-0] DEBUG Task1 - join() 2 + {1} = 3
00:21:06.980 [ForkJoinPool-1-worker-3] DEBUG Task1 - join() 3 + {2} = 6
00:21:06.980 [ForkJoinPool-1-worker-2] DEBUG Task1 - join() 4 + {3} = 10
00:21:06.980 [ForkJoinPool-1-worker-1] DEBUG Task1 - join() 5 + {4} = 15
00:21:06.980 [main] DEBUG ForkJoinPoolTest - 结果输出：15
```



### 案例改进

我们之前的案例有弊端，结果需要等待，我们可以将他们各自拆分并行执行，最后将结果汇总

这里我们使用二分法，每次拆一半，不进行全部拆分

```java
@Slf4j(topic = "Task2")
class Task2 extends RecursiveTask<Integer> {
    private int begin;
    private int end;
    public Task2(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }
    @Override
    public String toString() {
        return "{" + begin + "," + end + '}';
    }
    @Override
    protected Integer compute() {
        if (begin == end) {
            log.debug("join() {}", begin);
            return begin;
        }
        if (end - begin == 1) {
            log.debug("join() {} + {} = {}", begin, end, begin + end);
            return begin + end;
        }
        int mid = (end + begin) / 2;
        Task2 t1 = new Task2(begin, mid);
        t1.fork();
        Task2 t2 = new Task2(mid + 1, end);
        t2.fork();
        log.debug("fork() {} + {} = ?", t1, t2);

        int result = t1.join() + t2.join();
        log.debug("join() {} + {} = {}", t1, t2, result);
        return result;
    }
}
```

```java
00:47:20.278 [ForkJoinPool-1-worker-2] DEBUG Task2 - fork() {1,2} + {3,3} = ?
00:47:20.278 [ForkJoinPool-1-worker-3] DEBUG Task2 - join() 4 + 5 = 9
00:47:20.278 [ForkJoinPool-1-worker-1] DEBUG Task2 - fork() {1,3} + {4,5} = ?
00:47:20.278 [ForkJoinPool-1-worker-0] DEBUG Task2 - join() 1 + 2 = 3
00:47:20.282 [ForkJoinPool-1-worker-3] DEBUG Task2 - join() 3
00:47:20.282 [ForkJoinPool-1-worker-2] DEBUG Task2 - join() {1,2} + {3,3} = 6
00:47:20.282 [ForkJoinPool-1-worker-1] DEBUG Task2 - join() {1,3} + {4,5} = 15
15
```







# 八、工具之JUC

## AQS原理

### 概述

AQS的全称是 AbstractQueuedSynchronizer，是阻塞式锁和相关的同步器工具的框架，JUC类的底层很多都用到它

特点：

- 用 state 属性来表示资源的状态（分独占模式和共享模式），子类需要定义如何维护这个状态，控制如何获取锁和释放锁
    - getState：获取 state 状态
    - setState：设置 state 状态
    - compareAndSetState：cas机制设置 state
    - 独占模式只能有一个线程可以访问资源，共享模式可以多个线程访问资源
- 提供了基于 FIFO(先进先出) 的等待队列
- 条件变量实现等待、唤醒机制，拥有多个条件变量

子类需要实现的方法

- tryAcquire：尝试获取锁
- tryRelease：尝试释放锁
- tryAcquireShared：尝试获取共享锁
- tryReleaseShared：尝试释放共享锁
- isHeldExclusively：是否是独占锁



### AQS 中 Node 的状态

AQS的中 Node 的四大状态

```java
/** waitStatus value to indicate thread has cancelled */
static final int CANCELLED =  1;
/** waitStatus value to indicate successor's thread needs unparking */
static final int SIGNAL    = -1;
/** waitStatus value to indicate thread is waiting on condition */
static final int CONDITION = -2;
/**
 * waitStatus value to indicate the next acquireShared should
 * unconditionally propagate
 */
static final int PROPAGATE = -3;
```

这段代码定义了一些常量值，用于表示节点的等待状态。具体含义如下：  

- **CANCELLED**(1)：表示当前结点已取消调度。当timeout或被中断（响应中断的情况下），会触发变更为此状态，进入该状态后的结点将不会再变化。
- **SIGNAL**(-1)：表示后继结点在等待当前结点唤醒。后继结点入队时，会将前继结点的状态更新为SIGNAL。
- **CONDITION**(-2)：表示结点等待在Condition上，当其他线程调用了Condition的signal()方法后，CONDITION状态的结点将**从等待队列转移到同步队列中**，等待获取同步锁。
- **PROPAGATE**(-3)：共享模式下，前继结点不仅会唤醒其后继结点，同时也可能会唤醒后继的后继结点。
- **0**：新结点入队时的默认状态。

这些常量值用于在节点获取过程中判断前驱节点的状态，以便进行相应的处理。

例如，如果前驱节点的等待状态为SIGNAL，说明后继节点可以安全地继续执行，因此可以返回true。如果前驱节点的等待状态为CANCELLED，则需要跳过前驱节点并表示重试。如果等待状态为CONDITION，则需要等待条件满足后再继续执行。如果等待状态为PROPAGATE，则需要将此节点设置为可传播状态，以便后续的操作可以无条件传播。说白了就是通过这几个状态来标记当前节点线程的所处的状态

> 备注：后面我们看源码的时候会用到，在这里提前预习一下



### 自定义不可重入锁

#### 自定义同步器

```java
class MySync extends AbstractQueuedSynchronizer {

    // 尝试加锁
    @Override
    protected boolean tryAcquire(int arg) {
        if (arg == 1) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
        }
        return false;
    }
    // 尝试释放锁
    @Override
    protected boolean tryRelease(int arg) {
        if(arg == 1) {
            if(getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
        return false;
    }
    // 是否是独占锁
    @Override
    protected boolean isHeldExclusively() {
        return getState() == 1;
    }

    public Condition newCondition() {
        return new ConditionObject();
    }
}
```



#### 自定义锁（不可重入）

使用我们自定义的同步器来实现自定义锁

```java
@Slf4j(topic = "MyLock")    // 自定义锁（不可重入）
class MyLock implements Lock {
    static final MySync SYNC = new MySync();

    // 尝试，不成功进入等待队列
    @Override
    public void lock() {
        SYNC.acquire(1);
    }

    // 可打断锁
    @Override
    public void lockInterruptibly() throws InterruptedException {
        SYNC.acquireInterruptibly(1);
    }
	
    @Override
    public boolean tryLock() {
        return SYNC.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return SYNC.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        SYNC.release(1);
    }

    @Override
    public Condition newCondition() {
        return SYNC.newCondition();
    }
}
```



#### 测试

创建两个线程分别是 t1 和 t2，t1 执行久一点，观察他们两个线程的输出

```java
public static void main(String[] args) {
    MyLock myLock = new MyLock();
    new Thread(() -> {
        myLock.lock();
        try {
            log.debug("running...");
            TimeUtil.sleep(1);
        } finally {
            log.debug("unlock...");
            myLock.unlock();
        }
    }, "t1").start();
    new Thread(() -> {
        myLock.lock();
        try {
            log.debug("running...");
        } finally {
            log.debug("unlock...");
            myLock.unlock();
        }
    }, "t2").start();
}
```

```java
14:42:45.723 [t1] DEBUG AQSTest - running...
14:42:46.737 [t1] DEBUG AQSTest - unlock...
14:42:46.737 [t2] DEBUG AQSTest - running...
14:42:46.737 [t2] DEBUG AQSTest - unlock...
```

t1 先获取锁，所以是 t1 先执行，因为是独享锁，所以 t1 执行完成之后才到 t2





## ReentranLock原理

ReentranLock 的使用在前面我们已经讲过，忘记了可以回顾一下...

下图是 ReentranLock 的类图

![image-20231114155705085](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231114155705085.png)



### 非公平锁原理

#### 加锁解锁流程分析

ReentranLock 的默认实现是非公平锁

```java
public ReentrantLock() {
    sync = new NonfairSync();
}
```

没有线程竞争时

![image-20231115221153929](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115221153929.png) 

第一个竞争出现时

![image-20231115221210194](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115221210194.png) 

Thread-1 执行

1. CAS 尝试将 state 改为1，结果失败，Owner 已经存在了，此时 state 为 1
2. 再次进入 tryAcquire 逻辑，如果 Thread-0 还未释放锁，那么还会失败
3. 接下来进入 addWaiter 逻辑，构建 Node 阻塞队列
    - 下图中黄色三角表示该 Node 的 waitState 状态，其中 0 为默认正常状态
    - Node 的创建是懒惰的，它会先判断是否已经存在元素了，没有存在它就会创建一个
    - 其中第一个 Node 称为 Dummy（哑元）或哨兵，并不关联线程，它负责唤醒后面的线程

![image-20231115221557157](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115221557157.png) 

当线程进入 acquireQueued 逻辑

1. acquireQueued 会在一个死循环这on不断尝试获取锁，失败后进入 park阻塞

2. 如果当前节点的前一个节点是 head，那么再次 tryAcquire 尝试获取锁，如果此时 state 仍为 1，那么失败

3. 进入 shourldParkAfterFailedAcquire 逻辑，将前驱 node，即 head 的 waitStatus 改为 *SIGNAL*(-1)，这次返回 false

    ![image-20231115221846046](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115221846046.png)

4. 进入 parkAndCheckInterrupt，Thread01 park （灰色表示）

    ![image-20231115222045392](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115222045392.png) 

再次有多个线程经历上述过程竞争失败，就会变成下图：

![image-20231115222152656](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115222152656.png)

Thread-0 释放锁，进入 tryRelease 流程，如果成功

- 设置 exclusiveOwnerThread 为 null
- 设置 state 为 0

![image-20231115235342718](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115235342718.png)

当前队列不为 null，并且 head 的waitStatus=-1，进入 unparkSucccesor 流程

- 设置 waitStatus = 0

-  获取头节点

- 如果头节点不为空，那么将头节点的第二个节点唤醒

    ![image-20231115235858908](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231115235858908.png)

    Thread-1 加锁成功，执行加锁的步骤，然后刚刚的 head 会被清除 Thraed

    如果这个时候有其他的线程来竞争（非公平锁的体现），例如 Thread-4 来了

    ![image-20231116000127647](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231116000127647.png)

    如果被 Thread-4 先抢到锁，那么Thread-1再次进入 acquireQueued 流程，重新进入 park 阻塞

- 如果头节点为空 或者 waitStatus > 0，则从尾节点开始遍历获取 waitStatus <= 0 的节点作为新节点，然后将其唤醒



#### 加锁源码

我们看一下调用 lock 方法它做了什么

```java
public void lock() {
    sync.lock();
}
```

可以看到是调用的 NonfairSync#lock()

```java
@ReservedStackAccess
final void lock() {
    if (compareAndSetState(0, 1))
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);
}
```

主要实现：

1. 修改 state 为 1，如果修改成功就将当前线程设置为 Owner

2. acquire(1)

    ```java
    @ReservedStackAccess
    public final void acquire(int arg) {
        if (!tryAcquire(arg) // 尝试获取锁，如果获取失败就返回 fasle，因为 !false=true，所以就会执行下一个判断
    		// Node.EXCLUSIVE 独占锁模式
            && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
    ```

3. 首先它会执行 tryAcquire(arg) 

    ```java
    // 这个是非公平锁的实现
    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }
    ```

    ```java
     @ReservedStackAccess
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        // 还没有线程获取到锁
        if (c == 0) {
            // 尝试 cas 获取锁，这里体现的是非公平性；不去检查 AQS 队列
            if (compareAndSetState(0, acquires)) {
                // cas 成功，将 owner 设置为当前线程表示当前线程获取到锁
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        // 判断当前线程是否已经获取到锁了，如果是就表示发生了锁重入
        else if (current == getExclusiveOwnerThread()) {
            // 锁重入就添加次数
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            // 设置 state 为重入的次数
            setState(nextc);
            return true;
        }
        // 获取锁失败
        return false;
    }
    ```

4. 尝试获取锁失败就会进入 acquireQueued(addWaiter(Node.EXCLUSIVE), arg) 流程

5. 首先我们先进入 addWaiter(Node.EXCLUSIVE) 中

    ```java
    private Node addWaiter(Node mode) {
        // 创建一个 Node 节点，模式是独占模式		Node.EXCLUSIVE
        Node node = new Node(Thread.currentThread(), mode);
        // 尾节点
        Node pred = tail;
        // 尾节点不为空
        if (pred != null) {
            // 新进入的 node 前指针指向尾节点（因为它要成为新的尾节点） 
            node.prev = pred;
            // 这边执行 cas 操作将 node 置为新的尾节点
            if (compareAndSetTail(pred, node)) {
                // 上一个尾节点的 next 指针指向新加入的节点
                pred.next = node;
                return node;
            }
        }
        // 尾节点为空的情况执行 enq(node) 方法
        enq(node);
        return node;
    }
    ```

    假设我们现在还没有线程加入，队列的 head 和 tail 都为空，那么它就会走 enq(node) 方法

    ```java
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            // 这个判断主要是为了将哑元节点创建
            if (t == null) { // Must initialize
                // 尾节点为空就创建一个空节点为头节点和尾节点
                // 这个第一个的Node 称为 Dummy（哑元）或哨兵，并不关联线程，它来唤醒后面的线程
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                // 走到这里说明头节点已经被创建了
                // prev 指针指向尾节点
                node.prev = t;
                // node 成为新的 tail 节点
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }
    ```

6. 加入队列之后就会执行 acquireQueued 方法

    ```java
    final boolean acquireQueued(final Node node, int arg) {
        // 失败标记
        boolean failed = true;
        try {
            // 打断标记
            boolean interrupted = false;
            for (;;) {
                // 获取前一个节点
                final Node p = node.predecessor();
                // 如果前一个节点为头节点 和 再次尝试获取锁，tryAcquire 的逻辑上面讲过
                if (p == head && tryAcquire(arg)) {
                    // 设置当前节点为头节点
                    setHead(node);
                    // 之前的头节点不要了
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                // 如果前一个不是头节点，那么就会执行下面的逻辑（下面会讲）
      		   // 获取锁失败的锁应该 park 
                if (shouldParkAfterFailedAcquire(p, node) &&
                    // park 和 打断执行
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
    ```

7. 我们看一下 shouldParkAfterFailedAcquire(p, node) 和 做了什么 parkAndCheckInterrupt()

    ```java
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // 获取上一个节点的状态
        int ws = pred.waitStatus;
        // Node.SIGNAL 表示请求释放通知，也就阻塞了
        if (ws == Node.SIGNAL)
            // 如果前一个节点阻塞了，那么当前节点也阻塞好了
            return true;
        // > 0 表示取消状态
        if (ws > 0) {
            // 移除被打断的节点
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // 设置新加入的节点的前节点等待状态为 SIGNAL
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
    ```
    
    ```java
    private final boolean parkAndCheckInterrupt() {
        // 让当前线程暂停
        LockSupport.park(this);
        // 打断线程
        return Thread.interrupted();
    }
    ```



#### 释放锁源码

```java
public void unlock() {
    sync.release(1);
}
```

```java
@ReservedStackAccess
public final boolean release(int arg) {
    // 尝试释放锁
    if (tryRelease(arg)) {
        // 获取头节点
        Node h = head;
        // 头节点不为空 && 状态不等于0
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

tryRelease(arg) 的代码如下：

```java
@ReservedStackAccess
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    // c = 0 说明解锁成功 
    if (c == 0) {
        // 自由标识
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

返回 true 表示释放锁成功，接下来 头节点不为空且waitStatus != 0 就执行下一步 `unparkSuccessor(h);`

```java
private void unparkSuccessor(Node node) {
    // 当前 node 为 head 节点
    int ws = node.waitStatus;
    if (ws < 0)
        // 设置 state 为 0
        compareAndSetWaitStatus(node, ws, 0);
    // 获取第二个节点
    Node s = node.next;
    // 如果第二个节点为空 或者 节点的状态为 CANCELLED(1)，表示当前结点已取消调度（timeout或被打断）
    if (s == null || s.waitStatus > 0) {
        s = null;
        // 从尾节点开始遍历寻找不为 CANCELLED(1) 状态的节点
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        // 线程恢复运行
        LockSupport.unpark(s.thread);
}
```





### 可重入锁原理

每次加锁 state 就会 +1，解锁的时候就 -1，最后 state = 0 就表示锁被释放了

**我们看一下源码如何实现**

加锁

```java
// 调用顺序：lock -> acquire -> tryAcquire -> nonfairTryAcquire
@ReservedStackAccess
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    // 省略......
    // 判断当前线程是否已经获取到锁了，如果是就表示发生了锁重入
    else if (current == getExclusiveOwnerThread()) {
        // 锁重入就添加次数
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        // 设置 state 为重入的次数
        setState(nextc);
        return true;
    }
    // 获取锁失败
    return false;
}
```

释放锁

```java
// 调用顺序：unlock -> release -> tryRelease
@ReservedStackAccess
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    // c = 0 说明解锁成功 
    if (c == 0) {
        // 自由标识
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```





### 可打断原理

#### 不可打断模式

在此模式下，即使它被打断了，任然会驻留在 AQS 队列中，等待获取到锁的时候才会知道被打断了

```java
// lock -> acquire -> tryAcquire -> acquireQueued
// 目前是竞争失败，将 Node 放入到同步队列中
@ReservedStackAccess
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        // 打断标记
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                // 获取锁后才会将打断标记返回
                return interrupted;
            }
            // 设置前一个节点的 waitStatus 
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                // 这是因为 Thread.interrupted() 方法会清除标记，为了给下次循环便利的时候返回打断标记
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

```java
private final boolean parkAndCheckInterrupt() {
    // park 被打断就会往下执行
    // 注意：如果打断标记为 true，那么 park 就会失效，也就是说下次再进来就无效了
    LockSupport.park(this);
    // 返回打断标记
    return Thread.interrupted();
}
```

如果 acquireQueued 被打断返回为 true，那么：

```java
@ReservedStackAccess
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&		// true
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))	// 若返回 true
        // 执行下面方法
        selfInterrupt();
}
```

```java
static void selfInterrupt() {
    // 调用方法打断线程运行
    Thread.currentThread().interrupt();
}
```



#### 可打断模式

使用 lockInterruptibly() 的时候才是可打断模式

```java
public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
}
```

```java
public final void acquireInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        // 如果打断了，那就直接抛出错误
        throw new InterruptedException();
    // 尝试获取锁
    if (!tryAcquire(arg))
        doAcquireInterruptibly(arg);
}
```

```java
private void doAcquireInterruptibly(int arg)
    throws InterruptedException {
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                // 它这里和不可打断锁的区别就是它直接就抛出异常结束循环了
                throw new InterruptedException();
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```



### 公平锁实现原理

它比非公平锁多一个步骤就是检查 AQS队列 是否有元素，如果存在那么就 AQS队列中的优先获取锁

```java
// fair 为 true 就是使用公平锁
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

看一下它的 lock() 方法实现

```java
final void lock() {
    acquire(1);
}
```

```java
@ReservedStackAccess
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

acquire 方法和公平锁的实现一样的逻辑，但是 tryAcquire方法中存在不一样的逻辑

![image-20231116173218833](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231116173218833.png) 

首先它会先执行 hasQueuedPredecessors()，如果它为true表示有前驱节点，那么不会往下执行，如果没有前驱节点，就会执行是当前线程获取锁

```java
public final boolean hasQueuedPredecessors() {
    Node t = tail;
    Node h = head;
    Node s;
    return h != t 	// 第一个是哑元节点，h != t 表示存在 第二个元素
        // 第二个不为空才会执行
        && ((s = h.next) == null
        // 第二个节点不为空个而且不是当前线程，因为是当前线程的话就会返回 true 直接获取锁了
        || s.thread != Thread.currentThread());
}
```



### 条件变量

使用条件变量我们会创建一个条件变量

```java
public Condition newCondition() {
    return sync.newCondition();
}
final ConditionObject newCondition() {
    return new ConditionObject();
}
```

我们看一下 ConditionObject 类做了什么

```java
public class ConditionObject implements Condition, java.io.Serializable {
    private static final long serialVersionUID = 1173984872572414699L;
    private transient Node firstWaiter;
    private transient Node lastWaiter;
```

通过它的类属性我们可以看出来它底层也是维护了一个链表，它会将不满足条件变量的线程放入到链表中

每个条件变量对应着一个 ConditionObject 等待队列



#### await流程

当 Thread-0 调用了 await 方法就会将 Thread-0 放入到 ConditionObject 队列中，节点的 waitStatus 为Node.CONDITION(-2)

![image-20231116174909173](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231116174909173.png) 

加入队列过程：

1. 循环便利循环释放队列中已取消的节点
2. 设置当前节点，状态为 CONDITION
3. 队列中如果不存在节点，那么新加入的节点作为头节点，负责成为尾节点

释放当前节点持有的所有锁（锁重入情况）

![image-20231120163025942](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231120163025942.png) 

判断是否在 AQS队列（同步队列）中，从尾部开始找，如果不在那么就将当前线程 park

![image-20231120163203714](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231120163203714.png)



#### await 源码解读

```java
public final void await() throws InterruptedException {
    // 判断当前线程是否被打断了
    if (Thread.interrupted())
        throw new InterruptedException();
    // 将当前线程 添加到等待队列
    Node node = addConditionWaiter();
    // 释放节点持有的所有锁（考虑锁重入情况）
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    // 是否在同步队列中，从尾开始找
    while (!isOnSyncQueue(node)) {
        // 不在就 park
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

addConditionWaiter() 方法

```java
 private Node addConditionWaiter() {
    // 最后一个节点
    Node t = lastWaiter;
	// 所有已取消的 Node （调用了 signal 或 被打断了）从队列列表中删除
    if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters();
        t = lastWaiter;
    }
    // 设置当前节点，状态为 CONDITION
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    // 此时还没有元素，则新节点为 firstWaiter
    if (t == null)
        firstWaiter = node;
    else
        // 上面已经将节点便利到了最后一个节点，所以接入到它的下一个节点
        t.nextWaiter = node;
    // 尾指针指向当前节点
    lastWaiter = node;
    return node;
}
```

```java
private void unlinkCancelledWaiters() {
    Node t = firstWaiter;
    Node trail = null;
    // 头节点不为空
    while (t != null) {
        // 下一个 nextWaiter
        Node next = t.nextWaiter;
        // 如果状态不是 CONDITION，此时的头要被移除
        if (t.waitStatus != Node.CONDITION) {
            // 当前头节点的下个节点为空
            t.nextWaiter = null;
            // 此时 trail 为 null
            if (trail == null)
                // 之前的头节点的下一个节点称为头节点
                firstWaiter = next;
            else
                // 下次循环进来 trail 不为 null
                trail.nextWaiter = next;
            if (next == null)
                lastWaiter = trail;
        }
        else
            trail = t;
        t = next;
    }
}
```

fullyRelease 方法

```java
final int fullyRelease(Node node) {
    boolean failed = true;
    try {
        // 获取 state
        int savedState = getState();
        // release 会将所有的锁释放（考虑锁重入）
        if (release(savedState)) {
            failed = false;
            return savedState;
        } else {
            // 释放锁失败抛出异常
            throw new IllegalMonitorStateException();
        }
    } finally {
        if (failed)
            // CANCELLED(-2) 节点为取消调用状态
            node.waitStatus = Node.CANCELLED;
    }
}
```

```java
@ReservedStackAccess
public final boolean release(int arg) {
    // 尝试释放锁
    if (tryRelease(arg)) {
        Node h = head;
        // 头节点不为空且状态不是刚加入时候的
        if (h != null && h.waitStatus != 0)
            // 释放锁完成之后第二个节点恢复运行
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```



#### signal 流程

假设 Thread-1 要来唤醒  Thread-0

![image-20231120165028651](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231120165028651.png)

进入 doSignal 流程，取得等待队列中的第一个 Node，即 Thread-0

![image-20231120165201712](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231120165201712.png) 

执行 transferForSignal 流程：

1. waitStatus 修改：Node.CONDITION -> 0
2. 节点加入到 AQS 队列尾部
3. 如果前一个节点waitStatus 小于或等于0，则尝试将当前节点前一个节点的 waitStatus 设置为 Node.SIGNAL
    - 失败：说明此时前一个节点被别的节点抢先修改了，即锁被释放了，案例这里是 Thraed-1，接着就不用修改了，Thread-0 直接上位成为 ExclusiveOwnerThread
    - 成功：状态修改成功，成功加入 AQS 队列

![image-20231120165209374](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20231120165209374.png)



#### signal 源码解读

```java
public final void signal() {
    // 判断是否拥有锁，这里只有锁才能进来，所以不需要加锁
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    // 获取第一个节点
    Node first = firstWaiter;
    if (first != null)
        // 恢复节点
        doSignal(first);
}
```

```java
private void doSignal(Node first) {
    do {
        // 只有一个头节点，就将 lastWaiter
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        // 将要释放的节点断开
        first.nextWaiter = null;
        // 循环判断信号转换是否成功，成功就结束循环
    } while (!transferForSignal(first) 
             // 不成功就继续获取头节点来释放
             && (first = firstWaiter) != null);
}
```

transferForSignal(first) 源码如下：

```java
final boolean transferForSignal(Node node) {
    // waitStatus 转换 Node.CONDITION -> 0
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;
    // 加入到 AQS队列 尾部
    Node p = enq(node);
    int ws = p.waitStatus;
    // waitStatus 大于0
    if (ws > 0 
        // 将当前节点的前一个节点 waitStatus 设置为 SIGNAL
        || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        // waitStatus 设置失败说明有节点加入了 AQS队列中，ExclusiveOwnerThread 此时为空，那么可以让当前线程 unpark
        LockSupport.unpark(node.thread);
    return true;
}
```





## 读写锁(ReentranReadWriteLock)

### 概述

它和 ReentranLock 有什么区别呢？

ReentranLock 不管是读还是写都是使用的互斥锁来保证线程安全，而 ReentranReadWriteLock 更加细粒度化一点，读写操作分别上锁，对于读操作我们可以让线程并发执行，前提是写操作没有执行，当写操作执行的时只能有一个线程能访问共享资源。可以理解为是 `读-读` 并发，`写-写` 和 `读-写` 互斥

ReentranReadWriteLock 适合读多写少的场景下

**注意事项**

- 读锁不支持条件变量
- 重入时升级不支持：即持有读锁的情况下去获取写锁，会导致获取写锁永久等待
- 重入锁降级支持：即持有写锁的情况下获取读锁

 

### 使用

```java
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
ReentrantReadWriteLock.ReadLock r = rw.readLock();		// 读锁
ReentrantReadWriteLock.WriteLock w = rw.writeLock();	// 写锁
```

使用方法和 ReentranLock 一样

**案例**

```java
@Slf4j(topic = "DataContainer")
class DataContainer {
    private ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock r = rw.readLock();
    private ReentrantReadWriteLock.WriteLock w = rw.writeLock();

    public void read() {
        log.debug("获取读锁...");
        r.lock();
        try {
            log.debug("{} 读取...", Thread.currentThread().getName());
            TimeUtil.sleep(1);
        } finally {
            log.debug("释放读锁...");
            r.unlock();
        }
    }
    public void write() {
        log.debug("获取写锁...");
        w.lock();
        try {
            log.debug("{} 写入...", Thread.currentThread().getName());
            TimeUtil.sleep(1);
        } finally {
            log.debug("释放写锁...");
            w.unlock();
        }
    }
}
```

`读-读` 并发

```java
public static void main(String[] args) {
    DataContainer dataContainer = new DataContainer();
    new Thread(dataContainer::read, "t2").start();
    new Thread(dataContainer::read, "t1").start();
}
```

```java
15:08:32.542 [t1] DEBUG DataContainer - 获取读锁...
15:08:32.542 [t2] DEBUG DataContainer - 获取读锁...
15:08:32.546 [t2] DEBUG DataContainer - t2 读取...
15:08:32.545 [t1] DEBUG DataContainer - t1 读取...
15:08:33.553 [t1] DEBUG DataContainer - 释放读锁...
15:08:33.553 [t2] DEBUG DataContainer - 释放读锁...
```

`读-写` 或 `写-写` 互斥

```java
public static void main(String[] args) {
    DataContainer dataContainer = new DataContainer();
    new Thread(dataContainer::read, "t2").start();
    new Thread(dataContainer::write, "t1").start();
}
```

```java
15:10:28.055 [t1] DEBUG DataContainer - 获取写锁...
15:10:28.055 [t2] DEBUG DataContainer - 获取读锁...
15:10:28.058 [t1] DEBUG DataContainer - t1 写入...
15:10:29.069 [t1] DEBUG DataContainer - 释放写锁...
15:10:29.069 [t2] DEBUG DataContainer - t2 读取...
15:10:30.083 [t2] DEBUG DataContainer - 释放读锁...
```

可以看到获取写锁后是先写入完成然后再读取。



### 读写锁原理

#### 流程分析

#### 源码分析





## Semaphore

### 概述

信号量，限制访问资源的线程数量上限

举个例子：可以把它理解成是停车场，线程表示车，当停车场满了，车就不能进来了。当共享资源的线程达到设定的最大值，那么其他线程就只能阻塞等待活跃的线程执行完毕

> **注意**
>
> - StampedLock 不支持条件变量
> - StampedLock 不支持可重入



### 使用

```java
public static void main(String[] args) {
    ExecutorService threadPool = Executors.newFixedThreadPool(3);
    Semaphore semaphore = new Semaphore(3);
    for (int i = 0; i < 10; i++) {
        threadPool.submit(() -> {
            try {
                // 获取许可
                semaphore.acquire();
                log.debug("running...");
                sleep(1);
                log.debug("end...");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                semaphore.release();
            }
        });
    }
}
```

```java
20:58:04.375 [pool-1-thread-2] DEBUG SemaphoreTest - running...
20:58:04.375 [pool-1-thread-1] DEBUG SemaphoreTest - running...
20:58:04.375 [pool-1-thread-3] DEBUG SemaphoreTest - running...
20:58:05.394 [pool-1-thread-1] DEBUG SemaphoreTest - end...
20:58:05.394 [pool-1-thread-3] DEBUG SemaphoreTest - end...
20:58:05.394 [pool-1-thread-2] DEBUG SemaphoreTest - end...
20:58:05.394 [pool-1-thread-1] DEBUG SemaphoreTest - running...
20:58:05.394 [pool-1-thread-3] DEBUG SemaphoreTest - running...
20:58:05.394 [pool-1-thread-2] DEBUG SemaphoreTest - running...
20:58:06.407 [pool-1-thread-2] DEBUG SemaphoreTest - end...
20:58:06.407 [pool-1-thread-1] DEBUG SemaphoreTest - end...
20:58:06.407 [pool-1-thread-3] DEBUG SemaphoreTest - end...
20:58:06.407 [pool-1-thread-2] DEBUG SemaphoreTest - running...
20:58:06.407 [pool-1-thread-1] DEBUG SemaphoreTest - running...
20:58:06.407 [pool-1-thread-3] DEBUG SemaphoreTest - running...
20:58:07.420 [pool-1-thread-1] DEBUG SemaphoreTest - end...
20:58:07.420 [pool-1-thread-2] DEBUG SemaphoreTest - end...
20:58:07.420 [pool-1-thread-1] DEBUG SemaphoreTest - running...
20:58:07.420 [pool-1-thread-3] DEBUG SemaphoreTest - end...
20:58:08.431 [pool-1-thread-1] DEBUG SemaphoreTest - end...
```



### 应用 - 连接池 

修改我们之前的连接池

```java
public MockConnection getConnection() {
    // 遍历判断是否有空闲的连接
    try {
        // 获取许可
        semaphore.acquire();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    for (int i = 0; i < size; i++) {
        if (states.get(i) == 0 && states.compareAndSet(i, 0, 1)) {
            return connections[i];
        }
    }
    return null;
}

// 归还连接
public void free(Connection conn) {
    for (int i = 0; i < connections.length; i++) {
        if (conn == connections[i]) {
            // 设置成空闲状态，它不用进行 cas 因为它是此时是获取锁的，所以没有其他线程竞争
            states.set(i, 0);
            // 释放许可
            semaphore.release();
        }
    }
}
```



### 原理





## CountdownLatch

### 概述

用来进行线程同步协作，通过减少次数来使线程停止等待往下执行

举个例子：三人开车去郊游，小明和小红上厕所了，我们需要满人才能发车，此时小路在车上，未到人数-1，未到人数为 2，接着两人回来，未到人数为0，此时我们可以出发了。

通过这个例子我们可以知道 小明、小红、小路 就是我们的线程，我们可以通过最终数是否为 0 的方式来等待三个线程的执行，当三个线程完成了减数操作，那么我们就可以停止等待继续执行了

> TIP：我们可以使用 join，wait 等 方法来完成，但是这些都是偏底层的而且使用起来比较繁琐，推荐使用 JUC 并发包提供的类



### 使用

```java
// count 总计数
public CountDownLatch(int count)
```

使用方法

- await()：等待
- countDown：总计数 - 1

**注意**：

- 如果使用线程池，线程数量要和设定的数一样，这样才可以防止执行顺序错误。
- CountDownLatch 不能重复使用，它只能使用一次，需要重新创建



**实例**

```java
@Slf4j(topic = "CountdownLatchTest")
public class CountdownLatchTest {

    public static void main(String[] args) {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        new Thread(() -> {
            String name = Thread.currentThread().getName();
            log.debug("{} being...", name);
            sleep(1);
            countDownLatch.countDown();
            log.debug("{} end...{}", name, countDownLatch.getCount());
        }, "t1").start();
        new Thread(() -> {
            String name = Thread.currentThread().getName();
            log.debug("{} being...", name);
            sleep(2);
            countDownLatch.countDown();
            log.debug("{} end...{}", name, countDownLatch.getCount());
        }, "t2").start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.debug("结束...");
    }
}
```

```java
01:18:07.786 [t2] DEBUG CountdownLatchTest - t2 being...
01:18:07.786 [t1] DEBUG CountdownLatchTest - t1 being...
01:18:08.796 [t1] DEBUG CountdownLatchTest - t1 end...1
01:18:09.799 [t2] DEBUG CountdownLatchTest - t2 end...0
01:18:09.799 [main] DEBUG CountdownLatchTest - 结束...
```



### 应用

#### 同步等待多线程准备完毕

10 个线程分别加载游戏资源，这里我们模拟使用随机睡眠代替，都加载完毕，则游戏可以开始

```java
/**
 * @author xiaozhi
 *
 * 多线程加载游戏
 */
public class MultiThreadLoadingOfGame {
    public static void main(String[] args) {
        AtomicInteger num = new AtomicInteger(0);
        ExecutorService threadPool = Executors.newFixedThreadPool(10, (r) -> new Thread( r, "t" + num.getAndIncrement()));
        String[] schedule = new String[10];
        Random random = new Random();
        CountDownLatch countDownLatch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            int k = i;
            threadPool.submit(() -> {
                for (int j = 1; j <= 100; j++) {
                    TimeUtil.sleepMillisecond(random.nextInt(100));
                    schedule[k] = Thread.currentThread().getName() + "(" + j + "%)";
                    // 不换行 + r（回车符） 实现进度条效果
                    System.out.print("\r" + Arrays.toString(schedule));
                }
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
            System.out.println("\n游戏加载完成...");
            threadPool.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
```

```java
// 中间执行
[t0(37%), t1(39%), t2(37%), t3(44%), t4(42%), t5(45%), t6(45%), t7(48%), t8(49%), t9(39%)]
// 最后加载完毕
[t0(100%), t1(100%), t2(100%), t3(100%), t4(100%), t5(100%), t6(100%), t7(100%), t8(100%), t9(100%)]
游戏加载完成...
```



#### 同步等待多个远程调用结束

这里我们有一个场景就是加载主页的适合需要加载多个接口，那么此时我们就可以使用 CountDownLatch 同步等待回调，最后我们再将它们汇总返回给前端

这里我们使用睡眠来做调用时间

```java
@Slf4j(topic = "SynchronousWaitingForRemoteCalls")
public class SynchronousWaitingForRemoteCalls {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        remoteCell remoteCell = new remoteCell();
        Future<Object> userInfo = threadPool.submit(remoteCell::getUserInfo);
        Future<List<Object>> list = threadPool.submit(remoteCell::list);
        Future<List<Object>> list2 = threadPool.submit(remoteCell::list2);
        log.debug("{}", userInfo.get());
        log.debug("{}", list.get());
        log.debug("{}", list2.get());
        log.debug("执行完毕...");
        threadPool.shutdown();
    }
}

class remoteCell {
    public Object getUserInfo() {
        sleep(2);
        return new Object();
    }
    public List<Object> list() {
        List<Object> list = new ArrayList<>();
        list.add("a1");
        list.add("a2");
        list.add("a3");
        sleep(3);
        return list;
    }
    public List<Object> list2() {
        List<Object> list = new ArrayList<>();
        list.add("a5");
        list.add("a6");
        list.add("a7");
        sleep(1);
        return list;
    }
}
```

```java
20:21:55.350 [main] DEBUG SynchronousWaitingForRemoteCalls - java.lang.Object@22927a81
20:21:56.344 [main] DEBUG SynchronousWaitingForRemoteCalls - [a1, a2, a3]
20:21:56.344 [main] DEBUG SynchronousWaitingForRemoteCalls - [a5, a6, a7]
20:21:56.344 [main] DEBUG SynchronousWaitingForRemoteCalls - 执行完毕...
```

调用完毕才会执行完毕



##  CyclicBarrier

[ˈsaɪklɪk ˈbæriɚ]  循环栅栏，用来进行线程协作，当线程的数量达到设定的数值时才会往下执行，程序需要同步协作的是就可以用这个来做，使用它的 await 等待其他线程加入。

```java
@Slf4j(topic = "CyclicBarrierTest")
public class CyclicBarrierTest {
    public static void main(String[] args) {
        CyclicBarrier cb = new CyclicBarrier(2);
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.submit(() -> {
            log.debug("t1 线程开始...");
            try {
                // 等待其他线程加入直到计数到到设定值才执行
                cb.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            log.debug("t1 继续执行");
        });
        threadPool.submit(() -> {
            log.debug("t2 线程开始...");
            sleep(2);
            try {
                // 等待其他线程加入直到计数到到设定值才执行
                cb.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            log.debug("t2 继续执行");
        });
        threadPool.shutdown();
    }
}
```

```java
20:34:19.537 [pool-1-thread-2] DEBUG CyclicBarrierTest - t2 线程开始...
20:34:19.537 [pool-1-thread-1] DEBUG CyclicBarrierTest - t1 线程开始...
20:34:21.546 [pool-1-thread-2] DEBUG CyclicBarrierTest - t2 继续执行
20:34:21.546 [pool-1-thread-1] DEBUG CyclicBarrierTest - t1 继续执行
```

可以看到 t1 等待 t2 加入进来才会继续执行，调用 await 方法就算是进来，进来表示计数 - 1，当计数等于 0 时取消等待







# 九、线程安全集合类







## ConcurrentHashMap 原理



## Queue

队列就是出队和入队方法，使用方式这里就不讲解，这里讲解实现的原理



### LinkedBlockingQueue 原理



### ConcurrentLinkedQueue原理





## CopyOnWriteArrayList











