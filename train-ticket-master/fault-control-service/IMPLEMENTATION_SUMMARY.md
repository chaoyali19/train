# Chaos Mesh 故障注入功能实现总结

## 实现的功能

### 1. 拓扑页面故障注入
- ✅ 点击拓扑节点弹出故障注入模态框
- ✅ 下拉框选择故障类型（网络故障延时）
- ✅ 输入框设置延迟时间和持续时间
- ✅ 生成 chaos-mesh YAML 并调用 kubectl apply
- ✅ 故障应用后显示成功/失败消息

### 2. 故障管理功能
- ✅ 活跃故障列表显示
- ✅ 故障停止按钮和功能
- ✅ 实时刷新故障状态
- ✅ 故障停止后更新状态

### 3. 简化的节点显示
- ✅ 节点卡片只显示服务名称
- ✅ 去掉了响应时间、吞吐率等指标
- ✅ 保持节点点击功能

### 4. 服务名称处理
- ✅ 自动去掉 `chaos_` 前缀
- ✅ 支持带前缀和不带前缀的服务名称
- ✅ 在日志中显示原始名称和处理后的名称

## 新增的文件

### 后端文件
1. `ChaosMeshService.java` - Chaos Mesh 故障注入服务
2. `ChaosFaultRequest.java` - 故障注入请求模型
3. `ChaosFaultResponse.java` - 故障注入响应模型

### 前端修改
1. `topology.html` - 添加故障注入模态框和功能

### 文档和测试
1. `CHAOS_INJECTION_GUIDE.md` - 使用指南
2. `test-chaos-injection.sh` - 测试脚本
3. `test-chaos-prefix.sh` - chaos_ 前缀处理测试脚本
4. `IMPLEMENTATION_SUMMARY.md` - 实现总结

## API 接口

### 新增的 API 端点
- `POST /api/chaos/apply` - 应用故障
- `POST /api/chaos/stop` - 停止故障
- `GET /api/chaos/active` - 获取活跃故障

## 技术实现

### 后端实现
- **ChaosMeshService**: 负责与 Chaos Mesh 交互
- **YAML 生成**: 动态生成 chaos-mesh 配置
- **kubectl 调用**: 通过命令行工具应用故障
- **故障跟踪**: 维护活跃故障的内存映射

### 前端实现
- **模态框**: 原生 JavaScript 实现的弹窗
- **AJAX 请求**: 使用 fetch API 与后端通信
- **状态管理**: 实时更新故障状态和列表
- **用户体验**: 简化的节点显示和交互

## 使用方法

### 1. 访问拓扑页面
```
http://localhost:8090/topology
```

### 2. 注入故障
1. 点击任意服务节点
2. 选择"网络故障延时"
3. 设置延迟时间（1-60秒）
4. 设置持续时间（1-60分钟）
5. 点击"应用故障"

### 3. 管理故障
1. 查看页面下方的"活跃故障列表"
2. 点击"停止故障"按钮停止故障
3. 点击"刷新故障状态"更新列表

## 依赖要求

### 系统要求
- Kubernetes 集群
- kubectl 命令行工具
- Chaos Mesh 已安装

### Chaos Mesh 安装
```bash
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm install chaos-mesh chaos-mesh/chaos-mesh --namespace chaos-mesh --create-namespace
```

## 测试验证

### 运行测试脚本
```bash
cd fault-control-service
./test-chaos-injection.sh
```

### 手动测试
1. 启动故障控制服务
2. 访问拓扑页面
3. 点击服务节点进行故障注入
4. 验证故障是否生效
5. 测试故障停止功能

## 扩展性

### 添加新故障类型
1. 在 `ChaosMeshService` 中添加新方法
2. 在前端模态框中添加新选项
3. 在控制器中添加新故障类型处理

### 支持的故障类型
- 网络延迟 (network-delay) ✅
- 网络分区 (partition) - 待实现
- 网络丢包 (loss) - 待实现
- 容器故障 (pod-failure) - 待实现

## 注意事项

1. **权限要求**: 需要 kubectl 访问权限
2. **服务标签**: 服务需要有正确的 app 标签
3. **命名空间**: 默认使用 default 命名空间
4. **故障清理**: 故障会在指定时间后自动停止
5. **错误处理**: 包含完整的错误处理和日志记录

## 下一步改进

1. **更多故障类型**: 添加网络分区、丢包等故障
2. **故障模板**: 支持自定义故障模板
3. **批量操作**: 支持批量故障注入
4. **故障历史**: 记录和查看故障历史
5. **监控集成**: 与监控系统集成显示故障影响 