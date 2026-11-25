# java-common

常用 Java 注解与工具类集合，便于在各项目中复用。

## 模块

| 模块 | 说明 |
| --- | --- |
| `zhe-log-spring-boot-starter` | 方法日志打印：`@LogPrint` / `@RequestLog` / `@ResponseLog` |

## zhe-log-spring-boot-starter

基于 AOP 的方法日志打印 starter，引入依赖后自动装配，注解标注即生效。

### 引入依赖

```xml
<dependency>
    <groupId>com.zhe</groupId>
    <artifactId>zhe-log-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

> 兼容 Spring Boot 2.7+ / 3.x（同时提供 `spring.factories` 与 `AutoConfiguration.imports` 两种注册方式），Java 8+。
> 当前通过 `mvn clean install` 安装到本地仓库后引用；如需发布到 Maven Central，后续补充 GPG 签名与 distributionManagement 配置。

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

## License

[MIT](LICENSE)
