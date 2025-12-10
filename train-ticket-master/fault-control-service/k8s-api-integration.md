# Kubernetes API 集成说明

## 概述

本项目已从直接调用 `kubectl` 命令升级为使用 Kubernetes Java Client API，提供了更可靠、更高效的 Kubernetes 资源管理方式。

## 主要改进

### 1. 从 kubectl 命令到 Kubernetes API

**之前的方式**:
```bash
# 生成 YAML 文件
cat > /tmp/network-delay-service.yaml << EOF
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
...
EOF

# 执行 kubectl 命令
kubectl apply -f /tmp/network-delay-service.yaml
kubectl delete networkchaos chaos-name -n chaos
```

**现在的方式**:
```java
// 直接使用 Kubernetes API
kubernetesService.createNetworkChaos(chaosName, sourceService, targetService, delaySeconds, durationMinutes);
kubernetesService.deleteNetworkChaos(chaosName);
```

### 2. 优势对比

| 特性 | kubectl 命令 | Kubernetes API |
|------|-------------|----------------|
| 性能 | 较慢（进程启动开销） | 快速（直接 API 调用） |
| 可靠性 | 依赖外部命令 | 内置错误处理 |
| 类型安全 | 无 | 强类型支持 |
| 调试 | 困难 | 详细日志 |
| 依赖 | 需要 kubectl 安装 | 仅需 Java 客户端 |

## 技术实现

### 1. 依赖配置

```xml
<!-- Kubernetes Java Client -->
<dependency>
    <groupId>io.kubernetes</groupId>
    <artifactId>client-java</artifactId>
    <version>18.0.1</version>
</dependency>

<!-- Kubernetes Java Client Spring Boot Starter -->
<dependency>
    <groupId>io.kubernetes</groupId>
    <artifactId>client-java-spring-extended</artifactId>
    <version>18.0.1</version>
</dependency>
```

### 2. 核心服务类

#### KubernetesService
```java
@Service
public class KubernetesService {
    private final CustomObjectsApi customObjectsApi;
    
    // 创建 NetworkChaos 资源
    public boolean createNetworkChaos(String chaosName, String sourceService, String targetService, 
                                    int delaySeconds, int durationMinutes);
    
    // 删除 NetworkChaos 资源
    public boolean deleteNetworkChaos(String chaosName);
    
    // 检查资源是否存在
    public boolean networkChaosExists(String chaosName);
}
```

#### ChaosMeshService 集成
```java
@Service
public class ChaosMeshService {
    private final KubernetesService kubernetesService;
    
    // 使用 Kubernetes API 替代 kubectl 命令
    public String applyNetworkDelay(...) {
        boolean success = kubernetesService.createNetworkChaos(...);
        // 处理结果
    }
    
    public String stopFault(String chaosName) {
        boolean success = kubernetesService.deleteNetworkChaos(chaosName);
        // 处理结果
    }
}
```

### 3. 资源对象创建

#### 方向性延迟（上游流量）
```java
private Map<String, Object> createNetworkChaosObject(String chaosName, String sourceService, String targetService,
                                                    int delaySeconds, int durationMinutes) {
    Map<String, Object> networkChaos = new HashMap<>();
    
    // 元数据
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("name", chaosName);
    metadata.put("namespace", "chaos");
    networkChaos.put("metadata", metadata);
    
    // API 版本
    networkChaos.put("apiVersion", "chaos-mesh.org/v1alpha1");
    networkChaos.put("kind", "NetworkChaos");
    
    // 规格
    Map<String, Object> spec = new HashMap<>();
    spec.put("action", "delay");
    spec.put("mode", "one");
    spec.put("direction", "to");
    
    // 延迟配置
    Map<String, Object> delay = new HashMap<>();
    delay.put("latency", delaySeconds + "s");
    delay.put("correlation", "0");
    delay.put("jitter", "0ms");
    spec.put("delay", delay);
    
    // 持续时间
    spec.put("duration", durationMinutes + "m");
    
    // 选择器（目标服务）
    Map<String, Object> selector = new HashMap<>();
    selector.put("namespaces", new String[]{"chaos"});
    Map<String, Object> labelSelectors = new HashMap<>();
    labelSelectors.put("app", targetService);
    selector.put("labelSelectors", labelSelectors);
    spec.put("selector", selector);
    
    // 目标（源服务）
    Map<String, Object> target = new HashMap<>();
    target.put("mode", "one");
    Map<String, Object> targetSelector = new HashMap<>();
    targetSelector.put("namespaces", new String[]{"chaos"});
    Map<String, Object> targetLabelSelectors = new HashMap<>();
    targetLabelSelectors.put("app", sourceService);
    targetSelector.put("labelSelectors", targetLabelSelectors);
    target.put("selector", targetSelector);
    spec.put("target", target);
    
    networkChaos.put("spec", spec);
    return networkChaos;
}
```

#### 普通延迟（所有流量）
```java
private Map<String, Object> createSimpleNetworkChaosObject(String chaosName, String serviceName,
                                                         int delaySeconds, int durationMinutes) {
    // 简化的配置，没有 direction 和 target
    Map<String, Object> networkChaos = new HashMap<>();
    // ... 配置省略
    return networkChaos;
}
```

## 使用方式

### 1. API 调用

#### 延迟上游流量
```bash
curl -X POST http://localhost:8080/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-train-food-service",
    "faultType": "network-delay",
    "delaySeconds": 2,
    "durationMinutes": 5,
    "targetService": "ts-train-food-service",
    "sourceService": "ts-food-service",
    "delayUpstream": true
  }'
```

#### 延迟所有流量
```bash
curl -X POST http://localhost:8080/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-food-service",
    "faultType": "network-delay",
    "delaySeconds": 1,
    "durationMinutes": 3,
    "delayUpstream": false
  }'
```

### 2. Web 界面

- 打开拓扑页面
- 点击服务节点
- 选择故障类型和参数
- 应用故障

## 错误处理

### 1. API 异常处理
```java
try {
    Object result = customObjectsApi.createNamespacedCustomObject(...);
    return true;
} catch (ApiException e) {
    log.error("创建 NetworkChaos 失败: {}, 错误码: {}, 响应: {}", 
             chaosName, e.getCode(), e.getResponseBody(), e);
    return false;
}
```

### 2. 常见错误码
- `404`: 资源不存在
- `409`: 资源已存在
- `422`: 资源格式错误
- `500`: 服务器内部错误

## 测试验证

### 1. 运行测试脚本
```bash
chmod +x test-k8s-api.sh
./test-k8s-api.sh
```

### 2. 验证步骤
1. **应用启动检查**: 确认服务正常运行
2. **故障注入测试**: 验证上游流量延迟功能
3. **故障列表查看**: 确认故障正确创建
4. **故障停止测试**: 验证故障删除功能

## 监控和日志

### 1. 日志级别
```properties
# 应用日志
logging.level.fdse.microservice.service=DEBUG
logging.level.io.kubernetes=INFO
```

### 2. 关键日志
- Kubernetes 客户端初始化
- NetworkChaos 资源创建/删除
- API 调用结果
- 错误详情

## 性能优化

### 1. 连接池配置
```java
ApiClient client = ClientBuilder.cluster()
    .setHttpClientConfigCallback(httpClientBuilder -> {
        httpClientBuilder.setMaxConnTotal(100);
        httpClientBuilder.setMaxConnPerRoute(20);
        return httpClientBuilder;
    })
    .build();
```

### 2. 超时设置
```java
client.setConnectTimeout(5000);
client.setReadTimeout(30000);
```

## 故障排查

### 1. 常见问题

#### Kubernetes 客户端初始化失败
```bash
# 检查 kubeconfig
kubectl config view

# 检查权限
kubectl auth can-i create networkchaos --namespace chaos
```

#### API 调用失败
```bash
# 查看应用日志
kubectl logs -f deployment/fault-control-service -n chaos

# 检查 Chaos Mesh 安装
kubectl get pods -n chaos-mesh
```

### 2. 调试命令
```bash
# 查看 NetworkChaos 资源
kubectl get networkchaos -n chaos

# 查看资源详情
kubectl describe networkchaos <chaos-name> -n chaos

# 查看事件
kubectl get events -n chaos --sort-by='.lastTimestamp'
```

## 扩展功能

### 1. 支持更多故障类型
- PodChaos
- IOChaos
- TimeChaos
- KernelChaos

### 2. 批量操作
- 批量创建故障
- 批量删除故障
- 故障模板管理

### 3. 监控集成
- Prometheus 指标
- Grafana 仪表板
- 告警规则

## 总结

通过使用 Kubernetes Java Client API，我们实现了：

1. **更可靠的资源管理**: 直接 API 调用，减少外部依赖
2. **更好的错误处理**: 详细的异常信息和错误码
3. **更高的性能**: 避免进程启动开销
4. **更强的类型安全**: 编译时类型检查
5. **更好的调试体验**: 详细的日志和错误信息

这种改进使得故障注入功能更加稳定和高效，为生产环境提供了更好的支持。 