# Spring Data JPA

## 概述

> 官网：Spring Data JPA provides repository support for the Jakarta Persistence API (JPA). It eases development of applications with a consistent programming model that need to access JPA data sources.
>
> 翻译：Spring Data JPA为Jakarta Persistence API（JPA）提供存储库支持。它通过一致的编程模型简化了需要访问JPA数据源的应用程序的开发。

JPA 是 Java 5.0 的概念，规范持久化操作的API规范，而 Spring Data JPA 是对 JPA  的二次封装，提供给各个 ORM 框架遵循的 API 规范，底层负责实现各种功能的是各种的 ORM 框架，比如知名的 Hibernate 框架。

Spring Data JPA 默认使用的是 Hibernate，由它来完成数据库的一系列操作





## Getter Start









# Spring Data MongoDB

## 概述

spring-data-mongodb提供了与MongoRepository两种方式访问mongodb，MongoRepository操作简单，MongoTemplate操作灵活，我们在项目中可以灵活适用这两种方式操作mongodb，MongoRepository的缺点是不够灵活，MongoTemplate正好可以弥补不足



### 引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>

    <dependency>
        <groupId>joda-time</groupId>
        <artifactId>joda-time</artifactId>
        <version>2.10.1</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <groupId>org.junit.vintage</groupId>
                <artifactId>junit-vintage-engine</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>
```



### 配置

在application.properties文件添加配置

```properties
spring.data.mongodb.uri=mongodb://IP:端口号/test
```





## 基于MongoTemplate 开发CRUD

### 添加实体

```java
@Data
@Document("User")
public class User {
     @Id
     private String id;
     private String name;
     private Integer age;
     private String email;
     private String createDate;
}
```



### ②实现

**常用方法**

```java
 mongoTemplate.findAll(User.class): 查询User文档的全部数据
 mongoTemplate.findById(<id>, User.class): 查询User文档id为id的数据
 mongoTemplate.find(query, User.class);: 根据query内的查询条件查询
 mongoTemplate.upsert(query, update, User.class): 修改
 mongoTemplate.remove(query, User.class): 删除
 mongoTemplate.insert(User): 新增
```



**Query对象**
1、创建一个query对象（用来封装所有条件对象)，再创建一个criteria对象（用来构建条件）
2、 精准条件：criteria.and(“key”).is(“条件”)
 模糊条件：criteria.and(“key”).regex(“条件”)
3、封装条件：query.addCriteria(criteria)
4、大于（创建新的criteria）：Criteria gt = Criteria.where(“key”).gt（“条件”）
 小于（创建新的criteria）：Criteria lt = Criteria.where(“key”).lt（“条件”）
5、Query.addCriteria(new Criteria().andOperator(gt,lt));
6、一个query中只能有一个andOperator()。其参数也可以是Criteria数组。
7、排序 ：query.with（new Sort(Sort.Direction.ASC, "age"). and(new Sort(Sort.Direction.DESC, "date")))



### ③添加测试类

在/test/java下面添加测试类

```java
@SpringBootTest
class MongodbApplicationTests {

    @Autowired
    private MongoTemplate mongoTemplate;

    // 添加
    @Test
    public void Test1(){
        User user = new User();
        user.setName("xiaozhi");
        user.setAge(19);
        user.setEmail("23423432");
        User insert = mongoTemplate.insert(user);
        System.out.println(insert);
    }

    // 查询所有
    @Test
    public void Test2(){
        System.out.println(mongoTemplate.findAll(User.class));
    }

     //根据id查询
    @Test
    public void Test3(){
        User user = mongoTemplate.findById("609cf46c2ed6e460c8592fdc", User.class);
        System.out.println(user);
    }

    // 条件查询
    @Test
    public void Test4(){
        // 新建一个query对象
        Query query = new Query(Criteria
                .where("name").is("xiaozhi")
                .and("age").is(19));
        List<User> users = mongoTemplate.find(query, User.class);
        System.out.println(users);
    }

    // 模糊查询
    @Test
    public void Test5(){
        String name = "est";
        // 正则表达式
        // 只要包含est就进行匹配
        String regex = String.format("%s%s%s","^.*",name,".*$");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        // 创建一个query对象
        Query query = new Query(Criteria.where("name").regex(pattern));
        List<User> users = mongoTemplate.find(query, User.class);
        System.out.println(users);
    }

    // 分页查询
    @Test
    public void Test6(){
        int pageNo = 1;
        int pageSize = 5;
        Query query = new Query(Criteria.where("name").is("xiaozhi"));
        long count = mongoTemplate.count(query, User.class);
        List<User> users = mongoTemplate.find(query.skip((pageNo - 1) * pageSize).limit(pageSize), User.class);
        System.out.println(users);
    }

    // 修改
    @Test
    public void Test7(){
        User user = mongoTemplate.findById("609cf46c2ed6e460c8592fdc", User.class);
        user.setName("heigui");
        user.setAge(40);
        Query query = new Query(Criteria.where("_id").is(user.getId()));
        Update update = new Update();
        update.set("name",user.getName());
        update.set("age",user.getAge());
        UpdateResult result = mongoTemplate.upsert(query, update, User.class);
        // 影响行数
        long count = result.getModifiedCount();
        System.out.println(count);
    }

    // 删除操作
    @Test
    public void Test8(){
        Query query = new Query(Criteria.where("name").is("heigui"));
        DeleteResult remove = mongoTemplate.remove(query, User.class);
        // 影响的行数
        long deletedCount = remove.getDeletedCount();
        System.out.println(deletedCount);
    }
}
```







## 基于MongoRepository开发CRUD

### ①创建一个接口继承MongoRepository

```java
@Repository
public interface UserRepository extends MongoRepository<User,String> {

}
```



### ②进行操作

```java
@SpringBootTest
class MongodbApplicationTest2 {

    @Autowired
    private UserRepository userRepository;

    // 添加
    @Test
    public void Test1(){
        User user = new User();
        user.setName("lucy");
        user.setAge(18);
        user.setEmail("1@qq.com");
        userRepository.save(user);
    }

    // 查询所有
    @Test
    public void Test2(){
        System.out.println(userRepository.findAll());
    }

     //根据id查询
    @Test
    public void Test3(){
        // 加上get方法才是得到对应的实体类对象
        User user = userRepository.findById("60a1e470ac57e448a0a292ae").get();
        System.out.println(user);
    }


    // 条件查询
    @Test
    public void Test4(){
        // 查找名字为Lucy的记录
        User user = new User();
        user.setName("lucy");
        Example<User> example = Example.of(user);
        List<User> userList = userRepository.findAll(example);
        System.out.println(userList);
    }

    // 模糊查询
    @Test
    public void Test5(){
        // 创建匹配器，既如何使用查询条件
        ExampleMatcher matcher = ExampleMatcher.matching()      // 构建对象
                // 改变默认字符串匹配方式，模糊查询
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);  // 改变默认大小写忽略方式：忽略大小写
        User user = new User();
        user.setName("l");      // 查询名字中有l的
        Example<User> userExample = Example.of(user, matcher);
        List<User> userList = userRepository.findAll(userExample);
        System.out.println(userList);
    }

    // 分页查询
    @Test
    public void Test6(){
        // 0表示是第一页
        Pageable pageable = PageRequest.of(0,5);
        // 生成一个page对象，对象中有对应的属性值
        Page<User> userPage = userRepository.findAll(pageable);
        // 调用get方法得到一个Stream流对象
        userPage.get().forEach(System.out::println);
    }

    // 修改
    @Test
    public void Test7(){
        User user = userRepository.findById("60a1e470ac57e448a0a292ae").get();
        user.setName("major");
        user.setAge(18);
        User save = userRepository.save(user);
        System.out.println(save);
    }

    // 删除操作
    @Test
    public void Test8(){
        userRepository.deleteById("60a1e470ac57e448a0a292ae");
    }
}
```





## SpringDataMonogoDB进阶

### 实体类注解

常用的实体类注解如下：

- `@Document`：作用于类上面，被该注解修饰的类，会和`MongoDB`中的集合相映射，如果类名和集合名不一致，可以通过`collection`参数来指定。
- `@Id`：标识一个字段为主键，可以加在任意字段上，但如果该字段不为`_id`，每次插入需要自己生成全局唯一的主键；如果不设置`@Id`主键，`MongoDB`会默认插入一个`_id`值来作为主键。
- `@Transient`：被该注解修饰的属性，在`CRUD`操作发生时，`SpringData`会自动将其忽略，不会被传递给`MongoDB`。
- `@Field`：作用于普通属性上，如果`Java`属性名和`MongoDB`字段名不一致，可以通过该注解来做别名映射。
- `@DBRef`：一般用来修饰“嵌套文档”字段，主要用于关联另一个文档。
- `@Indexed`：可作用于任意属性上，被该注解修饰的属性，如果`MongoDB`中还未创建索引，在第一次插入时，`SpringData`会默认为其创建一个普通索引。
- `@CompoundIndex`：作用于类上，表示创建复合索引，可以通过`name`参数指定索引名，`def`参数指定组成索引的字段及排序方式。
- `@GeoSpatialIndexed、@TextIndexed`：和上面的`@Indexed`注解作用相同，前者代表空间索引，后者代表全文索引。





### 自定义方法

SpringDataMonoDB支持我们按照语义来编写对应的方法，我们只需要遵守它的规范即可

- `findBy<fieldName>`：根据指定的单个条件进行等值查询；
- `findBy<fieldName>And<fieldName>And<...>`：根据指定的多条件进行`and`查询；
- `findBy<fieldName>Or<fieldName>Or<...>`：根据指定的多条件进行`or`查询；
- `findBy<fieldName>Equals`：根据指定的单个条件进行等值查询；
- `findBy<fieldName>In`：对指定的单个字段进行`in`查询，入参为一个列表；
- `findBy<fieldName>Like`：对指定的单个字段进行`like`模糊查询；
- `findBy<fieldName>NotNull`：查询指定字段不为空的数据；
- `findBy<fieldName>GreaterThan`：对指定的单个字段进行`>`范围查询；
- `findBy<fieldName>GreaterThanEqual`：对指定的单个字段进行`>=`范围查询；
- `findBy<fieldName>LessThan`：对指定的单个字段进行`<`范围查询；
- `findBy<fieldName>LessThanEqual`：对指定的单个字段进行`<=`范围查询；
- `Page<...> findBy<...>`：根据指定的条件进行分页查询；
- `countBy<fieldName>`：根据指定的条件字段进行计数统计；
- `findTop<n>By<fieldName>`：根据指定字段做等值查询，并返回前`n`条数据；
- `findBy<fieldName>Between`：根据指定字段进行`between`范围查询；
- `findDistinctBy<fieldName>`：根据指定的单个条件进行去重查询；
- `findFirstBy<fieldName>`：根据指定的单个条件进行等值查询（只返回满足条件的第一个数据）；
- `findBy<fieldName1>OrderBy<fieldName2>`：根据第一个字段做等值查询，并根据第二个字段做排序；
- `……`：

可以参考官方文档：











作者：竹子爱熊猫
链接：https://juejin.cn/post/7276408879366111292
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。



