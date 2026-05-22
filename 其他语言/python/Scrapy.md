# 一、Scrapy框架概述

## 1	概述

### ①scrapy的概念

Scrapy是一个Python编写的开源网络爬虫框架。它是一个被设计用于爬取网络数据、提取结构性数据
的框架。
Scrapy 使用了Twisted['twɪstɪd]异步网络框架，可以加快我们的下载速度。
Scrapy文档地址：http://scrapy-chs.readthedocs.io/zh_CN/1.0/intro/overview.html



### ②scrapy框架的作用

少量的代码，就能够快速的抓取



## 2	scrapy框架的工作流程

### ①以往使用爬虫的工作流程

![image-20211119143720793](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211119143722.png)



### ②Scrapy的工作流程

![image-20211119143757804](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211119143759.png)

**描述**：

1.  首先构造一个请求对象，请求对象封装了我们请求需要的一些参数，比如url，然后通过爬虫中间件传递给scrapy引擎，由它来传递给调度器，调度器里面存放的是我们的请求队列
2.  调度器把request传到引擎，由引擎来传递给下载中间件，下载中间件再传递给下载器下载
3.  下载器获取response对象，然后传递给下载中间件，下载中间件传回给引擎，引擎又传递给爬虫中间件，然后传给爬虫模块进行数据解析提取数据
4.  数据提取完毕之后传给引擎，然后管道处理和保存数据

==总的来说，引擎就像是一个交通警察，由它来协助各个模块之间的运行，各个模块负责执行不同的功能==



### ③scrapy内置的三个对象

-   request请求对象：由url method post_data headers等构成
-   response响应对象：由url body status headers等构成
-   item数据对象：本质是个字典



### ④scrapy中每个模块的具体作用

![image-20211119145258019](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211119145259.png)

**注意**：

-   爬虫中间件和下载中间件只是运行逻辑的位置不同，作用是重复的：如替换UA等





# 二、scrapy入门

## 1	安装

```shell
命令:
    sudo apt-get install scrapy
或者：
    pip/pip3 install scrapy
```



## 2	scrapy项目开发流程







