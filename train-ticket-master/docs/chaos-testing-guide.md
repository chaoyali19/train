# Train-Ticket 故障模拟测试指南

本文档描述了如何在 Train-Ticket 微服务环境中模拟各种生产故障场景，用于测试系统的容错能力和故障恢复能力。

## 前置条件

### 1. 安装 Chaos Mesh

Chaos Mesh 是一个云原生混沌工程平台，用于模拟各种故障场景。

```bash
# 添加 Chaos Mesh Helm 仓库
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm repo update

# 安装 Chaos Mesh
helm install chaos-mesh chaos-mesh/chaos-mesh --namespace chaos-mesh --create-namespace
```

### 2. 确保 Train-Ticket 服务正常运行

```bash
# 检查服务状态
kubectl get pods -n chaos
kubectl get services -n chaos
```

## 故障模拟场景

### 故障1：异常大流量导致负载过高

**故障描述：** 系统突然接收到大量请求，导致服务负载过高，响应时间变长，甚至出现服务不可用。

**模拟方法：**
- 使用 `NetworkChaos` 增加网络延迟
- 使用 `StressChaos` 增加 CPU 负载
- 目标服务：`ts-order-service`（核心订单服务）

**影响范围：**
- 订单创建、查询功能响应缓慢
- 可能导致订单服务不可用
- 影响用户体验

**检测指标：**
- CPU 使用率 > 80%
- 响应时间 > 5秒
- 错误率上升

**恢复策略：**
- 自动扩缩容（HPA）
- 负载均衡
- 服务降级

### 故障2：依赖外部公共 API 服务不可用

**故障描述：** 系统依赖的外部第三方服务（如支付网关、短信服务）不可用，导致相关功能失效。

**模拟方法：**
- 使用 `NetworkChaos` 模拟网络分区
- 使用 `NetworkChaos` 增加网络延迟
- 目标服务：`ts-payment-service`、`ts-notification-service`

**影响范围：**
- 支付功能不可用
- 短信/邮件通知失败
- 相关业务流程中断

**检测指标：**
- 外部 API 调用失败率
- 服务超时错误
- 业务功能异常

**恢复策略：**
- 熔断器模式
- 降级处理
- 重试机制

### 故障7：业务逻辑实现错误

**故障描述：** 新版本部署后，业务逻辑实现错误，导致多个接口调用报错，影响大量上游服务。

**模拟方法：**
- 使用 `PodChaos` 模拟 Pod 故障
- 使用 `HTTPChaos` 注入 HTTP 错误
- 目标服务：`ts-order-service`、`ts-user-service`、`ts-gateway-service`

**影响范围：**
- 核心业务功能异常
- 多个服务级联故障
- 用户体验严重受损

**检测指标：**
- 错误率 > 10%
- 服务健康检查失败
- 业务指标异常

**恢复策略：**
- 快速回滚
- 蓝绿部署
- 服务隔离

### 故障8：Deployment 部署 Image 名称拼写错误

**故障描述：** 部署时镜像名称或标签拼写错误，导致 Pod 无法启动，服务不可用。

**模拟方法：**
- 创建带有错误镜像名称的 Deployment
- 目标：`ts-order-service-broken`

**影响范围：**
- 服务完全不可用
- Pod 启动失败
- 业务中断

**检测指标：**
- Pod 状态为 `ImagePullBackOff`
- 服务不可访问
- 健康检查失败

**恢复策略：**
- 修正镜像名称
- 重新部署
- 监控告警

## 使用方法

### 1. 运行单个故障模拟

```bash
# 模拟大流量负载过高
./scripts/chaos-test.sh 1

# 模拟外部API不可用
./scripts/chaos-test.sh 2

# 模拟业务逻辑错误
./scripts/chaos-test.sh 7

# 模拟镜像名称错误
./scripts/chaos-test.sh 8
```

### 2. 运行所有故障模拟

```bash
./scripts/chaos-test.sh all
```

### 3. 清理故障模拟

```bash
./scripts/chaos-test.sh cleanup
```

### 4. 查看帮助信息

```bash
./scripts/chaos-test.sh help
```

## 监控和观察

### 1. 服务状态监控

```bash
# 查看 Pod 状态
kubectl get pods -n chaos

# 查看服务状态
kubectl get services -n chaos

# 查看事件
kubectl get events -n chaos --sort-by='.lastTimestamp'
```

### 2. 资源使用监控

```bash
# 查看 CPU 和内存使用情况
kubectl top pods -n chaos

# 查看节点资源使用情况
kubectl top nodes
```

### 3. 日志监控

```bash
# 查看服务日志
kubectl logs -n chaos -l app=ts-order-service --tail=100

# 实时查看日志
kubectl logs -n chaos -l app=ts-order-service -f
```

### 4. 网络连接测试

```bash
# 测试服务连通性
kubectl exec -n chaos -it <pod-name> -- curl http://ts-order-service:8080/health

# 测试外部连接
kubectl exec -n chaos -it <pod-name> -- curl http://external-api.com
```

## 故障恢复验证

### 1. 自动恢复验证

- 检查 HPA 是否自动扩缩容
- 验证服务是否自动重启
- 确认负载均衡是否生效

### 2. 手动恢复验证

- 验证故障清理后服务是否正常
- 确认业务功能是否恢复
- 检查监控指标是否正常

### 3. 性能验证

- 测试服务响应时间
- 验证吞吐量是否正常
- 确认错误率是否降低

## 最佳实践

### 1. 故障模拟前准备

- 确保有足够的资源
- 备份重要数据
- 通知相关人员

### 2. 故障模拟期间

- 密切监控系统状态
- 记录故障现象
- 准备应急响应

### 3. 故障模拟后

- 及时清理故障
- 分析故障影响
- 总结改进措施

### 4. 持续改进

- 定期进行故障模拟
- 优化故障检测机制
- 完善恢复策略

## 注意事项

1. **生产环境谨慎使用**：故障模拟可能影响生产环境，建议在测试环境进行
2. **资源限制**：确保集群有足够资源处理故障模拟
3. **时间控制**：避免故障模拟时间过长，影响正常业务
4. **监控告警**：确保监控系统正常工作，及时发现异常
5. **团队协作**：故障模拟需要团队协作，确保沟通顺畅

## 故障模拟文件说明

- `chaos-load-test.yaml`: 大流量负载测试
- `chaos-external-api-failure.yaml`: 外部API故障模拟
- `chaos-business-logic-error.yaml`: 业务逻辑错误模拟
- `chaos-image-error.yaml`: 镜像错误模拟
- `chaos-test.sh`: 故障模拟脚本

## 相关资源

- [Chaos Mesh 官方文档](https://chaos-mesh.org/docs/)
- [Kubernetes 故障注入](https://kubernetes.io/docs/concepts/workloads/pods/disruptions/)
- [微服务故障模式](https://microservices.io/patterns/reliability/) 