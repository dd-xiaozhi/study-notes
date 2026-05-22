---
title: 第6章 实体映射与 Repository
sidebar_label: 第6章
---

# 第6章 实体映射与 Repository

本章将详细介绍 Spring Data MongoDB 中的实体映射机制和 Repository 层的开发。通过本章学习，你将掌握：

- 如何使用注解将 Java 对象映射为 MongoDB 文档
- 各种数据类型的映射规则
- Repository 接口的定义与使用
- CRUD 操作的完整实践
- 分页与排序的实现
- 基于方法命名规则的查询

## 6.1 @Document, @Id, @Field 注解详解

### 6.1.1 @Document 注解

`@Document` 注解用于标记一个类为 MongoDB 文档类，类似于 JPA 中的 `@Entity`。

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "users")  // 指定 MongoDB 集合名称
public class User {

    @Id
    private String id;

    @Indexed(unique = true)  // 创建唯一索引
    private String username;

    private String email;

    private Integer age;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 构造方法
    public User() {
    }

    public User(String username, String email, Integer age) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

**@Document 注解属性说明：**

| 属性 | 类型 | 说明 |
|------|------|------|
| collection | String | MongoDB 集合名称，默认使用类名首字母小写 |
| language | String | 集合的默认语言，用于文本搜索 |
| indexes | Index[] | 在集合级别定义的索引 |

### 6.1.2 @Id 注解

`@Id` 注解用于标记文档的主键字段。Spring Data MongoDB 支持两种主键策略：

1. **字符串类型主键**：自动生成 MongoDB ObjectId
2. **MongoDB ObjectId 类型**：使用 `org.bson.types.ObjectId`

```java
// 策略一：字符串类型主键（推荐）
@Id
private String id;  // 会自动生成 24 位十六进制字符串

// 策略二：ObjectId 类型主键
@Id
private ObjectId id;  // 直接使用 MongoDB ObjectId
```

### 6.1.3 @Field 注解

`@Field` 注解用于指定字段映射到 MongoDB 的详细信息。

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Field.Write;
import org.springframework.data.mongodb.core.mapping.Id;
import java.math.BigDecimal;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Field("product_name")  // 映射到 MongoDB 中的 product_name 字段
    private String productName;

    @Field("product_code")
    private String productCode;

    @Field(Write.ALWAYS)  // 写入时总是包含此字段
    private BigDecimal price;

    @Field("description")
    private String description;

    private String category;

    private Integer stock;

    // 构造方法
    public Product() {
    }

    public Product(String productName, String productCode, BigDecimal price, String description, String category, Integer stock) {
        this.productName = productName;
        this.productCode = productCode;
        this.price = price;
        this.description = description;
        this.category = category;
        this.stock = stock;
    }

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
```

### 6.1.4 @Indexed 注解

`@Indexed` 注解用于在字段上创建索引，提高查询性能。

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "orders")
@CompoundIndexes({
    @CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}"),
    @CompoundIndex(name = "createdat_status_idx", def = "{'createdAt': -1, 'status': 1}")
})
public class Order {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String orderNumber;

    @Indexed
    private String status;  // PENDING, PAID, SHIPPED, DELIVERED, CANCELLED

    private Double totalAmount;

    @Indexed(direction = Indexed.DESCENDING)
    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime updatedAt;

    // 嵌套地址文档
    private Address shippingAddress;

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    // 嵌套地址类
    public static class Address {
        private String province;
        private String city;
        private String district;
        private String street;
        private String zipCode;

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
    }
}
```

### 6.1.5 @Transient 注解

`@Transient` 注解用于标记不持久化的字段，这些字段不会存储到 MongoDB 中。

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Transient;

@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    private String name;

    private String department;

    @Transient  // 此字段不会被持久化到 MongoDB
    private String computedField;  // 例如缓存的计算字段

    private String email;

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getComputedField() {
        return computedField;
    }

    public void setComputedField(String computedField) {
        this.computedField = computedField;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

## 6.2 基本类型映射

### 6.2.1 简单类型映射

Spring Data MongoDB 自动支持以下 Java 类型到 MongoDB BSON 类型的映射：

| Java 类型 | MongoDB BSON 类型 |
|-----------|------------------|
| String | String |
| Integer / int | Int32 |
| Long / long | Int64 |
| Double / double | Double |
| Boolean / boolean | Boolean |
| java.util.Date | Date |
| java.time.LocalDate | LocalDate |
| java.time.LocalDateTime | LocalDateTime |
| java.time.Instant | Instant |
| byte[] | Binary |
| BigDecimal | Decimal128 |
| BigInteger | Decimal128 |

### 6.2.2 完整 User 实体示例

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("username")
    private String username;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password_hash")
    private String password;

    @Field("full_name")
    private String fullName;

    private Integer age;

    private LocalDate birthday;

    @Field("account_balance")
    private BigDecimal accountBalance;

    private Boolean active;

    private String status;  // ACTIVE, INACTIVE, BANNED

    private List<String> roles;  // 角色列表

    private Map<String, String> preferences;  // 用户偏好设置

    private List<Address> addresses;  // 多个地址

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Map<String, String> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 嵌套地址类
    public static class Address {
        private String label;  // HOME, OFFICE
        private String recipientName;
        private String phone;
        private String province;
        private String city;
        private String district;
        private String street;
        private String zipCode;
        private Boolean isDefault;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getRecipientName() {
            return recipientName;
        }

        public void setRecipientName(String recipientName) {
            this.recipientName = recipientName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public Boolean getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(Boolean isDefault) {
            this.isDefault = isDefault;
        }
    }
}
```

### 6.2.3 List 和 Map 映射

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;
import java.util.Map;

@Document(collection = "articles")
public class Article {

    @Id
    private String id;

    private String title;

    private String content;

    private String authorId;

    @Field("tags")
    private List<String> tags;  // 标签列表

    @Field("view_count")
    private Integer viewCount;

    @Field("like_count")
    private Integer likeCount;

    private List<Comment> comments;  // 嵌套评论列表

    private Map<String, Integer> metadata;  // 元数据 Map

    private List<Map<String, Object>> customFields;  // 自定义字段列表

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Map<String, Integer> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Integer> metadata) {
        this.metadata = metadata;
    }

    public List<Map<String, Object>> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<Map<String, Object>> customFields) {
        this.customFields = customFields;
    }

    // 嵌套评论类
    public static class Comment {
        private String id;
        private String userId;
        private String content;
        private Integer likeCount;
        private java.time.LocalDateTime createdAt;
        private List<Reply> replies;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getLikeCount() {
            return likeCount;
        }

        public void setLikeCount(Integer likeCount) {
            this.likeCount = likeCount;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public List<Reply> getReplies() {
            return replies;
        }

        public void setReplies(List<Reply> replies) {
            this.replies = replies;
        }
    }

    // 嵌套回复类
    public static class Reply {
        private String id;
        private String userId;
        private String content;
        private java.time.LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
```

### 6.2.4 枚举类型映射

```java
package com.example.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;

    @Indexed
    private ProductCategory category;  // 枚举类型

    private BigDecimal price;

    @Indexed
    private ProductStatus status;  // 枚举类型

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    // 枚举定义
    public enum ProductCategory {
        ELECTRONICS,      // 电子产品
        CLOTHING,         // 服装
        FOOD,             // 食品
        BOOKS,            // 图书
        HOME,             // 家居
        SPORTS,           // 运动
        TOYS              // 玩具
    }

    public enum ProductStatus {
        DRAFT,            // 草稿
        ACTIVE,          // 上架
        INACTIVE,         // 下架
        DELETED           // 已删除
    }
}
```

## 6.3 自定义 Repository 接口

### 6.3.1 MongoRepository 基础

Spring Data MongoDB 的 Repository 架构与 Spring Data JPA 类似，通过继承 `MongoRepository` 来获得基本的 CRUD 操作。

```java
package com.example.mongo.repository;

import com.example.mongo.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // 继承以下方法：
    // - save(S entity) - 保存或更新实体
    // - saveAll(Iterable<S> entities) - 批量保存
    // - findById(ID id) - 根据 ID 查询
    // - findAllById(Iterable<ID> ids) - 根据 ID 列表查询
    // - existsById(ID id) - 判断 ID 是否存在
    // - count() - 统计数量
    // - deleteById(ID id) - 根据 ID 删除
    // - delete(T entity) - 删除实体
    // - deleteAllById(Iterable<? extends ID> ids) - 批量删除
    // - deleteAll(Iterable<? extends T> entities) - 批量删除
    // - deleteAll() - 删除所有
    // - findAll() - 查询所有
}
```

### 6.3.2 自定义 Repository 接口定义

```java
package com.example.mongo.repository;

import com.example.mongo.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // ==================== 基本查询方法 ====================

    // 根据用户名查询
    Optional<User> findByUsername(String username);

    // 根据邮箱查询
    Optional<User> findByEmail(String email);

    // 根据状态查询用户列表
    List<User> findByStatus(String status);

    // ==================== 模糊查询方法 ====================

    // 用户名模糊查询
    List<User> findByUsernameContaining(String username);

    // 邮箱模糊查询
    List<User> findByEmailContaining(String email);

    // ==================== 精确匹配查询 ====================

    // 根据用户名和状态查询
    Optional<User> findByUsernameAndStatus(String username, String status);

    // 根据状态和角色查询
    List<User> findByStatusAndRoles(String status, String role);

    // ==================== 或查询 ====================

    // 用户名或邮箱查询
    List<User> findByUsernameOrEmail(String username, String email);

    // ==================== 统计查询 ====================

    // 统计指定状态的用户数量
    long countByStatus(String status);

    // ==================== 存在性检查 ====================

    // 检查用户名是否存在
    boolean existsByUsername(String username);

    // 检查邮箱是否存在
    boolean existsByEmail(String email);

    // ==================== 删除查询 ====================

    // 根据用户名删除
    void deleteByUsername(String username);

    // 根据状态删除
    long deleteByStatus(String status);
}
```

### 6.3.3 OrderRepository 自定义接口

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // 根据用户ID查询订单列表
    List<Order> findByUserId(String userId);

    // 根据订单号查询
    Optional<Order> findByOrderNumber(String orderNumber);

    // 根据用户ID和状态查询
    List<Order> findByUserIdAndStatus(String userId, String status);

    // 根据状态查询订单
    List<Order> findByStatus(String status);

    // 根据创建时间范围查询
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 查询用户最近N天的订单
    List<Order> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime after);

    // 根据状态统计订单数量
    long countByStatus(String status);

    // 检查订单号是否存在
    boolean existsByOrderNumber(String orderNumber);
}
```

### 6.3.4 ProductRepository 自定义接口

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Product;
import com.example.mongo.entity.Product.ProductCategory;
import com.example.mongo.entity.Product.ProductStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // 根据分类查询
    List<Product> findByCategory(ProductCategory category);

    // 根据状态查询
    List<Product> findByStatus(ProductStatus status);

    // 根据名称模糊查询
    List<Product> findByNameContaining(String name);

    // 价格区间查询
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 价格小于等于查询
    List<Product> findByPriceLessThanEqual(BigDecimal maxPrice);

    // 价格大于等于查询
    List<Product> findByPriceGreaterThanEqual(BigDecimal minPrice);

    // 分类和状态组合查询
    List<Product> findByCategoryAndStatus(ProductCategory category, ProductStatus status);

    // 名称包含且价格在范围内
    List<Product> findByNameContainingAndPriceBetween(
            String name, BigDecimal minPrice, BigDecimal maxPrice);

    // 库存小于指定值
    List<Product> findByStockLessThan(Integer stock);
}
```

### 6.3.5 ArticleRepository 自定义接口

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Article;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {

    // 根据作者ID查询
    List<Article> findByAuthorId(String authorId);

    // 根据标签查询
    List<Article> findByTagsContaining(String tag);

    // 根据多个标签查询（AND 条件）
    List<Article> findByTagsContainingAll(List<String> tags);

    // 根据标题模糊查询
    List<Article> findByTitleContaining(String title);

    // 根据作者和标签查询
    List<Article> findByAuthorIdAndTagsContaining(String authorId, String tag);

    // 查询浏览量大于指定值的文章
    List<Article> findByViewCountGreaterThan(Integer viewCount);

    // 查询浏览量排名前N的文章
    List<Article> findTop10ByOrderByViewCountDesc();
}
```

## 6.4 CRUD Repository 使用

### 6.4.1 UserService 完整实现

```java
package com.example.mongo.service;

import com.example.mongo.entity.User;
import com.example.mongo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ==================== 创建操作 ====================

    /**
     * 创建新用户
     */
    public User createUser(User user) {
        // 设置创建时间
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    /**
     * 批量创建用户
     */
    public List<User> createUsers(List<User> users) {
        LocalDateTime now = LocalDateTime.now();
        users.forEach(user -> {
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user.setActive(true);
            user.setStatus("ACTIVE");
        });
        return userRepository.saveAll(users);
    }

    // ==================== 读取操作 ====================

    /**
     * 根据ID查询用户
     */
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    /**
     * 根据用户名查询用户
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查询用户
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 根据状态查询用户列表
     */
    public List<User> getUsersByStatus(String status) {
        return userRepository.findByStatus(status);
    }

    /**
     * 模糊查询用户
     */
    public List<User> searchUsersByUsername(String username) {
        return userRepository.findByUsernameContaining(username);
    }

    /**
     * 分页查询用户
     */
    public org.springframework.data.domain.Page<User> getUsersPage(
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // ==================== 更新操作 ====================

    /**
     * 更新用户信息
     */
    public User updateUser(String id, User userDetails) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setFullName(userDetails.getFullName());
            user.setEmail(userDetails.getEmail());
            user.setAge(userDetails.getAge());
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        throw new RuntimeException("User not found with id: " + id);
    }

    /**
     * 更新用户状态
     */
    public User updateUserStatus(String id, String status) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setStatus(status);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        throw new RuntimeException("User not found with id: " + id);
    }

    /**
     * 使用 MongoTemplate 部分更新（只更新非空字段）
     */
    public void partialUpdateUser(String id, java.util.Map<String, Object> updates) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        updates.forEach((key, value) -> {
            if (value != null) {
                update.set(key, value);
            }
        });
        update.set("updatedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, User.class);
    }

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除用户
     */
    public void deleteUserById(String id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        } else {
            throw new RuntimeException("User not found with id: " + id);
        }
    }

    /**
     * 根据用户名删除用户
     */
    public void deleteUserByUsername(String username) {
        userRepository.deleteByUsername(username);
    }

    /**
     * 删除所有用户
     */
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    // ==================== 统计操作 ====================

    /**
     * 统计用户总数
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * 统计指定状态的用户数
     */
    public long countUsersByStatus(String status) {
        return userRepository.countByStatus(status);
    }

    // ==================== 存在性检查 ====================

    /**
     * 检查用户是否存在
     */
    public boolean existsUser(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否已被使用
     */
    public boolean existsEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
```

### 6.4.2 OrderService 完整实现

```java
package com.example.mongo.service;

import com.example.mongo.entity.Order;
import com.example.mongo.entity.Order.Address;
import com.example.mongo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    // ==================== 创建订单 ====================

    public Order createOrder(String userId, Double totalAmount, Address shippingAddress) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);
        order.setShippingAddress(shippingAddress);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    // ==================== 查询订单 ====================

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public List<Order> getOrdersByUserIdAndStatus(String userId, String status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
    }

    public List<Order> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByCreatedAtBetween(start, end);
    }

    public List<Order> getRecentOrdersByUser(String userId, int days) {
        LocalDateTime after = LocalDateTime.now().minusDays(days);
        return orderRepository.findByUserIdAndCreatedAtAfter(userId, after);
    }

    // ==================== 更新订单状态 ====================

    public Order updateOrderStatus(String orderId, String newStatus) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            order.setStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
        throw new RuntimeException("Order not found with id: " + orderId);
    }

    // 订单状态流转
    public Order payOrder(String orderId) {
        return updateOrderStatus(orderId, "PAID");
    }

    public Order shipOrder(String orderId) {
        return updateOrderStatus(orderId, "SHIPPED");
    }

    public Order deliverOrder(String orderId) {
        return updateOrderStatus(orderId, "DELIVERED");
    }

    public Order cancelOrder(String orderId) {
        return updateOrderStatus(orderId, "CANCELLED");
    }

    // ==================== 删除订单 ====================

    public void deleteOrder(String id) {
        orderRepository.deleteById(id);
    }

    // ==================== 统计 ====================

    public long countOrdersByStatus(String status) {
        return orderRepository.countByStatus(status);
    }

    public boolean existsByOrderNumber(String orderNumber) {
        return orderRepository.existsByOrderNumber(orderNumber);
    }

    // ==================== 工具方法 ====================

    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
```

### 6.4.3 ProductService 完整实现

```java
package com.example.mongo.service;

import com.example.mongo.entity.Product;
import com.example.mongo.entity.Product.ProductCategory;
import com.example.mongo.entity.Product.ProductStatus;
import com.example.mongo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // ==================== 创建产品 ====================

    public Product createProduct(Product product) {
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public List<Product> createProducts(List<Product> products) {
        products.forEach(product -> {
            product.setStatus(ProductStatus.ACTIVE);
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
        });
        return productRepository.saveAll(products);
    }

    // ==================== 查询产品 ====================

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByCategory(ProductCategory category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContaining(name);
    }

    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    public List<Product> getProductsByCategoryAndStatus(ProductCategory category, ProductStatus status) {
        return productRepository.findByCategoryAndStatus(category, status);
    }

    public List<Product> searchProductsByNameAndPriceRange(
            String name, BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByNameContainingAndPriceBetween(name, minPrice, maxPrice);
    }

    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByStockLessThan(threshold);
    }

    // ==================== 更新产品 ====================

    public Product updateProduct(String id, Product productDetails) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setName(productDetails.getName());
            product.setCategory(productDetails.getCategory());
            product.setPrice(productDetails.getPrice());
            product.setDescription(productDetails.getDescription());
            product.setStock(productDetails.getStock());
            product.setUpdatedAt(LocalDateTime.now());
            return productRepository.save(product);
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    public Product updateProductStatus(String id, ProductStatus status) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setStatus(status);
            product.setUpdatedAt(LocalDateTime.now());
            return productRepository.save(product);
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    public Product updateProductStock(String id, Integer newStock) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setStock(newStock);
            product.setUpdatedAt(LocalDateTime.now());
            return productRepository.save(product);
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    // ==================== 删除产品 ====================

    public void deleteProduct(String id) {
        // 软删除：设置为 DELETED 状态
        updateProductStatus(id, ProductStatus.DELETED);
    }

    public void hardDeleteProduct(String id) {
        productRepository.deleteById(id);
    }

    // ==================== 批量操作 ====================

    public void updateProductsStock(java.util.Map<String, Integer> stockUpdates) {
        stockUpdates.forEach((productId, stock) -> {
            updateProductStock(productId, stock);
        });
    }
}
```

## 6.5 分页与排序

### 6.5.1 分页查询基础

Spring Data MongoDB 支持通过 `Pageable` 和 `Page` 接口进行分页查询。

```java
package com.example.mongo.repository;

import com.example.mongo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // 分页查询所有用户
    Page<User> findAll(Pageable pageable);

    // 根据状态分页查询
    Page<User> findByStatus(String status, Pageable pageable);

    // 根据用户名分页模糊查询
    Page<User> findByUsernameContaining(String username, Pageable pageable);
}
```

### 6.5.2 Sort 排序详解

```java
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// 创建排序对象
Sort sort = Sort.by(Sort.Direction.ASC, "username");  // 按 username 升序
Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");  // 按 createdAt 降序

// 多字段排序
Sort sort = Sort.by(
    Sort.Order.desc("status"),
    Sort.Order.asc("username")
);

// 排序后分页
Pageable pageable = PageRequest.of(0, 10, sort);  // 第1页，每页10条，按sort排序
```

### 6.5.3 分页 Service 实现

```java
package com.example.mongo.service;

import com.example.mongo.entity.User;
import com.example.mongo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserPaginationService {

    @Autowired
    private UserRepository userRepository;

    // ==================== 基础分页查询 ====================

    /**
     * 分页查询所有用户
     * @param page 页码（从0开始）
     * @param size 每页大小
     */
    public Page<User> getUsersPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    /**
     * 分页查询并排序
     * @param page 页码
     * @param size 每页大小
     * @param sortField 排序字段
     * @param sortDirection 排序方向（ASC/DESC）
     */
    public Page<User> getUsersPageWithSort(int page, int size, String sortField, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);
    }

    /**
     * 按用户名分页模糊查询
     */
    public Page<User> searchUsersByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findByUsernameContaining(username, pageable);
    }

    /**
     * 按状态分页查询
     */
    public Page<User> getUsersByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findByStatus(status, pageable);
    }

    // ==================== 复杂分页场景 ====================

    /**
     * 获取首页数据（带排序）
     */
    public Page<User> getFirstPage(String sortField) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(0, 10, sort);
        return userRepository.findAll(pageable);
    }

    /**
     * 获取最后页数据
     */
    public Page<User> getLastPage(String sortField) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(0, 10, sort);
        Page<User> allUsers = userRepository.findAll(pageable);
        int totalPages = allUsers.getTotalPages();
        if (totalPages > 0) {
            pageable = PageRequest.of(totalPages - 1, 10, sort);
        }
        return userRepository.findAll(pageable);
    }

    /**
     * 分页查询返回列表（不返回分页元数据）
     */
    public List<User> getUsersAsList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable).getContent();
    }

    /**
     * 获取前N条热门用户（按某字段排序取前N条）
     */
    public List<User> getTopUsers(String sortField, int topN) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(0, topN, sort);
        return userRepository.findAll(pageable).getContent();
    }
}
```

### 6.5.4 Order 分页查询示例

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // 分页查询用户的订单
    Page<Order> findByUserId(String userId, Pageable pageable);

    // 分页查询用户的订单（带状态过滤）
    Page<Order> findByUserIdAndStatus(String userId, String status, Pageable pageable);

    // 分页查询某时间范围内的订单
    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // 查询用户最近的订单（分页）
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
```

```java
package com.example.mongo.service;

import com.example.mongo.entity.Order;
import com.example.mongo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderPaginationService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 分页获取用户订单
     */
    public Page<Order> getUserOrdersPaged(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * 分页获取用户订单（按状态筛选）
     */
    public Page<Order> getUserOrdersPagedByStatus(String userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * 分页查询时间范围内的订单
     */
    public Page<Order> getOrdersByDateRange(
            LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByCreatedAtBetween(start, end, pageable);
    }

    /**
     * 获取用户最近订单（第一页，按时间倒序）
     */
    public Page<Order> getRecentUserOrders(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 获取订单统计信息
     */
    public OrderStatistics getOrderStatistics(String userId) {
        List<Order> allOrders = orderRepository.findByUserId(userId);
        double totalAmount = allOrders.stream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
        long orderCount = allOrders.size();
        return new OrderStatistics(userId, orderCount, totalAmount);
    }

    // 统计内部类
    public static class OrderStatistics {
        private String userId;
        private long orderCount;
        private double totalAmount;

        public OrderStatistics(String userId, long orderCount, double totalAmount) {
            this.userId = userId;
            this.orderCount = orderCount;
            this.totalAmount = totalAmount;
        }

        public String getUserId() {
            return userId;
        }

        public long getOrderCount() {
            return orderCount;
        }

        public double getTotalAmount() {
            return totalAmount;
        }
    }
}
```

## 6.6 方法命名查询

### 6.6.1 方法命名规则概述

Spring Data MongoDB 根据方法名自动构建查询，常用规则：

| 方法名关键词 | 说明 | 示例 |
|------------|------|------|
| findBy | 查询 | findByUsername |
| findAllBy | 查询多条 | findAllByStatus |
| findFirstBy | 查询第一条 | findFirstByUsername |
| findTopBy | 查询前N条 | findTop10ByOrder |
| existsBy | 存在性检查 | existsByUsername |
| countBy | 统计 | countByStatus |
| deleteBy | 删除 | deleteByUsername |
| getBy | 获取（与 findBy 类似） | getByUsername |

### 6.6.2 支持的比较操作符

| 关键词 | 示例方法 | MongoDB 查询 |
|--------|---------|-------------|
| **等于（默认）** | findByUsername | {username: "value"} |
| **Is** | findByUsernameIs | {username: "value"} |
| **Equals** | findByUsernameEquals | {username: "value"} |
| **Containing** | findByEmailContaining | {email: /value/} |
| **StartsWith** | findByUsernameStartsWith | {username: /^value/} |
| **EndsWith** | findByUsernameEndsWith | {username: /value$/} |
| **Contains** | findByNameContains | {name: /value/} |
| **Like** | findByUsernameLike | {username: /value/} |
| **NotLike** | findByUsernameNotLike | {username: {$not: /value/}} |
| **In** | findByRoleIn | {role: {$in: [...]}} |
| **NotIn** | findByRoleNotIn | {role: {$nin: [...]}} |
| **Between** | findByAgeBetween | {age: {$gte: min, $lte: max}} |
| **LessThan** | findByAgeLessThan | {age: {$lt: value}} |
| **LessThanEqual** | findByAgeLessThanEqual | {age: {$lte: value}} |
| **GreaterThan** | findByAgeGreaterThan | {age: {$gt: value}} |
| **GreaterThanEqual** | findByAgeGreaterThanEqual | {age: {$gte: value}} |
| **IsNull** | findByNicknameIsNull | {nickname: null} |
| **IsNotNull** | findByNicknameIsNotNull | {nickname: {$ne: null}} |
| **True** | findByActiveTrue | {active: true} |
| **False** | findByActiveFalse | {active: false} |
| **OrderBy...Asc** | findByStatusOrderByCreatedAtAsc | sort: {createdAt: 1} |
| **OrderBy...Desc** | findByStatusOrderByCreatedAtDesc | sort: {createdAt: -1} |

### 6.6.3 UserRepository 方法命名查询完整示例

```java
package com.example.mongo.repository;

import com.example.mongo.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // ==================== 精确匹配查询 ====================

    // 根据用户名查询
    Optional<User> findByUsername(String username);

    // 根据邮箱查询
    Optional<User> findByEmail(String email);

    // 根据状态查询（返回列表）
    List<User> findAllByStatus(String status);

    // 根据角色查询
    List<User> findAllByRoles(String role);

    // ==================== 模糊查询 ====================

    // 用户名包含关键词
    List<User> findByUsernameContaining(String keyword);

    // 用户名以指定前缀开头
    List<User> findByUsernameStartingWith(String prefix);

    // 用户名以指定后缀结尾
    List<User> findByUsernameEndingWith(String suffix);

    // 邮箱模糊匹配
    List<User> findByEmailContaining(String keyword);

    // 名称模糊匹配
    List<User> findByFullNameContaining(String keyword);

    // ==================== 数值范围查询 ====================

    // 年龄等于指定值
    List<User> findByAge(Integer age);

    // 年龄在范围内
    List<User> findByAgeBetween(Integer minAge, Integer maxAge);

    // 年龄大于指定值
    List<User> findByAgeGreaterThan(Integer age);

    // 年龄小于指定值
    List<User> findByAgeLessThan(Integer age);

    // 年龄大于等于指定值
    List<User> findByAgeGreaterThanEqual(Integer age);

    // 年龄小于等于指定值
    List<User> findByAgeLessThanEqual(Integer age);

    // ==================== 日期范围查询 ====================

    // 创建时间在范围内
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 创建时间晚于指定时间
    List<User> findByCreatedAtAfter(LocalDateTime time);

    // 创建时间早于指定时间
    List<User> findByCreatedAtBefore(LocalDateTime time);

    // 生日在指定日期
    List<User> findByBirthday(LocalDate birthday);

    // 生日在日期范围内
    List<User> findByBirthdayBetween(LocalDate start, LocalDate end);

    // ==================== 布尔查询 ====================

    // 活跃用户
    List<User> findByActiveTrue();

    // 非活跃用户
    List<User> findByActiveFalse();

    // 状态为激活的用户
    List<User> findByStatusAndActiveTrue(String status);

    // ==================== AND/OR 查询 ====================

    // 多条件 AND 查询
    Optional<User> findByUsernameAndEmail(String username, String email);

    Optional<User> findByUsernameAndStatus(String username, String status);

    List<User> findByStatusAndAgeGreaterThan(String status, Integer age);

    // 多条件 OR 查询
    List<User> findByUsernameOrEmail(String username, String email);

    List<User> findByUsernameOrFullName(String username, String fullName);

    // 复杂 AND/OR 组合
    List<User> findByUsernameOrEmailAndStatus(String username, String email, String status);

    // ==================== IN 查询 ====================

    // 多个用户名查询
    List<User> findByUsernameIn(List<String> usernames);

    // 多个状态查询
    List<User> findByStatusIn(List<String> statuses);

    // 多个角色查询
    List<User> findByRolesIn(List<String> roles);

    // NOT IN 查询
    List<User> findByStatusNotIn(List<String> statuses);

    // ==================== NULL 查询 ====================

    // 昵称为空的用户
    List<User> findByNicknameIsNull();

    // 昵称不为空的用户
    List<User> findByNicknameIsNotNull();

    // 邮箱为空且状态为激活的用户
    List<User> findByEmailIsNullAndStatus(String status);

    // ==================== 去重查询 ====================

    // 查询所有不重复的状态
    List<String> findDistinctStatusBy();

    // 查询所有不重复的角色
    List<String> findDistinctRolesBy();

    // ==================== 统计查询 ====================

    // 统计指定状态的用户数量
    long countByStatus(String status);

    // 统计活跃用户数量
    long countByActiveTrue();

    // 统计年龄大于指定值的用户数量
    long countByAgeGreaterThan(Integer age);

    // ==================== 存在性检查 ====================

    // 用户名是否存在
    boolean existsByUsername(String username);

    // 邮箱是否存在
    boolean existsByEmail(String email);

    // 用户名和状态组合是否存在
    boolean existsByUsernameAndStatus(String username, String status);

    // ==================== 删除查询 ====================

    // 根据用户名删除
    long deleteByUsername(String username);

    // 根据状态删除
    long deleteByStatus(String status);

    // 删除并返回被删除的用户
    List<User> removeByStatus(String status);

    // ==================== 排序查询 ====================

    // 查询所有用户并按用户名升序
    List<User> findAllByOrderByUsernameAsc();

    // 查询所有用户并按创建时间降序
    List<User> findAllByOrderByCreatedAtDesc();

    // 按状态查询并排序
    List<User> findByStatusOrderByCreatedAtDesc(String status);

    List<User> findByStatusOrderByUsernameAsc(String status);

    // ==================== 分页+排序查询 ====================

    // 查询前N条（按某字段排序）
    List<User> findTop10ByOrderByViewCountDesc();

    List<User> findFirst5ByStatusOrderByCreatedAtDesc(String status);

    // 查询第一条
    Optional<User> findFirstByStatusOrderByCreatedAtDesc(String status);

    // ==================== LIKE 模糊查询 ====================

    // LIKE 查询（需要自己添加 %）
    List<User> findByUsernameLike(String likePattern);

    // IGNORE CASE 查询
    List<User> findByUsernameContainingIgnoreCase(String keyword);

    List<User> findByEmailStartingWithIgnoreCase(String prefix);
}
```

### 6.6.4 ArticleRepository 带条件查询完整示例

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {

    // ==================== 基本查询 ====================

    // 根据作者ID查询
    List<Article> findByAuthorId(String authorId);

    // 根据作者ID分页查询
    Page<Article> findByAuthorId(String authorId, Pageable pageable);

    // ==================== 标签查询 ====================

    // 包含指定标签的文章
    List<Article> findByTagsContaining(String tag);

    // 包含所有指定标签的文章
    List<Article> findByTagsContainingAll(List<String> tags);

    // 标签模糊匹配
    List<Article> findByTagsContainingAndTagsContaining(String tag1, String tag2);

    // ==================== 标题/内容查询 ====================

    // 标题模糊查询
    List<Article> findByTitleContaining(String keyword);

    // 标题模糊查询（忽略大小写）
    List<Article> findByTitleContainingIgnoreCase(String keyword);

    // 内容模糊查询
    List<Article> findByContentContaining(String keyword);

    // 标题或内容包含关键词
    List<Article> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword);

    // ==================== 浏览量/点赞量查询 ====================

    // 浏览量大于指定值
    List<Article> findByViewCountGreaterThan(Integer viewCount);

    // 浏览量排序查询前N
    List<Article> findTop10ByOrderByViewCountDesc();

    List<Article> findTop10ByOrderByLikeCountDesc();

    // 浏览量在某范围内
    List<Article> findByViewCountBetween(Integer minView, Integer maxView);

    // 点赞量大于指定值
    List<Article> findByLikeCountGreaterThanEqual(Integer likeCount);

    // ==================== 复杂组合查询 ====================

    // 作者+标签组合查询
    List<Article> findByAuthorIdAndTagsContaining(String authorId, String tag);

    // 作者+浏览量组合查询
    List<Article> findByAuthorIdAndViewCountGreaterThan(String authorId, Integer viewCount);

    // 标签+浏览量组合查询
    List<Article> findByTagsContainingAndViewCountGreaterThan(String tag, Integer viewCount);

    // ==================== 分页组合查询 ====================

    // 作者文章分页（按浏览量排序）
    Page<Article> findByAuthorIdOrderByViewCountDesc(String authorId, Pageable pageable);

    // 标签文章分页（按时间排序）
    Page<Article> findByTagsContainingOrderByCreatedAtDesc(String tag, Pageable pageable);

    // 热门文章分页
    Page<Article> findByViewCountGreaterThanOrderByViewCountDesc(
            Integer viewCount, Pageable pageable);

    // ==================== 统计查询 ====================

    // 统计作者的 文章数
    long countByAuthorId(String authorId);

    // 统计包含指定标签的文章数
    long countByTagsContaining(String tag);

    // 统计浏览量超过指定值的文章数
    long countByViewCountGreaterThan(Integer viewCount);

    // ==================== 存在性检查 ====================

    // 检查标签是否存在
    boolean existsByTagsContaining(String tag);

    // 检查作者是否有文章
    boolean existsByAuthorId(String authorId);

    // 检查标题是否存在
    boolean existsByTitle(String title);
}
```

### 6.6.5 复合查询的 OrderBy 使用

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Product;
import com.example.mongo.entity.Product.ProductCategory;
import com.example.mongo.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // 分类查询产品（按价格升序）
    List<Product> findByCategoryOrderByPriceAsc(ProductCategory category);

    // 分类查询产品（按价格降序）
    List<Product> findByCategoryOrderByPriceDesc(ProductCategory category);

    // 分类查询产品（按库存升序）
    List<Product> findByCategoryOrderByStockAsc(ProductCategory category);

    // 状态查询产品（按创建时间降序）
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);

    // 复合条件查询并排序（分类+状态，按价格升序）
    List<Product> findByCategoryAndStatusOrderByPriceAsc(
            ProductCategory category, ProductStatus status);

    // 复合条件查询并排序（分类+状态，按创建时间降序）
    List<Product> findByCategoryAndStatusOrderByCreatedAtDesc(
            ProductCategory category, ProductStatus status);

    // 价格范围查询并排序（按价格升序）
    List<Product> findByPriceBetweenOrderByPriceAsc(
            BigDecimal minPrice, BigDecimal maxPrice);

    // 价格范围查询并排序（按价格降序）
    List<Product> findByPriceBetweenOrderByPriceDesc(
            BigDecimal minPrice, BigDecimal maxPrice);

    // 分类查询并分页（按价格升序）
    Page<Product> findByCategoryOrderByPriceAsc(ProductCategory category, Pageable pageable);

    // 分类查询并分页（按价格降序）
    Page<Product> findByCategoryOrderByPriceDesc(ProductCategory category, Pageable pageable);

    // 多条件查询并分页排序
    Page<Product> findByCategoryAndStatusOrderByCreatedAtDesc(
            ProductCategory category,
            ProductStatus status,
            Pageable pageable);
}
```

## 6.7 Controller 层示例

### 6.7.1 UserController

```java
package com.example.mongo.controller;

import com.example.mongo.entity.User;
import com.example.mongo.service.UserService;
import com.example.mongo.service.UserPaginationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserPaginationService paginationService;

    // ==================== 创建 ====================

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<User>> createUsers(@RequestBody List<User> users) {
        List<User> created = userService.createUsers(users);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== 读取 ====================

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.getUserByUsername(username);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.getUserByEmail(email);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<User>> getUsersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(userService.getUsersByStatus(status));
    }

    // ==================== 分页查询 ====================

    @GetMapping("/page")
    public ResponseEntity<Page<User>> getUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paginationService.getUsersPage(page, size));
    }

    @GetMapping("/page/sort")
    public ResponseEntity<Page<User>> getUsersPageWithSort(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return ResponseEntity.ok(
            paginationService.getUsersPageWithSort(page, size, sortField, sortDirection));
    }

    @GetMapping("/search/username/{username}/page")
    public ResponseEntity<Page<User>> searchUsersByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            paginationService.searchUsersByUsername(username, page, size));
    }

    // ==================== 更新 ====================

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestBody User user) {
        try {
            User updated = userService.updateUser(id, user);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            User updated = userService.updateUserStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> partialUpdateUser(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        userService.partialUpdateUser(id, updates);
        return ResponseEntity.ok().build();
    }

    // ==================== 删除 ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUserById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }

    // ==================== 统计 ====================

    @GetMapping("/count")
    public ResponseEntity<Long> countUsers() {
        return ResponseEntity.ok(userService.countUsers());
    }

    @GetMapping("/count/status/{status}")
    public ResponseEntity<Long> countUsersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(userService.countUsersByStatus(status));
    }

    // ==================== 存在性检查 ====================

    @GetMapping("/exists/username/{username}")
    public ResponseEntity<Map<String, Boolean>> existsByUsername(@PathVariable String username) {
        return ResponseEntity.ok(Map.of("exists", userService.existsUser(username)));
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Map<String, Boolean>> existsByEmail(@PathVariable String email) {
        return ResponseEntity.ok(Map.of("exists", userService.existsEmail(email)));
    }
}
```

### 6.7.2 OrderController

```java
package com.example.mongo.controller;

import com.example.mongo.entity.Order;
import com.example.mongo.entity.Order.Address;
import com.example.mongo.service.OrderService;
import com.example.mongo.service.OrderPaginationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderPaginationService paginationService;

    // ==================== 创建订单 ====================

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Double totalAmount = ((Number) request.get("totalAmount")).doubleValue();

        @SuppressWarnings("unchecked")
        Map<String, String> addressMap = (Map<String, String>) request.get("shippingAddress");
        Address address = new Address();
        address.setProvince(addressMap.get("province"));
        address.setCity(addressMap.get("city"));
        address.setDistrict(addressMap.get("district"));
        address.setStreet(addressMap.get("street"));
        address.setZipCode(addressMap.get("zipCode"));

        Order created = orderService.createOrder(userId, totalAmount, address);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== 读取订单 ====================

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        Optional<Order> order = orderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        Optional<Order> order = orderService.getOrderByNumber(orderNumber);
        return order.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    // ==================== 分页查询 ====================

    @GetMapping("/user/{userId}/page")
    public ResponseEntity<Page<Order>> getUserOrdersPaged(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paginationService.getUserOrdersPaged(userId, page, size));
    }

    @GetMapping("/user/{userId}/status/{status}/page")
    public ResponseEntity<Page<Order>> getUserOrdersPagedByStatus(
            @PathVariable String userId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            paginationService.getUserOrdersPagedByStatus(userId, status, page, size));
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<Order>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            paginationService.getOrdersByDateRange(start, end, page, size));
    }

    // ==================== 订单状态更新 ====================

    @PatchMapping("/{id}/pay")
    public ResponseEntity<Order> payOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(orderService.payOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/ship")
    public ResponseEntity<Order> shipOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(orderService.shipOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<Order> deliverOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(orderService.deliverOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(orderService.cancelOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== 删除 ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 统计 ====================

    @GetMapping("/count/status/{status}")
    public ResponseEntity<Long> countOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.countOrdersByStatus(status));
    }

    @GetMapping("/exists/number/{orderNumber}")
    public ResponseEntity<Map<String, Boolean>> existsByOrderNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(
            Map.of("exists", orderService.existsByOrderNumber(orderNumber)));
    }
}
```

### 6.7.3 ProductController

```java
package com.example.mongo.controller;

import com.example.mongo.entity.Product;
import com.example.mongo.entity.Product.ProductCategory;
import com.example.mongo.entity.Product.ProductStatus;
import com.example.mongo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // ==================== 创建 ====================

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Product>> createProducts(@RequestBody List<Product> products) {
        List<Product> created = productService.createProducts(products);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== 读取 ====================

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @PathVariable ProductCategory category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Product>> getProductsByStatus(
            @PathVariable ProductStatus status) {
        return ResponseEntity.ok(productService.getProductsByStatus(status));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        if (name != null && minPrice != null && maxPrice != null) {
            return ResponseEntity.ok(
                productService.searchProductsByNameAndPriceRange(name, minPrice, maxPrice));
        } else if (name != null) {
            return ResponseEntity.ok(productService.searchProductsByName(name));
        } else if (minPrice != null && maxPrice != null) {
            return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    // ==================== 更新 ====================

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @RequestBody Product product) {
        try {
            return ResponseEntity.ok(productService.updateProduct(id, product));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Product> updateProductStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            ProductStatus status = ProductStatus.valueOf(body.get("status"));
            return ResponseEntity.ok(productService.updateProductStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateProductStock(
            @PathVariable String id,
            @RequestBody Map<String, Integer> body) {
        try {
            Integer stock = body.get("stock");
            return ResponseEntity.ok(productService.updateProductStock(id, stock));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== 删除 ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteProduct(@PathVariable String id) {
        productService.hardDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 6.8 测试用例

### 6.8.1 UserRepository 测试

```java
package com.example.mongo.repository;

import com.example.mongo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();

        // 创建测试用户
        User user1 = new User("john_doe", "john@example.com", 25);
        user1.setFullName("John Doe");
        user1.setBirthday(LocalDate.of(1999, 5, 15));
        user1.setStatus("ACTIVE");
        user1.setRoles(Arrays.asList("USER", "ADMIN"));
        user1.setActive(true);
        user1.setAccountBalance(new java.math.BigDecimal("1000.00"));
        userRepository.save(user1);

        User user2 = new User("jane_smith", "jane@example.com", 30);
        user2.setFullName("Jane Smith");
        user2.setBirthday(LocalDate.of(1994, 8, 20));
        user2.setStatus("ACTIVE");
        user2.setRoles(Arrays.asList("USER"));
        user2.setActive(true);
        user2.setAccountBalance(new java.math.BigDecimal("2000.00"));
        userRepository.save(user2);

        User user3 = new User("bob_wilson", "bob@example.com", 35);
        user3.setFullName("Bob Wilson");
        user3.setBirthday(LocalDate.of(1989, 3, 10));
        user3.setStatus("INACTIVE");
        user3.setRoles(Arrays.asList("USER"));
        user3.setActive(false);
        user3.setAccountBalance(new java.math.BigDecimal("500.00"));
        userRepository.save(user3);
    }

    // ==================== 基本查询测试 ====================

    @Test
    public void testFindByUsername() {
        Optional<User> user = userRepository.findByUsername("john_doe");
        assertTrue(user.isPresent());
        assertEquals("john@example.com", user.get().getEmail());
    }

    @Test
    public void testFindByEmail() {
        Optional<User> user = userRepository.findByEmail("jane@example.com");
        assertTrue(user.isPresent());
        assertEquals("jane_smith", user.get().getUsername());
    }

    @Test
    public void testFindByStatus() {
        List<User> activeUsers = userRepository.findAllByStatus("ACTIVE");
        assertEquals(2, activeUsers.size());
    }

    @Test
    public void testFindByUsernameOrEmail() {
        List<User> users = userRepository.findByUsernameOrEmail(
            "john_doe", "bob@example.com");
        assertEquals(2, users.size());
    }

    @Test
    public void testFindByUsernameAndStatus() {
        Optional<User> user = userRepository.findByUsernameAndStatus("john_doe", "ACTIVE");
        assertTrue(user.isPresent());

        Optional<User> inactiveUser = userRepository.findByUsernameAndStatus("bob_wilson", "ACTIVE");
        assertFalse(inactiveUser.isPresent());
    }

    // ==================== 模糊查询测试 ====================

    @Test
    public void testFindByUsernameContaining() {
        List<User> users = userRepository.findByUsernameContaining("doe");
        assertEquals(1, users.size());
        assertEquals("john_doe", users.get(0).getUsername());
    }

    @Test
    public void testFindByEmailContaining() {
        List<User> users = userRepository.findByEmailContaining("example");
        assertEquals(3, users.size());
    }

    // ==================== 范围查询测试 ====================

    @Test
    public void testFindByAgeBetween() {
        List<User> users = userRepository.findByAgeBetween(25, 35);
        assertEquals(3, users.size());
    }

    @Test
    public void testFindByAgeGreaterThan() {
        List<User> users = userRepository.findByAgeGreaterThan(30);
        assertEquals(2, users.size());
    }

    @Test
    public void testFindByAgeLessThan() {
        List<User> users = userRepository.findByAgeLessThan(30);
        assertEquals(2, users.size());
    }

    // ==================== IN 查询测试 ====================

    @Test
    public void testFindByUsernameIn() {
        List<User> users = userRepository.findByUsernameIn(
            Arrays.asList("john_doe", "jane_smith"));
        assertEquals(2, users.size());
    }

    @Test
    public void testFindByStatusIn() {
        List<User> users = userRepository.findByStatusIn(
            Arrays.asList("ACTIVE", "INACTIVE"));
        assertEquals(3, users.size());
    }

    // ==================== 布尔查询测试 ====================

    @Test
    public void testFindByActiveTrue() {
        List<User> activeUsers = userRepository.findByActiveTrue();
        assertEquals(2, activeUsers.size());
    }

    @Test
    public void testFindByActiveFalse() {
        List<User> inactiveUsers = userRepository.findByActiveFalse();
        assertEquals(1, inactiveUsers.size());
    }

    // ==================== 角色查询测试 ====================

    @Test
    public void testFindAllByRoles() {
        List<User> admins = userRepository.findAllByRoles("ADMIN");
        assertEquals(1, admins.size());
        assertEquals("john_doe", admins.get(0).getUsername());
    }

    @Test
    public void testFindByRolesIn() {
        List<User> users = userRepository.findByRolesIn(Arrays.asList("ADMIN", "USER"));
        assertEquals(3, users.size());
    }

    // ==================== 统计查询测试 ====================

    @Test
    public void testCountByStatus() {
        long activeCount = userRepository.countByStatus("ACTIVE");
        assertEquals(2, activeCount);

        long inactiveCount = userRepository.countByStatus("INACTIVE");
        assertEquals(1, inactiveCount);
    }

    @Test
    public void testCountByActiveTrue() {
        long count = userRepository.countByActiveTrue();
        assertEquals(2, count);
    }

    // ==================== 存在性检查测试 ====================

    @Test
    public void testExistsByUsername() {
        assertTrue(userRepository.existsByUsername("john_doe"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    public void testExistsByEmail() {
        assertTrue(userRepository.existsByEmail("john@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    // ==================== 分页查询测试 ====================

    @Test
    public void testFindAllWithPageable() {
        PageRequest pageable = PageRequest.of(0, 2);
        Page<User> page = userRepository.findAll(pageable);

        assertEquals(2, page.getSize());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertTrue(page.hasNext());
        assertFalse(page.hasPrevious());
    }

    @Test
    public void testFindByStatusWithPageable() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "username"));
        Page<User> page = userRepository.findByStatus("ACTIVE", pageable);

        assertEquals(2, page.getTotalElements());
        assertEquals("jane_smith", page.getContent().get(0).getUsername());
        assertEquals("john_doe", page.getContent().get(1).getUsername());
    }

    @Test
    public void testFindByUsernameContainingWithPageable() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.findByUsernameContaining("e", pageable);
        assertEquals(3, page.getTotalElements());
    }

    // ==================== 排序查询测试 ====================

    @Test
    public void testFindAllByOrderByUsernameAsc() {
        List<User> users = userRepository.findAllByOrderByUsernameAsc();
        assertEquals(3, users.size());
        assertEquals("bob_wilson", users.get(0).getUsername());
        assertEquals("jane_smith", users.get(1).getUsername());
        assertEquals("john_doe", users.get(2).getUsername());
    }

    @Test
    public void testFindAllByOrderByCreatedAtDesc() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        assertEquals(3, users.size());
    }

    // ==================== 删除查询测试 ====================

    @Test
    public void testDeleteByUsername() {
        long deleted = userRepository.deleteByUsername("john_doe");
        assertEquals(1, deleted);
        assertEquals(2, userRepository.count());
    }

    @Test
    public void testDeleteByStatus() {
        long deleted = userRepository.deleteByStatus("INACTIVE");
        assertEquals(1, deleted);
        assertEquals(2, userRepository.count());
    }

    // ==================== NULL 查询测试 ====================

    @Test
    public void testFindByNicknameIsNull() {
        // 所有用户的 nickname 都为 null（未设置）
        List<User> users = userRepository.findByNicknameIsNull();
        assertEquals(3, users.size());
    }
}
```

### 6.8.2 OrderRepository 测试

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Order;
import com.example.mongo.entity.Order.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private String testUserId = "user123";

    @BeforeEach
    public void setUp() {
        orderRepository.deleteAll();

        // 创建测试订单
        Order order1 = createOrder(testUserId, "ORD001", "PAID", 100.0);
        order1.setCreatedAt(LocalDateTime.now().minusDays(5));
        orderRepository.save(order1);

        Order order2 = createOrder(testUserId, "ORD002", "PENDING", 200.0);
        order2.setCreatedAt(LocalDateTime.now().minusDays(3));
        orderRepository.save(order2);

        Order order3 = createOrder(testUserId, "ORD003", "SHIPPED", 300.0);
        order3.setCreatedAt(LocalDateTime.now().minusDays(1));
        orderRepository.save(order3);

        Order order4 = createOrder("user456", "ORD004", "PAID", 400.0);
        order4.setCreatedAt(LocalDateTime.now().minusDays(2));
        orderRepository.save(order4);
    }

    private Order createOrder(String userId, String orderNumber, String status, Double amount) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(orderNumber);
        order.setStatus(status);
        order.setTotalAmount(amount);

        Address address = new Address();
        address.setProvince("Beijing");
        address.setCity("Beijing");
        address.setDistrict("Chaoyang");
        address.setStreet("Some Street");
        address.setZipCode("100000");
        order.setShippingAddress(address);

        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    // ==================== 基本查询测试 ====================

    @Test
    public void testFindByUserId() {
        List<Order> orders = orderRepository.findByUserId(testUserId);
        assertEquals(3, orders.size());
    }

    @Test
    public void testFindByOrderNumber() {
        Optional<Order> order = orderRepository.findByOrderNumber("ORD001");
        assertTrue(order.isPresent());
        assertEquals(testUserId, order.get().getUserId());
    }

    @Test
    public void testFindByUserIdAndStatus() {
        List<Order> orders = orderRepository.findByUserIdAndStatus(testUserId, "PAID");
        assertEquals(1, orders.size());
        assertEquals("ORD001", orders.get(0).getOrderNumber());
    }

    @Test
    public void testFindByStatus() {
        List<Order> orders = orderRepository.findByStatus("PAID");
        assertEquals(2, orders.size());
    }

    // ==================== 日期范围查询测试 ====================

    @Test
    public void testFindByCreatedAtBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(4);
        LocalDateTime end = LocalDateTime.now();
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        assertEquals(3, orders.size());
    }

    @Test
    public void testFindByUserIdAndCreatedAtAfter() {
        LocalDateTime after = LocalDateTime.now().minusDays(4);
        List<Order> orders = orderRepository.findByUserIdAndCreatedAtAfter(testUserId, after);
        assertEquals(2, orders.size());
    }

    // ==================== 统计查询测试 ====================

    @Test
    public void testCountByStatus() {
        long paidCount = orderRepository.countByStatus("PAID");
        assertEquals(2, paidCount);

        long pendingCount = orderRepository.countByStatus("PENDING");
        assertEquals(1, pendingCount);
    }

    // ==================== 存在性检查测试 ====================

    @Test
    public void testExistsByOrderNumber() {
        assertTrue(orderRepository.existsByOrderNumber("ORD001"));
        assertFalse(orderRepository.existsByOrderNumber("NONEXISTENT"));
    }

    // ==================== 分页查询测试 ====================

    @Test
    public void testFindByUserIdWithPageable() {
        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> page = orderRepository.findByUserId(testUserId, pageable);

        assertEquals(2, page.getSize());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    public void testFindByUserIdOrderByCreatedAtDesc() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Order> page = orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable);

        assertEquals(3, page.getTotalElements());
        // 最近的订单应该在第一个
        assertEquals("ORD003", page.getContent().get(0).getOrderNumber());
    }

    @Test
    public void testFindByUserIdAndStatusWithPageable() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Order> page = orderRepository.findByUserIdAndStatus(
            testUserId, "PAID", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("ORD001", page.getContent().get(0).getOrderNumber());
    }
}
```

### 6.8.3 ProductRepository 测试

```java
package com.example.mongo.repository;

import com.example.mongo.entity.Product;
import com.example.mongo.entity.Product.ProductCategory;
import com.example.mongo.entity.Product.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    public void setUp() {
        productRepository.deleteAll();

        // 创建测试产品
        Product p1 = createProduct("iPhone 15", ProductCategory.ELECTRONICS,
            new BigDecimal("7999.00"), 100, ProductStatus.ACTIVE);
        productRepository.save(p1);

        Product p2 = createProduct("MacBook Pro", ProductCategory.ELECTRONICS,
            new BigDecimal("14999.00"), 50, ProductStatus.ACTIVE);
        productRepository.save(p2);

        Product p3 = createProduct("Nike Shoes", ProductCategory.CLOTHING,
            new BigDecimal("899.00"), 200, ProductStatus.ACTIVE);
        productRepository.save(p3);

        Product p4 = createProduct("iPad Air", ProductCategory.ELECTRONICS,
            new BigDecimal("4999.00"), 75, ProductStatus.INACTIVE);
        productRepository.save(p4);

        Product p5 = createProduct("Java Book", ProductCategory.BOOKS,
            new BigDecimal("129.00"), 500, ProductStatus.ACTIVE);
        productRepository.save(p5);
    }

    private Product createProduct(String name, ProductCategory category,
            BigDecimal price, Integer stock, ProductStatus status) {
        Product product = new Product();
        product.setName(name);
        product.setProductCode("CODE-" + name.hashCode());
        product.setCategory(category);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(status);
        product.setDescription("Description for " + name);
        return product;
    }

    // ==================== 分类查询测试 ====================

    @Test
    public void testFindByCategory() {
        List<Product> electronics = productRepository.findByCategory(ProductCategory.ELECTRONICS);
        assertEquals(3, electronics.size());
    }

    @Test
    public void testFindByStatus() {
        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);
        assertEquals(4, activeProducts.size());
    }

    // ==================== 价格查询测试 ====================

    @Test
    public void testFindByPriceBetween() {
        List<Product> products = productRepository.findByPriceBetween(
            new BigDecimal("1000.00"),
            new BigDecimal("10000.00"));
        assertEquals(2, products.size());
    }

    @Test
    public void testFindByPriceLessThanEqual() {
        List<Product> products = productRepository.findByPriceLessThanEqual(
            new BigDecimal("1000.00"));
        assertEquals(2, products.size());
    }

    @Test
    public void testFindByPriceGreaterThanEqual() {
        List<Product> products = productRepository.findByPriceGreaterThanEqual(
            new BigDecimal("5000.00"));
        assertEquals(3, products.size());
    }

    // ==================== 模糊查询测试 ====================

    @Test
    public void testFindByNameContaining() {
        List<Product> products = productRepository.findByNameContaining("iPhone");
        assertEquals(1, products.size());
        assertEquals("iPhone 15", products.get(0).getName());
    }

    @Test
    public void testFindByNameContainingAll() {
        // 创建包含多个关键词的产品
        Product p = new Product();
        p.setName("iPhone 15 Pro Max");
        p.setProductCode("IP15PM");
        p.setCategory(ProductCategory.ELECTRONICS);
        p.setPrice(new BigDecimal("9999.00"));
        p.setStock(30);
        p.setStatus(ProductStatus.ACTIVE);
        productRepository.save(p);

        List<Product> products = productRepository.findByNameContaining("iPhone");
        assertEquals(2, products.size());
    }

    // ==================== 复合查询测试 ====================

    @Test
    public void testFindByCategoryAndStatus() {
        List<Product> products = productRepository.findByCategoryAndStatus(
            ProductCategory.ELECTRONICS, ProductStatus.ACTIVE);
        assertEquals(2, products.size());
    }

    @Test
    public void testFindByNameContainingAndPriceBetween() {
        List<Product> products = productRepository.findByNameContainingAndPriceBetween(
            "iPhone",
            new BigDecimal("5000.00"),
            new BigDecimal("10000.00"));
        assertEquals(1, products.size());
    }

    // ==================== 库存查询测试 ====================

    @Test
    public void testFindByStockLessThan() {
        List<Product> lowStock = productRepository.findByStockLessThan(100);
        assertEquals(3, lowStock.size());
    }

    @Test
    public void testFindByStockLessThanEqual() {
        List<Product> lowStock = productRepository.findByStockLessThanEqual(100);
        assertEquals(4, lowStock.size());
    }

    // ==================== 排序查询测试 ====================

    @Test
    public void testFindByCategoryOrderByPriceAsc() {
        List<Product> products = productRepository.findByCategoryOrderByPriceAsc(
            ProductCategory.ELECTRONICS);
        assertEquals(3, products.size());
        assertEquals("iPad Air", products.get(0).getName()); // 4999
        assertEquals("iPhone 15", products.get(1).getName()); // 7999
        assertEquals("MacBook Pro", products.get(2).getName()); // 14999
    }

    @Test
    public void testFindByCategoryOrderByPriceDesc() {
        List<Product> products = productRepository.findByCategoryOrderByPriceDesc(
            ProductCategory.ELECTRONICS);
        assertEquals(3, products.size());
        assertEquals("MacBook Pro", products.get(0).getName());
        assertEquals("iPhone 15", products.get(1).getName());
        assertEquals("iPad Air", products.get(2).getName());
    }

    // ==================== 统计查询测试 ====================

    @Test
    public void testCountByCategory() {
        long count = productRepository.findByCategory(ProductCategory.ELECTRONICS).size();
        assertEquals(3, count);
    }

    // ==================== 分页查询测试 ====================

    @Test
    public void testFindByCategoryOrderByPriceAscWithPageable() {
        org.springframework.data.domain.PageRequest pageable =
            org.springframework.data.domain.PageRequest.of(0, 2);
        org.springframework.data.domain.Page<Product> page =
            productRepository.findByCategoryOrderByPriceAsc(
                ProductCategory.ELECTRONICS, pageable);

        assertEquals(2, page.getSize());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    public void testFindByCategoryAndStatusOrderByCreatedAtDesc() {
        org.springframework.data.domain.PageRequest pageable =
            org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Product> page =
            productRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                ProductCategory.ELECTRONICS, ProductStatus.ACTIVE, pageable);

        assertEquals(2, page.getTotalElements());
    }
}
```

## 6.9 小结

本章详细介绍了 Spring Data MongoDB 的实体映射与 Repository 开发，主要内容包括：

### 核心注解
- `@Document`：标记 MongoDB 文档类
- `@Id`：标记主键字段
- `@Field`：指定字段映射细节
- `@Indexed`：创建索引
- `@Transient`：标记非持久化字段
- `@CreatedDate`/`@LastModifiedDate`：自动填充时间戳

### 数据类型映射
Spring Data MongoDB 自动支持 Java 基本类型、String、Date、LocalDateTime、BigDecimal、List、Map 以及嵌套文档的映射。

### Repository 接口
通过继承 `MongoRepository` 可以获得完整的 CRUD 操作，同时支持自定义方法命名查询，遵循 Spring Data 的方法命名规范。

### 分页与排序
使用 `Pageable` 和 `Page` 接口实现分页查询，使用 `Sort` 和 `Sort.Order` 实现多字段排序。

### 方法命名查询
支持丰富的查询关键词：`findBy`、`findAllBy`、`existsBy`、`countBy`、`deleteBy`，以及各种比较操作符：`Containing`、`Between`、`LessThan`、`GreaterThan`、`In`、`NotIn` 等。

---

下一章我们将学习 **MongoTemplate 高级操作**，包括复杂查询实现、聚合操作和批量操作。
