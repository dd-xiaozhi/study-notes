# 第九章：Go 并发编程

Go 语言以其简洁而强大的并发模型著称。通过 goroutine 和 channel，Go 提供了一种优雅的方式来处理并发任务。本章将深入探讨 Go 的并发机制，帮助您掌握高效并发编程的核心技能。

## 目录

1. [Goroutine 基础](#1-goroutine-基础)
2. [Channel 概念与使用](#2-channel-概念与使用)
3. [Channel 类型与操作](#3-channel-类型与操作)
4. [Select 语句](#4-select-语句)
5. [并发安全](#5-并发安全)
6. [上下文管理](#6-上下文管理)
7. [并发模式与图解](#7-并发模式与图解)
8. [实战代码示例](#8-实战代码示例)
9. [工程示例：并发任务调度器](#9-工程示例并发任务调度器)

---

## 1. Goroutine 基础

### 1.1 什么是 Goroutine

Goroutine 是 Go 语言中的轻量级线程，由 Go 运行时（runtime）管理。与传统操作系统线程相比，Goroutine 具有以下优势：

| 特性 | Goroutine | 传统线程 |
|------|-----------|----------|
| 创建成本 | ~2KB 栈空间 | ~1-8MB 栈空间 |
| 创建速度 | 纳秒级 | 毫秒级 |
| 切换成本 | ~200ns | ~1000-2000ns |
| 调度方式 | 多路复用（MPG模型） | 系统级调度 |

### 1.2 创建 Goroutine

使用 `go` 关键字即可启动一个 goroutine：

```go
package main

import (
    "fmt"
    "time"
)

func say(s string) {
    for i := 0; i < 5; i++ {
        fmt.Println(s)
        time.Sleep(100 * time.Millisecond)
    }
}

func main() {
    // 顺序执行
    say("sync")

    // 并发执行
    go say("async 1")
    go say("async 2")

    // 等待 goroutine 执行完成
    time.Sleep(time.Second)
    fmt.Println("main function done")
}
```

### 1.3 Goroutine 的生命周期

```go
func main() {
    // 启动一个 goroutine
    go func() {
        fmt.Println("goroutine started")
        // 模拟工作
        time.Sleep(2 * time.Second)
        fmt.Println("goroutine finished")
    }()

    // main goroutine 退出时，所有子 goroutine 也会终止
    time.Sleep(1 * time.Second)
    fmt.Println("main is about to exit")
}
```

### 1.4 匿名 Goroutine

```go
func main() {
    // 匿名函数作为 goroutine
    go func(x int) {
        fmt.Printf("Value: %d\n", x)
    }(42)

    time.Sleep(time.Second)
}
```

---

## 2. Channel 概念与使用

### 2.1 Channel 简介

Channel 是 Go 语言提供的用于 goroutine 之间的通信机制。它类似于管道，发送端可以向 channel 发送数据，接收端可以从 channel 接收数据。

### 2.2 创建 Channel

```go
// 创建一个传递 int 类型的 channel
ch := make(chan int)

// 创建一个有缓冲的 channel
ch buffered := make(chan int, 10)
```

### 2.3 Channel 的方向

```go
// 发送端函数（只能发送）
func send(ch chan<- int) {
    ch <- 42
}

// 接收端函数（只能接收）
func receive(ch <-chan int) {
    val := <-ch
    fmt.Println("Received:", val)
}

// 双向 channel（函数参数）
func process(ch chan int) {
    // 可以发送和接收
}
```

### 2.4 Channel 操作图解

```
┌─────────────────────────────────────────────────────────┐
│                      Channel                            │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Buffered Channel                    │   │
│  │  ┌────┬────┬────┬────┐                         │   │
│  │  │ 10 │ 20 │ 30 │    │  ← 容量: 4             │   │
│  │  └────┴────┴────┴────┘                         │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Send ──────────────────────────────→ │ ←────────── Recv │
│  goroutine A                            goroutine B     │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Channel 类型与操作

### 3.1 无缓冲 Channel

无缓冲 channel 要求发送和接收同时进行，否则会阻塞。

```go
func main() {
    ch := make(chan int)

    go func() {
        fmt.Println("Sending 42")
        ch <- 42 // 阻塞，直到有人接收
        fmt.Println("Sent 42")
    }()

    fmt.Println("Waiting to receive...")
    val := <-ch // 阻塞，直到有人发送
    fmt.Println("Received:", val)
}
```

### 3.2 有缓冲 Channel

有缓冲 channel 在缓冲区满时才会阻塞发送，在缓冲区空时才会阻塞接收。

```go
func main() {
    ch := make(chan int, 3)

    // 发送3个元素（不会阻塞）
    ch <- 1
    ch <- 2
    ch <- 3

    // 再发送会阻塞（缓冲区已满）
    // ch <- 4 // 永久阻塞

    // 接收所有元素
    for i := 0; i < 3; i++ {
        fmt.Println(<-ch)
    }
}
```

### 3.3 关闭 Channel

```go
ch := make(chan int, 3)
ch <- 1
ch <- 2
ch <- 3
close(ch) // 关闭 channel

// 遍历 channel（channel 关闭后，遍历自动结束）
for v := range ch {
    fmt.Println(v)
}

// 或者使用 ok 检查 channel 状态
val, ok := <-ch
if !ok {
    fmt.Println("Channel is closed")
}
```

### 3.4 Channel 状态图

```
                    Channel 状态转换
                    ┌─────────────┐
                    │   Open      │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
       ┌──────────┐  ┌──────────┐  ┌──────────┐
       │   nil    │  │  Active  │  │  Closed  │
       └──────────┘  └────┬─────┘  └──────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
         ▼                ▼                ▼
    ┌─────────┐     ┌───────────┐     ┌───────────┐
    │Send      │     │Send/Recv  │     │Recv only  │
    │blocked   │     │operable   │     │(returns   │
    │          │     │           │     │zero val)  │
    └─────────┘     └───────────┘     └───────────┘
```

---

## 4. Select 语句

### 4.1 Select 基本用法

`select` 语句类似于 `switch`，但用于监听多个 channel 的 IO 操作。

```go
select {
case v := <-ch1:
    fmt.Println("Received from ch1:", v)
case v := <-ch2:
    fmt.Println("Received from ch2:", v)
case ch3 <- 42:
    fmt.Println("Sent to ch3")
default:
    fmt.Println("No operation ready")
}
```

### 4.2 Select 等待多个 Channel

```go
func main() {
    ch1 := make(chan string)
    ch2 := make(chan string)

    go func() {
        time.Sleep(1 * time.Second)
        ch1 <- "from channel 1"
    }()

    go func() {
        time.Sleep(2 * time.Second)
        ch2 <- "from channel 2"
    }()

    for i := 0; i < 2; i++ {
        select {
        case msg1 := <-ch1:
            fmt.Println(msg1)
        case msg2 := <-ch2:
            fmt.Println(msg2)
        }
    }
}
```

### 4.3 Select 超时处理

```go
func main() {
    ch := make(chan int)

    go func() {
        time.Sleep(2 * time.Second)
        ch <- 42
    }()

    select {
    case v := <-ch:
        fmt.Println("Received:", v)
    case <-time.After(1 * time.Second):
        fmt.Println("Timeout! No message received")
    }
}
```

### 4.4 Select 退出机制

```go
func main() {
    done := make(chan struct{})
    ch := make(chan int)

    go func() {
        for {
            select {
            case v := <-ch:
                fmt.Println("Received:", v)
            case <-done:
                fmt.Println("Exiting goroutine")
                return
            }
        }
    }()

    ch <- 1
    ch <- 2
    close(done)
    time.Sleep(100 * time.Millisecond)
}
```

---

## 5. 并发安全

### 5.1 竞态条件

当多个 goroutine 同时访问共享资源时，可能会产生竞态条件：

```go
// 不安全的计数器
var counter int

func increment() {
    counter++ // 读->修改->写，不是原子操作
}

func main() {
    for i := 0; i < 1000; i++ {
        go increment()
    }
    time.Sleep(time.Second)
    fmt.Println(counter) // 可能不等于 1000
}
```

### 5.2 Mutex（互斥锁）

```go
import "sync"

var (
    counter int
    mu      sync.Mutex
)

func increment() {
    mu.Lock()
    defer mu.Unlock()
    counter++
}

func main() {
    var wg sync.WaitGroup
    wg.Add(1000)

    for i := 0; i < 1000; i++ {
        go func() {
            defer wg.Done()
            increment()
        }()
    }

    wg.Wait()
    fmt.Println(counter) // 正确输出 1000
}
```

### 5.3 RWMutex（读写锁）

读写锁允许多个读操作并发进行，但写操作需要独占访问：

```go
type SafeCounter struct {
    mu    sync.RWMutex
    count map[string]int
}

func (sc *SafeCounter) Inc(key string) {
    sc.mu.Lock()
    defer sc.mu.Unlock()
    sc.count[key]++
}

func (sc *SafeCounter) Get(key string) int {
    sc.mu.RLock()
    defer sc.mu.RUnlock()
    return sc.count[key]
}

// 批量读取可以共享访问
func (sc *SafeCounter) GetAll() map[string]int {
    sc.mu.RLock()
    defer sc.mu.RUnlock()
    result := make(map[string]int)
    for k, v := range sc.count {
        result[k] = v
    }
    return result
}
```

### 5.4 atomic 包

对于简单的数值操作，可以使用 `sync/atomic` 包：

```go
import "sync/atomic"

var counter int64

func increment() {
    atomic.AddInt64(&counter, 1)
}

func main() {
    var wg sync.WaitGroup
    wg.Add(1000)

    for i := 0; i < 1000; i++ {
        go func() {
            defer wg.Done()
            increment()
        }()
    }

    wg.Wait()
    fmt.Println(atomic.LoadInt64(&counter)) // 正确输出 1000
}
```

### 5.5 原子操作类型对比

| 操作 | atomic | mutex |
|------|--------|-------|
| 适用场景 | 简单数值、布尔 | 复杂数据结构 |
| 性能 | 更高（无锁） | 较低（有锁） |
| 代码复杂度 | 简单 | 中等 |

### 5.6 sync.Once 一次性初始化

```go
var (
    once     sync.Once
    instance *Singleton
)

func GetInstance() *Singleton {
    once.Do(func() {
        fmt.Println("Creating singleton instance")
        instance = &Singleton{}
    })
    return instance
}
```

### 5.7 sync.Map 并发安全 Map

```go
var safeMap sync.Map

// 存储
safeMap.Store("key", "value")

// 读取
if val, ok := safeMap.Load("key"); ok {
    fmt.Println(val)
}

// 删除
safeMap.Delete("key")

// 遍历
safeMap.Range(func(key, value interface{}) bool {
    fmt.Printf("%s: %s\n", key, value)
    return true
})
```

---

## 6. 上下文管理

### 6.1 Context 简介

`context` 包提供了上下文传播机制，用于在 goroutine 之间传递请求作用域的数据、取消信号和超时控制。

### 6.2 创建 Context

```go
import "context"

// 根 Context（不可取消、无值、无超时）
ctx := context.Background()

// 可取消的子 Context
ctx, cancel := context.WithCancel(context.Background())

// 带超时的 Context
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)

// 带截止时间的 Context
ctx, cancel := context.WithDeadline(context.Background(), time.Now().Add(24*time.Hour))

// 带值的 Context
ctx := context.WithValue(context.Background(), "userID", "12345")
```

### 6.3 Context 取消机制

```go
func main() {
    ctx, cancel := context.WithCancel(context.Background())

    go func() {
        for {
            select {
            case <-ctx.Done():
                fmt.Println("Goroutine cancelled:", ctx.Err())
                return
            default:
                fmt.Println("Working...")
                time.Sleep(500 * time.Millisecond)
            }
        }
    }()

    time.Sleep(2 * time.Second)
    fmt.Println("Cancelling...")
    cancel()
    time.Sleep(500 * time.Millisecond)
}
```

### 6.4 Context 传递值

```go
func main() {
    ctx := context.WithValue(context.Background(), "traceID", "abc-123")

    go processRequest(ctx)

    time.Sleep(time.Second)
}

func processRequest(ctx context.Context) {
    traceID := ctx.Value("traceID")
    fmt.Println("Processing request with traceID:", traceID)

    // 传递带有请求值的子 Context
    ctx = context.WithValue(ctx, "requestID", "req-456")
    callService(ctx)
}

func callService(ctx context.Context) {
    traceID := ctx.Value("traceID")
    requestID := ctx.Value("requestID")
    fmt.Printf("Service call: traceID=%s, requestID=%s\n", traceID, requestID)
}
```

### 6.5 HTTP 请求中的 Context

```go
func handler(w http.ResponseWriter, r *http.Request) {
    // 在 HTTP 请求中使用 Context
    ctx := r.Context()

    select {
    case <-time.After(5 * time.Second):
        fmt.Fprintln(w, "Response ready")
    case <-ctx.Done():
        fmt.Println("Request cancelled:", ctx.Err())
    }
}

func main() {
    http.HandleFunc("/", handler)
    http.ListenAndServe(":8080", nil)
}
```

### 6.6 Context 最佳实践

```go
// 正确：使用 context 参数传递
func doWork(ctx context.Context) error {
    // 在函数开头检查是否已取消
    select {
    case <-ctx.Done():
        return ctx.Err()
    default:
    }

    // 执行工作...
    return nil
}

// 错误：不应该将 Context 存储在结构体中
// type Worker struct {
//     ctx context.Context  // 不推荐
// }
```

---

## 7. 并发模式与图解

### 7.1 Goroutine 调度模型（MPG 模型）

```
┌─────────────────────────────────────────────────────────────┐
│                        OS Kernel                            │
│                    (System Threads)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Thread 1   │  │  Thread 2   │  │  Thread 3   │        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
└─────────┼─────────────────┼─────────────────┼───────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────┐
│                     Go Runtime                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Global Run Queue (GRQ)                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                 │
│     ┌─────────────────────┼─────────────────────┐           │
│     ▼                     ▼                     ▼           │
│  ┌─────────┐          ┌─────────┐          ┌─────────┐     │
│  │   P 1   │          │   P 2   │          │   P 3   │     │
│  │Processor│          │Processor│          │Processor│     │
│  └────┬────┘          └────┬────┘          └────┬────┘     │
│       │                     │                     │         │
│  ┌────┴────┐           ┌────┴────┐           ┌────┴────┐    │
│  │ G1, G2  │           │ G3, G4  │           │ G5, G6  │    │
│  │  Local  │           │  Local  │           │  Local  │    │
│  │  Queue  │           │  Queue  │           │  Queue  │    │
│  └─────────┘           └─────────┘           └─────────┘    │
└─────────────────────────────────────────────────────────────┘

M = Machine (OS Thread)
P = Processor (Execution Context)
G = Goroutine
```

### 7.2 生产者-消费者模式

```
                    Producer-Consumer Pattern
    ┌──────────────────────────────────────────────────────┐
    │                                                      │
    │   ┌──────────┐          ┌────────┐     ┌─────────┐  │
    │   │Producer 1│──────┐    │        │     │Consumer1│  │
    │   └──────────┘      │    │        │     └─────────┘  │
    │                      │    │        │                 │
    │   ┌──────────┐      ▼    │ Channel│     ┌─────────┐  │
    │   │Producer 2│───────────▶│  (FIFO)│────▶│Consumer2│  │
    │   └──────────┘      ▲    │        │     └─────────┘  │
    │                      │    │        │                 │
    │   ┌──────────┐      │    │        │     ┌─────────┐  │
    │   │Producer 3│──────┘    └────────┘     │Consumer3│  │
    │   └──────────┘                          └─────────┘  │
    │                                                      │
    └──────────────────────────────────────────────────────┘
```

### 7.3 Pipeline 模式

```
                    Pipeline Pattern
    ┌────────────────────────────────────────────────────┐
    │                                                    │
    │  ┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐     │
    │  │Stage1│───▶│Stage2│───▶│Stage3│───▶│Stage4│     │
    │  │ (In) │    │      │    │      │    │(Out) │     │
    │  └──────┘    └──────┘    └──────┘    └──────┘     │
    │      │           │           │           │         │
    │      ▼           ▼           ▼           ▼         │
    │   ┌─────┐     ┌─────┐     ┌─────┐     ┌─────┐     │
    │   │Ch 1 │     │Ch 2 │     │Ch 3 │     │Ch 4 │     │
    │   └─────┘     └─────┘     └─────┘     └─────┘     │
    │                                                    │
    │  Each stage: read from input, process, write out  │
    │                                                    │
    └────────────────────────────────────────────────────┘
```

### 7.4 并发爬虫模式

```
                    Web Crawler Pattern
    ┌──────────────────────────────────────────────────────┐
    │                                                      │
    │         ┌─────────────┐                             │
    │         │   URL       │                             │
    │         │   Queue     │                             │
    │         │  (Channel)  │                             │
    │         └──────┬──────┘                             │
    │                │                                     │
    │       ┌────────┼────────┐                          │
    │       ▼        ▼        ▼                           │
    │   ┌────────┐┌────────┐┌────────┐                   │
    │   │Worker 1││Worker 2││Worker 3│   ...             │
    │   └────┬───┘└────┬───┘└────┬───┘                   │
    │        │        │        │                         │
    │        ▼        ▼        ▼                         │
    │   ┌────────┐┌────────┐┌────────┐                   │
    │   │Fetch & ││Fetch & ││Fetch & │                   │
    │   │ Parse  ││ Parse  ││ Parse  │                   │
    │   └───┬────┘└───┬────┘└───┬────┘                   │
    │       │        │        │                         │
    │       ▼        ▼        ▼                         │
    │   ┌────────────────────────────────┐               │
    │   │      Result Channel            │               │
    │   │  (URLs + Extracted Data)       │               │
    │   └────────────────────────────────┘               │
    │                       │                            │
    │                       ▼                            │
    │               ┌─────────────┐                      │
    │               │  Scheduler  │                      │
    │               │ (Controls)  │                      │
    │               └─────────────┘                      │
    │                                                      │
    └──────────────────────────────────────────────────────┘
```

### 7.5 任务调度器架构

```
                Task Scheduler Architecture
    ┌──────────────────────────────────────────────────────┐
    │                                                      │
    │   ┌─────────────────────────────────────────────┐   │
    │   │              Task Queue (Priority)            │   │
    │   │  ┌─────┬─────┬─────┬─────┬─────┬─────┐      │   │
    │   │  │ T1  │ T2  │ T3  │ T4  │ T5  │ T6  │ ...  │   │
    │   │  │ Hi  │ Med │ Low │ Hi  │ Med │ Low │      │   │
    │   │  └─────┴─────┴─────┴─────┴─────┴─────┘      │   │
    │   └──────────────────┬──────────────────────────┘   │
    │                      │                               │
    │                      ▼                               │
    │   ┌─────────────────────────────────────────────┐   │
    │   │           Worker Pool (N workers)           │   │
    │   │  ┌─────────┐ ┌─────────┐ ┌─────────┐       │   │
    │   │  │Worker 1 │ │Worker 2 │ │Worker 3 │ ...    │   │
    │   │  │  idle   │ │ running │ │ running │       │   │
    │   │  └─────────┘ └─────────┘ └─────────┘       │   │
    │   └──────────────────┬──────────────────────────┘   │
    │                      │                               │
    │                      ▼                               │
    │   ┌─────────────────────────────────────────────┐   │
    │   │           Result Channel                     │   │
    │   │    (Completed Tasks / Errors)               │   │
    │   └─────────────────────────────────────────────┘   │
    │                                                      │
    └──────────────────────────────────────────────────────┘
```

---

## 8. 实战代码示例

### 8.1 生产者-消费者模型

```go
package main

import (
    "fmt"
    "math/rand"
    "sync"
    "time"
)

// Producer 生产者
func Producer(ctx context.Context, ch chan<- int, id int, count int) {
    for i := 0; i < count; i++ {
        select {
        case <-ctx.Done():
            fmt.Printf("Producer %d: cancelled\n", id)
            return
        case ch <- rand.Intn(100):
            fmt.Printf("Producer %d: sent %d\n", id, i)
        }
        time.Sleep(100 * time.Millisecond)
    }
    fmt.Printf("Producer %d: finished\n", id)
}

// Consumer 消费者
func Consumer(ctx context.Context, ch <-chan int, id int, wg *sync.WaitGroup) {
    defer wg.Done()
    for {
        select {
        case <-ctx.Done():
            fmt.Printf("Consumer %d: cancelled\n", id)
            return
        case val, ok := <-ch:
            if !ok {
                fmt.Printf("Consumer %d: channel closed\n", id)
                return
            }
            fmt.Printf("Consumer %d: received %d\n", id, val)
        }
    }
}

func main() {
    rand.Seed(time.Now().UnixNano())

    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    ch := make(chan int, 10)

    var wg sync.WaitGroup

    // 启动 2 个生产者
    for i := 1; i <= 2; i++ {
        go Producer(ctx, ch, i, 10)
    }

    // 启动 3 个消费者
    for i := 1; i <= 3; i++ {
        wg.Add(1)
        go Consumer(ctx, ch, i, &wg)
    }

    // 等待生产者完成
    time.Sleep(500 * time.Millisecond)
    close(ch)

    wg.Wait()
    fmt.Println("All done!")
}
```

### 8.2 并发爬虫

```go
package main

import (
    "context"
    "fmt"
    "sync"
    "sync/atomic"
    "time"
)

// URLResult 爬取结果
type URLResult struct {
    URL   string
    Title string
    Links []string
    Error error
}

// Crawler 并发爬虫
type Crawler struct {
    maxDepth    int
    maxWorkers  int
    seenURLs    sync.Map
    urlCount    int64
    foundCount  int64
}

func NewCrawler(maxDepth, maxWorkers int) *Crawler {
    return &Crawler{
        maxDepth:   maxDepth,
        maxWorkers: maxWorkers,
    }
}

func (c *Crawler) isSeen(url string) bool {
    _, loaded := c.seenURLs.LoadOrStore(url, true)
    return loaded
}

func (c *Crawler) Crawl(ctx context.Context, urls []string) []URLResult {
    results := make(chan URLResult, len(urls))
    urlQueue := make(chan string, 100)

    var wg sync.WaitGroup

    // 启动 worker 池
    for i := 0; i < c.maxWorkers; i++ {
        wg.Add(1)
        go func(workerID int) {
            defer wg.Done()
            c.worker(ctx, workerID, urlQueue, results)
        }(i)
    }

    // 发送初始 URLs
    go func() {
        for _, url := range urls {
            select {
            case <-ctx.Done():
                return
            case urlQueue <- url:
                atomic.AddInt64(&c.urlCount, 1)
            }
        }
    }()

    // 等待所有 worker 完成
    go func() {
        wg.Wait()
        close(results)
    }()

    // 收集结果
    var crawlResults []URLResult
    for result := range results {
        if result.Error == nil {
            crawlResults = append(crawlResults, result)
            fmt.Printf("Crawled: %s -> %s\n", result.URL, result.Title)
        }
    }

    return crawlResults
}

func (c *Crawler) worker(ctx context.Context, id int, urls <-chan string, results chan<- URLResult) {
    for {
        select {
        case <-ctx.Done():
            return
        case url, ok := <-urls:
            if !ok {
                return
            }

            // 检查是否已爬取
            if c.isSeen(url) {
                continue
            }

            // 模拟爬取
            result := c.fetchURL(ctx, url)
            results <- result

            // 处理发现的链接
            for _, link := range result.Links {
                select {
                case <-ctx.Done():
                    return
                case urls <- link:
                    atomic.AddInt64(&c.foundCount, 1)
                }
            }
        }
    }
}

func (c *Crawler) fetchURL(ctx context.Context, url string) URLResult {
    // 模拟网络请求
    select {
    case <-ctx.Done():
        return URLResult{URL: url, Error: ctx.Err()}
    case <-time.After(100 * time.Millisecond):
    }

    atomic.AddInt64(&c.urlCount, -1)

    return URLResult{
        URL:   url,
        Title: fmt.Sprintf("Title of %s", url),
        Links: []string{
            fmt.Sprintf("%s/page1", url),
            fmt.Sprintf("%s/page2", url),
        },
    }
}

func main() {
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()

    crawler := NewCrawler(maxDepth=3, maxWorkers=5)

    initialURLs := []string{
        "https://example.com",
        "https://example.org",
    }

    start := time.Now()
    results := crawler.Crawl(ctx, initialURLs)

    fmt.Printf("\n=== Crawl Summary ===\n")
    fmt.Printf("Duration: %v\n", time.Since(start))
    fmt.Printf("Pages crawled: %d\n", len(results))
    fmt.Printf("Total URLs processed: %d\n", atomic.LoadInt64(&crawler.urlCount))
    fmt.Printf("Links found: %d\n", atomic.LoadInt64(&crawler.foundCount))
}
```

---

## 9. 工程示例：并发任务调度器

### 9.1 设计概述

本节实现一个生产级别的任务调度器，具有以下特性：

- 支持优先级队列
- 可配置的 worker 池
- 任务超时控制
- 优雅关闭
- 任务状态追踪

### 9.2 完整实现

```go
package scheduler

import (
    "context"
    "fmt"
    "sync"
    "sync/atomic"
    "time"
)

// TaskPriority 任务优先级
type TaskPriority int

const (
    PriorityLow TaskPriority = iota
    PriorityMedium
    PriorityHigh
)

func (p TaskPriority) String() string {
    switch p {
    case PriorityLow:
        return "LOW"
    case PriorityMedium:
        return "MEDIUM"
    case PriorityHigh:
        return "HIGH"
    default:
        return "UNKNOWN"
    }
}

// Task 任务定义
type Task struct {
    ID       string
    Priority TaskPriority
    Payload  interface{}
    Execute  func(context.Context) (interface{}, error)
    Timeout  time.Duration
    createdAt time.Time
}

// TaskResult 任务执行结果
type TaskResult struct {
    Task   *Task
    Result interface{}
    Error  error
    Start  time.Time
    End    time.Time
}

// TaskStatus 任务状态
type TaskStatus int

const (
    StatusPending TaskStatus = iota
    StatusRunning
    StatusCompleted
    StatusFailed
    StatusCancelled
)

func (s TaskStatus) String() string {
    switch s {
    case StatusPending:
        return "PENDING"
    case StatusRunning:
        return "RUNNING"
    case StatusCompleted:
        return "COMPLETED"
    case StatusFailed:
        return "FAILED"
    case StatusCancelled:
        return "CANCELLED"
    default:
        return "UNKNOWN"
    }
}

// TaskInfo 任务信息
type TaskInfo struct {
    Task   *Task
    Status TaskStatus
    Result *TaskResult
}

// Scheduler 并发任务调度器
type Scheduler struct {
    workers    int
    taskQueue  chan *Task
    resultChan chan *TaskResult
    taskInfos  sync.Map // taskID -> *TaskInfo
    ctx        context.Context
    cancel     context.CancelFunc

    // 统计信息
    submitted  int64
    completed  int64
    failed    int64
    cancelled int64
    running   int64

    mu        sync.RWMutex
    started   bool
    closed    bool
}

// NewScheduler 创建调度器
func NewScheduler(workers int, queueSize int) *Scheduler {
    if workers <= 0 {
        workers = 4
    }
    if queueSize <= 0 {
        queueSize = 100
    }

    ctx, cancel := context.WithCancel(context.Background())

    return &Scheduler{
        workers:    workers,
        taskQueue:  make(chan *Task, queueSize),
        resultChan: make(chan *TaskResult, queueSize),
        ctx:        ctx,
        cancel:     cancel,
    }
}

// Start 启动调度器
func (s *Scheduler) Start() {
    s.mu.Lock()
    defer s.mu.Unlock()

    if s.started {
        return
    }

    for i := 0; i < s.workers; i++ {
        go s.worker(i)
    }

    go s.resultHandler()
    s.started = true

    fmt.Printf("Scheduler started with %d workers\n", s.workers)
}

// worker 处理任务
func (s *Scheduler) worker(id int) {
    fmt.Printf("Worker %d started\n", id)

    for {
        select {
        case <-s.ctx.Done():
            fmt.Printf("Worker %d: context cancelled\n", id)
            return

        case task, ok := <-s.taskQueue:
            if !ok {
                fmt.Printf("Worker %d: task queue closed\n", id)
                return
            }

            s.executeTask(task)
        }
    }
}

// executeTask 执行单个任务
func (s *Scheduler) executeTask(task *Task) {
    // 更新任务状态
    s.storeTaskInfo(task, StatusRunning, nil)

    atomic.AddInt64(&s.running, 1)
    defer atomic.AddInt64(&s.running, -1)

    start := time.Now()

    // 创建任务上下文
    taskCtx := s.ctx
    var cancel context.CancelFunc
    if task.Timeout > 0 {
        taskCtx, cancel = context.WithTimeout(s.ctx, task.Timeout)
        defer cancel()
    } else {
        taskCtx, cancel = context.WithCancel(s.ctx)
        defer cancel()
    }

    // 执行任务
    result, err := task.Execute(taskCtx)

    end := time.Now()

    taskResult := &TaskResult{
        Task:   task,
        Result: result,
        Error:  err,
        Start:  start,
        End:    end,
    }

    // 更新状态
    if err != nil {
        if err == context.Canceled || err == context.DeadlineExceeded {
            s.storeTaskInfo(task, StatusCancelled, taskResult)
            atomic.AddInt64(&s.cancelled, 1)
        } else {
            s.storeTaskInfo(task, StatusFailed, taskResult)
            atomic.AddInt64(&s.failed, 1)
        }
    } else {
        s.storeTaskInfo(task, StatusCompleted, taskResult)
        atomic.AddInt64(&s.completed, 1)
    }

    // 发送结果
    select {
    case s.resultChan <- taskResult:
    case <-s.ctx.Done():
    }
}

// Submit 提交任务
func (s *Scheduler) Submit(task *Task) error {
    if task == nil {
        return fmt.Errorf("task is nil")
    }

    task.createdAt = time.Now()

    if task.ID == "" {
        task.ID = fmt.Sprintf("task-%d", time.Now().UnixNano())
    }

    select {
    case s.taskQueue <- task:
        atomic.AddInt64(&s.submitted, 1)
        s.storeTaskInfo(task, StatusPending, nil)
        return nil
    default:
        return fmt.Errorf("task queue is full")
    }
}

// SubmitWithPriority 带优先级提交（实际实现中会使用优先队列）
func (s *Scheduler) SubmitWithPriority(task *Task) error {
    // 简化实现：直接提交到队列
    // 生产环境应使用优先队列
    return s.Submit(task)
}

// resultHandler 处理任务结果
func (s *Scheduler) resultHandler() {
    for result := range s.resultChan {
        if result.Error != nil {
            fmt.Printf("Task %s failed: %v\n", result.Task.ID, result.Error)
        } else {
            fmt.Printf("Task %s completed\n", result.Task.ID)
        }
    }
}

// GetTaskInfo 获取任务信息
func (s *Scheduler) GetTaskInfo(taskID string) (*TaskInfo, bool) {
    val, ok := s.taskInfos.Load(taskID)
    if !ok {
        return nil, false
    }
    return val.(*TaskInfo), true
}

func (s *Scheduler) storeTaskInfo(task *Task, status TaskStatus, result *TaskResult) {
    s.taskInfos.Store(task.ID, &TaskInfo{
        Task:   task,
        Status: status,
        Result: result,
    })
}

// ResultChan 返回结果通道
func (s *Scheduler) ResultChan() <-chan *TaskResult {
    return s.resultChan
}

// Stop 优雅停止调度器
func (s *Scheduler) Stop(timeout time.Duration) {
    s.mu.Lock()
    if s.closed {
        s.mu.Unlock()
        return
    }
    s.closed = true
    s.mu.Unlock()

    fmt.Println("Stopping scheduler...")

    // 取消上下文
    s.cancel()

    // 等待 worker 结束
   deadline := time.After(timeout)
    ticker := time.NewTicker(100 * time.Millisecond)
    defer ticker.Stop()

    for {
        select {
        case <-deadline:
            fmt.Println("Scheduler stop timeout")
            return
        case <-ticker.C:
            if atomic.LoadInt64(&s.running) == 0 {
                goto done
            }
        }
    }

done:
    close(s.taskQueue)
    close(s.resultChan)

    fmt.Println("Scheduler stopped")
    s.printStats()
}

// Stats 统计信息
func (s *Scheduler) Stats() map[string]int64 {
    return map[string]int64{
        "submitted":  atomic.LoadInt64(&s.submitted),
        "completed": atomic.LoadInt64(&s.completed),
        "failed":    atomic.LoadInt64(&s.failed),
        "cancelled": atomic.LoadInt64(&s.cancelled),
        "running":   atomic.LoadInt64(&s.running),
    }
}

func (s *Scheduler) printStats() {
    stats := s.Stats()
    fmt.Println("\n=== Scheduler Stats ===")
    for k, v := range stats {
        fmt.Printf("%s: %d\n", k, v)
    }
}
```

### 9.3 调度器使用示例

```go
package main

import (
    "context"
    "fmt"
    "time"

    "your_module/scheduler"
)

func main() {
    // 创建调度器：4 个 worker，队列大小 100
    sched := scheduler.NewScheduler(4, 100)

    // 启动调度器
    sched.Start()

    // 启动结果监听
    go func() {
        for result := range sched.ResultChan() {
            fmt.Printf("Result received - Task: %s, Duration: %v\n",
                result.Task.ID, result.End.Sub(result.Start))
        }
    }()

    // 提交各种任务
    tasks := []*scheduler.Task{
        {
            ID:       "task-1",
            Priority: scheduler.PriorityHigh,
            Payload:  "high priority task",
            Timeout:  5 * time.Second,
            Execute: func(ctx context.Context) (interface{}, error) {
                time.Sleep(1 * time.Second)
                return "task-1 result", nil
            },
        },
        {
            ID:       "task-2",
            Priority: scheduler.PriorityMedium,
            Payload:  "medium priority task",
            Timeout:  3 * time.Second,
            Execute: func(ctx context.Context) (interface{}, error) {
                select {
                case <-time.After(2 * time.Second):
                    return "task-2 result", nil
                case <-ctx.Done():
                    return nil, ctx.Err()
                }
            },
        },
        {
            ID:       "task-3",
            Priority: scheduler.PriorityLow,
            Payload:  "low priority task",
            Timeout:  10 * time.Second,
            Execute: func(ctx context.Context) (interface{}, error) {
                time.Sleep(500 * time.Millisecond)
                return "task-3 result", nil
            },
        },
        {
            ID:       "task-timeout",
            Priority: scheduler.PriorityHigh,
            Payload:  "this will timeout",
            Timeout:  1 * time.Second,
            Execute: func(ctx context.Context) (interface{}, error) {
                select {
                case <-time.After(5 * time.Second):
                    return "never returned", nil
                case <-ctx.Done():
                    return nil, ctx.Err()
                }
            },
        },
    }

    // 批量提交任务
    for _, task := range tasks {
        if err := sched.Submit(task); err != nil {
            fmt.Printf("Failed to submit task %s: %v\n", task.ID, err)
        } else {
            fmt.Printf("Submitted task: %s (priority: %s)\n", task.ID, task.Priority)
        }
    }

    // 等待一段时间后查看任务状态
    time.Sleep(2 * time.Second)

    for _, taskID := range []string{"task-1", "task-2", "task-3", "task-timeout"} {
        if info, ok := sched.GetTaskInfo(taskID); ok {
            fmt.Printf("Task %s status: %s\n", taskID, info.Status)
        }
    }

    // 打印统计信息
    fmt.Println("\nCurrent stats:", sched.Stats())

    // 优雅关闭
    sched.Stop(10 * time.Second)
}
```

### 9.4 调度器流程图

```
                Scheduler Flow Diagram
    ┌──────────────────────────────────────────────────────┐
    │                                                      │
    │  ┌──────────┐                                       │
    │  │  Submit  │◀────── User submits tasks            │
    │  │  Task    │                                       │
    │  └────┬─────┘                                       │
    │       │                                             │
    │       ▼                                             │
    │  ┌────────────────────────────────────────────┐    │
    │  │         Task Queue (Buffered Channel)      │    │
    │  │  ┌─────┬─────┬─────┬─────┬─────┬─────┐     │    │
    │  │  │ Hi  │ Hi  │ Med │ Med │ Low │ Low │ ... │    │
    │  │  └─────┴─────┴─────┴─────┴─────┴─────┘     │    │
    │  └──────────────────────┬───────────────────────┘    │
    │                         │                            │
    │       ┌─────────────────┼─────────────────┐          │
    │       │                 │                 │          │
    │       ▼                 ▼                 ▼          │
    │  ┌─────────┐       ┌─────────┐       ┌─────────┐    │
    │  │ Worker 1│       │ Worker 2│       │ Worker 3│    │
    │  │         │       │         │       │         │    │
    │  │ Select  │       │ Select  │       │ Select  │    │
    │  │  case   │       │  case   │       │  case   │    │
    │  │  task   │       │  task   │       │  task   │    │
    │  └────┬────┘       └────┬────┘       └────┬────┘    │
    │       │                 │                 │          │
    │       └─────────────────┼─────────────────┘          │
    │                         │                            │
    │                         ▼                            │
    │                  ┌─────────────┐                     │
    │                  │   Execute   │                     │
    │                  │   Task      │                     │
    │                  └──────┬──────┘                     │
    │                         │                            │
    │                         ▼                            │
    │                  ┌─────────────┐                     │
    │                  │   Result    │                     │
    │                  │   Channel   │                     │
    │                  └──────┬──────┘                     │
    │                         │                            │
    │                         ▼                            │
    │  ┌──────────────────────────────────────────────┐  │
    │  │         TaskInfo Map (sync.Map)                │  │
    │  │  taskID → {Status, Result}                    │  │
    │  └──────────────────────────────────────────────┘  │
    │                                                      │
    └──────────────────────────────────────────────────────┘
```

---

## 总结

本章介绍了 Go 语言强大的并发编程能力：

1. **Goroutine**：轻量级并发执行单元，由 Go 运行时管理
2. **Channel**：goroutine 之间的通信机制，支持同步和异步操作
3. **Select**：多路复用 channel 操作，实现复杂的并发控制
4. **并发安全**：使用 mutex、RWMutex、atomic 等工具保护共享资源
5. **Context**：请求级别的取消信号和值传递
6. **并发模式**：生产者-消费者、Pipeline、任务调度器等

Go 的并发模型设计哲学是：**不要通过共享内存来通信，而要通过通信来共享内存**。这种设计使得并发程序的编写更加简单和安全。

在实际开发中，应该：
- 优先使用 channel 进行并发控制
- 合理使用 mutex 保护复杂共享状态
- 善用 context 进行生命周期管理
- 使用 WaitGroup 管理 goroutine 生命周期
- 避免创建过多的 goroutine

---

> **延伸阅读**
> - Go Blog: [Go Concurrency Patterns](https://blog.golang.org/concurrency)
> - Go Blog: [Advanced Go Concurrency Patterns](https://blog.golang.org/advanced-go-concurrency-patterns)
> - Effective Go: [Concurrency](https://golang.org/doc/effective_go#concurrency)
