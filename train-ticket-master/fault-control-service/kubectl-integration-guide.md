# Kubectl 集成使用指南

## 概述

本项目已恢复到使用 `kubectl` 命令的方式来实现 Chaos Mesh 故障注入功能。这种方式更加稳定可靠，避免了 Kubernetes Java Client 的 SSL/TLS 兼容性问题。

## 主要特性

### 1. 自动 kubeconfig 加载
- 自动从 `~/.kube/config` 加载集群配置
- 支持多集群环境
- 无需额外的 Java 客户端配置

### 2. 故障注入功能
- 网络延迟故障注入
- 支持方向性延迟（指定源服务和目标服务）
- 支持普通延迟（对服务自身的延迟）
- 自动故障清理和状态管理

### 3. 错误处理
- 详细的错误日志
- 优雅的异常处理
- 自动重试机制

## 环境要求

### 1. 系统要求
- Linux/Unix 系统
- Java 8+
- kubectl 命令行工具
- 可访问的 Kubernetes 集群

### 2. 集群要求
- Kubernetes 1.16+
- Chaos Mesh 已安装
- chaos 命名空间存在

## 安装和配置

### 1. 安装 kubectl
```bash
# 下载 kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

# 安装
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# 验证安装
kubectl version --client
```

### 2. 配置 kubeconfig
```bash
# 复制 kubeconfig 文件到用户目录
cp your-kubeconfig ~/.kube/config

# 设置权限
chmod 600 ~/.kube/config

# 测试连接
kubectl cluster-info
```

### 3. 创建 chaos 命名空间
```bash
kubectl create namespace chaos
```

## 使用方法

### 1. 启动应用
```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 2. 测试 kubectl 集成
```bash
# 运行测试脚本
./test-kubectl-integration.sh
```

### 3. API 接口

#### 应用故障注入
```bash
curl -X POST http://localhost:8090/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-config-service",
    "delaySeconds": 1,
    "durationMinutes": 15,
    "targetService": "ts-config-service",
    "sourceService": "ts-config-service",
    "delayUpstream": false
  }'
```

#### 停止故障注入
```bash
curl -X POST http://localhost:8090/api/chaos/stop \
  -H "Content-Type: application/json" \
  -d '{
    "chaosName": "network-delay-ts-config-service-1234567890"
  }'
```

#### 停止所有故障注入
```bash
curl -X POST http://localhost:8090/api/chaos/stop-all \
  -H "Content-Type: application/json"
```

#### 获取活跃故障列表
```bash
curl http://localhost:8090/api/chaos/active
```

## 故障排查

### 1. 常见问题

#### kubectl 命令失败
```bash
# 检查 kubectl 是否可用
kubectl version

# 检查集群连接
kubectl cluster-info

# 检查权限
kubectl auth can-i create networkchaos --namespace chaos
```

#### Chaos Mesh 未安装
```bash
# 检查 Chaos Mesh 组件
kubectl get pods -n chaos-mesh

# 安装 Chaos Mesh（如果未安装）
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm repo update
helm install chaos-mesh chaos-mesh/chaos-mesh --namespace chaos-mesh --create-namespace
```

#### 命名空间不存在
```bash
# 创建 chaos 命名空间
kubectl create namespace chaos
```

### 2. 日志查看
```bash
# 查看应用日志
tail -f logs/application.log

# 查看 kubectl 命令输出
kubectl logs -f deployment/fault-control-service -n chaos
```

### 3. 调试命令
```bash
# 查看 NetworkChaos 资源
kubectl get networkchaos -n chaos

# 查看资源详情
kubectl describe networkchaos <chaos-name> -n chaos

# 查看事件
kubectl get events -n chaos --sort-by='.lastTimestamp'
```

## 性能优化

### 1. 连接池配置
```bash
# 设置 kubectl 超时
export KUBECTL_TIMEOUT=30s
```

### 2. 资源限制
```bash
# 设置内存限制
export JAVA_OPTS="-Xmx512m -Xms256m"
```

## 安全考虑

### 1. 权限管理
- 使用最小权限原则
- 定期轮换 kubeconfig 证书
- 监控异常访问

### 2. 网络安全
- 使用 TLS 加密通信
- 限制网络访问范围
- 定期更新证书

## 监控和告警

### 1. 指标监控
- 故障注入成功率
- 命令执行时间
- 错误率统计

### 2. 告警规则
- 故障注入失败告警
- 集群连接异常告警
- 资源使用率告警

## 总结

使用 kubectl 命令的方式具有以下优势：

1. **稳定性高**: 避免了 Java 客户端的 SSL/TLS 兼容性问题
2. **兼容性好**: 支持各种 Kubernetes 版本和配置
3. **调试方便**: 可以直接使用 kubectl 命令调试
4. **维护简单**: 无需管理复杂的 Java 客户端依赖

这种方式特别适合生产环境，提供了稳定可靠的故障注入能力。 