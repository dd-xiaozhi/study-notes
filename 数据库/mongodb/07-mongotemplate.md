---
title: 第7章 MongoTemplate 高级操作
sidebar_label: 第7章
---

# 第7章 MongoTemplate 高级操作

## 7.1 MongoTemplate 简介

MongoTemplate 是 Spring Data MongoDB 提供的核心类，提供了操作 MongoDB 的底层方法。相比 MongoRepository，MongoTemplate 提供了更灵活、更强大的操作能力。

### 7.1.1 MongoTemplate vs Repository

| 特性 | MongoTemplate | MongoRepository |
|------|---------------|-----------------|
| 灵活性和控制力 | 高，完全控制查询和操作 | 中等，依赖方法名约定 |
| 查询复杂度 | 支持复杂查询、聚合、批量操作 | 支持基本 CRUD 和简单查询 |
| 代码量 | 需要编写更多代码 | 代码简洁 |
| 性能优化 | 可精细控制 | 自动化处理 |
| 学习曲线 | 较陡 | 较平缓 |
| 适用场景 | 复杂业务逻辑、动态查询 | 标准 CRUD 操作 |

### 7.1.2 MongoTemplate 配置

**方式一：自动配置（Spring Boot 自动配置）**

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
```

**方式二：Java 配置类**

```java
package com.example.mongo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {
        String connectionString = "mongodb://localhost:27017/mydb";
        return MongoClients.create(connectionString);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, "mydb");
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}
```

**方式三：带认证的配置**

```java
@Configuration
public class MongoConfigWithAuth {

    @Bean
    public MongoClient mongoClient() {
        String connectionString = "mongodb://username:password@localhost:27017/mydb?authSource=admin";
        return MongoClients.create(connectionString);
    }
}
```

---

## 7.2 MongoTemplate 基础操作

### 7.2.1 实体类定义

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String email;

    private Integer age;

    private String status;

    private List<String> roles;

    private Map<String, Object> profile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 省略 getter/setter/toString
}
```

### 7.2.2 插入操作 (insert)

**插入单个文档**

```java
@Autowired
private MongoTemplate mongoTemplate;

public void insertUser() {
    User user = new User();
    user.setUsername("zhangsan");
    user.setEmail("zhangsan@example.com");
    user.setAge(25);
    user.setStatus("ACTIVE");
    user.setRoles(Arrays.asList("USER", "ADMIN"));
    user.setCreatedAt(LocalDateTime.now());

    // 插入并返回主键
    User savedUser = mongoTemplate.insert(user);
    System.out.println("插入的用户ID: " + savedUser.getId());
}
```

**批量插入**

```java
public void insertUsers() {
    List<User> users = Arrays.asList(
        createUser("user1", "user1@example.com", 20),
        createUser("user2", "user2@example.com", 22),
        createUser("user3", "user3@example.com", 24)
    );

    // 批量插入
    List<User> savedUsers = mongoTemplate.insert(users, User.class);
    System.out.println("插入数量: " + savedUsers.size());
}

private User createUser(String username, String email, Integer age) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setAge(age);
    user.setStatus("ACTIVE");
    user.setCreatedAt(LocalDateTime.now());
    return user;
}
```

### 7.2.3 查询操作

**根据 ID 查询**

```java
public void findByIdExample() {
    String userId = "507f1f77bcf86cd799439011";

    // 根据 ID 查询
    User user = mongoTemplate.findById(userId, User.class);
    if (user != null) {
        System.out.println("找到用户: " + user.getUsername());
    } else {
        System.out.println("用户不存在");
    }
}
```

**查询所有**

```java
public void findAllExample() {
    // 查询集合中所有文档
    List<User> users = mongoTemplate.findAll(User.class);
    users.forEach(user -> System.out.println(user.getUsername()));
}
```

**条件查询**

```java
public void findByCondition() {
    // 查询年龄大于 20 的用户
    Query query = new Query(Criteria.where("age").gt(20));
    List<User> users = mongoTemplate.find(query, User.class);
    users.forEach(user -> System.out.println(user.getUsername()));
}
```

### 7.2.4 更新操作

**更新单个文档**

```java
public void updateUser() {
    String userId = "507f1f77bcf86cd799439011";

    // 查询条件
    Query query = new Query(Criteria.where("_id").is(userId));

    // 更新内容
    Update update = new Update()
        .set("email", "newemail@example.com")
        .set("updatedAt", LocalDateTime.now())
        .inc("age", 1);  // 年龄 +1

    UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);
    System.out.println("修改文档数: " + result.getModifiedCount());
}
```

**批量更新**

```java
public void updateUsers() {
    // 查询条件：状态为 INACTIVE 的用户
    Query query = new Query(Criteria.where("status").is("INACTIVE"));

    // 更新内容
    Update update = new Update()
        .set("status", "ACTIVE")
        .set("updatedAt", LocalDateTime.now());

    UpdateResult result = mongoTemplate.updateMulti(query, update, User.class);
    System.out.println("修改文档数: " + result.getModifiedCount());
}
```

**Upsert 操作（不存在则插入）**

```java
public void upsertUser() {
    Query query = new Query(Criteria.where("username").is("newuser"));

    Update update = new Update()
        .set("username", "newuser")
        .set("email", "newuser@example.com")
        .set("age", 30)
        .set("status", "ACTIVE")
        .setOnInsert("createdAt", LocalDateTime.now());

    UpdateResult result = mongoTemplate.upsert(query, update, User.class);
    System.out.println("Upsert 结果: " + result.getUpsertedId());
}
```

### 7.2.5 删除操作

**删除单个文档**

```java
public void deleteUser() {
    String userId = "507f1f77bcf86cd799439011";

    Query query = new Query(Criteria.where("_id").is(userId));
    DeleteResult result = mongoTemplate.remove(query, User.class);
    System.out.println("删除文档数: " + result.getDeletedCount());
}
```

**删除所有匹配文档**

```java
public void deleteUsers() {
    Query query = new Query(Criteria.where("status").is("DELETED"));
    DeleteResult result = mongoTemplate.remove(query, User.class);
    System.out.println("删除文档数: " + result.getDeletedCount());
}
```

---

## 7.3 Query 和 Criteria 构建复杂查询

### 7.3.1 Criteria 条件构建

```java
// 等值查询
Criteria.where("username").is("zhangsan")

// 不等于
Criteria.where("username").ne("zhangsan")

// 大于
Criteria.where("age").gt(20)

// 大于等于
Criteria.where("age").gte(20)

// 小于
Criteria.where("age").lt(30)

// 小于等于
Criteria.where("age").lte(30)

// 范围查询
Criteria.where("age").gte(20).lte(30)

// 模糊查询
Criteria.where("username").regex("^zhang")

// 包含（在数组中）
Criteria.where("roles").in("ADMIN", "USER")

// 不包含
Criteria.where("roles").nin("GUEST")

// 存在性检查
Criteria.where("email").exists(true)

// NULL 检查
Criteria.where("email").isnull()
Criteria.where("email").ne(null)

// 字符串开头/结尾
Criteria.where("email").regex("^admin")
Criteria.where("email").regex("example.com$")
```

### 7.3.2 逻辑运算

**AND 条件**

```java
// 方式一：链式调用（默认就是 AND）
Criteria criteria = Criteria.where("age").gt(20)
                           .and("status").is("ACTIVE");

// 方式二：使用 andOperator
Criteria criteria = new Criteria().andOperator(
    Criteria.where("age").gt(20),
    Criteria.where("status").is("ACTIVE")
);
```

**OR 条件**

```java
// 使用 orOperator
Criteria criteria = new Criteria().orOperator(
    Criteria.where("username").is("zhangsan"),
    Criteria.where("email").is("zhangsan@example.com"),
    Criteria.where("age").lt(25)
);
```

**组合 AND 和 OR**

```java
public void complexQuery() {
    // 查找 age > 20 且 (status = 'ACTIVE' 或 username = 'admin')
    Criteria criteria = new Criteria().andOperator(
        Criteria.where("age").gt(20),
        new Criteria().orOperator(
            Criteria.where("status").is("ACTIVE"),
            Criteria.where("username").is("admin")
        )
    );

    Query query = new Query(criteria);
    List<User> users = mongoTemplate.find(query, User.class);
}
```

### 7.3.3 Query 高级用法

**投影（只查询指定字段）**

```java
public void projectionQuery() {
    Query query = new Query();
    query.fields().include("username", "email");  // 只包含指定字段
    query.fields().exclude("_id");                // 排除 ID

    List<User> users = mongoTemplate.find(query, User.class);
    users.forEach(user -> {
        // 只有 username 和 email 有值，其他为 null
        System.out.println(user.getUsername() + ": " + user.getEmail());
    });
}
```

**排序**

```java
public void sortQuery() {
    Query query = new Query();
    // 按 age 降序，按 createdAt 升序
    query.with(Sort.by(
        Sort.Order.desc("age"),
        Sort.Order.asc("createdAt")
    ));

    List<User> users = mongoTemplate.find(query, User.class);
}
```

**分页查询**

```java
public void pageableQuery() {
    int page = 0;  // 第几页（从 0 开始）
    int size = 10; // 每页大小

    Query query = new Query();
    query.with(Sort.by(Sort.Order.desc("createdAt")));

    // 获取总数
    long total = mongoTemplate.count(query, User.class);

    // 分页
    query.skip(page * size).limit(size);
    List<User> users = mongoTemplate.find(query, User.class);

    System.out.println("总数: " + total);
    System.out.println("当前页: " + page);
    System.out.println("每页大小: " + size);
}
```

**使用 Pageable 分页**

```java
public void pageableQuery2() {
    int page = 0;
    int size = 10;

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
    Query query = new Query(pageable);

    List<User> users = mongoTemplate.find(query, User.class);
}
```

### 7.3.4 嵌套文档查询

**嵌套文档结构示例**

```json
{
  "_id": "1",
  "username": "zhangsan",
  "profile": {
    "name": "张三",
    "phone": "13800138000",
    "address": {
      "city": "北京",
      "district": "朝阳区"
    }
  }
}
```

**查询嵌套字段**

```java
public void nestedQuery() {
    // 查询 profile.name 为 "张三" 的用户
    Query query = new Query(Criteria.where("profile.name").is("张三"));

    // 查询地址城市为 "北京" 的用户
    Query query2 = new Query(Criteria.where("profile.address.city").is("北京"));

    List<User> users = mongoTemplate.find(query, User.class);
}
```

### 7.3.5 数组查询

**数组精确匹配**

```java
public void arrayQuery() {
    // 查询 roles 数组正好等于 ["USER", "ADMIN"] 的用户
    Query query = new Query(Criteria.where("roles").is(Arrays.asList("USER", "ADMIN")));
    List<User> users = mongoTemplate.find(query, User.class);
}
```

**数组包含查询**

```java
public void arrayContainsQuery() {
    // 查询 roles 数组包含 "ADMIN" 的用户
    Query query = new Query(Criteria.where("roles").all(Arrays.asList("ADMIN")));
    List<User> users = mongoTemplate.find(query, User.class);
}
```

**数组元素查询**

```java
public void arrayElementQuery() {
    // 查询数组长度
    Query query = new Query(Criteria.where("roles").size(2));

    // 查询数组至少包含某些元素
    Query query2 = new Query(Criteria.where("roles").all(Arrays.asList("USER", "ADMIN")));

    List<User> users = mongoTemplate.find(query, User.class);
}
```

### 7.3.6 完整查询示例

```java
@Service
public class UserQueryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 复杂条件查询示例
     * 条件：
     * - age >= 18 且 age <= 60
     * - status 为 ACTIVE 或 VIP
     * - username 以 "test" 开头或 email 包含 "example"
     * - 按 age 降序排列
     * - 分页查询，每页 20 条
     */
    public Page<User> complexSearch(UserSearchDTO searchDTO) {
        // 构建查询条件
        List<Criteria> criteriaList = new ArrayList<>();

        // 年龄范围
        if (searchDTO.getMinAge() != null) {
            criteriaList.add(Criteria.where("age").gte(searchDTO.getMinAge()));
        }
        if (searchDTO.getMaxAge() != null) {
            criteriaList.add(Criteria.where("age").lte(searchDTO.getMaxAge()));
        }

        // 状态列表
        if (searchDTO.getStatuses() != null && !searchDTO.getStatuses().isEmpty()) {
            criteriaList.add(Criteria.where("status").in(searchDTO.getStatuses()));
        }

        // 用户名或邮箱条件
        if (StringUtils.hasText(searchDTO.getKeyword())) {
            Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("username").regex("^" + searchDTO.getKeyword()),
                Criteria.where("email").regex(searchDTO.getKeyword())
            );
            criteriaList.add(keywordCriteria);
        }

        // 组合所有条件
        Criteria criteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 构建 Query
        Query query = new Query(criteria);

        // 排序
        if (StringUtils.hasText(searchDTO.getSortBy())) {
            Sort.Direction direction = searchDTO.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
            query.with(Sort.by(direction, searchDTO.getSortBy()));
        } else {
            query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        // 查询总数
        long total = mongoTemplate.count(query, User.class);

        // 分页
        int page = searchDTO.getPage();
        int size = searchDTO.getSize();
        query.skip(page * size).limit(size);

        // 执行查询
        List<User> users = mongoTemplate.find(query, User.class);

        // 返回分页结果
        return new PageImpl<>(users, PageRequest.of(page, size), total);
    }

    /**
     * 根据用户名模糊搜索（自动补全）
     */
    public List<String> autocomplete(String prefix, int limit) {
        Query query = new Query(Criteria.where("username").regex("^" + prefix, "i"));
        query.fields().include("username");
        query.limit(limit);

        List<User> users = mongoTemplate.find(query, User.class);
        return users.stream()
            .map(User::getUsername)
            .collect(Collectors.toList());
    }
}
```

---

## 7.4 Update 构建更新操作

### 7.4.1 常用更新操作

```java
// 设置字段值
Update.update("field", value)

// 递增/递减
Update.inc("field", delta)
Update.inc("age", 1)      // age + 1
Update.inc("age", -1)     // age - 1

// 重命名字段
Update.rename("oldName", "newName")

// 删除字段
Update.unset("field")

// 乘以指定值
Update.mul("field", multiplier)

// 日期操作
Update.currentDate("field")           // 设置为当前日期
Update.currentTimestamp("field")      // 设置为当前时间戳

// 数组操作
Update.push("arrayField", value)               // 追加元素
Update.pushAll("arrayField", values)           // 追加多个元素
Update.addToSet("arrayField", value)           // 去重追加
Update.pop("arrayField", Update.Position.FIRST) // 删除第一个元素
Update.pop("arrayField", Update.Position.LAST)  // 删除最后一个元素
Update.pull("arrayField", value)               // 删除指定元素
Update.pullAll("arrayField", values)            // 删除多个指定元素
```

### 7.4.2 数组更新示例

```java
public void arrayUpdateExample() {
    String userId = "507f1f77bcf86cd799439011";

    // 1. 追加元素到数组
    Query query1 = new Query(Criteria.where("_id").is(userId));
    Update update1 = new Update().push("roles", "SUPERADMIN");
    mongoTemplate.updateFirst(query1, update1, User.class);

    // 2. 去重追加（如果不存在才添加）
    Update update2 = new Update().addToSet("roles", "SUPERADMIN");
    mongoTemplate.updateFirst(query1, update2, User.class);

    // 3. 从数组中删除元素
    Update update3 = new Update().pull("roles", "GUEST");
    mongoTemplate.updateFirst(query1, update3, User.class);

    // 4. 删除数组的第一个或最后一个元素
    Update update4 = new Update().pop("roles", Update.Position.FIRST);
    mongoTemplate.updateFirst(query1, update4, User.class);
}
```

### 7.4.3 嵌套文档更新

```java
public void nestedUpdateExample() {
    String userId = "507f1f77bcf86cd799439011";
    Query query = new Query(Criteria.where("_id").is(userId));

    // 更新嵌套字段
    Update update = new Update()
        .set("profile.name", "新名字")
        .set("profile.phone", "13900139000")
        .set("profile.address.city", "上海");

    mongoTemplate.updateFirst(query, update, User.class);
}
```

### 7.4.4 条件更新

```java
public void conditionalUpdate() {
    // 只更新匹配条件的文档
    Query query = new Query(
        Criteria.where("age").gte(18)
                .and("status").is("ACTIVE")
    );

    Update update = new Update()
        .set("status", "VERIFIED")
        .currentDate("updatedAt");

    UpdateResult result = mongoTemplate.updateMulti(query, update, User.class);
    System.out.println("更新了 " + result.getModifiedCount() + " 个文档");
}
```

### 7.4.5 更新并返回更新后的文档

```java
public void findAndModify() {
    String userId = "507f1f77bcf86cd799439011";
    Query query = new Query(Criteria.where("_id").is(userId));

    Update update = new Update()
        .inc("loginCount", 1)
        .set("lastLoginAt", LocalDateTime.now());

    // FindAndModifyOptions：配置返回选项
    FindAndModifyOptions options = FindAndModifyOptions.options()
        .returnNew(true)  // 返回更新后的文档
        .upsert(false);  // 不存在则不插入

    User user = mongoTemplate.findAndModify(query, update, options, User.class);
    System.out.println("更新后的用户: " + user.getUsername());
}
```

### 7.4.6 完整更新服务示例

```java
@Service
public class UserUpdateService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新用户信息
     */
    public boolean updateUserInfo(String userId, UserUpdateDTO updateDTO) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update();

        if (updateDTO.getEmail() != null) {
            update.set("email", updateDTO.getEmail());
        }
        if (updateDTO.getAge() != null) {
            update.set("age", updateDTO.getAge());
        }
        if (updateDTO.getStatus() != null) {
            update.set("status", updateDTO.getStatus());
        }

        update.set("updatedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);
        return result.getModifiedCount() > 0;
    }

    /**
     * 批量更新用户状态
     */
    public long batchUpdateStatus(List<String> userIds, String newStatus) {
        Query query = new Query(Criteria.where("_id").in(userIds));
        Update update = new Update()
            .set("status", newStatus)
            .currentDate("updatedAt");

        UpdateResult result = mongoTemplate.updateMulti(query, update, User.class);
        return result.getModifiedCount();
    }

    /**
     * 添加用户角色
     */
    public boolean addUserRole(String userId, String role) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update()
            .addToSet("roles", role)  // 去重添加
            .currentDate("updatedAt");

        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);
        return result.getModifiedCount() > 0;
    }

    /**
     * 删除用户角色
     */
    public boolean removeUserRole(String userId, String role) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update()
            .pull("roles", role)
            .currentDate("updatedAt");

        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);
        return result.getModifiedCount() > 0;
    }

    /**
     * 用户年龄增长（用于用户积分等场景）
     */
    public void incrementUserAge(String userId, int delta) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update()
            .inc("age", delta)
            .currentDate("updatedAt");

        mongoTemplate.updateFirst(query, update, User.class);
    }
}
```

---

## 7.5 聚合操作

### 7.5.1 聚合框架基础

MongoDB 聚合管道由多个阶段组成，每个阶段对文档进行处理并传递给下一个阶段。

```
[ $match ] --> [ $group ] --> [ $sort ] --> [ $project ]
```

### 7.5.2 常用聚合阶段

```java
// $match - 过滤文档
Aggregation.match(Criteria.where("status").is("ACTIVE"))

// $group - 分组
Aggregation.group("field")                      // 按字段分组
Aggregation.group("field1", "field2")          // 复合分组
Aggregation.group().count().as("count")        // 计数
Aggregation.group().sum("amount").as("total")  // 求和
Aggregation.group().avg("price").as("avgPrice") // 平均值
Aggregation.group().min("price").as("minPrice") // 最小值
Aggregation.group().max("price").as("maxPrice") // 最大值
Aggregation.group().addToSet("field").as("items") // 去重收集

// $sort - 排序
Aggregation.sort(Sort.Direction.DESC, "count")

// $limit - 限制数量
Aggregation.limit(10)

// $skip - 跳过数量
Aggregation.skip(20)

// $project - 投影，控制输出字段
Aggregation.project("field1", "field2")
Aggregation.project().andExclude("_id")

// $unwind - 展开数组
Aggregation.unwind("arrayField")

// $count - 计数
Aggregation.count().as("total")
```

### 7.5.3 简单聚合示例

**计数统计**

```java
public void countExample() {
    // 统计所有用户数量
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where("status").is("ACTIVE")),
        Aggregation.count().as("total")
    );

    AggregationResults<Document> results = mongoTemplate.aggregate(
        aggregation, "users", Document.class
    );

    Document result = results.getUniqueMappedResult();
    long total = result.getLong("total");
    System.out.println("活跃用户数: " + total);
}
```

**求和统计**

```java
public void sumExample() {
    // 假设有一个 Order 实体，包含 amount 字段
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where("status").is("COMPLETED")),
        Aggregation.group().sum("amount").as("totalAmount")
    );

    AggregationResults<Document> results = mongoTemplate.aggregate(
        aggregation, "orders", Document.class
    );

    Document result = results.getUniqueMappedResult();
    Double totalAmount = result.getDouble("totalAmount");
    System.out.println("订单总金额: " + totalAmount);
}
```

### 7.5.4 分组统计

**按字段分组统计**

```java
public void groupByExample() {
    // 按状态分组统计用户数量
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.group("status").count().as("count")
    );

    AggregationResults<Document> results = mongoTemplate.aggregate(
        aggregation, "users", Document.class
    );

    results.forEach(doc -> {
        String status = doc.getString("_id");
        Integer count = doc.getInteger("count");
        System.out.println("状态: " + status + ", 数量: " + count);
    });
}
```

**多字段分组**

```java
public void multiGroupByExample() {
    // 按状态和年龄段分组
    Aggregation aggregation = Aggregation.newAggregation(
        // 阶段1：过滤有效数据
        Aggregation.match(Criteria.where("status").ne("DELETED")),
        // 阶段2：添加年龄段字段
        Aggregation.project()
            .and("status").as("status")
            .and(ConditionalOperators.when(Criteria.where("age").lt(18))
                .then("未成年")
                .otherwise("成年")).as("ageGroup"),
        // 阶段3：分组统计
        Aggregation.group("status", "ageGroup").count().as("count"),
        // 阶段4：排序
        Aggregation.sort(Sort.Direction.DESC, "count")
    );

    AggregationResults<Document> results = mongoTemplate.aggregate(
        aggregation, "users", Document.class
    );

    results.forEach(doc -> {
        System.out.println(doc.getString("_id") + ": " + doc.getInteger("count"));
    });
}
```

### 7.5.5 复杂聚合示例

**统计每日订单数据**

```java
public void dailyOrderStats() {
    // 按日期分组，统计订单数量、总金额、平均金额
    Aggregation aggregation = Aggregation.newAggregation(
        // 过滤已完成订单
        Aggregation.match(Criteria.where("status").is("COMPLETED")),
        // 投影日期字段（只取日期部分）
        Aggregation.project()
            .and("amount").as("amount")
            .and(DateOperators.DateToString.dateOf("createdAt")
                .toString("%Y-%m-%d")).as("date"),
        // 按日期分组
        Aggregation.group("date")
            .count().as("orderCount")
            .sum("amount").as("totalAmount")
            .avg("amount").as("avgAmount"),
        // 排序（按日期降序）
        Aggregation.sort(Sort.Direction.DESC, "_id"),
        // 限制返回数量
        Aggregation.limit(30)
    );

    AggregationResults<Document> results = mongoTemplate.aggregate(
        aggregation, "orders", Document.class
    );

    results.forEach(doc -> {
        System.out.println(String.format("日期: %s, 订单数: %d, 总金额: %.2f, 平均金额: %.2f",
            doc.getString("_id"),
            doc.getInteger("orderCount"),
            doc.getDouble("totalAmount"),
            doc.getDouble("avgAmount")));
    });
}
```

**关联集合统计（$lookup 替代方案）**

```java
public void aggregationWithLookup() {
    // 假设有 orders 集合和 products 集合
    // orders 中有 productIds 数组字段
    // 使用 unwind + group 实现类似 $lookup 的功能

    Aggregation aggregation = Aggregation.newAggregation(
        // 展开产品数组
        Aggregation.unwind("productIds"),
        // 关联产品表（实际使用 $lookup）
        Aggregation.lookup("products", "productIds", "_id", "product"),
        Aggregation.unwind("product"),
        // 分组统计
        Aggregation.group("product.category")
            .count().as("orderCount")
            .sum("amount").as("totalSales"),
        // 排序
        Aggregation.sort(Sort.Direction.DESC, "totalSales")
    );

    AggregationResults<Document> results = mongoTemplate.aggregate(
        aggregation, "orders", Document.class
    );
}
```

### 7.5.6 聚合结果映射到 DTO

```java
// 定义 DTO 类
public class UserStatsDTO {
    private String status;
    private Long count;
    private Double avgAge;
    private Double maxAge;
    private Double minAge;

    // getter/setter
}

// 使用投影将结果映射到 DTO
public void aggregateToDTO() {
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where("status").ne("DELETED")),
        Aggregation.group("status")
            .count().as("count")
            .avg("age").as("avgAge")
            .max("age").as("maxAge")
            .min("age").as("minAge"),
        Aggregation.project()
            .and("_id").as("status")
            .and("count").as("count")
            .and("avgAge").as("avgAge")
            .and("maxAge").as("maxAge")
            .and("minAge").as("minAge")
    );

    AggregationResults<UserStatsDTO> results = mongoTemplate.aggregate(
        aggregation, "users", UserStatsDTO.class
    );

    results.forEach(dto -> {
        System.out.println(String.format("状态: %s, 数量: %d, 平均年龄: %.1f",
            dto.getStatus(), dto.getCount(), dto.getAvgAge()));
    });
}
```

### 7.5.7 完整聚合服务示例

```java
@Service
public class AggregationService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 获取用户活跃度统计
     */
    public UserActivityStats getUserActivityStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("lastLoginAt").gte(thirtyDaysAgo)),
            Aggregation.project()
                .and(ConditionalOperators.WhenNull.valueOf("lastLoginAt")
                    .then(thirtyDaysAgo)).as("lastLogin"),
            Aggregation.group()
                .count().as("activeUsers")
                .avg("age").as("avgAge"),
            Aggregation.project()
                .and("_id").as("ignored")
                .and("activeUsers").as("activeUsers")
                .and("avgAge").as("avgAge")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
            aggregation, "users", Document.class
        );

        Document doc = results.getUniqueMappedResult();
        if (doc == null) {
            return new UserActivityStats(0L, 0.0);
        }

        return new UserActivityStats(
            doc.getLong("activeUsers"),
            doc.getDouble("avgAge")
        );
    }

    /**
     * 获取销售排行榜 TOP N
     */
    public List<ProductSalesDTO> getTopSellingProducts(int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("status").is("COMPLETED")),
            Aggregation.unwind("items"),
            Aggregation.group("items.productId")
                .sum("items.quantity").as("totalQuantity")
                .sum("items.price").as("totalRevenue"),
            Aggregation.sort(Sort.Direction.DESC, "totalRevenue"),
            Aggregation.limit(limit),
            Aggregation.project()
                .and("_id").as("productId")
                .and("totalQuantity").as("totalQuantity")
                .and("totalRevenue").as("totalRevenue")
        );

        AggregationResults<ProductSalesDTO> results = mongoTemplate.aggregate(
            aggregation, "orders", ProductSalesDTO.class
        );

        return results.getMappedResults();
    }

    /**
     * 计算用户留存率
     */
    public Map<Integer, Double> calculateRetentionRate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);

        // 获取 30 天前活跃的用户
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("lastLoginAt").gte(sixtyDaysAgo)
                        .lt(thirtyDaysAgo)
            ),
            Aggregation.group("_id").count().as("churnedUsers")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
            aggregation, "users", Document.class
        );

        Document doc = results.getUniqueMappedResult();
        long churnedUsers = doc != null ? doc.getLong("churnedUsers") : 0;

        // 简化计算，实际需要更复杂的逻辑
        return Map.of(30, churnedUsers > 0 ? 0.85 : 0.0);
    }
}
```

---

## 7.6 批量操作

### 7.6.1 BulkOperations 介绍

BulkOperations 允许在单次请求中执行多个操作，提高批量操作的性能。

```java
// 获取 BulkOperations 实例
BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, User.class);

// BulkMode.UNORDERED - 顺序执行，遇到错误继续执行
// BulkMode.ORDERED   - 顺序执行，遇到错误停止
```

### 7.6.2 批量插入

```java
public void bulkInsert() {
    List<User> users = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        User user = new User();
        user.setUsername("user" + i);
        user.setEmail("user" + i + "@example.com");
        user.setAge(20 + (i % 50));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        users.add(user);
    }

    BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, User.class);
    bulkOps.insert(users);

    BulkWriteResult result = bulkOps.execute();
    System.out.println("插入数量: " + result.getInsertedCount());
}
```

### 7.6.3 批量更新

```java
public void bulkUpdate() {
    BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, User.class);

    // 批量更新：给所有 30 岁以上的用户添加 VIP 角色
    Query query1 = new Query(Criteria.where("age").gt(30));
    Update update1 = new Update().addToSet("roles", "VIP");
    bulkOps.updateMulti(query1, update1);

    // 批量更新：给所有 25 岁以下用户设置为 YOUNG 状态
    Query query2 = new Query(Criteria.where("age").lt(25));
    Update update2 = new Update().set("status", "YOUNG");
    bulkOps.updateMulti(query2, update2);

    BulkWriteResult result = bulkOps.execute();
    System.out.println("修改数量: " + result.getModifiedCount());
}
```

### 7.6.4 混合批量操作

```java
public void mixedBulkOperations() {
    BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.ORDERED, User.class);

    // 1. 插入新用户
    User newUser = new User();
    newUser.setUsername("newuser");
    newUser.setEmail("newuser@example.com");
    newUser.setAge(25);
    newUser.setStatus("ACTIVE");
    bulkOps.insert(newUser);

    // 2. 更新已有用户
    Query query = new Query(Criteria.where("username").is("existinguser"));
    Update update = new Update().set("status", "UPDATED");
    bulkOps.updateOne(query, update);

    // 3. 删除用户
    Query deleteQuery = new Query(Criteria.where("username").is("todelete"));
    bulkOps.remove(deleteQuery);

    BulkWriteResult result = bulkOps.execute();
    System.out.println("插入: " + result.getInsertedCount());
    System.out.println("修改: " + result.getModifiedCount());
    System.out.println("删除: " + result.getDeletedCount());
}
```

### 7.6.5 完整批量操作服务示例

```java
@Service
public class BulkOperationService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 批量导入用户
     */
    public BulkImportResult importUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            return new BulkImportResult(0, 0, 0);
        }

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, User.class);

        users.forEach(user -> {
            user.setCreatedAt(LocalDateTime.now());
            bulkOps.insert(user);
        });

        try {
            BulkWriteResult result = bulkOps.execute();
            return new BulkImportResult(
                result.getInsertedCount(),
                0,
                0
            );
        } catch (BulkOperationException e) {
            // 处理部分失败
            BulkWriteResult result = e.getResult();
            return new BulkImportResult(
                result.getInsertedCount(),
                result.getModifiedCount(),
                users.size() - result.getInsertedCount()
            );
        }
    }

    /**
     * 批量更新用户状态
     */
    public long batchUpdateUserStatus(List<String> userIds, String newStatus) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.ORDERED, User.class);

        userIds.forEach(userId -> {
            Query query = new Query(Criteria.where("_id").is(userId));
            Update update = new Update()
                .set("status", newStatus)
                .currentDate("updatedAt");
            bulkOps.updateMulti(query, update);
        });

        BulkWriteResult result = bulkOps.execute();
        return result.getModifiedCount();
    }

    /**
     * 批量删除过期数据
     */
    public long batchDeleteExpiredData(LocalDateTime before) {
        // 分批删除，每批 1000 条
        long totalDeleted = 0;
        boolean hasMore = true;

        while (hasMore) {
            Query query = new Query(Criteria.where("createdAt").lt(before));
            query.limit(1000);

            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, User.class);
            bulkOps.remove(query);

            BulkWriteResult result = bulkOps.execute();
            long deleted = result.getDeletedCount();
            totalDeleted += deleted;

            hasMore = deleted == 1000;
        }

        return totalDeleted;
    }
}
```

---

## 7.7 事务支持

### 7.7.1 MongoDB 事务简介

MongoDB 4.0+ 支持多文档事务，Spring Data MongoDB 提供了两种事务管理方式：
1. 声明式事务 (@Transactional)
2. 编程式事务 (SessionCallback)

### 7.7.2 配置事务管理器

```java
@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

### 7.7.3 声明式事务

```java
@Service
public class OrderService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Transactional
    public void createOrder(Order order, List<Product> products) {
        // 1. 创建订单
        mongoTemplate.insert(order);

        // 2. 更新库存
        products.forEach(product -> {
            Query query = new Query(Criteria.where("_id").is(product.getId()));
            Update update = new Update().inc("stock", -product.getQuantity());
            mongoTemplate.updateFirst(query, update, Product.class);
        });

        // 3. 更新用户积分
        Query userQuery = new Query(Criteria.where("_id").is(order.getUserId()));
        Update userUpdate = new Update().inc("points", order.getAmount());
        mongoTemplate.updateFirst(userQuery, userUpdate, User.class);

        // 如果这里抛出异常，整个事务会回滚
    }
}
```

### 7.7.4 编程式事务 (SessionCallback)

```java
@Service
public class TransferService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 转账操作：使用 SessionCallback 实现编程式事务
     */
    public void transfer(String fromUserId, String toUserId, double amount) {
        mongoTemplate.execute(SessionCallback(session -> {
            // 开启事务
            session.startTransaction();

            try {
                // 1. 扣款
                Query fromQuery = new Query(Criteria.where("_id").is(fromUserId));
                Update fromUpdate = new Update().inc("balance", -amount);
                mongoTemplate.updateFirst(fromQuery, fromUpdate, User.class);

                // 2. 验证余额
                User fromUser = mongoTemplate.findById(fromUserId, User.class);
                if (fromUser.getBalance() < 0) {
                    throw new RuntimeException("余额不足");
                }

                // 3. 收款
                Query toQuery = new Query(Criteria.where("_id").is(toUserId));
                Update toUpdate = new Update().inc("balance", amount);
                mongoTemplate.updateFirst(toQuery, toUpdate, User.class);

                // 4. 记录转账日志
                TransferLog log = new TransferLog();
                log.setFromUserId(fromUserId);
                log.setToUserId(toUserId);
                log.setAmount(amount);
                log.setCreatedAt(LocalDateTime.now());
                mongoTemplate.insert(log);

                // 提交事务
                session.commitTransaction();

            } catch (Exception e) {
                // 回滚事务
                session.abortTransaction();
                throw e;
            }

            return null;
        }));
    }
}
```

### 7.7.5 事务选项配置

```java
public void transferWithOptions(String fromUserId, String toUserId, double amount) {
    // 配置事务选项
    TransactionOptions options = TransactionOptions.builder()
        .readConcern(ReadConcern.SNAPSHOT)
        .writeConcern(WriteConcern.MAJORITY)
        .readPreference(ReadPreference.primary())
        .build();

    mongoTemplate.execute(SessionCallback(session -> {
        session.startTransaction(options);

        try {
            // 执行转账操作
            deductBalance(fromUserId, amount);
            addBalance(toUserId, amount);

            session.commitTransaction();
        } catch (Exception e) {
            session.abortTransaction();
            throw e;
        }

        return null;
    }));
}
```

### 7.7.6 完整事务服务示例

```java
@Service
public class ShoppingCartService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 完成订单：扣库存、创建订单、扣款
     * 使用声明式事务
     */
    @Transactional(rollbackFor = Exception.class)
    public Order checkout(String userId, List<CartItem> cartItems) {
        // 1. 计算总价
        double totalAmount = cartItems.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();

        // 2. 扣减库存
        for (CartItem item : cartItems) {
            Query query = new Query(Criteria.where("_id").is(item.getProductId()));
            Update update = new Update().inc("stock", -item.getQuantity());
            UpdateResult result = mongoTemplate.updateFirst(query, update, Product.class);

            if (result.getModifiedCount() == 0) {
                throw new RuntimeException("库存不足: " + item.getProductId());
            }
        }

        // 3. 扣款
        Query userQuery = new Query(Criteria.where("_id").is(userId));
        Update userUpdate = new Update().inc("balance", -totalAmount);
        UpdateResult balanceResult = mongoTemplate.updateFirst(userQuery, userUpdate, User.class);

        if (balanceResult.getModifiedCount() == 0) {
            throw new RuntimeException("余额不足");
        }

        // 4. 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setItems(cartItems);
        order.setTotalAmount(totalAmount);
        order.setStatus("COMPLETED");
        order.setCreatedAt(LocalDateTime.now());
        mongoTemplate.insert(order);

        // 5. 清空购物车
        Query cartQuery = new Query(Criteria.where("userId").is(userId));
        mongoTemplate.remove(cartQuery, Cart.class);

        return order;
    }

    /**
     * 使用编程式事务处理更复杂场景
     */
    public void refundWithTransaction(String orderId) {
        mongoTemplate.execute(SessionCallback(session -> {
            TransactionOptions options = TransactionOptions.builder()
                .readConcern(ReadConcern.SNAPSHOT)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

            session.startTransaction(options);

            try {
                // 1. 查询订单
                Order order = mongoTemplate.findById(orderId, Order.class);
                if (order == null) {
                    throw new RuntimeException("订单不存在");
                }

                if ("REFUNDED".equals(order.getStatus())) {
                    throw new RuntimeException("订单已退款");
                }

                // 2. 退还库存
                for (CartItem item : order.getItems()) {
                    Query query = new Query(Criteria.where("_id").is(item.getProductId()));
                    Update update = new Update().inc("stock", item.getQuantity());
                    mongoTemplate.updateFirst(query, update, Product.class);
                }

                // 3. 退还金额
                Query userQuery = new Query(Criteria.where("_id").is(order.getUserId()));
                Update userUpdate = new Update().inc("balance", order.getTotalAmount());
                mongoTemplate.updateFirst(userQuery, userUpdate, User.class);

                // 4. 更新订单状态
                Query orderQuery = new Query(Criteria.where("_id").is(orderId));
                Update statusUpdate = new Update()
                    .set("status", "REFUNDED")
                    .set("refundedAt", LocalDateTime.now());
                mongoTemplate.updateFirst(orderQuery, statusUpdate, Order.class);

                // 5. 记录退款日志
                RefundLog log = new RefundLog();
                log.setOrderId(orderId);
                log.setUserId(order.getUserId());
                log.setAmount(order.getTotalAmount());
                log.setCreatedAt(LocalDateTime.now());
                mongoTemplate.insert(log);

                session.commitTransaction();

            } catch (Exception e) {
                session.abortTransaction();
                throw e;
            }

            return null;
        }));
    }
}
```

---

## 7.8 最佳实践与注意事项

### 7.8.1 性能优化

1. **使用索引**：确保查询字段已建立索引
2. **投影筛选**：只查询需要的字段，减少网络传输
3. **分页查询**：避免一次性查询大量数据
4. **批量操作**：使用 BulkOperations 减少网络往返
5. **连接池配置**：合理配置 MongoDB 连接池参数

### 7.8.2 常见问题处理

```java
// 1. 处理空结果
User user = mongoTemplate.findById(id, User.class);
if (user == null) {
    // 处理未找到的情况
}

// 2. 处理多个结果
Query query = new Query(Criteria.where("username").is(username));
List<User> users = mongoTemplate.find(query, User.class);
if (users.size() > 1) {
    throw new RuntimeException("数据异常：找到多个用户");
}

// 3. 投影时排除 _id
query.fields().exclude("_id");

// 4. 处理日期类型
query.addCriteria(Criteria.where("createdAt").gte(startDate).lt(endDate));
```

### 7.8.3 安全建议

1. **防止注入**：使用参数化查询，不要拼接用户输入
2. **权限控制**：MongoDB 数据库使用最小权限原则
3. **敏感数据**：对敏感字段加密存储
4. **审计日志**：记录关键操作日志

---

## 7.9 小结

本章详细介绍了 MongoTemplate 的高级操作：

- **基础操作**：insert、find、update、remove 的使用方法
- **复杂查询**：Query 和 Criteria 构建各类查询条件
- **更新操作**：Update 的各种更新类型和数组操作
- **聚合操作**：Aggregation 构建复杂的数据分析管道
- **批量操作**：BulkOperations 提高批量操作性能
- **事务支持**：声明式和编程式事务的使用

MongoTemplate 提供了比 Repository 更强大的灵活性和控制力，是处理复杂业务逻辑的首选。合理使用这些高级特性，可以构建出高效、可靠的 MongoDB 数据访问层。

---

**课后练习**

1. 实现一个用户搜索功能，支持按用户名、邮箱、状态、年龄范围等条件搜索
2. 使用聚合操作统计每日的用户注册数量
3. 使用 BulkOperations 实现批量导入用户功能
4. 使用事务实现一个转账功能，确保数据一致性
