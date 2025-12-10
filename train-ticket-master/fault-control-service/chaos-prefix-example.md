# Chaos_ 前缀处理功能示例

## 功能说明

当服务名称以 `chaos_` 开头时，系统会自动去掉这个前缀，以便正确生成 Chaos Mesh 的故障配置。

## 处理逻辑

```java
// 去掉 chaos_ 前缀
String cleanServiceName = serviceName.startsWith("chaos_") ? 
    serviceName.substring(6) : serviceName;
```

## 示例对比

### 示例 1: 带 chaos_ 前缀的服务

**输入服务名称**: `chaos_ts-user-service`

**处理过程**:
1. 检测到 `chaos_` 前缀
2. 去掉前缀: `ts-user-service`
3. 生成故障名称: `network-delay-ts-user-service-1234567890`

**生成的 YAML**:
```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay-ts-user-service-1234567890
  namespace: default
spec:
  action: delay
  mode: one
  selector:
    namespaces:
      - default
    labelSelectors:
      app: ts-user-service  # 注意这里使用的是处理后的名称
  delay:
    latency: "5s"
    correlation: "100"
    jitter: "0ms"
  duration: "2m"
```

### 示例 2: 不带 chaos_ 前缀的服务

**输入服务名称**: `ts-user-service`

**处理过程**:
1. 没有检测到 `chaos_` 前缀
2. 保持原名称: `ts-user-service`
3. 生成故障名称: `network-delay-ts-user-service-1234567890`

**生成的 YAML**:
```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay-ts-user-service-1234567890
  namespace: default
spec:
  action: delay
  mode: one
  selector:
    namespaces:
      - default
    labelSelectors:
      app: ts-user-service
  delay:
    latency: "5s"
    correlation: "100"
    jitter: "0ms"
  duration: "2m"
```

## 测试方法

### 1. 使用测试脚本
```bash
./test-chaos-prefix.sh
```

### 2. 手动测试
```bash
# 测试带 chaos_ 前缀的服务
curl -X POST http://localhost:8090/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "chaos_ts-user-service",
    "faultType": "network-delay",
    "delaySeconds": 5,
    "durationMinutes": 2
  }'

# 测试不带 chaos_ 前缀的服务
curl -X POST http://localhost:8090/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-user-service",
    "faultType": "network-delay",
    "delaySeconds": 5,
    "durationMinutes": 2
  }'
```

## 日志输出示例

### 成功情况
```
2024-01-01 12:00:00 INFO  - 成功应用网络延迟故障 - 服务: chaos_ts-user-service (清理后: ts-user-service), 延迟: 5s, 持续时间: 2m
```

### 失败情况
```
2024-01-01 12:00:00 ERROR - 应用网络延迟故障失败 - 服务: chaos_ts-user-service (清理后: ts-user-service), 结果: error: no matches for kind "NetworkChaos"
```

## 注意事项

1. **前缀检测**: 只检测以 `chaos_` 开头的服务名称
2. **大小写敏感**: 前缀检测是大小写敏感的
3. **日志记录**: 日志中会同时显示原始名称和处理后的名称
4. **故障跟踪**: 活跃故障列表中保存的是原始服务名称
5. **向后兼容**: 不带前缀的服务名称不受影响

## 支持的格式

| 输入格式 | 处理结果 | 说明 |
|---------|---------|------|
| `chaos_ts-user-service` | `ts-user-service` | 去掉 chaos_ 前缀 |
| `ts-user-service` | `ts-user-service` | 保持不变 |
| `Chaos_ts-user-service` | `Chaos_ts-user-service` | 大小写不匹配，保持不变 |
| `chaos_` | `chaos_` | 只有前缀，没有服务名，保持不变 | 