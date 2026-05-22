# Redis Feed 流实战

> 本文档介绍如何使用 Redis 实现 Feed 流功能，基于 Java 21 + Spring Boot 3.x

## 目录

- [1. Feed 流模式概述](#1-feed-流模式概述)
  - [1.1 拉模式（Pull）](#11-拉模式pull)
  - [1.2 推模式（Push）](#12-推模式push)
  - [1.3 推拉结合模式](#13-推拉结合模式)
- [2. Timeline 实现方案](#2-timeline-实现方案)
  - [2.1 基于 ZSET 的时间线](#21-基于-zset-的时间线)
  - [2.2 分页查询](#22-分页查询)
- [3. 实战案例1：用户动态推送（推模式）](#3-实战案例1用户动态推送推模式)
- [4. 实战案例2：关注列表最新动态（拉模式）](#4-实战案例2关注列表最新动态拉模式)
- [5. 实战案例3：微博式 Feed 流](#5-实战案例3微博式-feed-流)
- [6. 去重与已读标记](#6-去重与已读标记)
- [7. 总结](#7-总结)

---

## 1. Feed 流模式概述

Feed 流（信息流）是一种常见的内容分发模式，广泛应用于社交网络、新闻订阅、短视频推荐等场景。

### 1.1 拉模式（Pull）

拉模式也称为"读扩散"，用户需要查看 Feed 时，主动拉取关注者发布的内容。

```
用户查看 Feed → 查询关注列表 → 拉取每个关注者的新内容 → 合并排序
```

**优点：**
- 写操作简单，适合内容发布量较少的场景
- 用户不需要关注发布者的性能

**缺点：**
- 读操作复杂，用户查看 Feed 时需要聚合多个数据源
- 如果用户关注人数多，读延迟较高

**适用场景：**
- 粉丝数量少、关注人数多的用户（如大V）
- 内容更新频率较低的场景

### 1.2 推模式（Push）

推模式也称为"写扩散"，当用户发布内容时，主动推送到所有粉丝的收件箱。

```
用户发布内容 → 推送到所有粉丝的收件箱 → 用户直接读取收件箱
```

**优点：**
- 读操作简单，用户直接读取自己的收件箱
- 查看 Feed 延迟低

**缺点：**
- 写操作复杂，当粉丝数量多时，需要写入多个收件箱
- 对于大V用户（粉丝数百万），会产生大量写操作

**适用场景：**
- 粉丝数量少、关注人数多的用户（如普通用户）
- 需要快速获取新内容更新的场景

### 1.3 推拉结合模式

也称为"混合模式"，结合推和拉的优点。

**策略：**
- 对于普通用户发布的内容，采用推模式（写扩散）
- 对于大V用户发布的内容，采用拉模式（读扩散）
- 用户查看 Feed 时，合并推模式收件箱和拉模式的大V内容

**优点：**
- 平衡读写压力
- 避免大V粉丝过多导致的写放大问题

**适用场景：**
- 微博、Twitter 等明星与普通用户共存的平台
- 粉丝数量差异较大的社交网络

---

## 2. Timeline 实现方案

### 2.1 基于 ZSET 的时间线

使用 Redis 的 ZSET（有序集合）存储时间线，score 为时间戳，member 为内容ID。

```java
package com.example.feedservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 ZSET 的时间线服务
 * 使用时间戳作为分数，内容ID作为成员
 */
@Service
public class ZsetTimelineService {

    private final StringRedisTemplate redisTemplate;

    public ZsetTimelineService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 向时间线添加内容
     *
     * @param timelineKey 时间线 Redis Key，如 "timeline:user:123"
     * @param contentId   内容ID
     * @param timestamp   时间戳（毫秒）
     */
    public void addToTimeline(String timelineKey, String contentId, long timestamp) {
        redisTemplate.opsForZSet().add(timelineKey, contentId, timestamp);
    }

    /**
     * 获取时间线内容（按时间倒序）
     *
     * @param timelineKey 时间线 Key
     * @param start       起始索引
     * @param end         结束索引（-1 表示到最后）
     * @return 内容ID列表
     */
    public Set<String> getTimeline(String timelineKey, long start, long end) {
        // ZREVRANGE 返回按分数倒序的结果
        return redisTemplate.opsForZSet().reverseRange(timelineKey, start, end);
    }

    /**
     * 获取时间线内容并返回分数（时间戳）
     *
     * @param timelineKey 时间线 Key
     * @param start       起始索引
     * @param end         结束索引
     * @return 内容ID和对应时间戳的映射
     */
    public Set<String> getTimelineWithScores(String timelineKey, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(timelineKey, start, end);
    }

    /**
     * 获取时间线内容数量
     */
    public Long getTimelineSize(String timelineKey) {
        return redisTemplate.opsForZSet().zCard(timelineKey);
    }

    /**
     * 删除时间线中的指定内容
     */
    public void removeFromTimeline(String timelineKey, String contentId) {
        redisTemplate.opsForZSet().remove(timelineKey, contentId);
    }

    /**
     * 获取指定时间之后的内容（用于增量拉取）
     *
     * @param timelineKey 时间线 Key
     * @param since        起始时间戳（毫秒）
     * @param limit        返回数量限制
     */
    public Set<String> getTimelineSince(String timelineKey, long since, int limit) {
        // 使用 ZREVRANGEBYSCORE 获取时间戳大于 since 的内容
        return redisTemplate.opsForZSet().reverseRangeByScore(
            timelineKey,
            since,
            Double.MAX_VALUE,
            0,
            limit
        );
    }

    /**
     * 获取两个时间戳之间的内容（用于时间范围查询）
     */
    public Set<String> getTimelineBetween(String timelineKey, long minTime, long maxTime) {
        return redisTemplate.opsForZSet().reverseRangeByScore(
            timelineKey,
            minTime,
            maxTime
        );
    }
}
```

### 2.2 分页查询

时间线分页通常有两种方式：**回屏分页**和**游标分页**。

```java
package com.example.feedservice.dto;

import java.util.List;

/**
 * 分页结果封装
 *
 * @param items       当前页的数据
 * @param nextCursor  下一页的游标（用于游标分页）
 * @param hasMore     是否还有更多数据
 * @param totalCount  总数量（可选）
 */
public record PageResult<T>(
    List<T> items,
    Long nextCursor,
    boolean hasMore,
    Long totalCount
) {
    /**
     * 创建空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), null, false, 0L);
    }

    /**
     * 创建有数据的分页结果
     */
    public static <T> PageResult<T> of(List<T> items, Long nextCursor, boolean hasMore) {
        return new PageResult<>(items, nextCursor, hasMore, (long) items.size());
    }
}
```

```java
package com.example.feedservice.service;

import com.example.feedservice.dto.PageResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 时间线分页服务
 */
@Service
public class TimelinePaginationService {

    private final StringRedisTemplate redisTemplate;

    public TimelinePaginationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 回屏分页（Offset-Based Pagination）
     * 适用于页面固定大小的分页
     *
     * @param timelineKey 时间线 Key
     * @param page        页码（从0开始）
     * @param pageSize    每页大小
     * @return 分页结果
     */
    public PageResult<String> getTimelineByPage(String timelineKey, int page, int pageSize) {
        long start = (long) page * pageSize;
        long end = start + pageSize - 1;

        Set<String> items = redisTemplate.opsForZSet().reverseRange(timelineKey, start, end);

        if (items == null || items.isEmpty()) {
            return PageResult.empty();
        }

        // 检查是否还有更多数据
        long totalSize = redisTemplate.opsForZSet().zCard(timelineKey);
        boolean hasMore = (start + pageSize) < totalSize;

        // 下一页的起始索引
        Long nextCursor = hasMore ? (long) (page + 1) : null;

        return PageResult.of(List.copyOf(items), nextCursor, hasMore);
    }

    /**
     * 游标分页（Cursor-Based Pagination）
     * 适用于实时性要求高的场景，避免数据重复或遗漏
     *
     * @param timelineKey 时间线 Key
     * @param cursor       游标（上一页最后一条的时间戳，null 表示第一页）
     * @param pageSize     每页大小
     * @return 分页结果
     */
    public PageResult<String> getTimelineByCursor(String timelineKey, Long cursor, int pageSize) {
        if (cursor == null) {
            // 第一次查询，返回最新数据
            Set<String> items = redisTemplate.opsForZSet().reverseRange(timelineKey, 0, pageSize - 1);
            return buildPageResult(timelineKey, items, pageSize);
        }

        // 使用游标查询：获取时间戳小于 cursor 的数据
        Set<String> items = redisTemplate.opsForZSet().reverseRangeByScore(
            timelineKey,
            0,          // 从最早期望的时间戳
            cursor - 1,  // 小于上一页最后一条的时间戳
            0,
            pageSize
        );

        return buildPageResult(timelineKey, items, pageSize);
    }

    /**
     * 构建分页结果
     */
    private PageResult<String> buildPageResult(String timelineKey, Set<String> items, int pageSize) {
        if (items == null || items.isEmpty()) {
            return PageResult.empty();
        }

        List<String> itemList = List.copyOf(items);

        // 获取最后一条的时间戳作为下一页的游标
        // 注意：ZSET 的 score 就是我们存储的时间戳
        Long lastScore = getLastScore(timelineKey, itemList.get(itemList.size() - 1));

        boolean hasMore = items.size() >= pageSize;
        Long nextCursor = hasMore ? lastScore : null;

        return PageResult.of(itemList, nextCursor, hasMore);
    }

    /**
     * 获取内容的分数（时间戳）
     */
    private Long getLastScore(String timelineKey, String member) {
        Double score = redisTemplate.opsForZSet().score(timelineKey, member);
        return score != null ? score.longValue() : null;
    }
}
```

---

## 3. 实战案例1：用户动态推送（推模式）

推模式适用于粉丝数量较少的用户，当用户发布动态时，将动态推送给所有粉丝。

```java
package com.example.feedservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 配置
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 StringRedisTemplate
     * StringRedisTemplate 对 key 和 value 都使用 String 序列化
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
```

```java
package com.example.feedservice.entity;

import java.time.Instant;
import java.util.List;

/**
 * 动态/帖子实体
 */
public class Post {

    private String id;           // 帖子唯一标识
    private String authorId;     // 作者ID
    private String authorName;  // 作者名称
    private String content;      // 内容
    private String mediaUrl;     // 媒体URL（可选）
    private Instant createTime;  // 创建时间
    private long likeCount;      // 点赞数
    private long commentCount;   // 评论数

    // 省略 getter/setter/constructor
}
```

```java
package com.example.feedservice.service;

import com.example.feedservice.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

/**
 * 推模式 Feed 流服务
 *
 * 核心思想：当用户发布动态时，将动态推送给所有粉丝的收件箱
 *
 * Redis 数据结构：
 * - 收件箱：ZSET，Key 为 "inbox:{userId}"，score 为时间戳，member 为帖子ID
 * - 用户的帖子集：SET，Key 为 "posts:{userId}"，存储用户发布的所有帖子ID
 */
@Service
public class PushFeedService {

    private static final Logger log = LoggerFactory.getLogger(PushFeedService.class);

    /**
     * 收件箱 Key 前缀
     */
    private static final String INBOX_KEY_PREFIX = "inbox:";

    /**
     * 用户发帖集 Key 前缀
     */
    private static final String POSTS_KEY_PREFIX = "posts:";

    /**
     * 收件箱最大容量（防止内存溢出）
     */
    private static final long INBOX_MAX_SIZE = 1000;

    private final StringRedisTemplate redisTemplate;

    public PushFeedService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发布动态
     *
     * 流程：
     * 1. 保存帖子到帖子的 ZSET
     * 2. 获取作者的粉丝列表
     * 3. 将帖子ID推送到每个粉丝的收件箱
     *
     * @param post 帖子
     * @param followerIds 粉丝ID列表
     */
    public void publishPost(Post post, Set<String> followerIds) {
        String postId = post.getId();
        long timestamp = post.getCreateTime().toEpochMilli();

        // 1. 保存帖子到用户发帖集（用于后续可能的查询）
        String postsKey = POSTS_KEY_PREFIX + post.getAuthorId();
        redisTemplate.opsForZSet().add(postsKey, postId, timestamp);

        // 2. 推送给所有粉丝的收件箱
        for (String followerId : followerIds) {
            String inboxKey = INBOX_KEY_PREFIX + followerId;

            // 使用 ZSET 添加到收件箱，score 为时间戳
            redisTemplate.opsForZSet().add(inboxKey, postId, timestamp);

            // 3. 限制收件箱大小，只保留最新的 N 条
            trimInbox(inboxKey);
        }

        log.info("用户 {} 发布帖子 {}，已推送给 {} 个粉丝",
                post.getAuthorId(), postId, followerIds.size());
    }

    /**
     * 获取用户的收件箱（Feed 流）
     *
     * @param userId 用户ID
     * @param since 起始游标（上一页最后一条的时间戳），null 表示第一页
     * @param limit 每页大小
     * @return 帖子ID列表（按时间倒序）
     */
    public Set<String> getInbox(String userId, Long since, int limit) {
        String inboxKey = INBOX_KEY_PREFIX + userId;

        if (since == null) {
            // 第一页：获取最新的 N 条
            return redisTemplate.opsForZSet().reverseRange(inboxKey, 0, limit - 1);
        }

        // 后续页：获取时间戳小于 since 的 N 条
        return redisTemplate.opsForZSet().reverseRangeByScore(
                inboxKey,
                0,
                since - 1,
                0,
                limit
        );
    }

    /**
     * 获取收件箱大小
     */
    public Long getInboxSize(String userId) {
        return redisTemplate.opsForZSet().zCard(INBOX_KEY_PREFIX + userId);
    }

    /**
     * 限制收件箱大小，保留最新的内容
     */
    private void trimInbox(String inboxKey) {
        Long size = redisTemplate.opsForZSet().zCard(inboxKey);
        if (size != null && size > INBOX_MAX_SIZE) {
            // 移除最旧的内容
            redisTemplate.opsForZSet().removeRange(inboxKey, 0, size - INBOX_MAX_SIZE - 1);
        }
    }

    /**
     * 删除帖子（从所有收件箱中移除）
     *
     * @param postId 帖子ID
     * @param authorId 作者ID
     * @param followerIds 粉丝ID列表
     */
    public void deletePost(String postId, String authorId, Set<String> followerIds) {
        // 从用户发帖集中删除
        String postsKey = POSTS_KEY_PREFIX + authorId;
        redisTemplate.opsForZSet().remove(postsKey, postId);

        // 从所有粉丝的收件箱中删除
        for (String followerId : followerIds) {
            String inboxKey = INBOX_KEY_PREFIX + followerId;
            redisTemplate.opsForZSet().remove(inboxKey, postId);
        }

        log.info("删除帖子 {}，已从 {} 个收件箱中移除", postId, followerIds.size());
    }
}
```

```java
package com.example.feedservice.service;

import com.example.feedservice.entity.Post;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * 模拟的数据库服务
 * 实际应用中应该连接数据库
 */
@Service
public class MockDataService {

    /**
     * 模拟获取用户的粉丝列表
     */
    public Set<String> getFollowers(String userId) {
        // 模拟数据：返回 3 个粉丝
        return Set.of(
            "user:1001",
            "user:1002",
            "user:1003"
        );
    }

    /**
     * 模拟根据ID列表获取帖子
     */
    public List<Post> getPostsByIds(List<String> postIds) {
        // 模拟数据
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < postIds.size(); i++) {
            Post post = new Post();
            post.setId(postIds.get(i));
            post.setAuthorId("user:" + (i + 1));
            post.setAuthorName("用户" + (i + 1));
            post.setContent("这是第 " + (i + 1) + " 条动态内容");
            post.setCreateTime(Instant.now().minusSeconds(postIds.size() - i));
            post.setLikeCount(new Random().nextInt(100));
            post.setCommentCount(new Random().nextInt(50));
            posts.add(post);
        }
        return posts;
    }

    /**
     * 模拟创建新帖子
     */
    public Post createPost(String authorId, String content) {
        Post post = new Post();
        post.setId(UUID.randomUUID().toString());
        post.setAuthorId(authorId);
        post.setAuthorName("用户" + authorId);
        post.setContent(content);
        post.setCreateTime(Instant.now());
        return post;
    }
}
```

```java
package com.example.feedservice.controller;

import com.example.feedservice.entity.Post;
import com.example.feedservice.service.MockDataService;
import com.example.feedservice.service.PushFeedService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 推模式 Feed 流控制器
 */
@RestController
@RequestMapping("/api/feed/push")
public class PushFeedController {

    private final PushFeedService pushFeedService;
    private final MockDataService mockDataService;

    public PushFeedController(PushFeedService pushFeedService, MockDataService mockDataService) {
        this.pushFeedService = pushFeedService;
        this.mockDataService = mockDataService;
    }

    /**
     * 发布动态
     * POST /api/feed/push/publish
     */
    @PostMapping("/publish")
    public String publishPost(@RequestParam String authorId,
                              @RequestParam String content) {
        // 1. 创建帖子
        Post post = mockDataService.createPost(authorId, content);

        // 2. 获取粉丝列表
        Set<String> followers = mockDataService.getFollowers(authorId);

        // 3. 推送给所有粉丝
        pushFeedService.publishPost(post, followers);

        return "帖子已发布，已推送给 " + followers.size() + " 个粉丝";
    }

    /**
     * 获取用户 Feed 流
     * GET /api/feed/push/inbox/{userId}
     */
    @GetMapping("/inbox/{userId}")
    public List<Post> getInbox(@PathVariable String userId,
                                @RequestParam(required = false) Long since,
                                @RequestParam(defaultValue = "10") int limit) {
        // 1. 获取帖子ID列表
        Set<String> postIds = pushFeedService.getInbox(userId, since, limit);

        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }

        // 2. 根据ID获取帖子详情
        return mockDataService.getPostsByIds(List.copyOf(postIds));
    }

    /**
     * 获取收件箱大小
     */
    @GetMapping("/inbox/{userId}/size")
    public Long getInboxSize(@PathVariable String userId) {
        return pushFeedService.getInboxSize(userId);
    }
}
```

---

## 4. 实战案例2：关注列表最新动态（拉模式）

拉模式适用于大V用户或关注人数较多的场景，当用户查看 Feed 时，主动拉取关注者的最新动态。

```java
package com.example.feedservice.service;

import com.example.feedservice.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 拉模式 Feed 流服务
 *
 * 核心思想：用户查看 Feed 时，主动从关注对象的帖子列表中拉取最新内容
 *
 * Redis 数据结构：
 * - 用户发帖集：ZSET，Key 为 "user:posts:{userId}"，score 为时间戳，member 为帖子ID
 * - 关注列表：SET，Key 为 "following:{userId}"，存储用户关注的对象ID
 */
@Service
public class PullFeedService {

    private static final Logger log = LoggerFactory.getLogger(PullFeedService.class);

    /**
     * 用户发帖集 Key 前缀
     */
    private static final String USER_POSTS_KEY_PREFIX = "user:posts:";

    /**
     * 关注列表 Key 前缀
     */
    private static final String FOLLOWING_KEY_PREFIX = "following:";

    /**
     * 每次拉取每个关注对象的最新帖子数
     */
    private static final int POSTS_PER_AUTHOR = 10;

    private final StringRedisTemplate redisTemplate;

    public PullFeedService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 用户发布新帖子
     *
     * @param post 发布的帖子
     */
    public void publishPost(Post post) {
        String postsKey = USER_POSTS_KEY_PREFIX + post.getAuthorId();
        long timestamp = post.getCreateTime().toEpochMilli();

        // 将帖子添加到用户发帖集
        redisTemplate.opsForZSet().add(postsKey, post.getId(), timestamp);

        // 限制用户发帖集大小（保留最近 1000 条）
        trimUserPosts(postsKey);

        log.info("用户 {} 发布帖子 {}", post.getAuthorId(), post.getId());
    }

    /**
     * 获取用户的 Feed 流（拉取关注对象的最新帖子）
     *
     * @param userId 用户ID
     * @param limit  返回的 Feed 总数量
     * @return 合并排序后的帖子ID列表
     */
    public List<String> getFeed(String userId, int limit) {
        // 1. 获取用户的关注列表
        Set<String> following = redisTemplate.opsForSet().members(FOLLOWING_KEY_PREFIX + userId);

        if (following == null || following.isEmpty()) {
            log.info("用户 {} 未关注任何人", userId);
            return Collections.emptyList();
        }

        log.info("用户 {} 关注了 {} 个对象，开始拉取 Feed", userId, following.size());

        // 2. 从每个关注对象的发帖集中拉取最新帖子
        List<FeedItem> allItems = new ArrayList<>();

        for (String authorId : following) {
            String postsKey = USER_POSTS_KEY_PREFIX + authorId;
            Set<String> posts = redisTemplate.opsForZSet().reverseRange(postsKey, 0, POSTS_PER_AUTHOR - 1);

            if (posts != null) {
                for (String postId : posts) {
                    Double score = redisTemplate.opsForZSet().score(postsKey, postId);
                    if (score != null) {
                        allItems.add(new FeedItem(postId, score.longValue(), authorId));
                    }
                }
            }
        }

        // 3. 按时间戳倒序合并排序
        return allItems.stream()
                .sorted(Comparator.comparingLong(FeedItem::timestamp).reversed())
                .limit(limit)
                .map(FeedItem::postId)
                .collect(Collectors.toList());
    }

    /**
     * 获取增量 Feed（自指定时间以来的新帖子）
     *
     * @param userId  用户ID
     * @param since   起始时间戳（毫秒）
     * @param limit   返回数量限制
     * @return 增量帖子ID列表
     */
    public List<String> getIncrementalFeed(String userId, long since, int limit) {
        Set<String> following = redisTemplate.opsForSet().members(FOLLOWING_KEY_PREFIX + userId);

        if (following == null || following.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeedItem> allItems = new ArrayList<>();

        for (String authorId : following) {
            String postsKey = USER_POSTS_KEY_PREFIX + authorId;

            // 获取指定时间之后发布的帖子
            Set<String> posts = redisTemplate.opsForZSet().reverseRangeByScore(
                    postsKey,
                    since,
                    Double.MAX_VALUE,
                    0,
                    limit
            );

            if (posts != null) {
                for (String postId : posts) {
                    Double score = redisTemplate.opsForZSet().score(postsKey, postId);
                    if (score != null) {
                        allItems.add(new FeedItem(postId, score.longValue(), authorId));
                    }
                }
            }
        }

        return allItems.stream()
                .sorted(Comparator.comparingLong(FeedItem::timestamp).reversed())
                .limit(limit)
                .map(FeedItem::postId)
                .collect(Collectors.toList());
    }

    /**
     * 关注用户
     */
    public void follow(String userId, String targetUserId) {
        redisTemplate.opsForSet().add(FOLLOWING_KEY_PREFIX + userId, targetUserId);
        log.info("用户 {} 关注了用户 {}", userId, targetUserId);
    }

    /**
     * 取消关注
     */
    public void unfollow(String userId, String targetUserId) {
        redisTemplate.opsForSet().remove(FOLLOWING_KEY_PREFIX + userId, targetUserId);
        log.info("用户 {} 取消关注了用户 {}", userId, targetUserId);
    }

    /**
     * 获取关注列表
     */
    public Set<String> getFollowing(String userId) {
        return redisTemplate.opsForSet().members(FOLLOWING_KEY_PREFIX + userId);
    }

    /**
     * 限制用户发帖集大小
     */
    private void trimUserPosts(String postsKey) {
        Long size = redisTemplate.opsForZSet().zCard(postsKey);
        if (size != null && size > 1000) {
            redisTemplate.opsForZSet().removeRange(postsKey, 0, size - 1001);
        }
    }

    /**
     * Feed 条目封装
     */
    private record FeedItem(String postId, long timestamp, String authorId) {}
}
```

```java
package com.example.feedservice.controller;

import com.example.feedservice.entity.Post;
import com.example.feedservice.service.MockDataService;
import com.example.feedservice.service.PullFeedService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 拉模式 Feed 流控制器
 */
@RestController
@RequestMapping("/api/feed/pull")
public class PullFeedController {

    private final PullFeedService pullFeedService;
    private final MockDataService mockDataService;

    public PullFeedController(PullFeedService pullFeedService, MockDataService mockDataService) {
        this.pullFeedService = pullFeedService;
        this.mockDataService = mockDataService;
    }

    /**
     * 发布帖子（模拟）
     */
    @PostMapping("/publish")
    public String publishPost(@RequestParam String authorId,
                              @RequestParam String content) {
        Post post = mockDataService.createPost(authorId, content);
        pullFeedService.publishPost(post);
        return "帖子发布成功：" + post.getId();
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow")
    public String follow(@RequestParam String userId,
                         @RequestParam String targetUserId) {
        pullFeedService.follow(userId, targetUserId);
        return "关注成功";
    }

    /**
     * 取消关注
     */
    @PostMapping("/unfollow")
    public String unfollow(@RequestParam String userId,
                           @RequestParam String targetUserId) {
        pullFeedService.unfollow(userId, targetUserId);
        return "取消关注成功";
    }

    /**
     * 获取关注列表
     */
    @GetMapping("/following/{userId}")
    public Set<String> getFollowing(@PathVariable String userId) {
        return pullFeedService.getFollowing(userId);
    }

    /**
     * 获取用户的 Feed 流
     */
    @GetMapping("/feed/{userId}")
    public List<Post> getFeed(@PathVariable String userId,
                               @RequestParam(defaultValue = "20") int limit) {
        List<String> postIds = pullFeedService.getFeed(userId, limit);

        if (postIds.isEmpty()) {
            return List.of();
        }

        return mockDataService.getPostsByIds(postIds);
    }

    /**
     * 获取增量 Feed（自指定时间以来的新帖子）
     */
    @GetMapping("/feed/{userId}/incremental")
    public List<Post> getIncrementalFeed(@PathVariable String userId,
                                         @RequestParam long since,
                                         @RequestParam(defaultValue = "20") int limit) {
        List<String> postIds = pullFeedService.getIncrementalFeed(userId, since, limit);

        if (postIds.isEmpty()) {
            return List.of();
        }

        return mockDataService.getPostsByIds(postIds);
    }
}
```

---

## 5. 实战案例3：微博式 Feed 流

微博式 Feed 流采用推拉结合模式：
- 普通用户发微博 -> 推送给粉丝（推模式）
- 大V用户发微博 -> 不主动推送，用户拉取时合并（拉模式）
- 用户查看 Feed 时 -> 合并收件箱和大V的帖子

```java
package com.example.feedservice.service;

import com.example.feedservice.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 微博式 Feed 流服务（推拉结合模式）
 *
 * 策略：
 * 1. 普通用户（粉丝数 < threshold）发布的内容采用推模式，写入粉丝收件箱
 * 2. 大V用户（粉丝数 >= threshold）发布的内容采用拉模式，用户拉取时合并
 * 3. 用户 Feed = 收件箱内容（推模式）+ 大V内容（拉模式）
 *
 * Redis 数据结构：
 * - 收件箱：ZSET，"inbox:{userId}"
 * - 大V发帖集：ZSET，"/posts:celebrity:{userId}"
 * - 普通用户发帖集：ZSET，"posts:normal:{userId}"
 * - 用户关注的普通用户集：SET，"following:normal:{userId}"
 * - 用户关注的大V集：SET，"following:celebrity:{userId}"
 */
@Service
public class HybridFeedService {

    private static final Logger log = LoggerFactory.getLogger(HybridFeedService.class);

    // 大V阈值：粉丝数超过此值认为是大V
    private static final int CELEBRITY_THRESHOLD = 100_000;

    // 收件箱最大容量
    private static final long INBOX_MAX_SIZE = 2000;

    // 拉取每个大V的最新帖子数
    private static final int CELEBRITY_POSTS_LIMIT = 20;

    // Key 前缀
    private static final String INBOX_KEY = "inbox:";
    private static final String CELEBRITY_POSTS_KEY = "posts:celebrity:";
    private static final String NORMAL_POSTS_KEY = "posts:normal:";
    private static final String FOLLOWING_NORMAL_KEY = "following:normal:";
    private static final String FOLLOWING_CELEBRITY_KEY = "following:celebrity:";

    private final StringRedisTemplate redisTemplate;

    public HybridFeedService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发布帖子（根据作者类型选择推/拉模式）
     *
     * @param post           帖子
     * @param followerCount  粉丝数量
     * @param followerIds     粉丝ID列表
     */
    public void publishPost(Post post, long followerCount, Set<String> followerIds) {
        String postId = post.getId();
        String authorId = post.getAuthorId();
        long timestamp = post.getCreateTime().toEpochMilli();
        boolean isCelebrity = followerCount >= CELEBRITY_THRESHOLD;

        if (isCelebrity) {
            // 大V：采用拉模式，只保存到大V发帖集
            publishAsCelebrity(authorId, postId, timestamp);
            log.info("大V用户 {} 发布帖子 {}（拉模式）", authorId, postId);
        } else {
            // 普通用户：采用推模式，推送到粉丝收件箱
            publishAsNormal(postId, timestamp, followerIds);
            log.info("普通用户 {} 发布帖子 {}（推模式），推送给 {} 个粉丝",
                    authorId, postId, followerIds.size());
        }
    }

    /**
     * 大V发布帖子（拉模式）
     */
    private void publishAsCelebrity(String authorId, String postId, long timestamp) {
        String postsKey = CELEBRITY_POSTS_KEY + authorId;
        redisTemplate.opsForZSet().add(postsKey, postId, timestamp);
        trimPosts(postsKey, 500); // 大V保留更多帖子
    }

    /**
     * 普通用户发布帖子（推模式）
     */
    private void publishAsNormal(String postId, long timestamp, Set<String> followerIds) {
        String postsKey = NORMAL_POSTS_KEY + postId.split(":")[0]; // 简化：作者ID从postId获取

        for (String followerId : followerIds) {
            String inboxKey = INBOX_KEY + followerId;
            redisTemplate.opsForZSet().add(inboxKey, postId, timestamp);
            trimInbox(inboxKey);
        }
    }

    /**
     * 获取用户的完整 Feed 流
     *
     * @param userId 用户ID
     * @param cursor 游标（上一页最后一条的时间戳），null 表示第一页
     * @param limit  每页大小
     * @return Feed 列表
     */
    public FeedResult getFeed(String userId, Long cursor, int limit) {
        // 1. 获取收件箱内容（推模式）
        List<FeedItem> inboxItems = getInboxItems(userId, cursor, limit);

        // 2. 获取关注的大V的最新帖子（拉模式）
        List<FeedItem> celebrityItems = getCelebrityItems(userId, cursor, limit);

        // 3. 合并两个列表
        List<FeedItem> allItems = new ArrayList<>();
        allItems.addAll(inboxItems);
        allItems.addAll(celebrityItems);

        // 4. 按时间倒序排序
        allItems.sort(Comparator.comparingLong(FeedItem::timestamp).reversed());

        // 5. 限制返回数量
        boolean hasMore = allItems.size() > limit;
        List<FeedItem> pageItems = allItems.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // 6. 计算下一页游标
        Long nextCursor = null;
        if (!pageItems.isEmpty() && hasMore) {
            nextCursor = pageItems.get(pageItems.size() - 1).timestamp();
        }

        log.info("用户 {} 获取 Feed，共 {} 条（收件箱:{}，大V:{}），hasMore:{}",
                userId, pageItems.size(), inboxItems.size(), celebrityItems.size(), hasMore);

        return new FeedResult(pageItems, nextCursor, hasMore);
    }

    /**
     * 获取收件箱条目
     */
    private List<FeedItem> getInboxItems(String userId, Long cursor, int limit) {
        String inboxKey = INBOX_KEY + userId;

        Set<String> postIds;
        if (cursor == null) {
            postIds = redisTemplate.opsForZSet().reverseRange(inboxKey, 0, limit - 1);
        } else {
            postIds = redisTemplate.opsForZSet().reverseRangeByScore(
                    inboxKey,
                    0,
                    cursor - 1,
                    0,
                    limit
            );
        }

        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeedItem> items = new ArrayList<>();
        for (String postId : postIds) {
            Double score = redisTemplate.opsForZSet().score(inboxKey, postId);
            if (score != null) {
                items.add(new FeedItem(postId, score.longValue(), FeedSource.INBOX));
            }
        }
        return items;
    }

    /**
     * 获取关注的大V的最新帖子
     */
    private List<FeedItem> getCelebrityItems(String userId, Long cursor, int limit) {
        Set<String> celebrities = redisTemplate.opsForSet().members(FOLLOWING_CELEBRITY_KEY + userId);

        if (celebrities == null || celebrities.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeedItem> allItems = new ArrayList<>();

        for (String celebrityId : celebrities) {
            String postsKey = CELEBRITY_POSTS_KEY + celebrityId;

            Set<String> postIds;
            if (cursor == null) {
                postIds = redisTemplate.opsForZSet().reverseRange(postsKey, 0, CELEBRITY_POSTS_LIMIT - 1);
            } else {
                postIds = redisTemplate.opsForZSet().reverseRangeByScore(
                        postsKey,
                        0,
                        cursor - 1,
                        0,
                        CELEBRITY_POSTS_LIMIT
                );
            }

            if (postIds != null) {
                for (String postId : postIds) {
                    Double score = redisTemplate.opsForZSet().score(postsKey, postId);
                    if (score != null) {
                        allItems.add(new FeedItem(postId, score.longValue(), FeedSource.CELEBRITY));
                    }
                }
            }
        }

        return allItems;
    }

    /**
     * 关注用户
     */
    public void follow(String userId, String targetUserId, boolean isCelebrity) {
        String key = isCelebrity
                ? FOLLOWING_CELEBRITY_KEY + userId
                : FOLLOWING_NORMAL_KEY + userId;
        redisTemplate.opsForSet().add(key, targetUserId);
        log.info("用户 {} 关注了 {}（{}）", userId, targetUserId, isCelebrity ? "大V" : "普通用户");
    }

    /**
     * 取消关注
     */
    public void unfollow(String userId, String targetUserId, boolean isCelebrity) {
        String key = isCelebrity
                ? FOLLOWING_CELEBRITY_KEY + userId
                : FOLLOWING_NORMAL_KEY + userId;
        redisTemplate.opsForSet().remove(key, targetUserId);
        log.info("用户 {} 取消关注了 {}", userId, targetUserId);
    }

    /**
     * 限制收件箱大小
     */
    private void trimInbox(String inboxKey) {
        Long size = redisTemplate.opsForZSet().zCard(inboxKey);
        if (size != null && size > INBOX_MAX_SIZE) {
            redisTemplate.opsForZSet().removeRange(inboxKey, 0, size - INBOX_MAX_SIZE - 1);
        }
    }

    /**
     * 限制发帖集大小
     */
    private void trimPosts(String postsKey, int maxSize) {
        Long size = redisTemplate.opsForZSet().zCard(postsKey);
        if (size != null && size > maxSize) {
            redisTemplate.opsForZSet().removeRange(postsKey, 0, size - maxSize - 1);
        }
    }

    /**
     * Feed 来源枚举
     */
    private enum FeedSource {
        INBOX,    // 来自收件箱（推模式）
        CELEBRITY // 来自大V（拉模式）
    }

    /**
     * Feed 条目
     */
    private record FeedItem(String postId, long timestamp, FeedSource source) {}

    /**
     * Feed 结果封装
     */
    public record FeedResult(
        List<FeedItem> items,
        Long nextCursor,
        boolean hasMore
    ) {}
}
```

```java
package com.example.feedservice.controller;

import com.example.feedservice.entity.Post;
import com.example.feedservice.service.HybridFeedService;
import com.example.feedservice.service.MockDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 微博式 Feed 流控制器（推拉结合模式）
 */
@RestController
@RequestMapping("/api/feed/hybrid")
public class HybridFeedController {

    private final HybridFeedService hybridFeedService;
    private final MockDataService mockDataService;

    public HybridFeedController(HybridFeedService hybridFeedService, MockDataService mockDataService) {
        this.hybridFeedService = hybridFeedService;
        this.mockDataService = mockDataService;
    }

    /**
     * 发布帖子
     */
    @PostMapping("/publish")
    public String publishPost(@RequestParam String authorId,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "1000") long followerCount) {
        Post post = mockDataService.createPost(authorId, content);

        // 模拟获取粉丝列表（实际应从数据库获取）
        var followers = mockDataService.getFollowers(authorId);

        hybridFeedService.publishPost(post, followerCount, followers);

        String userType = followerCount >= 100_000 ? "大V" : "普通用户";
        return String.format("%s %s 发布帖子成功：%s", userType, authorId, post.getId());
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow")
    public String follow(@RequestParam String userId,
                          @RequestParam String targetUserId,
                          @RequestParam(defaultValue = "false") boolean isCelebrity) {
        hybridFeedService.follow(userId, targetUserId, isCelebrity);
        return "关注成功";
    }

    /**
     * 取消关注
     */
    @PostMapping("/unfollow")
    public String unfollow(@RequestParam String userId,
                            @RequestParam String targetUserId,
                            @RequestParam(defaultValue = "false") boolean isCelebrity) {
        hybridFeedService.unfollow(userId, targetUserId, isCelebrity);
        return "取消关注成功";
    }

    /**
     * 获取 Feed 流
     */
    @GetMapping("/feed/{userId}")
    public FeedResponse getFeed(@PathVariable String userId,
                                 @RequestParam(required = false) Long cursor,
                                 @RequestParam(defaultValue = "20") int limit) {
        HybridFeedService.FeedResult result = hybridFeedService.getFeed(userId, cursor, limit);

        if (result.items().isEmpty()) {
            return new FeedResponse(List.of(), null, false);
        }

        // 获取帖子详情
        List<String> postIds = result.items().stream()
                .map(HybridFeedService.FeedItem::postId)
                .toList();

        List<Post> posts = mockDataService.getPostsByIds(postIds);

        return new FeedResponse(posts, result.nextCursor(), result.hasMore());
    }

    /**
     * Feed 响应封装
     */
    public record FeedResponse(
        List<Post> posts,
        Long nextCursor,
        boolean hasMore
    ) {}
}
```

---

## 6. 去重与已读标记

在 Feed 流系统中，去重和已读标记是常见需求。

```java
package com.example.feedservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Feed 流去重与已读标记服务
 *
 * 功能：
 * 1. 内容去重：使用 SET 存储已推送过的内容ID
 * 2. 已读标记：使用 SET 存储用户已读的内容ID
 * 3. 未读计数：快速获取用户的未读消息数量
 */
@Service
public class FeedDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(FeedDeduplicationService.class);

    // 已推送内容集合 Key 前缀
    private static final String PUSHED_CONTENT_KEY = "pushed:";

    // 用户已读内容集合 Key 前缀
    private static final String READ_CONTENT_KEY = "read:";

    // 用户未读计数 Key 前缀
    private static final String UNREAD_COUNT_KEY = "unread:";

    // 内容去重集合有效期（7天）
    private static final Duration CONTENT_EXPIRE_DAYS = Duration.ofDays(7);

    // 已读标记有效期（30天）
    private static final Duration READ_EXPIRE_DAYS = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    public FeedDeduplicationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查内容是否已推送过（去重）
     *
     * @param contentId 内容ID
     * @return true 表示已推送过，false 表示新内容
     */
    public boolean isContentPushed(String contentId) {
        String key = PUSHED_CONTENT_KEY + contentId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, contentId);
        return Boolean.TRUE.equals(isMember);
    }

    /**
     * 标记内容已推送（去重）
     *
     * @param contentId 内容ID
     */
    public void markAsPushed(String contentId) {
        String key = PUSHED_CONTENT_KEY + contentId;
        redisTemplate.opsForSet().add(key, contentId);
        // 设置过期时间
        redisTemplate.expire(key, CONTENT_EXPIRE_DAYS);
    }

    /**
     * 推送内容前检查是否重复
     *
     * @param contentId 内容ID
     * @return true 表示是新内容（可以推送），false 表示已重复
     */
    public boolean tryMarkAsPushed(String contentId) {
        // 使用 SETNX 原子操作
        String key = PUSHED_CONTENT_KEY + contentId;
        Boolean result = redisTemplate.opsForSet().add(key, contentId);

        if (Boolean.TRUE.equals(result) && result == 1) {
            // 添加成功，说明是新内容
            redisTemplate.expire(key, CONTENT_EXPIRE_DAYS);
            return true;
        }
        return false;
    }

    /**
     * 检查用户是否已读某内容
     *
     * @param userId    用户ID
     * @param contentId 内容ID
     * @return true 表示已读，false 表示未读
     */
    public boolean isContentRead(String userId, String contentId) {
        String key = READ_CONTENT_KEY + userId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, contentId);
        return Boolean.TRUE.equals(isMember);
    }

    /**
     * 标记内容为已读
     *
     * @param userId    用户ID
     * @param contentId 内容ID
     */
    public void markAsRead(String userId, String contentId) {
        String key = READ_CONTENT_KEY + userId;
        redisTemplate.opsForSet().add(key, contentId);
        redisTemplate.expire(key, READ_EXPIRE_DAYS);

        // 减少未读计数
        String unreadKey = UNREAD_COUNT_KEY + userId;
        redisTemplate.opsForValue().decrement(unreadKey);

        log.debug("用户 {} 已读内容 {}", userId, contentId);
    }

    /**
     * 批量标记内容为已读
     *
     * @param userId     用户ID
     * @param contentIds 内容ID列表
     * @return 实际标记成功的数量
     */
    public int markAsReadBatch(String userId, Set<String> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return 0;
        }

        String key = READ_CONTENT_KEY + userId;
        Long count = redisTemplate.opsForSet().add(key, contentIds.toArray(new String[0]));
        redisTemplate.expire(key, READ_EXPIRE_DAYS);

        // 减少未读计数
        String unreadKey = UNREAD_COUNT_KEY + userId;
        if (count != null && count > 0) {
            redisTemplate.opsForValue().decrement(unreadKey, count);
        }

        log.info("用户 {} 批量已读 {} 条内容", userId, count);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 获取用户已读内容列表
     *
     * @param userId 用户ID
     * @return 已读内容ID列表
     */
    public Set<String> getReadContent(String userId) {
        String key = READ_CONTENT_KEY + userId;
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 获取用户未读数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    public Long getUnreadCount(String userId) {
        String key = UNREAD_COUNT_KEY + userId;
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0L;
    }

    /**
     * 增加未读计数
     *
     * @param userId 用户ID
     * @param delta  增加的数量
     */
    public void incrementUnreadCount(String userId, long delta) {
        String key = UNREAD_COUNT_KEY + userId;
        redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 重置未读计数
     *
     * @param userId 用户ID
     */
    public void resetUnreadCount(String userId) {
        String key = UNREAD_COUNT_KEY + userId;
        redisTemplate.opsForValue().set(key, "0");
    }

    /**
     * 清除用户已读历史（可选操作）
     *
     * @param userId 用户ID
     */
    public void clearReadHistory(String userId) {
        String key = READ_CONTENT_KEY + userId;
        redisTemplate.delete(key);
        log.info("清除用户 {} 的已读历史", userId);
    }
}
```

```java
package com.example.feedservice.service;

import com.example.feedservice.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 整合去重和已读功能的完整 Feed 流服务
 *
 * 特性：
 * 1. 发布时去重：使用 BloomFilter 或 SET 检查内容是否已推送
 * 2. 获取时过滤：返回 Feed 时过滤掉已读内容
 * 3. 已读标记：支持单条和批量标记已读
 * 4. 未读计数：维护用户的未读消息计数
 */
@Service
public class CompleteFeedService {

    private static final Logger log = LoggerFactory.getLogger(CompleteFeedService.class);

    // 收件箱 Key 前缀
    private static final String INBOX_KEY = "inbox:";

    // 已读内容 Key 前缀
    private static final String READ_KEY = "read:";

    // 未读计数 Key 前缀
    private static final String UNREAD_KEY = "unread:";

    // 去重 Key 前缀
    private static final String DEDUP_KEY = "dedup:";

    private final StringRedisTemplate redisTemplate;
    private final FeedDeduplicationService deduplicationService;

    public CompleteFeedService(StringRedisTemplate redisTemplate,
                                 FeedDeduplicationService deduplicationService) {
        this.redisTemplate = redisTemplate;
        this.deduplicationService = deduplicationService;
    }

    /**
     * 发布内容（带去重）
     *
     * @param post        帖子
     * @param followerIds 粉丝ID列表
     * @return true 表示发布成功，false 表示内容重复
     */
    public boolean publishWithDeduplication(Post post, Set<String> followerIds) {
        String postId = post.getId();

        // 1. 检查是否已推送过（去重）
        if (!deduplicationService.tryMarkAsPushed(postId)) {
            log.info("内容 {} 已推送过，跳过", postId);
            return false;
        }

        // 2. 推送到所有粉丝的收件箱
        String inboxKeyPrefix = INBOX_KEY;
        long timestamp = post.getCreateTime().toEpochMilli();

        for (String followerId : followerIds) {
            String inboxKey = inboxKeyPrefix + followerId;
            redisTemplate.opsForZSet().add(inboxKey, postId, timestamp);

            // 增加未读计数（如果未读计数已初始化）
            if (hasUnreadCounter(followerId)) {
                deduplicationService.incrementUnreadCount(followerId, 1);
            }
        }

        log.info("内容 {} 已发布，推送给 {} 个粉丝", postId, followerIds.size());
        return true;
    }

    /**
     * 获取用户的 Feed 流（自动过滤已读）
     *
     * @param userId      用户ID
     * @param cursor      游标
     * @param limit       每页大小
     * @param filterRead  是否过滤已读内容
     * @return Feed 内容列表
     */
    public List<FeedEntry> getFeed(String userId, Long cursor, int limit, boolean filterRead) {
        String inboxKey = INBOX_KEY + userId;

        // 1. 获取收件箱内容
        Set<String> postIds;
        if (cursor == null) {
            postIds = redisTemplate.opsForZSet().reverseRange(inboxKey, 0, limit * 2 - 1); // 多取一些用于过滤
        } else {
            postIds = redisTemplate.opsForZSet().reverseRangeByScore(
                    inboxKey, 0, cursor - 1, 0, limit * 2
            );
        }

        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取已读内容集合
        Set<String> readContentIds = filterRead
                ? deduplicationService.getReadContent(userId)
                : Collections.emptySet();

        // 3. 构建 Feed 条目，过滤已读
        List<FeedEntry> entries = new ArrayList<>();
        for (String postId : postIds) {
            // 过滤已读
            if (filterRead && readContentIds.contains(postId)) {
                continue;
            }

            Double score = redisTemplate.opsForZSet().score(inboxKey, postId);
            if (score != null) {
                entries.add(new FeedEntry(postId, score.longValue()));
            }

            if (entries.size() >= limit) {
                break;
            }
        }

        return entries;
    }

    /**
     * 标记内容已读
     *
     * @param userId    用户ID
     * @param contentId 内容ID
     */
    public void markAsRead(String userId, String contentId) {
        deduplicationService.markAsRead(userId, contentId);
    }

    /**
     * 批量标记已读
     *
     * @param userId     用户ID
     * @param contentIds 内容ID列表
     */
    public void markAsReadBatch(String userId, Set<String> contentIds) {
        deduplicationService.markAsReadBatch(userId, contentIds);
    }

    /**
     * 获取未读数量
     */
    public Long getUnreadCount(String userId) {
        return deduplicationService.getUnreadCount(userId);
    }

    /**
     * 初始化用户的未读计数（首次使用收件箱时调用）
     */
    public void initUnreadCount(String userId) {
        String inboxKey = INBOX_KEY + userId;
        Long size = redisTemplate.opsForZSet().zCard(inboxKey);
        deduplicationService.resetUnreadCount(userId);
        if (size != null && size > 0) {
            deduplicationService.incrementUnreadCount(userId, size);
        }
    }

    /**
     * 检查用户是否有未读计数
     */
    private boolean hasUnreadCounter(String userId) {
        String key = UNREAD_KEY + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Feed 条目
     */
    public record FeedEntry(
        String postId,
        long timestamp
    ) {}
}
```

---

## 7. 总结

### 架构对比

| 模式 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| 拉模式（Pull） | 大V用户、关注人数多 | 写操作简单 | 读操作复杂，延迟高 |
| 推模式（Push） | 普通用户、粉丝人数少 | 读操作简单，延迟低 | 写操作复杂，可能产生大量写入 |
| 推拉结合 | 混合场景（微博式） | 平衡读写压力 | 实现复杂度高 |

### Redis 数据结构选择

| 功能 | Redis 数据结构 | Key 模式 | 说明 |
|------|---------------|---------|------|
| 时间线 | ZSET | `timeline:{userId}` | score=时间戳，member=内容ID |
| 收件箱 | ZSET | `inbox:{userId}` | 推模式使用 |
| 用户发帖集 | ZSET | `posts:{userId}` | 存储用户发布的帖子 |
| 关注列表 | SET | `following:{userId}` | 存储关注对象ID |
| 已读标记 | SET | `read:{userId}` | 存储已读内容ID |
| 去重 | SET | `pushed:{contentId}` | 全局内容去重 |

### 分页策略

1. **回屏分页（Offset-Based）**：适用于固定页面大小，但数据可能重复或遗漏
2. **游标分页（Cursor-Based）**：基于时间戳，提供更好的实时性，推荐使用

### 性能优化建议

1. **收件箱容量限制**：使用 `ZREMRANGEBYRANK` 限制大小，防止内存溢出
2. **异步推送**：对于大V的推送操作，使用消息队列异步处理
3. **分层存储**：热门内容使用推模式，冷门内容使用拉模式
4. **缓存预热**：大V用户的内容可以提前缓存到 CDN

### 扩展阅读

- [Redis 基础篇](../Redis基础篇.md)
- [Redis 高级篇](../Redis高级篇.md)
- [Redis 实战大纲](../Redis实战大纲.md)
