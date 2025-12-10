# 基于拓扑关系的上游服务延迟功能

## 功能概述

本功能允许根据拓扑图中的服务关系，精确地对选中服务的上游流量进行网络延迟故障注入，而不是对所有流量都进行延迟。

## 功能特点

1. **精确控制**: 只对进入目标服务的上游流量进行延迟
2. **拓扑感知**: 自动识别服务间的依赖关系
3. **可视化配置**: 在拓扑页面直观选择故障参数
4. **灵活配置**: 支持延迟所有流量或仅延迟上游流量

## 技术实现

### 后端实现

#### 1. 扩展故障请求模型

```java
public class ChaosFaultRequest {
    private String serviceName;        // 目标服务
    private String faultType;          // 故障类型
    private Integer delaySeconds;      // 延迟秒数
    private Integer durationMinutes;   // 持续时间
    private String targetService;      // 目标服务（被延迟的服务）
    private String sourceService;      // 源服务（发起延迟的服务）
    private Boolean delayUpstream;     // 是否延迟上游流量
}
```

#### 2. 增强 Chaos Mesh YAML 生成

当 `delayUpstream=true` 时，生成的 YAML 包含方向性配置：

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay-service-1234567890
  namespace: chaos
spec:
  action: delay
  mode: one
  direction: to  # 只对进入目标服务的流量进行延迟
  selector:
    namespaces:
      - chaos
    labelSelectors:
      app: source-service
  target:
    selector:
      namespaces:
        - chaos
      labelSelectors:
        app: target-service
  delay:
    latency: "2s"
    correlation: "0"
    jitter: "0ms"
  duration: "5m"
```

### 前端实现

#### 1. 故障注入界面增强

- 添加"仅延迟上游服务流量"复选框
- 动态显示上游服务列表
- 支持选择特定的上游服务

#### 2. 拓扑关系分析

```javascript
function findUpstreamServices(targetService) {
    const upstreamServices = new Set();
    
    currentGraphData.edges.forEach(edge => {
        if (edge.target === targetService) {
            const sourceNode = currentGraphData.nodes.find(node => node.id === edge.source);
            if (sourceNode && sourceNode.name !== 'browser') {
                upstreamServices.add(sourceNode.name);
            }
        }
    });
    
    return Array.from(upstreamServices);
}
```

## 使用方法

### 1. 通过 Web 界面使用

1. 打开拓扑页面
2. 点击任意服务节点
3. 选择"网络延迟"故障类型
4. 勾选"仅延迟上游服务流量"选项
5. 设置延迟时间和持续时间
6. 点击"应用故障"

### 2. 通过 API 使用

#### 延迟上游流量

```bash
curl -X POST http://localhost:8080/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-user-service",
    "faultType": "network-delay",
    "delaySeconds": 2,
    "durationMinutes": 5,
    "targetService": "ts-user-service",
    "sourceService": "ts-auth-service",
    "delayUpstream": true
  }'
```

#### 延迟所有流量

```bash
curl -X POST http://localhost:8080/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-order-service",
    "faultType": "network-delay",
    "delaySeconds": 1,
    "durationMinutes": 3,
    "delayUpstream": false
  }'
```

## 故障类型对比

| 故障类型 | 影响范围 | 适用场景 |
|---------|---------|---------|
| 延迟所有流量 | 服务的所有网络流量 | 模拟服务整体网络问题 |
| 延迟上游流量 | 仅进入目标服务的上游流量 | 模拟特定服务间网络问题 |

## 测试验证

### 1. 运行测试脚本

```bash
chmod +x test-upstream-delay.sh
./test-upstream-delay.sh
```

### 2. 验证步骤

1. **获取拓扑信息**: 确认服务间依赖关系
2. **应用上游延迟**: 对特定服务应用上游流量延迟
3. **查看活跃故障**: 确认故障已正确应用
4. **应用全量延迟**: 对比不同延迟模式
5. **停止故障**: 验证故障停止功能

## 注意事项

1. **服务名称**: 系统会自动处理 `chaos_` 前缀
2. **拓扑依赖**: 确保拓扑配置正确反映服务间关系
3. **权限要求**: 需要 kubectl 权限来应用 Chaos Mesh 资源
4. **故障清理**: 系统会自动清理过期的故障

## 故障排查

### 常见问题

1. **找不到上游服务**: 检查拓扑配置是否正确
2. **故障应用失败**: 检查 kubectl 权限和 Chaos Mesh 安装
3. **延迟不生效**: 确认服务标签和命名空间配置

### 日志查看

```bash
# 查看应用日志
kubectl logs -f deployment/fault-control-service -n chaos

# 查看 Chaos Mesh 资源
kubectl get networkchaos -n chaos

# 查看故障详情
kubectl describe networkchaos <chaos-name> -n chaos
```

## 扩展功能

未来可以考虑添加的功能：

1. **多上游服务选择**: 支持选择多个上游服务
2. **故障组合**: 支持同时应用多种故障类型
3. **故障模板**: 预定义常用的故障配置
4. **故障历史**: 记录和查看历史故障
5. **故障回放**: 重现历史故障场景 