# java-helper-libs-dpk

Annotation-driven helper libraries for **timing**, **logging**, and **resilience** in Java 21 applications. Drop an annotation on any method and get automatic instrumentation — no boilerplate.

Built with AspectJ + Spring AOP.

---

## Modules

| Module | Purpose | Runtime dependencies |
|---|---|---|
| `timing-annotations` | `@Timed`, `@TimedClass`, `@Tag` | **None** |
| `timing-aop` | Timing aspect + `MetricsReporter` | AspectJ, Spring Expression, SLF4J |
| `logging-annotations` | `@LogEntry`, `@LogExit`, `@MaskField`, `@LogPerformance` | **None** |
| `logging-aop` | Logging aspect with parameter masking | AspectJ, SLF4J |
| `resilience-annotations` | `@Retry`, `@CircuitBreaker`, `@Fallback` | **None** |
| `resilience-aop` | Retry, circuit breaker, and fallback aspects | AspectJ, SLF4J |
| `helper-bom` | BOM for version alignment | — |

---

## Quick Start

### 1. Add dependencies

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation(platform("com.dpk.helper:helper-bom:0.1.0-SNAPSHOT"))

    // Pick what you need:
    implementation("com.dpk.helper:timing-annotations")
    implementation("com.dpk.helper:timing-aop")
    implementation("com.dpk.helper:logging-annotations")
    implementation("com.dpk.helper:logging-aop")
    implementation("com.dpk.helper:resilience-annotations")
    implementation("com.dpk.helper:resilience-aop")
}
```

**Maven:**

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.dpk.helper</groupId>
            <artifactId>helper-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Pick what you need -->
    <dependency>
        <groupId>com.dpk.helper</groupId>
        <artifactId>timing-aop</artifactId>
    </dependency>
    <dependency>
        <groupId>com.dpk.helper</groupId>
        <artifactId>logging-aop</artifactId>
    </dependency>
    <dependency>
        <groupId>com.dpk.helper</groupId>
        <artifactId>resilience-aop</artifactId>
    </dependency>
</dependencies>
```

### 2. Register the aspects

```java
@Configuration
@EnableAspectJAutoProxy
public class HelperConfig {

    // Timing
    @Bean
    public TimedAspect timedAspect() {
        return new TimedAspect(); // uses Slf4jMetricsReporter by default
    }

    // Logging
    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

    // Resilience
    @Bean
    public RetryAspect retryAspect() {
        return new RetryAspect();
    }

    @Bean
    public CircuitBreakerAspect circuitBreakerAspect() {
        return new CircuitBreakerAspect();
    }

    @Bean
    public FallbackAspect fallbackAspect() {
        return new FallbackAspect();
    }
}
```

### 3. Annotate your code

```java
@Service
public class OrderService {

    @Timed
    @LogEntry
    @LogExit
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }
}
```

---

# Timing

## `@Timed` — Method level

Instruments a single method and reports execution duration.

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

## `@TimedClass` — Class level

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

## Precedence rules

When both `@Timed` and `@TimedClass` are present, **method-level `@Timed` wins**. The class-level tags are NOT merged — the method-level annotation is used exclusively.

```java
@TimedClass(prefix = "orders", tags = @Tag(key = "component", value = "order"))
public class OrderService {

    // Uses @TimedClass -> metric: "orders.processOrder", tags: {component=order}
    public void processOrder() { ... }

    // Uses @Timed -> metric: "custom.metric", tags: {} (class tags NOT inherited)
    @Timed("custom.metric")
    public void specialMethod() { ... }
}
```

## Dynamic Tags (SpEL)

Dynamic tags are resolved at runtime using [Spring Expression Language](https://docs.spring.io/spring-framework/reference/core/expressions.html). Format: `key=expression`.

| Variable | Type | Description |
|---|---|---|
| `#args` | `Object[]` | Method arguments by index |
| `#target` | `Object` | The object the method is invoked on |
| `#paramName` | varies | Named parameters (requires `-parameters` javac flag) |

```java
@Timed(dynamicTags = "orderId=#args[0]")
public Order getOrder(String orderId) { ... }

@Timed(dynamicTags = {"userId=#userId", "count=#args.length"})
public void bulkProcess(String userId, List<Item> items) { ... }
```

> **Note:** The `-parameters` compiler flag must be enabled for named parameter access. This project enables it by default in `build.gradle.kts`.

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
        Timer.Builder builder = Timer.builder(metricName);
        tags.forEach(builder::tag);
        if (exception != null) {
            builder.tag("exception", exception.getClass().getSimpleName());
        }
        builder.register(registry).record(Duration.ofNanos(durationNanos));
    }
}
```

Wire it in:

```java
@Bean
public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(new MicrometerMetricsReporter(registry));
}
```

## Timing Log Output

**Success (INFO):**
```
TIMED [OrderService.getOrder] completed in 12.34ms tags={env=prod, region=us-east}
```

**Exception (WARN):**
```
TIMED [OrderService.getOrder] failed in 5.67ms tags={env=prod} exception=IllegalArgumentException
```

---

# Logging

## `@LogEntry` — Log method entry

Logs method name and parameter values when the method is invoked.

```java
@LogEntry
public Order getOrder(String orderId) { ... }
// DEBUG --> OrderService.getOrder(orderId=ORD-123)

@LogEntry(level = LogLevel.INFO, message = "fetching order")
public Order getOrder(String orderId) { ... }
// INFO  --> OrderService.getOrder(orderId=ORD-123) [fetching order]
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `level` | `LogLevel` | `DEBUG` | Log level (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`) |
| `message` | `String` | `""` | Custom message appended to the log line |

## `@LogExit` — Log method exit

Logs method return value (or exception) on exit.

```java
@LogExit
public Order getOrder(String orderId) { ... }
// DEBUG <-- OrderService.getOrder() => Order{id=ORD-123}

@LogExit(includeReturnValue = false)
public Order getOrder(String orderId) { ... }
// DEBUG <-- OrderService.getOrder()
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `level` | `LogLevel` | `DEBUG` | Log level |
| `includeReturnValue` | `boolean` | `true` | Whether to include the return value |
| `logExceptions` | `boolean` | `true` | Whether to log on exception (at WARN level) |

Combine `@LogEntry` + `@LogExit` for full entry/exit logging:

```java
@LogEntry
@LogExit
public Order getOrder(String orderId) { ... }
// DEBUG --> OrderService.getOrder(orderId=ORD-123)
// DEBUG <-- OrderService.getOrder() => Order{id=ORD-123}
```

## `@MaskField` — Mask sensitive parameters

Prevents sensitive parameter values from appearing in log output.

```java
@LogEntry
public void login(String username, @MaskField String password) { ... }
// DEBUG --> LoginService.login(username=admin, password=***)

@LogEntry
public void setToken(String user, @MaskField(mask = "[REDACTED]") String token) { ... }
// DEBUG --> TokenService.setToken(user=admin, token=[REDACTED])
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `mask` | `String` | `"***"` | The replacement string |

## `@LogPerformance` — Combined timing + structured log

Single annotation that logs entry parameters, exit result, and execution duration in one log line.

```java
@LogPerformance
public Order getOrder(String orderId) { ... }
// INFO  PERF OrderService.getOrder(orderId=ORD-123) [42ms]

@LogPerformance(includeReturnValue = true)
public int calculate(int a, int b) { ... }
// INFO  PERF Calculator.calculate(a=10, b=20) => 30 [1ms]

@LogPerformance(thresholdMs = 500)
public void fastMethod() { ... }
// (nothing logged — completed under 500ms threshold)
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `level` | `LogLevel` | `INFO` | Log level (exceptions always log at WARN) |
| `includeArgs` | `boolean` | `true` | Whether to include parameters |
| `includeReturnValue` | `boolean` | `false` | Whether to include the return value |
| `thresholdMs` | `long` | `0` | Only log if execution exceeds this (ms). 0 = always |

---

# Resilience

## `@Retry` — Automatic retries

Retries a method on failure with configurable backoff.

```java
@Retry(maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
public String callExternalService() { ... }
// Attempt 1 fails -> wait 100ms
// Attempt 2 fails -> wait 200ms
// Attempt 3 succeeds (or throws if it also fails)
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `maxAttempts` | `int` | `3` | Total attempts (initial + retries) |
| `backoff` | `@Backoff` | `@Backoff` | Backoff configuration |
| `retryOn` | `Class[]` | `{}` | Exception types that trigger retry (empty = all) |
| `noRetryOn` | `Class[]` | `{}` | Exception types that skip retry (takes precedence) |

### `@Backoff` — Backoff configuration

| Attribute | Type | Default | Description |
|---|---|---|---|
| `delay` | `long` | `100` | Initial delay in milliseconds |
| `multiplier` | `double` | `1.0` | Multiplier applied after each retry |
| `maxDelay` | `long` | `0` | Maximum delay cap (0 = no cap) |

### Selective retry

```java
// Only retry on network errors, not on validation errors
@Retry(
    maxAttempts = 3,
    retryOn = {IOException.class, TimeoutException.class},
    noRetryOn = IllegalArgumentException.class,
    backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000)
)
public String callApi(String request) { ... }
```

## `@CircuitBreaker` — Circuit breaker pattern

Prevents repeated calls to a failing service. Tracks consecutive failures and opens the circuit when the threshold is reached.

```java
@CircuitBreaker(failureThreshold = 5, resetTimeoutMs = 30000)
public String callExternalService() { ... }
```

**States:**
- **CLOSED** — Normal operation. Failures are counted.
- **OPEN** — All calls fail immediately with `CircuitBreakerOpenException`. No execution.
- **HALF_OPEN** — After `resetTimeoutMs`, one trial call is allowed. Success closes the circuit; failure re-opens it.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | `""` | Circuit name (default: `ClassName.methodName`). Methods with the same name share state. |
| `failureThreshold` | `int` | `5` | Consecutive failures before opening |
| `resetTimeoutMs` | `long` | `30000` | Time (ms) before transitioning OPEN to HALF_OPEN |
| `failOn` | `Class[]` | `{}` | Exception types that count as failures (empty = all) |

### Handling the open circuit

```java
try {
    service.callExternalService();
} catch (CircuitBreakerOpenException e) {
    // Circuit is open — use cached data, return default, etc.
    log.warn("Circuit {} is open, using fallback", e.getCircuitName());
}
```

## `@Fallback` — Fallback methods

Invokes a fallback method when the annotated method throws an exception.

```java
@Fallback(fallbackMethod = "getOrderFallback")
public Order getOrder(String orderId) {
    return externalService.fetchOrder(orderId);
}

// Option A: Same parameters
public Order getOrderFallback(String orderId) {
    return Order.defaultOrder(orderId);
}

// Option B: Same parameters + Throwable (receives the cause)
public Order getOrderFallback(String orderId, Throwable ex) {
    log.warn("Falling back for {}: {}", orderId, ex.getMessage());
    return Order.defaultOrder(orderId);
}
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `fallbackMethod` | `String` | — | Name of the fallback method (must be in the same class) |
| `applyOn` | `Class[]` | `{}` | Exception types that trigger the fallback (empty = all) |

**Fallback method requirements:**
- Must be in the same class
- Must have the same return type
- Must accept either the same parameters, or the same parameters + a trailing `Throwable`

### Selective fallback

```java
// Only fall back on service errors, not on validation errors
@Fallback(fallbackMethod = "fallback", applyOn = {IOException.class, ServiceException.class})
public String callService(String id) { ... }
```

## Combining resilience annotations

Annotations can be stacked. The aspect execution order is: **Fallback > Retry > CircuitBreaker** (outermost to innermost).

```java
@Fallback(fallbackMethod = "fallbackHandler")
@Retry(maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
@CircuitBreaker(failureThreshold = 5, resetTimeoutMs = 30000)
public String callService(String id) {
    return externalApi.call(id);
}

public String fallbackHandler(String id, Throwable ex) {
    return "default-value";
}
// Flow: Fallback wraps Retry wraps CircuitBreaker
// If circuit is open -> CircuitBreakerOpenException -> retried -> fallback
// If all retries fail -> fallback kicks in
```

---

## Requirements

- **Java 21+**
- **Spring AOP** (for `@EnableAspectJAutoProxy` proxy-based weaving)
- **SLF4J** binding (e.g., Logback, slf4j-simple) for log output

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

Run tests (52 tests across all modules):

```bash
./gradlew test
```

Publish to local Maven repository:

```bash
./gradlew publishToMavenLocal
```

---

## License

This project is unlicensed. Add a `LICENSE` file to specify terms.
