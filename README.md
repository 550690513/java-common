# java-common

常用 Java 注解与工具类集合，便于在各项目中复用。

## 模块

| 模块 | 说明 |
| --- | --- |
| `zhe-log-spring-boot-starter` | 方法日志打印：`@LogPrint` / `@RequestLog` / `@ResponseLog` |

> groupId 为 `io.github.550690513`（Maven Central 的 GitHub 命名空间），代码包名保持 `com.zhe.*`。

## zhe-log-spring-boot-starter

基于 AOP 的方法日志打印 starter，引入依赖后自动装配，注解标注即生效。

### 引入依赖

```xml
<dependency>
    <groupId>io.github.550690513</groupId>
    <artifactId>zhe-log-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

> 兼容 Spring Boot 2.7+ / 3.x（同时提供 `spring.factories` 与 `AutoConfiguration.imports` 两种注册方式），Java 8+。
> 未发布前可通过 `mvn clean install` 安装到本地仓库引用；发布到 Maven Central 见下文[发布流程](#发布到-maven-central)。

### 注解说明

| 注解 | 作用 |
| --- | --- |
| `@LogPrint` | 入口打印参数，返回时打印结果与总耗时（嵌套调用只在外层打印一次结果，异常时打印异常与耗时） |
| `@RequestLog` | 入口打印参数，抛异常时打印异常 |
| `@ResponseLog` | 正常返回后打印返回值 |

三个注解均可标注在**方法或类**上，标注在类上时对该类所有 public 方法生效。切面捕获自身异常，日志打印失败不影响业务执行。

### 快速开始

```java
@RestController
public class UserController {

    @LogPrint
    @PostMapping("/user")
    public User create(@RequestBody User user) {
        ...
    }
}
```

日志输出示例（Logback 默认格式）：

```
INFO  c.z.l.a.LogPrintAspect : user=null com.example.UserController#create() param={"user":{"name":"tom"}}
INFO  c.z.l.a.LogPrintAspect : user=null com.example.UserController#create() result={"id":1,"name":"tom"}, cost: 12ms
```

异常路径：

```
ERROR c.z.l.a.LogPrintAspect : user=null com.example.UserService#update() throw=java.lang.IllegalStateException: boom, cost: 3ms
```

参数序列化说明：

- 优先打印具名参数（宿主应用编译需开启 `-parameters`，Spring Boot 默认已开启）；否则退化为参数数组
- `HttpServletRequest` / `HttpServletResponse` / `MultipartFile` / `InputStream` 类型只打印 `[Xxx skipped]` 占位，不序列化
- 序列化失败的单个参数降级为 `[unserializable: 类名]`，不影响其余参数打印
- `Instant` 序列化为 `yyyy-MM-dd HH:mm:ss`

### 在日志中带上用户标识

库内提供 `com.zhe.log.context.LogContext`，在宿主应用的认证拦截器 / 过滤器中设置即可：

```java
try {
    LogContext.setUserId(userId);
    chain.doFilter(request, response);
} finally {
    LogContext.clear();
}
```

设置后，所有日志前缀会带上 `user=123`。

### 配置项

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `zhe.log.enabled` | `true` | 整体开关，设为 `false` 可在生产环境关闭 |
| `zhe.log.log-print-order` | `Ordered.LOWEST_PRECEDENCE` | `@LogPrint` 切面顺序，数值越小越靠外层 |
| `zhe.log.request-log-order` | `Ordered.LOWEST_PRECEDENCE` | `@RequestLog` 切面顺序 |
| `zhe.log.response-log-order` | `Ordered.LOWEST_PRECEDENCE` | `@ResponseLog` 切面顺序 |

如需与登录 / 权限等切面配合排序，例如：

```yaml
zhe:
  log:
    log-print-order: 2
    request-log-order: 3
    response-log-order: 4
```

### 自定义切面

starter 中的三个切面均以 `@ConditionalOnMissingBean` 注册，宿主应用声明自己的同名类型 Bean 即可完全替换。

## 发布到 Maven Central

首次发布前需要完成一次性配置：

1. **注册 [Maven Central Portal](https://central.sonatype.com)** 账号
2. **验证命名空间** `io.github.550690513`：在 Portal 的 Namespaces 页面添加，按提示在 GitHub 账号下创建同名验证仓库后点击验证
3. **生成 Portal Token**：Portal 账号设置中生成 user token，配置到 `~/.m2/settings.xml`：

```xml
<servers>
    <server>
        <id>central</id>
        <username>Portal Token 用户名</username>
        <password>Portal Token 密码</password>
    </server>
</servers>
```

4. **准备 GPG 密钥**（Central 要求所有构件带签名）：

```bash
gpg --full-generate-key                                # RSA 4096，姓名/邮箱与 pom 中 developers 一致
gpg --list-keys --keyid-format long                    # 记下 KEYID
gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>   # 公钥发布到密钥服务器供 Central 校验
```

之后每次发版：

```bash
mvn clean deploy -Prelease
```

构建会自动附加 sources jar、javadoc jar 并做 GPG 签名，上传后 Portal 校验通过即自动发布（约 10-30 分钟内可在 central.sonatype.com 与镜像站检索到）。

本地验证发布产物（不签名、不上传）：

```bash
mvn clean package -Prelease -Dgpg.skip=true
```

## License

[MIT](LICENSE)
