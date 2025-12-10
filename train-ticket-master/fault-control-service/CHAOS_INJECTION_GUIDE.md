# Chaos Mesh 故障注入功能使用指南

## 功能概述

本功能在拓扑页面上集成了 Chaos Mesh 故障注入功能，允许用户通过图形界面对服务进行网络故障注入。

## 主要特性

### 1. 图形化故障注入
- **点击节点**: 点击拓扑图上的服务节点即可打开故障注入弹窗
- **故障类型选择**: 目前支持网络故障延时
- **参数配置**: 可设置延迟时间和持续时间
- **一键应用**: 点击应用故障按钮即可注入故障

### 2. 故障管理
- **活跃故障列表**: 显示当前所有活跃的故障注入
- **故障停止**: 可以手动停止正在运行的故障
- **状态刷新**: 实时刷新故障状态

### 3. 简化的节点显示
- **只显示服务名称**: 节点卡片只显示服务名称，去掉了响应时间、吞吐率等指标
- **故障状态指示**: 有故障的节点会显示特殊样式

## 使用方法

### 1. 访问拓扑页面
```
http://localhost:8090/topology
```

### 2. 注入故障
1. 点击拓扑图上的任意服务节点
2. 在弹出的模态框中选择"网络故障延时"
3. 设置延迟时间（秒）和持续时间（分钟）
4. 点击"应用故障"按钮

### 3. 管理故障
1. 在页面下方的"活跃故障列表"中查看当前故障
2. 点击"停止故障"按钮可以手动停止故障
3. 点击"刷新故障状态"按钮更新故障列表

## API 接口

### 应用故障
```
POST /api/chaos/apply
Content-Type: application/json

{
  "serviceName": "ts-user-service",
  "faultType": "network-delay",
  "delaySeconds": 5,
  "durationMinutes": 2
}
```

### 停止故障
```
POST /api/chaos/stop
Content-Type: application/x-www-form-urlencoded

chaosName=network-delay-ts-user-service-1234567890
```

### 获取活跃故障
```
GET /api/chaos/active
```

## 技术实现

### 后端实现
- **ChaosMeshService**: 负责与 Chaos Mesh 交互
- **YAML 生成**: 动态生成 chaos-mesh 的 YAML 配置
- **kubectl 调用**: 通过 kubectl 命令应用和删除故障
- **故障跟踪**: 维护活跃故障的列表

### 前端实现
- **模态框交互**: 使用原生 JavaScript 实现弹窗
- **AJAX 请求**: 通过 fetch API 与后端通信
- **状态管理**: 实时更新故障状态和列表

## 故障类型

### 网络故障延时 (network-delay)
- **延迟时间**: 1-60 秒
- **持续时间**: 1-60 分钟
- **影响范围**: 指定服务的网络通信延迟

## 服务名称处理

### chaos_ 前缀自动处理
系统会自动处理服务名称中的 `chaos_` 前缀：

- **输入**: `chaos_ts-user-service`
- **处理后**: `ts-user-service`
- **生成的故障名称**: `network-delay-ts-user-service-1234567890`

### 处理逻辑
```java
// 去掉 chaos_ 前缀
String cleanServiceName = serviceName.startsWith("chaos_") ? 
    serviceName.substring(6) : serviceName;
```

### 示例
| 输入服务名称 | 处理后服务名称 | 生成的故障名称 |
|-------------|---------------|---------------|
| `chaos_ts-user-service` | `ts-user-service` | `network-delay-ts-user-service-1234567890` |
| `ts-user-service` | `ts-user-service` | `network-delay-ts-user-service-1234567890` |
| `chaos_ts-payment-service` | `ts-payment-service` | `network-delay-ts-payment-service-1234567890` |

## 注意事项

1. **Chaos Mesh 安装**: 需要先安装 Chaos Mesh
2. **kubectl 权限**: 需要 kubectl 访问权限
3. **服务标签**: 服务需要有正确的 app 标签
4. **故障自动清理**: 故障会在指定时间后自动停止
5. **手动停止**: 可以随时手动停止故障
6. **服务名称处理**: 如果服务名称以 `chaos_` 开头，系统会自动去掉这个前缀

## 安装要求

### Chaos Mesh 安装
```bash
# 添加 Helm 仓库
helm repo add chaos-mesh https://charts.chaos-mesh.org

# 安装 Chaos Mesh
helm install chaos-mesh chaos-mesh/chaos-mesh --namespace chaos-mesh --create-namespace
```

### 验证安装
```bash
# 检查 CRD 是否安装
kubectl get crd networkchaos.chaos-mesh.org

# 检查 Chaos Mesh 组件
kubectl get pods -n chaos-mesh
```

## 故障排除

### 常见问题

1. **故障注入失败**
   - 检查 Chaos Mesh 是否正确安装
   - 检查 kubectl 权限
   - 检查服务标签是否正确

2. **故障不生效**
   - 检查服务是否在正确的命名空间
   - 检查网络策略是否阻止故障注入
   - 查看 Chaos Mesh 日志

3. **无法停止故障**
   - 检查故障名称是否正确
   - 检查 kubectl 权限
   - 手动使用 kubectl 删除

### 调试命令
```bash
# 查看 Chaos Mesh 日志
kubectl logs -n chaos-mesh -l app.kubernetes.io/name=chaos-mesh

# 查看网络故障
kubectl get networkchaos -A

# 查看故障详情
kubectl describe networkchaos <chaos-name> -n default
```

## 扩展功能

### 添加新的故障类型
1. 在 `ChaosMeshService` 中添加新的故障方法
2. 在前端模态框中添加新的故障类型选项
3. 在控制器中添加新的故障类型处理逻辑

### 故障模板
可以基于现有的 YAML 模板扩展更多故障类型：
- 网络分区 (partition)
- 网络丢包 (loss)
- 网络带宽限制 (bandwidth)
- 容器故障 (pod-failure)
- 节点故障 (node-failure) 