# 一、简介

![Apache Shiro Logo](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/apache-shiro-logo.png)



## 1、什么是Shiro

一个简单易用的安全管理框架，用来做权限管理，javaSE和javaEE都可以使用，还可以用于第三方语言



## 2、shiro整体架构

![Shiro Architecture Diagram](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/ShiroArchitecture.png)

-   **Subject**：主体，可以看到主体可以是任何可以与应用交互的 “用户”，可以是程序、用户；
-   **SecurityManager**：shiro的核心，负责认证、授权、会话、缓存的管理
-   **Authenticator**：认证器，负责主体认证的，这是一个扩展点，如果用户觉得 Shiro 默认的不好，可以自定义实现；其需要认证策略（Authentication Strategy），即什么情况下算用户认证通过了；
-   **Authorizer**：授权器，或者访问控制器，用来决定主体是否有权限进行相应的操作；即控制着用户能访问应用中的哪些功能；
-   **Realm**：域，可以有 1 个或多个 Realm，可以认为它是安全数据源，通过它我们来判断用户是否是拥有访问权限，保存对应的验证信息，比如web登录，保存的是我们的用户名和密码信息，通过这个我们判断用户是否有登录网站的权限；
-   **SessionManager**：如果写过 Servlet 就应该知道 Session 的概念，Session 呢需要有人去管理它的生命周期，这个组件就是 SessionManager；而 Shiro 并不仅仅可以用在 Web 环境，也可以用在如普通的 JavaSE 环境、EJB 等环境；所以呢，Shiro 就抽象了一个自己的 Session 来管理主体与应用之间交互的数据；这样的话，比如我们在 Web 环境用，刚开始是一台 Web 服务器；接着又上了台 EJB 服务器；这时想把两台服务器的会话数据放到一个地方，这个时候就可以实现自己的分布式会话（如把数据放到 Memcached 服务器）；
-   **SessionDAO**：DAO 大家都用过，数据访问对象，用于会话的 CRUD，比如我们想把 Session 保存到数据库，那么可以实现自己的 SessionDAO，通过如 JDBC 写到数据库；比如想把 Session 放到 Memcached 中，可以实现自己的 Memcached SessionDAO；另外 SessionDAO 中可以使用 Cache 进行缓存，以提高性能；
-   **CacheManager**：缓存控制器，来管理如用户、角色、权限等的缓存的；因为这些数据基本上很少去改变，放到缓存中后可以提高访问的性能
-   **Cryptography**：密码模块，Shiro 提供了一些常见的加密组件用于如密码加密 / 解密的，比如MD5加密。





## 3、shiro基本功能

![Apache Shiro Features](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/ShiroFeatures.png)

-   **Authentication**：身份认证 / 登录，验证用户是不是拥有相应的身份；
-   **Authorization**：授权，即权限验证，验证某个已认证的用户是否拥有某个权限；即判断用户是否能做事情，常见的如：验证某个用户是否拥有某个角色。或者细粒度的验证某个用户对某个资源是否具有某个权限；
-   **Session** **Management**：会话管理，即用户登录后就是一次会话，在没有退出之前，它的所有信息都在会话中；会话可以是普通 JavaSE 环境的，也可以是如 Web 环境的；
-   **Cryptography**：加密，保护数据的安全性，如密码加密存储到数据库，而不是明文存储；
-   **Web Support**：Web 支持，可以非常容易的集成到 Web 环境；
-   **Caching**：缓存，比如用户登录后，其用户信息、拥有的角色 / 权限不必每次去查，这样可以提高效率；
-   **Concurrency**：shiro 支持多线程应用的并发验证，即如在一个线程中开启另一个线程，能把权限自动传播过去；
-   **Testing**：提供测试支持；
-   **Run As**：允许一个用户假装为另一个用户（如果他们允许）的身份进行访问；
-   **Remember Me**：记住我，这个是非常常见的功能，即一次登录后，下次再来的话不用登录了。



## 4、shiro实现

![Shiro Basic Architecture Diagram](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/ShiroBasicArchitecture.png)

-   **subject**：主体，表示当前用户，它并不只限定于人，可以是爬虫和机器人
-   **SecurityManager**：它是shiro的核心，由它来对主体进行授权和认证
-   **Realm**：域，它是一个安全数据源，**SecurityManager**验证身份要从Realm中获取对应用户的信息，判断它是否有对应的操作权限







# 二、基本使用

## 1、认证

### 1.1 简介

通过提供信息判断你是否是系统中的用户或者是系统认证的

-   **principals**：身份，即主体的标识属性，可以是任何东西，如用户名、邮箱等，唯一即可。一个主体可以有多个 `principals`，但只有一个 `Primary principals`，一般是用户名 / 密码 / 手机号。
-   **credentials**：证明 / 凭证，即只有主体知道的安全值，如密码 / 数字证书等。最常见的 `principals` 和 `credentials` 组合就是用户名 / 密码了。接下来先进行一个基本的身份认证。另外两个相关的概念是之前提到的 **`Subject`** 及 **`Realm`**，分别是主体及验证主体的数据源。



### 1.2 登录登出实现

1.  **创建maven项目，引入依赖**

    ```xml
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
    </dependency>
    <dependency>
        <groupId>commons-logging</groupId>
        <artifactId>commons-logging</artifactId>
        <version>1.1.3</version>
    </dependency>
    <dependency>
        <groupId>org.apache.shiro</groupId>
        <artifactId>shiro-core</artifactId>
        <version>1.9.0</version>
    </dependency>
    ```

2.  **编写`test.ini`文件**

    说明：`ini`文件是windows的一种`k-v`键值对类型的配置文件，用来保存我们的`(身份)principals`和`(凭证)credentials`，键为`principals`，值为`credentials`，方便我们测试学习所用，后面我们会将数据放入数据库中

    ```ini
    [users]
    heizai=123
    heigui=123456
    ```

    注意：格式必须严格按照上面的来，`[xxxx]`表示一个配置项(目前猜测)，配置项下面是数据

3.  **测试**

    ```java
    @Test
    public void testUseShiro(){
        // 1 创建安全管理器对象
        DefaultSecurityManager securityManager = new DefaultSecurityManager();
        // 2 给安全管理器设置Realm
        securityManager.setRealm(new IniRealm("classpath:test.ini"));
        // 3 SecurityUtils 给全局安全工具类设置安全管理器
        SecurityUtils.setSecurityManager(securityManager);
        // 4 关键对象 Subject 主体
        Subject subject = SecurityUtils.getSubject();
        // 5 创建令牌
        UsernamePasswordToken token = new UsernamePasswordToken("heizai", "123");
        // 6 身份认证
        try {
            // 输出是否认证成功
            System.out.println(subject.isAuthenticated());
            subject.login(token);
            System.out.println(subject.isAuthenticated());
        } catch (IncorrectCredentialsException e) {
            System.out.println("密码错误，请重新输入");
        } catch (UnknownAccountException e) {
            System.out.println("用户名不存在，请重新输入");
        }
    
        // 输出身份
        System.out.println(subject.getPrincipal());
        // 7 退出
        subject.logout();
        System.out.println(subject.getPrincipal());
    }
    ```



### 1.3 代码说明

1.  首先我们要拿到安全管理器(`SecurityManager`)，这样才能进行操作

    ![image-20220709233852388](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220709233852388.png)

2.  设置Reaml，在这里我们的数据是写死的，以`.ini`文件的形式存储我们的身份和凭证

3.  SecurityUtils，用来获取主体(`Subject`)

4.  创建令牌，`subject`调用`login方法`，接收`AuthenticationToken`接口，`UsernamePasswordToken`是它的实现类

    ![image-20220709234600055](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220709234600055.png)

5.  到此，身份认证的基本功能就实现了，可以通过`subject.isAuthenticated()`来判断是否认证成功



### 1.4 认证流程的源码

#### 解析

1.  首先我们找到认证入口 `subject.login(xxxx);`，给它打上断点

    ![image-20220710143640907](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710143640907.png)

2.  会进入到`DelegatingSubject`类中的`login()`方法中

    ![image-20220710145307615](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710145307615.png)

    可以看到，还是由安全管理器来处理认证

3.  进入`securityManager.login(this, token);`中，会进入到`DefaultSecurityManager`的核心方法`authenticate(token);`，由它来进行认证实现

    ![image-20220710145522652](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710145522652.png)

4.  进入到`AuthenticatingSecurityManager`类`authenticate`方法中，由认证器来执行认证

    ![image-20220710145725778](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710145725778.png)

5.  往下走，进入`AbstractAuthenticator`类中的`authenticate`方法中，找到`doAuthenticate(token)`方法

    ![image-20220710150048135](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710150048135.png)

6.  进入到`doAuthenticate(token)`方法中，找到`doSingleRealmAuthentication()`方法

    ```java
    protected AuthenticationInfo doAuthenticate(AuthenticationToken authenticationToken) throws AuthenticationException {
        // 断言有配置 Realm
        assertRealmsConfigured();
        // 获取realms
        Collection<Realm> realms = getRealms();
        if (realms.size() == 1) {
            // 简单的身份认证
            return doSingleRealmAuthentication(realms.iterator().next(), authenticationToken);
        } else {
            // 执行多个realm的身份认证
            return doMultiRealmAuthentication(realms, authenticationToken);
        }
    }
    ```

7.  进入`doSingleRealmAuthentication`方法中，找到`getAuthenticationInfo`进入

    ![image-20220710150648881](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710150648881.png)

8.  `getAuthenticationInfo`方法中，找到核心方法`doGetAuthenticationInfo(token)`，这个就是我们的主要功能方法

    ![image-20220710150805377](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710150805377.png)

9.  `doGetAuthenticationInfo(token)`，由它来获取`域(Realm)`数据

    ```java
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // 获取token信息
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        // 从 域(reaml) 中获取到匹配的用户名，这里是从.ini文件中取数据
        SimpleAccount account = getUser(upToken.getUsername());
    
        if (account != null) {
    
            if (account.isLocked()) {
                throw new LockedAccountException("Account [" + account + "] is locked.");
            }
            // 查看凭证是否过期
            if (account.isCredentialsExpired()) {
                String msg = "The credentials for account [" + account + "] are expired";
                throw new ExpiredCredentialsException(msg);
            }
    
        }
    
        return account;
    }
    ```

    进入到`getUser(upToken.getUsername())`中，这个方法就是从域(realm)中获取到匹配的数据

    ```java
    ------------SimpleAccountRealm类中--------------
    protected SimpleAccount getUser(String username) {
        USERS_LOCK.readLock().lock();
        try {
           	// users的数据就是从.ini文件中获取的用户名，如果没有就返回null
            return this.users.get(username);
        } finally {
            USERS_LOCK.readLock().unlock();
        }
    }
    ```

    `getUser(upToken.getUsername())`方法返回null就是没有找到对应的用户名，抛出用户名错误异常 -> `UnknownAccountException`

    ![image-20220710152805611](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710152805611.png)

10.  执行完`doGetAuthenticationInfo`，回到`getAuthenticationInfo`方法中

11.  进入`assertCredentialsMatch`方法中

     <img src="https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710153214359.png" alt="image-20220710153214359" style="zoom:67%;" />

     <img src="https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710153346105.png" alt="image-20220710153346105" style="zoom:67%;" />

     执行证书匹配，在这里也就是密码匹配

12.  进入`doCredentialsMatch`方法

     ```java
     public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
         // 获取证书（这里证书就是密码）
         Object tokenCredentials = getCredentials(token);
         // 用用户名匹配的认证信息获取证书
         Object accountCredentials = getCredentials(info);
         // 然后用equals进行对比
         return equals(tokenCredentials, accountCredentials);
     }
     ```



#### 总结

我们可以看一下Realm的继承树

![image-20220710154702717](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220710154702717.png)

可以看到我们刚才认证使用的`SimpleAccountRealm`是继承`AuthorizingRealm`的，`AuthorizingRealm`又继承`AuthenticatingRealm`，所以`SimpleAccountRealm`是有这两个类的功能的

`AuthorizingRealm`中的`doGetAuthorizationInfo`方法就是用来做身份认证的

`AuthenticatingRealm`中的`doGetAuthenticationInfo`方法是用来做授权的

所以我们要将Realm的数据源换成数据库的，我们可以继承`AuthorizingRealm`类，然后实现`doGetAuthorizationInfo`方法来换源





### 1.5 自定义Realm

我们之前测试用的是`.ini`文件，现在我们需要更换成数据库的

上面讲到我们可以继承`AuthorizingRealm`类，然后实现`doGetAuthorizationInfo`方法来换源



**代码实现**

```java
/**
 * @author xiaozhi
 * @description 自定义Realm，将认证 / 授权来源转为数据库
 * @create 2022-07-2022/7/10 16:05
 */
public class CustomerRealm extends AuthorizingRealm {

    // 授权
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

        return null;
    }

    // 认证
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // 获取用户名
        String username = (String) token.getPrincipal();
        // 查询数据库是否存在该用户名，这里写死
        if (username.equals("heizai")) {
            /*
                参数一：数据库中正确用户名
                参数二：数据库中正确密码
                参数三：Realm名
             */
            SimpleAuthenticationInfo info = new SimpleAuthenticationInfo("heizai", "123", this.getName());
            // 可以查看AuthenticationInfo的继承树返回它的实现类
            return info;
        }
        return null;
    }
}
```

**测试**

```java
@Test
public void test01(){
    DefaultSecurityManager manager = new DefaultSecurityManager();
    manager.setRealm(new CustomerRealm());
    SecurityUtils.setSecurityManager(manager);
    Subject subject = SecurityUtils.getSubject();
    UsernamePasswordToken token = new UsernamePasswordToken("heizai", "123");
    try {
        subject.login(token);
    } catch (AuthenticationException e) {
        System.out.println("账号或错误");
    }
}
```



### 1.6 MD5+salf+hash

使用MD5加盐(salf)加散列(hash)

**MD5**

-   不可逆的算法，只能通过明文得到密文，不能通过密文得到明文
-   无论是程序还是文件、字符串，经过MD5算法会生成16进制、32位的字符串
-   同样的内容，MD5加密都是一样的密文，所以可以用来加密或者签名，签名用于文件内容对比

**salf**

-   盐，一串字符串，可以是随机的或者固定的，建议是随机的
-   别人可以通过穷举算法得到明文和密文，加盐可以提高安全性，可以加在明文前面，中间，后面

**Hash**：散列



**使用shiro自带的MD5算法**

```java
@Test
public void testMD5(){
    Md5Hash m1 = new Md5Hash("123");
    System.out.println("未加盐\n" + m1.toHex());
    Md5Hash m2 = new Md5Hash("123", "s8df34");
    System.out.println("加盐\n" + m2.toHex());
    Md5Hash m3 = new Md5Hash("123", "s8df34", 1024);
    System.out.println("加盐+hash\n" + m3.toHex());
}
```



**实现自定义Realm加MD5+salf+hash**

```java
/**
 * @author xiaozhi
 * @description 自定义Realm，使用MD5+salf+hash
 * @create 2022-07-2022/7/10 17:09
 */
public class CustomerRealmMD5 extends AuthorizingRealm {

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // 获取用户名
        String username = (String) token.getPrincipal();
        // 查询数据库是否存在该用户名，这里写死
        if (username.equals("heizai")) {
            /*
                参数一：数据库中正确用户名
                参数二：数据库中加盐过的密码
                参数三：盐(salf)
                参数四：Realm名
             */
            SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(
                    "heizai",
                    "c1f17b15b81ecedeb51a54cc3d49186c",
                    ByteSource.Util.bytes("s8df34"),
                    this.getName());
            // 可以查看AuthenticationInfo的继承树返回它的实现类
            return info;
        }
        return null;
    }
}
```

vkpassword8*8



**测试**

```java
@Test
public void test(){
    DefaultSecurityManager manager = new DefaultSecurityManager();
    CustomerRealmMD5 realm = new CustomerRealmMD5();
    // 创建Hash凭证匹配器
    HashedCredentialsMatcher matcher = new HashedCredentialsMatcher();
    // 设置使用MD5
    matcher.setHashAlgorithmName("MD5");
    // 散列次数
    matcher.setHashIterations(1024);
    // 设置realm使用Hash凭证匹配器
    realm.setCredentialsMatcher(matcher);
    manager.setRealm(realm);
    SecurityUtils.setSecurityManager(manager);
    Subject subject = SecurityUtils.getSubject();
    UsernamePasswordToken token = new UsernamePasswordToken("heizai","123");
    try {
        subject.login(token);
        System.out.println("登录成功");
    } catch (AuthenticationException e) {
        System.out.println("账号或密码错误");
    }
}
```







## 2、授权

### 2.1 是什么

即访问控制，控制谁能访问那些资源，对应的资源需要对应的权限才能访问



### 2.2关键对象

**主体**：主体需要访问系统中的资源

**资源**：如系统菜单、页面、按钮、类方法、系统商品信息等，资源包括`资源类型`和`资源实例`，比如`商品信息为资源类型`，编号为t01的商品为`资源实例`，资源类型包含了资源实例

**权限 / 许可**：规定了主体对资源的操作许可，比如管理员拥有系统是有的权限，用户只有部分的权限



### 2.3 授权流程

认证完成之后才是授权，不然没意义

![image-20220711235641828](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220711235641828.png)



### 2.4 授权方式

-   基于角色的访问控制

    RBAC基于角色的访问控制(Role-Based Access Control)：是以角色为中心进行访问控制

    ```java
    if (subject.hasRole("admin")) {
        // 操作什么资源
    }
    ```

    拥有对应角色可以操作什么资源，比如拥有管理员角色，那么我可以操作整个系统的资源

    

-   基于资源的访问控制

    RBAC基于资源的访问控制(Resource-Based Access Control)：是以资源为中心进行访问控制

    ```java
    if (subject.isPermission("user:update:01")) { // 资源实例
        // 对01用户有修改权限
    }
    if (subject.isPermission("user:update:*")) {    // 资源类型
        // 对所有用户有修改权限
    }
    ```

注：这里的例子是伪代码





### 2.5 权限字符串

**规则**：`资源标识符: 操作: 资源实例标识符`

**说明**：对那个资源的那个实例具有什么操作权限，":"是资源/操作/实例的分隔符，权限字符串月可以使用通配符

**例子**：

-   用户创建权限：`user:create`或`user:create:*，`
-   用户修改实例001的权限：`user:update:001`，对用户001有修改权限
-   用户实例001的所有权限：`user: *: 001`，对用户001进行所有操作





### 2.6 shiro中授权变成实现方式

-   编程式

    ```java
    Subject subject = SecurityUtils.getSubject();
    if (subject.hasRole("admin")) {
        // 有权限
    } else {
        // 无权限
    }
    ```

-   注解式

    ```java
    @RequiresRoles("admin")
    public void test() {
        // 有权限
    }
    ```

    

-   标签式

    JSP/GSP 标签：在JSP/GSP 页面通过相应的标签完成：

    ```jsp
    <shiro:hasRole name="admin">
        <!- 有权限 ->
    <shiro:hasRole>
    ```

    **注**：在thymeleaf中需要额外集成





### 2.7 授权开发

```java
/**
 * @author xiaozhi
 * @description 自定义Realm，使用MD5+salf+hash
 * @create 2022-07-2022/7/10 17:09
 */
public class CustomerRealmMD5 extends AuthorizingRealm {

    // 授权
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        System.out.println("身份：" + principals.getPrimaryPrincipal());
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();

        /* 注意：这里拿到的字符串是从数据库中拿取的，这里写死 */

        // 基于角色权限管理
        // ①拥有单个角色
        simpleAuthorizationInfo.addRole("admin");
        // ②拥有多个角色
        simpleAuthorizationInfo.addRoles(Arrays.asList("admin", "user"));

        // 基于资源权限管理
        // 对01用户有创建权限
        simpleAuthorizationInfo.addStringPermission("user:create:01");
        simpleAuthorizationInfo.addStringPermissions(Arrays.asList("user:*:01", "order:update:01"));

        return simpleAuthorizationInfo;
    }

    // 认证
    ......
}
```

**测试**

```java
@Test
public void test() {
	// ...认证...

    // 授权
    // 是否已经认证
    if (subject.isAuthenticated()) {
        System.out.println("=============授权===========");
        System.out.println("------------基于角色权限管理-----------");
        System.out.println("拥有admin角色是否可以访问：" + subject.hasRole("admin"));
        // 有该角色的为true，没有的为false
        boolean[] booleans = subject.hasRoles(Arrays.asList("admin", "super", "user"));
        for (boolean b : booleans) {
            System.out.print(b + "、");
        }
        //
        System.out.println("是否同时拥有admin和user两个角色" + subject.hasAllRoles(Arrays.asList("admin", "user")));

        System.out.println("-----------基于资源权限管理-----------");
        // 满足单个权限
        System.out.println("是否有对01用户有创建的权限" + subject.isPermitted("user:create:01"));
        // 分别满足单个权限
        for (boolean b : subject.isPermitted("user:update", "order:*", "item:create:01")) {
            System.out.print(b + "、");
        }
        System.out.println();
        // 是否同时拥有满足两个权限
        System.out.println("是否同时拥有对01用户的创建和对01订单有全部的权限："
                + subject.isPermittedAll("user:create:01", "order:*:01"));
    }
}
```

















# 三、springBoot与shiro整合

## 1、springBoot、shiro、JSP

### 1.1 创建项目，引入依赖

1. 创建springBoot依赖，main下创建webapp文件夹，webapp下创建index.jsp文件

    ```html
    <!doctype html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport"
              content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
        <meta http-equiv="X-UA-Compatible" content="ie=edge">
        <title>Document</title>
    </head>
    <body>
        <h1>系统主页</h1>
    </body>
    </html>
    ```

2. 引入依赖

    ```xml
    <!--jsp解析依赖-->
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-jasper</artifactId>
    </dependency>
    <dependency>
        <groupId>jstl</groupId>
        <artifactId>jstl</artifactId>
        <version>1.2</version>
    </dependency>
    <!--引入shiro整合springBoot依赖-->
    <dependency>
        <groupId>org.apache.shiro</groupId>
        <artifactId>shiro-spring-boot-starter</artifactId>
        <version>1.9.0</version>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.2.2</version>
    </dependency>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
        <version>1.2.11</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>5.1.49</version>
    </dependency>
    ```

3. 配置application.properties

    ```properties
    server.port=8888
    server.servlet.context-path=/shiro
    spring.application.name=shiro
    
    spring.mvc.view.prefix=/
    spring.mvc.view.suffix=.jsp
    
    spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
    spring.datasource.driver-class-name=com.mysql.jdbc.Driver
    spring.datasource.url=jdbc:mysql://localhost:3306/shiro?characterEncoding=UTF-8
    spring.datasource.username=root
    spring.datasource.password=root
    
    # ------------mybatis配置------------
    mybatis.type-aliases-package=com.example.entity
    mybatis.mapper-locations=classpath:com/example/mapper/*.xml
    ```

4. 测试，访问http://localhost:8888/shiro/index.jsp，环境搭建成功



### 1.2 整合shiro

创建一个ShiroConfig类，这里放Shiro相关的配置

```java
@Configuration
public class ShiroConfig {

    // 创建shiroFilter - 负责拦截所有请求
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager defaultWebSecurityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();

        // 给 filter 设置安全管理器
        shiroFilter.setSecurityManager(defaultWebSecurityManager);
        return shiroFilter;
    }

    // 安全管理器 -> web环境下的
    @Bean
    public DefaultWebSecurityManager defaultWebSecurityManager(Realm realm) {
        DefaultWebSecurityManager manager = new DefaultWebSecurityManager();
        // 给安全管理器设置Realm
        manager.setRealm(realm);
        return manager;
    }

    // Realm
    @Bean
    public Realm realm() {
        CustomerRealm customerRealm = new CustomerRealm();
        return customerRealm;
    }
}
```

自定义Realm

```java
public class CustomerRealm extends AuthorizingRealm {
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        return null;
    }
}
```

基本搭建完成，



### 1.3 路径拦截

shiroFilter负责拦截所有的请求，拦截之后判断交给安全管理器进行认证和授权，shiro的拦截方式

在`ShiroFilterFactoryBean`配置拦截的路径和对应的拦截器

```java
@Bean
public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager defaultWebSecurityManager) {
    ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
    
    // 参数一：拦截路径     // 参数二：指定拦截器
    Map<String, String> map = new HashMap<>();
    // 配置公共资源
    // anon：匿名访问，不需要认证和授权就能访问
    map.put("/user/login", "anon");
    map.put("/register.jsp", "anon");
    map.put("/user/register", "anon");
    
    // 配置受限资源
    // 拦截所有路径   authc：需要对应认证和授权才能访问
    map.put("/**", "authc");
    shiroFilter.setFilterChainDefinitionMap(map);

    // 设置认证界面默认路径
    shiroFilter.setLoginUrl("/login.jsp");

    // 给 filter 设置安全管理器
    shiroFilter.setSecurityManager(defaultWebSecurityManager);
    return shiroFilter;
}
```

` shiroFilter.setFilterChainDefinitionMap(map);`设置一个map，键是拦截的路径，值是用什么拦截器进行拦截



#### shiro默认的拦截器

Shiro 内置了很多默认的拦截器，比如身份验证、授权等相关的。默认拦截器可以参考 org.apache.shiro.web.filter.mgt.DefaultFilter 中的枚举拦截器：

| 默认拦截器名      | 拦截器类                                                     | 说明（括号里的表示默认值）                                   |
| ----------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 身份验证相关的    |                                                              |                                                              |
| authc             | org.apache.shiro.web.filter.authc.FormAuthenticationFilter   | 基于表单的拦截器；如 “`/**=authc`”，如果没有登录会跳到相应的登录页面登录；主要属性：usernameParam：表单提交的用户名参数名（ username）；  passwordParam：表单提交的密码参数名（password）； rememberMeParam：表单提交的密码参数名（rememberMe）； loginUrl：登录页面地址（/login.jsp）；successUrl：登录成功后的默认重定向地址； failureKeyAttribute：登录失败后错误信息存储 key（shiroLoginFailure）； |
| authcBasic        | org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter | Basic HTTP 身份验证拦截器，主要属性： applicationName：弹出登录框显示的信息（application）； |
| logout            | org.apache.shiro.web.filter.authc.LogoutFilter               | 退出拦截器，主要属性：redirectUrl：退出成功后重定向的地址（/）; 示例 “/logout=logout” |
| user              | org.apache.shiro.web.filter.authc.UserFilter                 | 用户拦截器，用户已经身份验证 / 记住我登录的都可；示例 “/**=user” |
| anon              | org.apache.shiro.web.filter.authc.AnonymousFilter            | 匿名拦截器，即不需要登录即可访问；一般用于静态资源过滤；示例 “/static/**=anon” |
| **授权相关的**    |                                                              |                                                              |
| roles             | org.apache.shiro.web.filter.authz.RolesAuthorizationFilter   | 角色授权拦截器，验证用户是否拥有所有角色；主要属性： loginUrl：登录页面地址（/login.jsp）；unauthorizedUrl：未授权后重定向的地址；示例 “/admin/**=roles[admin]” |
| perms             | org.apache.shiro.web.filter.authz.PermissionsAuthorizationFilter | 权限授权拦截器，验证用户是否拥有所有权限；属性和 roles 一样；示例 “/user/**=perms["user:create"]” |
| port              | org.apache.shiro.web.filter.authz.PortFilter                 | 端口拦截器，主要属性：port（80）：可以通过的端口；示例 “/test= port[80]”，如果用户访问该页面是非 80，将自动将请求端口改为 80 并重定向到该 80 端口，其他路径 / 参数等都一样 |
| rest              | org.apache.shiro.web.filter.authz.HttpMethodPermissionFilter | rest 风格拦截器，自动根据请求方法构建权限字符串（GET=read, POST=create,PUT=update,DELETE=delete,HEAD=read,TRACE=read,OPTIONS=read, MKCOL=create）构建权限字符串；示例 “/users=rest[user]”，会自动拼出“user:read,user:create,user:update,user:delete” 权限字符串进行权限匹配（所有都得匹配，isPermittedAll）； |
| ssl               | org.apache.shiro.web.filter.authz.SslFilter                  | SSL 拦截器，只有请求协议是 https 才能通过；否则自动跳转会 https 端口（443）；其他和 port 拦截器一样； |
| **其他**          |                                                              |                                                              |
| noSessionCreation | org.apache.shiro.web.filter.session.NoSessionCreationFilter  | 不创建会话拦截器，调用 subject.getSession(false) 不会有什么问题，但是如果 subject.getSession(true) 将抛出 DisabledSessionException 异常； |

另外还提供了一个 org.apache.shiro.web.filter.authz.HostFilter，即主机拦截器，比如其提供了属性：authorizedIps：已授权的 ip 地址，deniedIps：表示拒绝的 ip 地址；不过目前还没有完全实现，不可用。

这些默认的拦截器会自动注册，可以直接在 ini 配置文件中通过 “拦截器名. 属性” 设置其属性：`perms.unauthorizedUrl=/unauthorized`

另外如果某个拦截器不想使用了可以直接通过如下配置直接禁用：

`perms.enabled=false`



#### 拦截器对应的类

在`DefaultFilter`枚举类中列举了对应的类

```java
anon(AnonymousFilter.class),
authc(FormAuthenticationFilter.class),
authcBasic(BasicHttpAuthenticationFilter.class),
authcBearer(BearerHttpAuthenticationFilter.class),
logout(LogoutFilter.class),
noSessionCreation(NoSessionCreationFilter.class),
perms(PermissionsAuthorizationFilter.class),
port(PortFilter.class),
rest(HttpMethodPermissionFilter.class),
roles(RolesAuthorizationFilter.class),
ssl(SslFilter.class),
user(UserFilter.class),
invalidRequest(InvalidRequestFilter.class);
```





### 1.3 注册与登录

首先，我们的密码存储在数据库中是以密文的形式存储的，所以我们要在用户注册的时候就要进行加密，接下来做一个注册流程实现



#### ①创建表

```sql
CREATE TABLE `t_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `password` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `salt` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
```



#### ②注册

webapp/register.jsp

```jsp
<%@page contentType="text/html; UTF-8" pageEncoding="utf-8" isELIgnored="false" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Document</title>
</head>
<body>

    <h1>用户注册</h1>

    <form action="${pageContext.request.contextPath}/user/register" method="post">
        用户名：<input type="text" name="username"><br>
        密码 ： <input type="password" name="password" id=""><br>
        <input type="submit" value="注册">

    </form>
</body>
</html>
```

UserMapper

```java
@Insert("insert into t_user(username, password, salt) values (#{username}, #{password}, #{salt})")
void save(User user);
```

UserController

```java
@PostMapping("/register")
public String register(User user) {
    // 生成随机盐，这里用固定的
    String salt = "sdfsf923";
    // 对密码进行加密
    String jiami = new Md5Hash(user.getPassword(), salt, 1024).toHex();
    user.setPassword(jiami);
    user.setSalt(salt);
    userMapper.save(user);
    return "redirect:/login.jsp";
}
```



#### ③ApplicationContextUtils

```java
@Component
public class ApplicationContextUtils implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    /**
     * 根据bean名字获取工厂中指定的bean
     * @param beanName
     * @return
     */
    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }
}
```



#### ④认证

webapp/login.jsp

```html
<%@page contentType="text/html; UTF-8" pageEncoding="utf-8" isELIgnored="false" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Document</title>
</head>
<body>

    <h1>用户登录</h1>

    <form action="${pageContext.request.contextPath}/user/login" method="post">
        用户名：<input type="text" name="username"><br>
        密码 ： <input type="password" name="password" id=""><br>
        <input type="submit" value="登录">

    </form>
</body>
</html>
```

webapp/index.jsp

```html
<%@page contentType="text/html; UTF-8" pageEncoding="utf-8" isELIgnored="false" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Document</title>
</head>
<body>

    <h1>系统主页</h1>
    <a href="${pageContext.request.contextPath}/user/logout">登出</a>
    <ul>
        <li><a href="">用户管理</a></li>
        <li><a href="">商品管理</a></li>
        <li><a href="">物流管理</a></li>
        <li><a href="">订单管理</a></li>
    </ul>

</body>
</html>
```

UserMapper

```java
@Select("select * from `t_user` where `username` = #{username}")
User selectUserByUsername(@Param("username") String username);
```

UserController

```java
@PostMapping("/login")
public String login(String username, String password) {
    Subject subject = SecurityUtils.getSubject();
    try {
        subject.login(new UsernamePasswordToken(username, password));
        return "redirect:/index.jsp";
    } catch (AuthenticationException e) {
        System.out.println("用户名或密码错误");
    }
    return "redirect:/login.jsp";
}
```

**修改认证功能**

```java
public class CustomerRealm extends AuthorizingRealm {
    // 授权
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    // 认证
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String principal = (String) token.getPrincipal();
        // 使用工厂获取mapper
        UserMapper userMapper = (UserMapper) ApplicationContextUtils.getBean("userMapper");
        // 查看是否存在此用户
        User user = userMapper.selectUserByUsername(principal);
        if (!ObjectUtils.isEmpty(user)) {
            SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(
                    user.getUsername(), user.getPassword(), ByteSource.Util.bytes(user.getSalt()), this.getName());
            return info;
        }
        return null;
    }
}
```



### 1.4 授权

前面我们讲了登录时如何进行认证，那么认证之后就是授权了

`CustomerRealm`

```java
// 授权
@Override
protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String principal = (String) principals.getPrimaryPrincipal();
    System.out.println("主体身份：" + principal);
    SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
    // 这里数据库查询用户拥有的角色，这里写死
    authorizationInfo.addRoles(Arrays.asList("admin", "user"));
    // 用户拥有的权限 -> 对用户有创建权限、订单有创建和修改的权限
    authorizationInfo.addStringPermissions(Arrays.asList("user:create", "order:update", "order:create"));
    return authorizationInfo;
}
```



#### ①jsp标签的方式





#### ②代码方式





#### ③注解方式







### 1.5 数据库中获取权限

上面我们的数据是写死的，我们真实开发是要从数据库获取数据的，所以下面我们将数据源更换为数据库



#### ①存储方式

-   用户 -> 角色 -> 权限 -> 资源
-   用户 -> 角色
-   用户 -> 权限

注：根据业务选择方式，我们这里选择第一种



#### ②表创建

需要两张表，角色表(`t_role`)、权限字符串表(`t_perms`)

这两张表与`t_user`表的关系如下

<img src="https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220713233459046.png" alt="image-20220713233459046" style="zoom:67%;" />

```sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_perms
-- ----------------------------
DROP TABLE IF EXISTS `t_perms`;
CREATE TABLE `t_perms`  (
  `id` int(6) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_perms
-- ----------------------------

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role`  (
  `id` int(6) NOT NULL AUTO_INCREMENT,
  `name` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_role
-- ----------------------------

-- ----------------------------
-- Table structure for t_role_perms
-- ----------------------------
DROP TABLE IF EXISTS `t_role_perms`;
CREATE TABLE `t_role_perms`  (
  `id` int(6) NOT NULL AUTO_INCREMENT,
  `roleid` int(6) NULL DEFAULT NULL,
  `permsid` int(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_role_perms
-- ----------------------------

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` int(6) NOT NULL AUTO_INCREMENT,
  `username` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `password` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `salt` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, 'heizai', '77ca684d9ae4d040583e724122c4fd05', 'sdfsf923');
INSERT INTO `t_user` VALUES (2, 'xiaohei', '77ca684d9ae4d040583e724122c4fd05', 'sdfsf923');

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role`  (
  `id` int(6) NOT NULL AUTO_INCREMENT,
  `userid` int(6) NULL DEFAULT NULL,
  `roleid` int(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user_role
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
```



#### ③测试获取角色

给`role表`写入假数据，在`t_user_role表`中建立关系

-   User

    ```java
    public class User {
    
        private Integer id;
        private String username;
        private String password;
        private String salt;
    
        // 定义角色集合
        private List<Role> roles;
    }
    ```

-   UserMapper

    ```java
    // 根据用户名查询所有角色
    User findRolesByUsername(String username);
    ```

-   UserMapper.xml

    ```xml
    <resultMap id="userMap" type="user">
        <id column="uid" property="id"/>
        <result column="username" property="username"/>
        <!--角色信息-->
        <collection property="roles" javaType="list" ofType="role">
            <id column="rid" property="id"/>
            <result column="rname" property="name"/>
        </collection>
    </resultMap>
    <select id="findRolesByUsername" resultMap="userMap">
        SELECT
            u.id uid,
            u.username,
            r.id rid,
            r.`name` rname
        FROM
            t_user u
                LEFT JOIN t_user_role ur ON u.id = ur.userid
                LEFT JOIN t_role r ON ur.roleid = r.id
        WHERE
            username = #{username}
    </select>
    ```

-   **修改授权逻辑**

    ```java
    // 授权
        @Override
        protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
            String principal = (String) principals.getPrimaryPrincipal();
            System.out.println("主体身份：" + principal);
            SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
    //        // 这里数据库查询用户拥有的角色，这里写死
    //        // authorizationInfo.addRoles(Arrays.asList("admin", "user"));
    //        authorizationInfo.addRole("user");
    //        // 用户拥有的权限 -> 对用户有创建权限、订单有创建和修改的权限
    //        authorizationInfo.addStringPermissions(Arrays.asList("user:create", "user:update", "order:create", "order:save"));
    
            // 从数据库中获取数据
            // 使用工厂获取mapper
            UserMapper userMapper = (UserMapper) ApplicationContextUtils.getBean("userMapper");
            User user = userMapper.findRolesByUsername(principal);
            if (!CollectionUtils.isEmpty(user.getRoles())) {
                for (Role role : user.getRoles()) {
                    authorizationInfo.addRole(role.getName());
                }
            }
            return authorizationInfo;
        }
    ```

-   修改OrderController

    ```java
    @GetMapping("/save")
    @RequiresRoles(value = {"admin"})
    public String saveOrder() {
        System.out.println("save");
        return "redirect:/index.jsp";
    }
    ```



#### ④测试获取权限字符串

`t_perms`写入数据，`t_role_perms`中建立关系

-   改造role，添加一个属性

    ```java
    // 定义权限集合
    private List<Perms> perms;
    ```

-   UserMapper

    ```java
    User findRolesByUsername(String username);
    ```

-   UserMapper.xml

    ```xml
    <resultMap id="userMap" type="user">
        <id column="uid" property="id"/>
        <result column="username" property="username"/>
        <!--角色信息-->
        <collection property="roles" javaType="list" ofType="role">
            <id column="rid" property="id"/>
            <result column="rname" property="name"/>
            <collection property="perms" javaType="list" ofType="perms">
                <id column="pid" property="id"/>
                <result column="pname" property="name"/>
            </collection>
        </collection>
    </resultMap>
    <select id="findRolesByUsername" resultMap="userMap">
        SELECT
            u.id uid,
            u.username,
            r.id rid,
            r.`name` rname,
            p.id pid,
            p.`name` pname
        FROM
            t_user u
                LEFT JOIN t_user_role ur ON u.id = ur.userid
                LEFT JOIN t_role r ON ur.roleid = r.id
                LEFT JOIN t_role_perms rp ON r.id = rp.roleid
                LEFT JOIN t_perms p ON rp.permsid = p.id
        WHERE
            username = #{username}
    </select>
    ```

-   CustomerRealm

    ```java
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String principal = (String) principals.getPrimaryPrincipal();
        System.out.println("主体身份：" + principal);
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        //        // 这里数据库查询用户拥有的角色，这里写死
        //        // authorizationInfo.addRoles(Arrays.asList("admin", "user"));
        //        authorizationInfo.addRole("user");
        //        // 用户拥有的权限 -> 对用户有创建权限、订单有创建和修改的权限
        //        authorizationInfo.addStringPermissions(Arrays.asList("user:create", "user:update", "order:create", "order:save"));
    
        // 从数据库中获取数据
        // 使用工厂获取mapper
        UserMapper userMapper = (UserMapper) ApplicationContextUtils.getBean("userMapper");
        User user = userMapper.findRolesByUsername(principal);
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            for (Role role : user.getRoles()) {
                authorizationInfo.addRole(role.getName());
                if (!CollectionUtils.isEmpty(role.getPerms())) {
                    for (Perms perm : role.getPerms()) {
                        authorizationInfo.addStringPermission(perm.getName());
                    }
                }
            }
        }
        return authorizationInfo;
    }
    ```








### 1.6 缓存

shiro中有缓存管理器，我们需要配置缓存管理器，将缓存接入进来



#### ①整合ehcache框架

1.  引入依赖

    ```xml
    <!--对应shiro版本-->
    <dependency>
        <groupId>org.apache.shiro</groupId>
        <artifactId>shiro-ehcache</artifactId>
        <version>1.9.0</version>
    </dependency>
    ```

2.  开启缓存

    shiroConfig

    ```java
    @Bean
    public Realm realm() {
    	......
    
        // 开启缓存
        customerRealm.setCacheManager(new EhCacheManager());
        // 设置开启缓存，必须要设置，不然不会生效
        customerRealm.setCachingEnabled(true);
        // 设置认证缓存名
        customerRealm.setAuthenticationCacheName("authenticationCache");
        // 设置授权缓存名
        customerRealm.setAuthorizationCacheName("authorizationCache");
        return customerRealm;
    }
    ```

3.  完成配置





#### ②使用redis缓存

1.  配置redis

    ```java
    @Configuration
    public class RedisConfig {
    
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            // 创建RedisTemplate对象
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            // 设置连接工厂
            template.setConnectionFactory(connectionFactory);
            // 创建JSON序列化工具
            GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
            // 设置key的序列化
            template.setKeySerializer(RedisSerializer.string());
            template.setHashKeySerializer(RedisSerializer.string());
            // 创建value的序列化
            template.setValueSerializer(jsonRedisSerializer);
            template.setHashValueSerializer(jsonRedisSerializer);
            return template;
        }
    }
    ```

2.  自定义缓存管理器

    需要实现`CacheManager`接口

    ```java
    public class RedisCacheManager implements CacheManager {
    
        @Override
        public <K, V> Cache<K, V> getCache(String s) throws CacheException {
            return new RedisCache<K, V>(s);
        }
    }
    ```

3.  自定义`Cache`

    ```java
    public class RedisCache<K, V> implements Cache<K, V> {
    
        private String cacheName;
    
        public RedisCache(String cacheName) {
            this.cacheName = cacheName;
        }
    
        public RedisCache() {
        }
    
        @Override
        public V get(K k) throws CacheException {
            System.out.println("获取值：" + this.cacheName);
            return (V) getRedisTemplate().opsForHash().get(this.cacheName, k.toString());
        }
    
        @Override
        public V put(K k, V v) throws CacheException {
            getRedisTemplate().opsForHash().put(this.cacheName, k.toString(), v);
            return null;
        }
    
        @Override
        public V remove(K k) throws CacheException {
            return (V) getRedisTemplate().opsForHash().delete(this.cacheName, k.toString());
        }
    
        @Override
        public void clear() throws CacheException {
            getRedisTemplate().delete(this.cacheName);
        }
    
        @Override
        public int size() {
            return getRedisTemplate().opsForHash().size(this.cacheName).intValue();
        }
    
        @Override
        public Set<K> keys() {
            return getRedisTemplate().opsForHash().keys(this.cacheName);
        }
    
        @Override
        public Collection<V> values() {
            return getRedisTemplate().opsForHash().values(this.cacheName);
        }
    
       	/**
         * 获取 redisTemplate
         * @return
         */
        private RedisTemplate getRedisTemplate() {
            return (RedisTemplate) ApplicationContextUtils.getBean("redisTemplate");
        }
    }
    ```





### 1.7 Remerber功能

前端页面添加一个多选框

```html
记住我：<input type="checkbox" name="rememberMe" value="true">
```



#### ①使用shiro自带实现

shiro配置类文件中添加如下功能

```java
// 安全管理器
@Bean
public DefaultWebSecurityManager defaultWebSecurityManager(Realm realm) {
	......
    manager.setRememberMeManager(rememberMeManager());
    return manager;
}


private CookieRememberMeManager rememberMeManager() {
    CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
    cookieRememberMeManager.setCookie(rememberMeCookie());
    // 设置加密算法
    cookieRememberMeManager.setCipherKey(Base64.getDecoder().decode("4AvVhmFLUs0KTA3Kprsdag=="));
    return cookieRememberMeManager;
}

private SimpleCookie rememberMeCookie() {
    SimpleCookie simpleCookie = new SimpleCookie("remeberMe");
    simpleCookie.setPath("/");
    simpleCookie.setHttpOnly(true);
    // 保存30天
    simpleCookie.setMaxAge(60 * 60 * 24 * 30);
    return simpleCookie;
}
```

修改登录方法

```java
@PostMapping("/login")
public String login(String username, String password, boolean rememberMe) {
	......
        subject.login(new UsernamePasswordToken(username, password, rememberMe));
	......
}
```

**注意**：配置到这里会出现session丢失的问题，shiro觉得rememberMe不安全，所以`authc拦截器`不会放行，`user拦截器`就会放行，所以我们可以将需要将需要记住我功能的资源放行、

配置shiro拦截器

```java
......
map.put("/index.jsp", "user");
shiroFilter.setFilterChainDefinitionMap(map);
......
```



#### ②自定义

我们使用shiro自带的就不能使用`authc`这个拦截器进行拦截，只能使用`user拦截器`，所以我们需要自定义一个拦截器来代替`authc拦截器`

我们知道`authc拦截器`它是`FormAuthenticationFilter`这个拦截器的映射，这个类本质是一个`认证过滤器`，所以我们可以继承这个类，然后重写`isAccessAllowed `方法，在方法中编写我们自己的逻辑











## 2、springBoot、shiro、thymeleaf

基本环境和上面的一致



### 2.1 shiro与thymeleaf整合

-   引入整合包

    ```xml
    <!--shiro与thymeleaf整合-->
    <dependency>
        <groupId>com.github.theborakompanioni</groupId>
        <artifactId>thymeleaf-extras-shiro</artifactId>
        <version>2.0.0</version>
    </dependency>
    ```

-   shiro配置类中配置shiro方言

    ```java
    /**
     * 配置方言对象，配置这个才能使用shiro的标签
     * @return
     */
    @Bean
    public ShiroDialect shiroDialect() {
        return new ShiroDialect();
    }
    ```

-   使用shiro标签和属性

    ```html
    <body>
    
        欢迎来到主页
    
        <!--未登录访客可以访问-->
        <span shiro:guest="">未登录的访客可以看到的页面</span><br>
    
        <!--认证通过或记住的用户-->
        <span shiro:user="">我是认证通过或记住的用户</span><br>
    
        <!--认证通过的用户，不包含记住的用户，这是与user标签不同的地方-->
        <span shiro:authenticated="">我是认证通过的用户</span><br>
    
        <!--输出当前用户信息，通常为登录帐号信息-->
        <span><shiro:principal/></span><br>
    
        <!--未认证的用户-->
        <span shiro:notAuthenticated="">请登录</span><br>
    
        <!--验证当前用户是否有这角色，角色多个时要同时拥有才会显示-->
        <span shiro:hasRole="admin">有admin角色</span><br>
        <span shiro:hasRole="user">有user角色</span><br>
    
        <!--验证当前用户是否没有这角色-->
        <span shiro:lacksRole="admin">没有admin角色</span><br>
    
        <!--验证当前用户是否有以下所有角色-->
        <span shiro:hasAllRoles="user, admin">有admin、user角色</span><br>
    
        <!--验证当前用户是否有以下任一角色-->
        <span shiro:hasAnyRoles="admin, user">有admin或user角色</span><br>
    
        <!--验证当前用户是否拥有指定权限-->
        <span shiro:hasPermission="user:create">可以创建user</span><br>
    
        <!--当前用户没有指定权限时，验证通过-->
        <span shiro:lacksPermission="order:*">没有订单模块的所有操作权限</span><br>
    
        <!--验证当前用户是否拥有以下所有权限-->
        <span shiro:hasAllPermissions="user:create, user:update">有用户创建和删除的权限</span>
    
        <!--验证当前用户是否拥有以下任意一个权限-->
        <span shiro:hasAnyPermissions="user:create, user:update">有用户创建或删除的权限</span>
    
    </body>
    ```

    

























