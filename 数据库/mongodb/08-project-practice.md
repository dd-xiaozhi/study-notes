---
title: 第8章 Spring Boot + MongoDB 实战项目
sidebar_label: 第8章
---

# 第8章 Spring Boot + MongoDB 实战项目

本文档详细介绍三个完整的 Spring Boot + MongoDB 实战项目：用户管理系统、博客系统和电商订单系统。通过实际项目案例，深入讲解 MongoDB 在不同业务场景下的应用。

## 8.1 需求分析与数据库设计

### 8.1.1 项目工程结构

```
spring-boot-mongodb-project/
├── pom.xml
├── src/main/java/com/example/mongodb/
│   ├── MongoDBApplication.java
│   ├── config/
│   │   └── MongoConfig.java
│   ├── user/
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Role.java
│   │   │   └── Permission.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── RoleRepository.java
│   │   │   └── PermissionRepository.java
│   │   ├── service/
│   │   │   ├── UserService.java
│   │   │   └── RoleService.java
│   │   └── controller/
│   │       └── UserController.java
│   ├── blog/
│   │   ├── entity/
│   │   │   ├── Article.java
│   │   │   ├── Comment.java
│   │   │   └── Tag.java
│   │   ├── repository/
│   │   │   ├── ArticleRepository.java
│   │   │   ├── CommentRepository.java
│   │   │   └── TagRepository.java
│   │   ├── service/
│   │   │   ├── ArticleService.java
│   │   │   └── CommentService.java
│   │   └── controller/
│   │       └── ArticleController.java
│   └── order/
│       ├── entity/
│       │   ├── Product.java
│       │   ├── Cart.java
│       │   ├── CartItem.java
│       │   └── Order.java
│       ├── repository/
│       │   ├── ProductRepository.java
│       │   ├── CartRepository.java
│       │   └── OrderRepository.java
│       ├── service/
│       │   ├── ProductService.java
│       │   ├── CartService.java
│       │   └── OrderService.java
│       └── controller/
│           └── OrderController.java
└── src/main/resources/
    └── application.yml
```

### 8.1.2 用户管理系统设计

#### 数据模型设计

**用户表 (users)**

```json
{
  "_id": "ObjectId",
  "username": "string",
  "password": "string",
  "email": "string",
  "phone": "string",
  "nickname": "string",
  "avatar": "string",
  "status": "number",        // 0: 禁用, 1: 启用
  "roles": ["Role对象"],
  "permissions": ["Permission对象"],
  "createTime": "Date",
  "updateTime": "Date",
  "lastLoginTime": "Date"
}
```

**角色表 (roles)**

```json
{
  "_id": "ObjectId",
  "name": "string",
  "code": "string",
  "description": "string",
  "permissions": ["Permission对象"],
  "createTime": "Date",
  "updateTime": "Date"
}
```

**权限表 (permissions)**

```json
{
  "_id": "ObjectId",
  "name": "string",
  "code": "string",
  "type": "string",         // menu, button, api
  "url": "string",
  "method": "string",       // GET, POST, PUT, DELETE
  "createTime": "Date"
}
```

### 8.1.3 博客系统设计

#### 数据模型设计

**文章表 (articles)**

```json
{
  "_id": "ObjectId",
  "title": "string",
  "content": "string",
  "summary": "string",
  "author": "User对象",
  "tags": ["Tag对象"],
  "category": "string",
  "coverImage": "string",
  "viewCount": "number",
  "likeCount": "number",
  "commentCount": "number",
  "status": "number",       // 0: 草稿, 1: 已发布, 2: 已下架
  "publishTime": "Date",
  "createTime": "Date",
  "updateTime": "Date"
}
```

**评论表 (comments)**

```json
{
  "_id": "ObjectId",
  "articleId": "ObjectId",
  "content": "string",
  "author": "User对象",
  "parentId": "ObjectId",   // 回复的评论ID
  "replyTo": "User对象",    // 回复的用户
  "likeCount": "number",
  "status": "number",       // 0: 待审核, 1: 通过, 2: 违规
  "createTime": "Date"
}
```

**标签表 (tags)**

```json
{
  "_id": "ObjectId",
  "name": "string",
  "slug": "string",
  "color": "string",
  "articleCount": "number",
  "createTime": "Date"
}
```

### 8.1.4 电商订单系统设计

#### 数据模型设计

**商品表 (products)**

```json
{
  "_id": "ObjectId",
  "name": "string",
  "description": "string",
  "price": "number",
  "originalPrice": "number",
  "stock": "number",
  "images": ["string"],
  "category": "string",
  "tags": ["string"],
  "status": "number",       // 0: 下架, 1: 上架
  "createTime": "Date",
  "updateTime": "Date"
}
```

**购物车表 (carts)**

```json
{
  "_id": "ObjectId",
  "userId": "ObjectId",
  "items": [
    {
      "productId": "ObjectId",
      "productName": "string",
      "price": "number",
      "quantity": "number",
      "selected": "boolean"
    }
  ],
  "totalPrice": "number",
  "updateTime": "Date"
}
```

**订单表 (orders)**

```json
{
  "_id": "ObjectId",
  "orderNo": "string",
  "userId": "ObjectId",
  "items": [
    {
      "productId": "ObjectId",
      "productName": "string",
      "price": "number",
      "quantity": "number"
    }
  ],
  "totalPrice": "number",
  "status": "number",       // 0: 待支付, 1: 已支付, 2: 发货中, 3: 已完成, 4: 已取消
  "shippingAddress": {
    "receiver": "string",
    "phone": "string",
    "address": "string"
  },
  "createTime": "Date",
  "payTime": "Date",
  "shipTime": "Date",
  "completeTime": "Date"
}
```

---

## 8.2 用户管理系统

### 8.2.1 配置文件

**pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>spring-boot-mongodb</artifactId>
    <version>1.0.0</version>
    <name>Spring Boot MongoDB</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data MongoDB -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**application.yml**

```yaml
spring:
  application:
    name: spring-boot-mongodb
  data:
    mongodb:
      uri: mongodb://localhost:27017/mongodb_demo
      database: mongodb_demo

server:
  port: 8080

jwt:
  secret: mySecretKey1234567890abcdefghijklmnopqrstuvwxyz
  expiration: 86400000
```

### 8.2.2 实体类设计

**User.java**

```java
package com.example.mongodb.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    @Indexed(unique = true)
    private String email;

    private String phone;

    private String nickname;

    private String avatar;

    /**
     * 0: 禁用, 1: 启用
     */
    @Builder.Default
    private Integer status = 1;

    private List<Role> roles;

    private List<Permission> permissions;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime lastLoginTime;
}
```

**Role.java**

```java
package com.example.mongodb.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String code;

    private String description;

    private List<Permission> permissions;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**Permission.java**

```java
package com.example.mongodb.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permissions")
public class Permission {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String code;

    /**
     * menu, button, api
     */
    private String type;

    private String url;

    /**
     * GET, POST, PUT, DELETE
     */
    private String method;

    private LocalDateTime createTime;
}
```

### 8.2.3 Repository 层

**UserRepository.java**

```java
package com.example.mongodb.user.repository;

import com.example.mongodb.user.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("{ 'username': { $regex: ?0 } }")
    List<User> searchByUsername(String keyword);

    @Query("{ 'roles.code': ?0 }")
    List<User> findByRoleCode(String roleCode);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
```

**RoleRepository.java**

```java
package com.example.mongodb.user.repository;

import com.example.mongodb.user.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);
}
```

**PermissionRepository.java**

```java
package com.example.mongodb.user.repository;

import com.example.mongodb.user.entity.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);
}
```

### 8.2.4 Service 层

**UserService.java**

```java
package com.example.mongodb.user.service;

import com.example.mongodb.user.entity.Permission;
import com.example.mongodb.user.entity.Role;
import com.example.mongodb.user.entity.User;
import com.example.mongodb.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     */
    public User register(User user) {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否存在
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 设置默认角色
        Role defaultRole = Role.builder()
                .id("1")
                .name("普通用户")
                .code("user")
                .build();
        user.setRoles(List.of(defaultRole));

        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        return userRepository.save(user);
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 根据ID查找用户
     */
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 获取用户所有权限
     */
    public List<Permission> getUserPermissions(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(User user, String permissionCode) {
        return getUserPermissions(user).stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    /**
     * 更新用户信息
     */
    public User updateUser(String id, User updatedUser) {
        User user = findById(id);

        if (updatedUser.getNickname() != null) {
            user.setNickname(updatedUser.getNickname());
        }
        if (updatedUser.getEmail() != null) {
            user.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getPhone() != null) {
            user.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getAvatar() != null) {
            user.setAvatar(updatedUser.getAvatar());
        }

        user.setUpdateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 修改用户状态
     */
    public void changeStatus(String id, Integer status) {
        User user = findById(id);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 给用户分配角色
     */
    public User assignRoles(String userId, List<Role> roles) {
        User user = findById(userId);
        user.setRoles(roles);
        user.setUpdateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(String id, String oldPassword, String newPassword) {
        User user = findById(id);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 删除用户
     */
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    /**
     * 搜索用户
     */
    public List<User> searchUsers(String keyword) {
        return userRepository.searchByUsername(keyword);
    }

    /**
     * 获取所有用户
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
```

**RoleService.java**

```java
package com.example.mongodb.user.service;

import com.example.mongodb.user.entity.Permission;
import com.example.mongodb.user.entity.Role;
import com.example.mongodb.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * 创建角色
     */
    public Role createRole(Role role) {
        if (roleRepository.existsByCode(role.getCode())) {
            throw new RuntimeException("角色编码已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        role.setCreateTime(now);
        role.setUpdateTime(now);

        return roleRepository.save(role);
    }

    /**
     * 更新角色
     */
    public Role updateRole(String id, Role updatedRole) {
        Role role = findById(id);

        if (updatedRole.getName() != null) {
            role.setName(updatedRole.getName());
        }
        if (updatedRole.getDescription() != null) {
            role.setDescription(updatedRole.getDescription());
        }
        if (updatedRole.getPermissions() != null) {
            role.setPermissions(updatedRole.getPermissions());
        }

        role.setUpdateTime(LocalDateTime.now());
        return roleRepository.save(role);
    }

    /**
     * 根据ID查找角色
     */
    public Role findById(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
    }

    /**
     * 根据编码查找角色
     */
    public Role findByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
    }

    /**
     * 删除角色
     */
    public void deleteRole(String id) {
        roleRepository.deleteById(id);
    }

    /**
     * 获取所有角色
     */
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * 给角色添加权限
     */
    public Role addPermission(String roleId, Permission permission) {
        Role role = findById(roleId);
        role.getPermissions().add(permission);
        role.setUpdateTime(LocalDateTime.now());
        return roleRepository.save(role);
    }

    /**
     * 移除角色权限
     */
    public Role removePermission(String roleId, String permissionCode) {
        Role role = findById(roleId);
        role.getPermissions().removeIf(p -> p.getCode().equals(permissionCode));
        role.setUpdateTime(LocalDateTime.now());
        return roleRepository.save(role);
    }
}
```

### 8.2.5 Controller 层

**UserController.java**

```java
package com.example.mongodb.user.controller;

import com.example.mongodb.user.entity.User;
import com.example.mongodb.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        User registeredUser = userService.register(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "注册成功");
        response.put("data", registeredUser);

        return ResponseEntity.ok(response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        User user = userService.login(username, password);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登录成功");
        response.put("data", user);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
        User user = userService.findById(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", user);

        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id) {
        User user = userService.findById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", user);

        return ResponseEntity.ok(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String id,
            @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "更新成功");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * 修改密码
     */
    @PostMapping("/{id}/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable String id,
            @RequestBody Map<String, String> passwordData) {
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        userService.changePassword(id, oldPassword, newPassword);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "密码修改成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 修改用户状态
     */
    @PostMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @PathVariable String id,
            @RequestBody Map<String, Integer> statusData) {
        userService.changeStatus(id, statusData.get("status"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "状态修改成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        List<User> users = userService.searchUsers(keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", users);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有用户
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userService.findAllUsers();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", users);

        return ResponseEntity.ok(response);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "删除成功");

        return ResponseEntity.ok(response);
    }
}
```

---

## 8.3 博客系统

### 8.3.1 实体类设计

**Article.java**

```java
package com.example.mongodb.blog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "articles")
public class Article {

    @Id
    private String id;

    @TextIndexed(weight = 10)
    private String title;

    @TextIndexed(weight = 5)
    private String content;

    private String summary;

    private String authorId;

    private String authorName;

    private String authorAvatar;

    private List<Tag> tags;

    @Indexed
    private String category;

    private String coverImage;

    @Builder.Default
    private Integer viewCount = 0;

    @Builder.Default
    private Integer likeCount = 0;

    @Builder.Default
    private Integer commentCount = 0;

    /**
     * 0: 草稿, 1: 已发布, 2: 已下架
     */
    @Builder.Default
    private Integer status = 0;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**Comment.java**

```java
package com.example.mongodb.blog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    @Indexed
    private String articleId;

    private String content;

    private String authorId;

    private String authorName;

    private String authorAvatar;

    /**
     * 回复的评论ID
     */
    @Indexed
    private String parentId;

    /**
     * 回复的用户ID
     */
    private String replyToId;

    private String replyToName;

    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 0: 待审核, 1: 通过, 2: 违规
     */
    @Builder.Default
    private Integer status = 1;

    private LocalDateTime createTime;
}
```

**Tag.java**

```java
package com.example.mongodb.blog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tags")
public class Tag {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String slug;

    private String color;

    @Builder.Default
    private Integer articleCount = 0;

    private LocalDateTime createTime;
}
```

### 8.3.2 Repository 层

**ArticleRepository.java**

```java
package com.example.mongodb.blog.repository;

import com.example.mongodb.blog.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {

    /**
     * 根据状态查询文章
     */
    Page<Article> findByStatus(Integer status, Pageable pageable);

    /**
     * 根据作者ID查询文章
     */
    Page<Article> findByAuthorId(String authorId, Pageable pageable);

    /**
     * 根据分类查询文章
     */
    Page<Article> findByCategory(String category, Pageable pageable);

    /**
     * 根据标签名称查询文章
     */
    @Query("{ 'tags.name': ?0 }")
    Page<Article> findByTagName(String tagName, Pageable pageable);

    /**
     * 搜索文章（按标题）
     */
    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    Page<Article> searchByTitle(String keyword, Pageable pageable);

    /**
     * 查询热门文章
     */
    @Query("{ 'status': 1 }")
    Page<Article> findHotArticles(Pageable pageable);

    /**
     * 根据ID列表查询
     */
    List<Article> findByIdIn(List<String> ids);
}
```

**CommentRepository.java**

```java
package com.example.mongodb.blog.repository;

import com.example.mongodb.blog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    /**
     * 根据文章ID查询评论
     */
    Page<Comment> findByArticleId(String articleId, Pageable pageable);

    /**
     * 查询顶级评论（parentId为null）
     */
    @Query("{ 'articleId': ?0, 'parentId': null }")
    Page<Comment> findTopLevelComments(String articleId, Pageable pageable);

    /**
     * 根据父评论ID查询回复
     */
    List<Comment> findByParentId(String parentId);

    /**
     * 统计文章评论数
     */
    long countByArticleId(String articleId);

    /**
     * 根据用户ID查询评论
     */
    Page<Comment> findByAuthorId(String authorId, Pageable pageable);
}
```

**TagRepository.java**

```java
package com.example.mongodb.blog.repository;

import com.example.mongodb.blog.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    /**
     * 查询热门标签
     */
    List<Tag> findTop10ByOrderByArticleCountDesc();
}
```

### 8.3.3 Service 层

**ArticleService.java**

```java
package com.example.mongodb.blog.service;

import com.example.mongodb.blog.entity.Article;
import com.example.mongodb.blog.entity.Tag;
import com.example.mongodb.blog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    /**
     * 创建文章
     */
    public Article createArticle(Article article) {
        LocalDateTime now = LocalDateTime.now();
        article.setCreateTime(now);
        article.setUpdateTime(now);

        if (article.getStatus() == 1) {
            article.setPublishTime(now);
        }

        return articleRepository.save(article);
    }

    /**
     * 更新文章
     */
    public Article updateArticle(String id, Article updatedArticle) {
        Article article = findById(id);

        if (updatedArticle.getTitle() != null) {
            article.setTitle(updatedArticle.getTitle());
        }
        if (updatedArticle.getContent() != null) {
            article.setContent(updatedArticle.getContent());
        }
        if (updatedArticle.getSummary() != null) {
            article.setSummary(updatedArticle.getSummary());
        }
        if (updatedArticle.getCoverImage() != null) {
            article.setCoverImage(updatedArticle.getCoverImage());
        }
        if (updatedArticle.getCategory() != null) {
            article.setCategory(updatedArticle.getCategory());
        }
        if (updatedArticle.getTags() != null) {
            article.setTags(updatedArticle.getTags());
        }

        // 如果从草稿发布，设置发布时间
        if (article.getStatus() == 0 && updatedArticle.getStatus() == 1) {
            article.setPublishTime(LocalDateTime.now());
        }

        article.setStatus(updatedArticle.getStatus());
        article.setUpdateTime(LocalDateTime.now());

        return articleRepository.save(article);
    }

    /**
     * 根据ID查找文章
     */
    public Article findById(String id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
    }

    /**
     * 删除文章
     */
    public void deleteArticle(String id) {
        articleRepository.deleteById(id);
    }

    /**
     * 分页查询已发布文章
     */
    public Page<Article> getPublishedArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishTime"));
        return articleRepository.findByStatus(1, pageable);
    }

    /**
     * 根据作者查询文章
     */
    public Page<Article> getArticlesByAuthor(String authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return articleRepository.findByAuthorId(authorId, pageable);
    }

    /**
     * 根据分类查询文章
     */
    public Page<Article> getArticlesByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishTime"));
        return articleRepository.findByCategory(category, pageable);
    }

    /**
     * 根据标签查询文章
     */
    public Page<Article> getArticlesByTag(String tagName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishTime"));
        return articleRepository.findByTagName(tagName, pageable);
    }

    /**
     * 搜索文章
     */
    public Page<Article> searchArticles(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishTime"));
        return articleRepository.searchByTitle(keyword, pageable);
    }

    /**
     * 获取热门文章
     */
    public List<Article> getHotArticles(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return articleRepository.findHotArticles(pageable).getContent();
    }

    /**
     * 增加浏览量
     */
    public void incrementViewCount(String id) {
        Article article = findById(id);
        article.setViewCount(article.getViewCount() + 1);
        articleRepository.save(article);
    }

    /**
     * 增加点赞数
     */
    public void incrementLikeCount(String id) {
        Article article = findById(id);
        article.setLikeCount(article.getLikeCount() + 1);
        articleRepository.save(article);
    }

    /**
     * 增加评论数
     */
    public void incrementCommentCount(String id) {
        Article article = findById(id);
        article.setCommentCount(article.getCommentCount() + 1);
        articleRepository.save(article);
    }

    /**
     * 减少评论数
     */
    public void decrementCommentCount(String id) {
        Article article = findById(id);
        if (article.getCommentCount() > 0) {
            article.setCommentCount(article.getCommentCount() - 1);
            articleRepository.save(article);
        }
    }
}
```

**CommentService.java**

```java
package com.example.mongodb.blog.service;

import com.example.mongodb.blog.entity.Comment;
import com.example.mongodb.blog.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleService articleService;

    /**
     * 添加评论
     */
    public Comment addComment(Comment comment) {
        comment.setCreateTime(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        // 增加文章评论数
        articleService.incrementCommentCount(comment.getArticleId());

        return savedComment;
    }

    /**
     * 回复评论
     */
    public Comment replyComment(String parentId, Comment reply) {
        Comment parentComment = findById(parentId);

        reply.setParentId(parentId);
        reply.setArticleId(parentComment.getArticleId());
        reply.setReplyToId(parentComment.getAuthorId());
        reply.setReplyToName(parentComment.getAuthorName());
        reply.setCreateTime(LocalDateTime.now());

        return commentRepository.save(reply);
    }

    /**
     * 根据ID查找评论
     */
    public Comment findById(String id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
    }

    /**
     * 获取文章的所有顶级评论
     */
    public Page<Comment> getArticleComments(String articleId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createTime"));
        return commentRepository.findTopLevelComments(articleId, pageable);
    }

    /**
     * 获取评论的所有回复
     */
    public List<Comment> getCommentReplies(String parentId) {
        return commentRepository.findByParentId(parentId);
    }

    /**
     * 点赞评论
     */
    public void likeComment(String id) {
        Comment comment = findById(id);
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
    }

    /**
     * 审核评论
     */
    public void moderateComment(String id, Integer status) {
        Comment comment = findById(id);
        comment.setStatus(status);
        commentRepository.save(comment);
    }

    /**
     * 删除评论
     */
    public void deleteComment(String id) {
        Comment comment = findById(id);
        commentRepository.delete(comment);

        // 减少文章评论数
        articleService.decrementCommentCount(comment.getArticleId());
    }

    /**
     * 获取用户的评论
     */
    public Page<Comment> getUserComments(String authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return commentRepository.findByAuthorId(authorId, pageable);
    }
}
```

### 8.3.4 Controller 层

**ArticleController.java**

```java
package com.example.mongodb.blog.controller;

import com.example.mongodb.blog.entity.Article;
import com.example.mongodb.blog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 创建文章
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createArticle(@RequestBody Article article) {
        Article createdArticle = articleService.createArticle(article);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文章创建成功");
        response.put("data", createdArticle);

        return ResponseEntity.ok(response);
    }

    /**
     * 更新文章
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateArticle(
            @PathVariable String id,
            @RequestBody Article article) {
        Article updatedArticle = articleService.updateArticle(id, article);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文章更新成功");
        response.put("data", updatedArticle);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getArticleById(@PathVariable String id) {
        // 增加浏览量
        articleService.incrementViewCount(id);

        Article article = articleService.findById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", article);

        return ResponseEntity.ok(response);
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteArticle(@PathVariable String id) {
        articleService.deleteArticle(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文章删除成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取已发布文章列表
     */
    @GetMapping("/published")
    public ResponseEntity<Map<String, Object>> getPublishedArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Article> articles = articleService.getPublishedArticles(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles.getContent());
        response.put("total", articles.getTotalElements());
        response.put("totalPages", articles.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据作者获取文章
     */
    @GetMapping("/author/{authorId}")
    public ResponseEntity<Map<String, Object>> getArticlesByAuthor(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Article> articles = articleService.getArticlesByAuthor(authorId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles.getContent());
        response.put("total", articles.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据分类获取文章
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getArticlesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Article> articles = articleService.getArticlesByCategory(category, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles.getContent());
        response.put("total", articles.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据标签获取文章
     */
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<Map<String, Object>> getArticlesByTag(
            @PathVariable String tagName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Article> articles = articleService.getArticlesByTag(tagName, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles.getContent());
        response.put("total", articles.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 搜索文章
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Article> articles = articleService.searchArticles(keyword, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles.getContent());
        response.put("total", articles.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取热门文章
     */
    @GetMapping("/hot")
    public ResponseEntity<Map<String, Object>> getHotArticles(
            @RequestParam(defaultValue = "10") int limit) {
        List<Article> articles = articleService.getHotArticles(limit);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles);

        return ResponseEntity.ok(response);
    }

    /**
     * 点赞文章
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> likeArticle(@PathVariable String id) {
        articleService.incrementLikeCount(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "点赞成功");

        return ResponseEntity.ok(response);
    }
}
```

---

## 8.4 电商订单系统

### 8.4.1 实体类设计

**Product.java**

```java
package com.example.mongodb.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    @Builder.Default
    private Integer stock = 0;

    private List<String> images;

    @Indexed
    private String category;

    private List<String> tags;

    /**
     * 0: 下架, 1: 上架
     */
    @Builder.Default
    private Integer status = 1;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**Cart.java**

```java
package com.example.mongodb.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private BigDecimal totalPrice;

    private LocalDateTime updateTime;

    /**
     * 计算购物车总价
     */
    public void calculateTotalPrice() {
        this.totalPrice = items.stream()
                .filter(CartItem::getSelected)
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

**CartItem.java**

```java
package com.example.mongodb.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    private String productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    @Builder.Default
    private Boolean selected = true;

    /**
     * 计算小计
     */
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

**Order.java**

```java
package com.example.mongodb.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderNo;

    @Indexed
    private String userId;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalPrice;

    /**
     * 0: 待支付, 1: 已支付, 2: 发货中, 3: 已完成, 4: 已取消
     */
    @Builder.Default
    private Integer status = 0;

    private ShippingAddress shippingAddress;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime completeTime;

    private LocalDateTime cancelTime;
}
```

**OrderItem.java**

```java
package com.example.mongodb.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private String productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}
```

**ShippingAddress.java**

```java
package com.example.mongodb.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    private String receiver;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    public String getFullAddress() {
        return province + city + district + detail;
    }
}
```

### 8.4.2 Repository 层

**ProductRepository.java**

```java
package com.example.mongodb.order.repository;

import com.example.mongodb.order.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    /**
     * 根据分类查询商品
     */
    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * 根据状态查询商品
     */
    Page<Product> findByStatus(Integer status, Pageable pageable);

    /**
     * 搜索商品
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Page<Product> searchByName(String keyword, Pageable pageable);

    /**
     * 根据标签查询商品
     */
    @Query("{ 'tags': ?0 }")
    List<Product> findByTag(String tag);

    /**
     * 查询上架商品
     */
    List<Product> findByStatusOrderByCreateTimeDesc(Integer status);
}
```

**CartRepository.java**

```java
package com.example.mongodb.order.repository;

import com.example.mongodb.order.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByUserId(String userId);
}
```

**OrderRepository.java**

```java
package com.example.mongodb.order.repository;

import com.example.mongodb.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    /**
     * 根据订单号查询
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 根据用户ID查询订单
     */
    Page<Order> findByUserId(String userId, Pageable pageable);

    /**
     * 根据用户ID和状态查询订单
     */
    Page<Order> findByUserIdAndStatus(String userId, Integer status, Pageable pageable);
}
```

### 8.4.3 Service 层

**ProductService.java**

```java
package com.example.mongodb.order.service;

import com.example.mongodb.order.entity.Product;
import com.example.mongodb.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 创建商品
     */
    public Product createProduct(Product product) {
        LocalDateTime now = LocalDateTime.now();
        product.setCreateTime(now);
        product.setUpdateTime(now);
        return productRepository.save(product);
    }

    /**
     * 更新商品
     */
    public Product updateProduct(String id, Product updatedProduct) {
        Product product = findById(id);

        if (updatedProduct.getName() != null) {
            product.setName(updatedProduct.getName());
        }
        if (updatedProduct.getDescription() != null) {
            product.setDescription(updatedProduct.getDescription());
        }
        if (updatedProduct.getPrice() != null) {
            product.setPrice(updatedProduct.getPrice());
        }
        if (updatedProduct.getOriginalPrice() != null) {
            product.setOriginalPrice(updatedProduct.getOriginalPrice());
        }
        if (updatedProduct.getStock() != null) {
            product.setStock(updatedProduct.getStock());
        }
        if (updatedProduct.getImages() != null) {
            product.setImages(updatedProduct.getImages());
        }
        if (updatedProduct.getCategory() != null) {
            product.setCategory(updatedProduct.getCategory());
        }
        if (updatedProduct.getTags() != null) {
            product.setTags(updatedProduct.getTags());
        }
        if (updatedProduct.getStatus() != null) {
            product.setStatus(updatedProduct.getStatus());
        }

        product.setUpdateTime(LocalDateTime.now());
        return productRepository.save(product);
    }

    /**
     * 根据ID查找商品
     */
    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
    }

    /**
     * 删除商品
     */
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    /**
     * 分页查询上架商品
     */
    public Page<Product> getOnSaleProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByStatus(1, pageable);
    }

    /**
     * 根据分类查询商品
     */
    public Page<Product> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByCategory(category, pageable);
    }

    /**
     * 搜索商品
     */
    public Page<Product> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.searchByName(keyword, pageable);
    }

    /**
     * 减少库存
     */
    public void reduceStock(String productId, Integer quantity) {
        Product product = findById(productId);
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }
        product.setStock(product.getStock() - quantity);
        product.setUpdateTime(LocalDateTime.now());
        productRepository.save(product);
    }

    /**
     * 增加库存
     */
    public void increaseStock(String productId, Integer quantity) {
        Product product = findById(productId);
        product.setStock(product.getStock() + quantity);
        product.setUpdateTime(LocalDateTime.now());
        productRepository.save(product);
    }

    /**
     * 上架商品
     */
    public void publishProduct(String id) {
        updateProductStatus(id, 1);
    }

    /**
     * 下架商品
     */
    public void unpublishProduct(String id) {
        updateProductStatus(id, 0);
    }

    private void updateProductStatus(String id, Integer status) {
        Product product = findById(id);
        product.setStatus(status);
        product.setUpdateTime(LocalDateTime.now());
        productRepository.save(product);
    }
}
```

**CartService.java**

```java
package com.example.mongodb.order.service;

import com.example.mongodb.order.entity.Cart;
import com.example.mongodb.order.entity.CartItem;
import com.example.mongodb.order.entity.Product;
import com.example.mongodb.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;

    /**
     * 获取用户购物车
     */
    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
    }

    /**
     * 创建空购物车
     */
    private Cart createEmptyCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .build();
        return cartRepository.save(cart);
    }

    /**
     * 添加商品到购物车
     */
    public Cart addToCart(String userId, String productId, Integer quantity) {
        Product product = productService.findById(productId);

        if (product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }

        Cart cart = getCart(userId);

        // 检查商品是否已在购物车中
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // 数量增加
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            // 添加新商品
            CartItem newItem = CartItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .productImage(product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0) : null)
                    .price(product.getPrice())
                    .quantity(quantity)
                    .selected(true)
                    .build();
            cart.getItems().add(newItem);
        }

        cart.calculateTotalPrice();
        cart.setUpdateTime(LocalDateTime.now());

        return cartRepository.save(cart);
    }

    /**
     * 从购物车移除商品
     */
    public Cart removeFromCart(String userId, String productId) {
        Cart cart = getCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.calculateTotalPrice();
        cart.setUpdateTime(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * 更新购物车商品数量
     */
    public Cart updateCartItemQuantity(String userId, String productId, Integer quantity) {
        Cart cart = getCart(userId);

        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));

        cart.calculateTotalPrice();
        cart.setUpdateTime(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * 选择/取消选择购物车商品
     */
    public Cart toggleCartItemSelection(String userId, String productId, Boolean selected) {
        Cart cart = getCart(userId);

        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setSelected(selected));

        cart.calculateTotalPrice();
        cart.setUpdateTime(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * 清空购物车
     */
    public void clearCart(String userId) {
        Cart cart = getCart(userId);
        cart.getItems().clear();
        cart.setTotalPrice(java.math.BigDecimal.ZERO);
        cart.setUpdateTime(LocalDateTime.now());
        cartRepository.save(cart);
    }

    /**
     * 获取已选择的商品
     */
    public List<CartItem> getSelectedItems(String userId) {
        Cart cart = getCart(userId);
        return cart.getItems().stream()
                .filter(CartItem::getSelected)
                .toList();
    }
}
```

**OrderService.java**

```java
package com.example.mongodb.order.service;

import com.example.mongodb.order.entity.*;
import com.example.mongodb.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductService productService;

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(String userId, ShippingAddress shippingAddress) {
        // 获取已选择的购物车商品
        List<CartItem> selectedItems = cartService.getSelectedItems(userId);

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("购物车中没有选中的商品");
        }

        // 构建订单项
        List<OrderItem> orderItems = selectedItems.stream()
                .map(cartItem -> {
                    // 扣减库存
                    productService.reduceStock(cartItem.getProductId(), cartItem.getQuantity());

                    return OrderItem.builder()
                            .productId(cartItem.getProductId())
                            .productName(cartItem.getProductName())
                            .productImage(cartItem.getProductImage())
                            .price(cartItem.getPrice())
                            .quantity(cartItem.getQuantity())
                            .subtotal(cartItem.getSubtotal())
                            .build();
                })
                .collect(Collectors.toList());

        // 计算总价
        BigDecimal totalPrice = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 生成订单号
        String orderNo = generateOrderNo();

        // 创建订单
        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .items(orderItems)
                .totalPrice(totalPrice)
                .status(0) // 待支付
                .shippingAddress(shippingAddress)
                .createTime(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 清空购物车中已购买的商品
        clearPurchasedItemsFromCart(userId, selectedItems);

        return savedOrder;
    }

    /**
     * 清除购物车中已购买的商品
     */
    private void clearPurchasedItemsFromCart(String userId, List<CartItem> purchasedItems) {
        Cart cart = cartService.getCart(userId);
        List<String> purchasedProductIds = purchasedItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        cart.getItems().removeIf(item -> purchasedProductIds.contains(item.getProductId()));
        cart.calculateTotalPrice();
        cart.setUpdateTime(LocalDateTime.now());
        cartService.getCart(userId); // 保存更新
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD" + timestamp + uuid;
    }

    /**
     * 根据订单号查询订单
     */
    public Order findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    /**
     * 根据ID查询订单
     */
    public Order findById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    /**
     * 获取用户订单列表
     */
    public Page<Order> getUserOrders(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * 获取用户特定状态的订单
     */
    public Page<Order> getUserOrdersByStatus(String userId, Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
        return orderRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * 支付订单
     */
    @Transactional
    public Order payOrder(String orderNo) {
        Order order = findByOrderNo(orderNo);

        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不允许支付");
        }

        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * 发货
     */
    public Order shipOrder(String orderNo) {
        Order order = findByOrderNo(orderNo);

        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许发货");
        }

        order.setStatus(2); // 发货中
        order.setShipTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * 确认收货
     */
    public Order confirmReceipt(String orderNo) {
        Order order = findByOrderNo(orderNo);

        if (order.getStatus() != 2) {
            throw new RuntimeException("订单状态不允许确认收货");
        }

        order.setStatus(3); // 已完成
        order.setCompleteTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public Order cancelOrder(String orderNo) {
        Order order = findByOrderNo(orderNo);

        if (order.getStatus() > 1) {
            throw new RuntimeException("订单已发货，无法取消");
        }

        // 如果已支付，退款并恢复库存
        if (order.getStatus() == 1) {
            refundAndRestoreStock(order);
        }

        order.setStatus(4); // 已取消
        order.setCancelTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * 退款并恢复库存
     */
    private void refundAndRestoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }
    }
}
```

### 8.4.4 Controller 层

**OrderController.java**

```java
package com.example.mongodb.order.controller;

import com.example.mongodb.order.entity.Cart;
import com.example.mongodb.order.entity.Order;
import com.example.mongodb.order.entity.Product;
import com.example.mongodb.order.entity.ShippingAddress;
import com.example.mongodb.order.service.CartService;
import com.example.mongodb.order.service.OrderService;
import com.example.mongodb.order.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;

    // ==================== 商品接口 ====================

    /**
     * 创建商品
     */
    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "商品创建成功");
        response.put("data", createdProduct);

        return ResponseEntity.ok(response);
    }

    /**
     * 更新商品
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable String id,
            @RequestBody Product product) {
        Product updatedProduct = productService.updateProduct(id, product);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "商品更新成功");
        response.put("data", updatedProduct);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable String id) {
        Product product = productService.findById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", product);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取上架商品列表
     */
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getOnSaleProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Product> products = productService.getOnSaleProducts(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products.getContent());
        response.put("total", products.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 搜索商品
     */
    @GetMapping("/products/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Product> products = productService.searchProducts(keyword, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products.getContent());
        response.put("total", products.getTotalElements());

        return ResponseEntity.ok(response);
    }

    // ==================== 购物车接口 ====================

    /**
     * 获取购物车
     */
    @GetMapping("/cart/{userId}")
    public ResponseEntity<Map<String, Object>> getCart(@PathVariable String userId) {
        Cart cart = cartService.getCart(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", cart);

        return ResponseEntity.ok(response);
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/cart/{userId}/items")
    public ResponseEntity<Map<String, Object>> addToCart(
            @PathVariable String userId,
            @RequestBody Map<String, Object> cartData) {
        String productId = (String) cartData.get("productId");
        Integer quantity = (Integer) cartData.get("quantity");

        Cart cart = cartService.addToCart(userId, productId, quantity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "商品已添加到购物车");
        response.put("data", cart);

        return ResponseEntity.ok(response);
    }

    /**
     * 更新购物车商品数量
     */
    @PutMapping("/cart/{userId}/items/{productId}")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @PathVariable String userId,
            @PathVariable String productId,
            @RequestBody Map<String, Integer> quantityData) {
        Integer quantity = quantityData.get("quantity");

        Cart cart = cartService.updateCartItemQuantity(userId, productId, quantity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "数量更新成功");
        response.put("data", cart);

        return ResponseEntity.ok(response);
    }

    /**
     * 从购物车移除商品
     */
    @DeleteMapping("/cart/{userId}/items/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @PathVariable String userId,
            @PathVariable String productId) {
        Cart cart = cartService.removeFromCart(userId, productId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "商品已从购物车移除");
        response.put("data", cart);

        return ResponseEntity.ok(response);
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/cart/{userId}")
    public ResponseEntity<Map<String, Object>> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "购物车已清空");

        return ResponseEntity.ok(response);
    }

    // ==================== 订单接口 ====================

    /**
     * 创建订单
     */
    @PostMapping("/orders/{userId}")
    public ResponseEntity<Map<String, Object>> createOrder(
            @PathVariable String userId,
            @RequestBody ShippingAddress shippingAddress) {
        Order order = orderService.createOrder(userId, shippingAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "订单创建成功");
        response.put("data", order);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/orders/no/{orderNo}")
    public ResponseEntity<Map<String, Object>> getOrderByNo(@PathVariable String orderNo) {
        Order order = orderService.findByOrderNo(orderNo);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", order);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserOrders(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Order> orders = orderService.getUserOrders(userId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", orders.getContent());
        response.put("total", orders.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户特定状态的订单
     */
    @GetMapping("/orders/user/{userId}/status/{status}")
    public ResponseEntity<Map<String, Object>> getUserOrdersByStatus(
            @PathVariable String userId,
            @PathVariable Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Order> orders = orderService.getUserOrdersByStatus(userId, status, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", orders.getContent());
        response.put("total", orders.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 支付订单
     */
    @PostMapping("/orders/{orderNo}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(@PathVariable String orderNo) {
        Order order = orderService.payOrder(orderNo);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "支付成功");
        response.put("data", order);

        return ResponseEntity.ok(response);
    }

    /**
     * 发货
     */
    @PostMapping("/orders/{orderNo}/ship")
    public ResponseEntity<Map<String, Object>> shipOrder(@PathVariable String orderNo) {
        Order order = orderService.shipOrder(orderNo);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "已发货");
        response.put("data", order);

        return ResponseEntity.ok(response);
    }

    /**
     * 确认收货
     */
    @PostMapping("/orders/{orderNo}/confirm")
    public ResponseEntity<Map<String, Object>> confirmReceipt(@PathVariable String orderNo) {
        Order order = orderService.confirmReceipt(orderNo);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "确认收货成功");
        response.put("data", order);

        return ResponseEntity.ok(response);
    }

    /**
     * 取消订单
     */
    @PostMapping("/orders/{orderNo}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderNo) {
        Order order = orderService.cancelOrder(orderNo);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "订单已取消");
        response.put("data", order);

        return ResponseEntity.ok(response);
    }
}
```

---

## 8.5 关键技术总结

### 8.5.1 MongoDB 索引设计

针对不同查询场景，创建合适的索引可以显著提升查询性能：

```java
// 单字段索引
@Indexed
private String username;

// 复合索引
@CompoundIndex(def = "{'status': 1, 'publishTime': -1}")

// 唯一索引
@Indexed(unique = true)

// 文本索引（用于全文搜索）
@TextIndexed(weight = 10)
private String title;
```

### 8.5.2 分页查询实现

```java
// 使用 Pageable 进行分页
Pageable pageable = PageRequest.of(page, size, Sort.by(
    Sort.Direction.DESC, "createTime"));
Page<Article> articles = articleRepository.findByStatus(1, pageable);

// 获取分页信息
articles.getTotalElements();    // 总记录数
articles.getTotalPages();       // 总页数
articles.getContent();          // 当前页数据
```

### 8.5.3 事务处理

```java
@Transactional
public Order createOrder(String userId, ShippingAddress shippingAddress) {
    // 创建订单
    Order order = orderRepository.save(order);

    // 扣减库存
    productService.reduceStock(productId, quantity);

    // 清空购物车
    cartService.clearCart(userId);

    return order;
}
```

### 8.5.4 聚合查询示例

```java
// 统计每个分类的商品数量
Aggregation aggregation = Aggregation.newAggregation(
    Aggregation.group("category").count().as("count"),
    Aggregation.sort(Sort.Direction.DESC, "count")
);

AggregationResults<Document> results = mongoTemplate.aggregate(
    aggregation, "products", Document.class);
```

---

本章节通过三个完整的实战项目，详细介绍了 Spring Boot + MongoDB 的开发流程，包括数据库设计、Repository 层、Service 层和 Controller 层的完整实现。希望对您的学习有所帮助。
