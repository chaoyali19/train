# ChaosMesh 配置指南

本文档说明了如何配置 ChaosMesh 故障注入服务的相关参数。

## 配置项说明

### 1. 基础配置

在 `application.yml` 文件中，可以配置以下 ChaosMesh 相关参数：

```yaml
# ChaosMesh 故障注入配置
chaos:
  mesh:
    namespace: chaos          # Kubernetes 命名空间，默认为 "chaos"
    jvm-port: 9277           # JVM 故障注入端口，默认为 9277
    timeout: 30000           # 故障注入超时时间（毫秒），默认为 30000
    enabled: true            # 是否启用故障注入，默认为 true
```

### 2. 配置项详解

#### namespace
- **说明**: 指定故障注入资源部署的 Kubernetes 命名空间
- **默认值**: `chaos`
- **用途**: 用于部署 NetworkChaos 和 JVMChaos 资源
- **示例**: 如果您的集群使用不同的命名空间，可以修改此值

#### jvm-port
- **说明**: JVM 故障注入使用的端口号
- **默认值**: `9277`
- **用途**: 用于 JVM 字节码注入的通信端口
- **注意**: 确保此端口在目标 Pod 中可用且未被占用

#### timeout
- **说明**: 故障注入操作的超时时间
- **默认值**: `30000` (30秒)
- **用途**: 控制 kubectl 命令的执行超时时间
- **建议**: 根据网络环境和集群性能调整此值

#### enabled
- **说明**: 是否启用故障注入功能
- **默认值**: `true`
- **用途**: 可以临时禁用故障注入功能
- **注意**: 设置为 `false` 时，所有故障注入操作将返回错误

## 环境特定配置

### 开发环境
```yaml
chaos:
  mesh:
    namespace: chaos-dev
    timeout: 60000
    enabled: true
```

### 测试环境
```yaml
chaos:
  mesh:
    namespace: chaos-test
    timeout: 45000
    enabled: true
```

### 生产环境
```yaml
chaos:
  mesh:
    namespace: chaos-prod
    timeout: 30000
    enabled: false  # 生产环境建议禁用
```

## 配置验证

### 1. 检查配置加载
启动服务后，检查日志中是否包含配置信息：
```
ChaosMeshConfig{namespace='chaos', jvmPort=9277, timeout=30000, enabled=true}
```

### 2. 验证命名空间
确保配置的命名空间在 Kubernetes 集群中存在：
```bash
kubectl get namespace chaos
```

### 3. 测试故障注入
使用故障注入接口测试配置是否正确：
```bash
curl -X POST http://localhost:8091/api/jvm/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-user-service",
    "interfaceName": "UserController",
    "methodName": "getUserById",
    "durationMinutes": 5
  }'
```

## 故障排除

### 1. 命名空间不存在
**错误**: `namespace "chaos" not found`
**解决**: 创建命名空间或修改配置中的 namespace 值

### 2. 权限不足
**错误**: `Forbidden` 或 `Unauthorized`
**解决**: 检查 ServiceAccount 权限，确保有操作目标命名空间的权限

### 3. 端口冲突
**错误**: JVM 故障注入失败
**解决**: 检查 jvm-port 配置，确保端口可用

### 4. 超时错误
**错误**: 操作超时
**解决**: 增加 timeout 配置值，或检查网络连接

## 最佳实践

1. **命名空间管理**: 为不同环境使用不同的命名空间
2. **权限控制**: 限制故障注入服务的权限范围
3. **监控告警**: 监控故障注入的成功率和影响
4. **备份恢复**: 定期备份故障注入配置
5. **测试验证**: 在生产环境使用前充分测试

## 相关文件

- `ChaosMeshConfig.java`: 配置类定义
- `JvmFaultService.java`: JVM 故障注入服务
- `ChaosMeshService.java`: 网络故障注入服务
- `application.yml`: 主配置文件 