# java-helper-libs-dpk

Annotation-driven helper libraries for **timing**, **logging** (planned), and **resilience** (planned) in Java 21 applications. Drop a `@Timed` annotation on any method and get automatic duration tracking — no boilerplate.

Built with AspectJ + Spring AOP. Pluggable metrics reporting so you can wire in Micrometer, Prometheus, StatsD, or anything else.

---

## Modules

| Module | Purpose | Runtime dependencies |
|---|---|---|
| `timing-annotations` | `@Timed`, `@TimedClass`, `@Tag` annotation definitions | **None** |
| `timing-aop` | AspectJ aspect + `MetricsReporter` interface | AspectJ, Spring Expression, SLF4J |
| `helper-bom` | BOM for version alignment | — |

---

## Quick Start

### 1. Add dependencies

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("com.dpk.helper:timing-annotations:0.1.0-SNAPSHOT")
    implementation("com.dpk.helper:timing-aop:0.1.0-SNAPSHOT")
}
```

Or use the BOM for version alignment:

```kotlin
dependencies {
    implementation(platform("com.dpk.helper:helper-bom:0.1.0-SNAPSHOT"))
    implementation("com.dpk.helper:timing-annotations")
    implementation("com.dpk.helper:timing-aop")
}
```

**Maven:**

```xml
<dependency>
    <groupId>com.dpk.helper</groupId>
    <artifactId>timing-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.dpk.helper</groupId>
    <artifactId>timing-aop</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Enable AspectJ auto-proxying

In your Spring configuration:

```java
@Configuration
@EnableAspectJAutoProxy
public class AppConfig {

    @Bean
    public TimedAspect timedAspect() {
        return new TimedAspect(); // uses Slf4jMetricsReporter by default
    }
}
```

### 3. Annotate your code

```java
@Service
public class OrderService {

    @Timed
    public Order getOrder(String orderId) {
        // metric name: "OrderService.getOrder"
        return orderRepository.findById(orderId);
    }
}
```

That's it. Timing data will be logged via SLF4J:

```
INFO  TIMED [OrderService.getOrder] completed in 12.34ms tags={}
```

---

## Annotations

### `@Timed` — Method level

Instruments a single method.

```java
@Timed(
    value = "orders.fetch",           // metric name (default: ClassName.methodName)
    tags = {
        @Tag(key = "env", value = "prod"),
        @Tag(key = "region", value = "us-east")
    },
    dynamicTags = {                   // SpEL expressions resolved at runtime
        "orderId=#args[0]",
        "userId=#userId"              // named parameter (requires -parameters compiler flag)
    },
    reportExceptions = true           // report timing even when method throws (default: true)
)
public Order getOrder(String orderId, String userId) { ... }
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | `""` | Metric name. Empty = `ClassName.methodName` |
| `tags` | `Tag[]` | `{}` | Static key-value tags |
| `dynamicTags` | `String[]` | `{}` | SpEL expressions in `key=expression` format |
| `reportExceptions` | `boolean` | `true` | Whether to report timing when the method throws |

### `@TimedClass` — Class level

Instruments **all public methods** in the class.

```java
@TimedClass(prefix = "orders", tags = @Tag(key = "component", value = "order"))
@Service
public class OrderService {

    public Order getOrder(String id) { ... }      // metric: "orders.getOrder"
    public void deleteOrder(String id) { ... }    // metric: "orders.deleteOrder"
    String internalMethod() { ... }               // NOT instrumented (package-private)
}
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `prefix` | `String` | `""` | Prepended to method name as `prefix.methodName` |
| `tags` | `Tag[]` | `{}` | Static tags applied to all methods |

### `@Tag` — Key-value pair

Used inside `@Timed` and `@TimedClass` to attach static metadata.

```java
@Timed(tags = { @Tag(key = "env", value = "staging"), @Tag(key = "team", value = "payments") })
```

### Precedence rules

When both `@Timed` and `@TimedClass` are present, **method-level `@Timed` wins**. The class-level tags are NOT merged — the method-level annotation is used exclusively.

```java
@TimedClass(prefix = "orders", tags = @Tag(key = "component", value = "order"))
public class OrderService {

    // Uses @TimedClass config -> metric: "orders.processOrder", tags: {component=order}
    public void processOrder() { ... }

    // Uses @Timed config -> metric: "custom.metric", tags: {} (class tags NOT inherited)
    @Timed("custom.metric")
    public void specialMethod() { ... }
}
```

---

## Dynamic Tags (SpEL)

Dynamic tags are resolved at runtime using [Spring Expression Language](https://docs.spring.io/spring-framework/reference/core/expressions.html). The format is `key=expression`.

### Available variables

| Variable | Type | Description |
|---|---|---|
| `#args` | `Object[]` | Method arguments by index |
| `#target` | `Object` | The object the method is invoked on |
| `#paramName` | varies | Named parameters (requires `-parameters` javac flag) |

### Examples

```java
// Access by argument index
@Timed(dynamicTags = "orderId=#args[0]")
public Order getOrder(String orderId) { ... }

// Access by parameter name
@Timed(dynamicTags = "name=#name")
public User findUser(String name) { ... }

// Access nested properties
@Timed(dynamicTags = "userId=#args[0].userId")
public void process(Request request) { ... }

// Multiple dynamic tags
@Timed(dynamicTags = { "userId=#userId", "count=#args.length" })
public void bulkProcess(String userId, List<Item> items) { ... }
```

> **Note:** The `-parameters` compiler flag must be enabled for named parameter access. This project enables it by default in `build.gradle.kts`.

---

## Custom Metrics Reporting

The default `Slf4jMetricsReporter` logs timing data. To integrate with your metrics backend, implement `MetricsReporter`:

```java
public interface MetricsReporter {
    void report(String metricName, long durationNanos, Map<String, String> tags, Throwable exception);
}
```

### Micrometer example

```java
public class MicrometerMetricsReporter implements MetricsReporter {

    private final MeterRegistry registry;

    public MicrometerMetricsReporter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void report(String metricName, long durationNanos, Map<String, String> tags, Throwable exception) {
        Timer.Builder builder = Timer.builder(metricName)
                .description("Method execution time");

        tags.forEach(builder::tag);

        if (exception != null) {
            builder.tag("exception", exception.getClass().getSimpleName());
        } else {
            builder.tag("exception", "none");
        }

        builder.register(registry)
               .record(Duration.ofNanos(durationNanos));
    }
}
```

### Wire it in

```java
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    @Bean
    public MetricsReporter metricsReporter(MeterRegistry registry) {
        return new MicrometerMetricsReporter(registry);
    }

    @Bean
    public TimedAspect timedAspect(MetricsReporter reporter) {
        return new TimedAspect(reporter);
    }
}
```

---

## Log Output Format

The default `Slf4jMetricsReporter` produces:

**Success (INFO):**
```
TIMED [OrderService.getOrder] completed in 12.34ms tags={env=prod, region=us-east}
```

**Exception (WARN):**
```
TIMED [OrderService.getOrder] failed in 5.67ms tags={env=prod} exception=IllegalArgumentException
```

---

## Requirements

- **Java 21+**
- **Spring AOP** (for `@EnableAspectJAutoProxy` proxy-based weaving)
- **SLF4J** binding (e.g., Logback, slf4j-simple) for default reporter

### Dependency versions

| Dependency | Version |
|---|---|
| AspectJ | 1.9.21 |
| Spring Expression | 6.1.14 |
| SLF4J | 2.0.16 |

---

## Building from Source

```bash
git clone https://github.com/oyedpk/java-helper-libs-dpk.git
cd java-helper-libs-dpk
./gradlew build
```

Run tests:

```bash
./gradlew test
```

Publish to local Maven repository:

```bash
./gradlew publishToMavenLocal
```

---

## Roadmap

| Phase | Module | Status |
|---|---|---|
| 1 | `timing-annotations` + `timing-aop` | Done |
| 2 | `logging-annotations` + `logging-aop` — `@LogEntry`, `@LogExit`, `@MaskField` | Planned |
| 3 | `resilience-annotations` + `resilience-aop` — `@Retry`, `@CircuitBreaker`, `@Fallback` | Planned |

---

## License

This project is unlicensed. Add a `LICENSE` file to specify terms.
