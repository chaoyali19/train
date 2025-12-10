# 压测功能实现总结

## 概述

成功在故障控制平台中集成了压测功能，实现了通过Web界面触发Python压测程序进行压力测试的完整解决方案。

## 实现的功能

### 1. 后端服务层

#### StressTestService.java
- **功能**: 压测任务管理服务
- **特性**:
  - 异步执行Python压测程序
  - 实时监控任务状态和输出日志
  - 支持启动、停止和查询任务
  - 任务生命周期管理

#### FaultControlController.java
- **新增接口**:
  - `GET /stress` - 压测控制页面
  - `POST /stress/start` - 启动压测任务
  - `POST /stress/stop` - 停止压测任务
  - `GET /stress/status` - 获取任务状态
  - `GET /stress/scenarios` - 获取可用场景

### 2. 前端界面层

#### stress.html
- **功能**: 压测控制页面
- **特性**:
  - 现代化的Bootstrap界面
  - 压测参数配置表单
  - 实时任务监控面板
  - 任务状态可视化显示
  - 输出日志实时查看

#### index.html
- **更新**: 添加导航栏，链接到压测页面

### 3. 配置管理

#### application.yml
- **新增配置**:
  ```yaml
  stress:
    test:
      python:
        path: "../train-ticket-auto-query"
      venv:
        path: "../train-ticket-auto-query/.venv/bin/python"
  ```

## 支持的压测场景

| 场景 | 描述 | 建议参数 |
|------|------|----------|
| `high_speed` | 高铁票查询 | 并发10-50，请求数100-1000 |
| `normal` | 普通列车票查询 | 并发10-50，请求数100-1000 |
| `food` | 食品查询 | 并发5-20，请求数50-500 |
| `parallel` | 并行车票查询 | 并发20-100，请求数200-2000 |
| `pay` | 查询并支付订单 | 并发5-20，请求数50-500 |
| `cancel` | 查询并取消订单 | 并发5-20，请求数50-500 |
| `consign` | 查询并添加托运信息 | 并发5-20，请求数50-500 |

## 任务状态管理

### 状态类型
- **starting**: 任务正在初始化
- **running**: 任务正在执行压测
- **completed**: 任务成功完成
- **failed**: 任务执行失败
- **stopped**: 任务被手动停止

### 监控信息
- 任务ID、场景、状态
- 并发数、总请求数、运行时间
- 实时输出日志和错误信息
- 任务控制操作（停止）

## API接口设计

### 1. 启动压测任务
```http
POST /stress/start
Content-Type: application/x-www-form-urlencoded

scenario=high_speed&concurrent=10&count=100
```

**响应**:
```json
{
  "success": true,
  "message": "压测任务已启动",
  "taskId": "task_1234567890_123"
}
```

### 2. 停止压测任务
```http
POST /stress/stop
Content-Type: application/x-www-form-urlencoded

taskId=task_1234567890_123
```

### 3. 获取任务状态
```http
GET /stress/status
GET /stress/status?taskId=task_1234567890_123
```

### 4. 获取可用场景
```http
GET /stress/scenarios
```

## 技术实现细节

### 1. 异步任务执行
- 使用 `CompletableFuture` 实现异步执行
- 进程管理和输出流读取
- 任务状态实时更新

### 2. 进程管理
- 使用 `ProcessBuilder` 启动Python程序
- 实时读取进程输出
- 优雅的进程终止机制

### 3. 前端交互
- AJAX异步请求
- 实时状态轮询（每1秒）
- 实时日志更新（每1秒）
- 动态UI更新
- 自动滚动日志显示

### 4. 错误处理
- 完善的异常捕获和处理
- 用户友好的错误提示
- 详细的日志记录

## 文件结构

```
fault-control-service/
├── src/main/java/fdse/microservice/
│   ├── controller/
│   │   └── FaultControlController.java (更新)
│   └── service/
│       └── StressTestService.java (新增)
├── src/main/resources/
│   ├── application.yml (更新)
│   └── templates/
│       ├── index.html (更新)
│       └── stress.html (新增)
├── test-stress.sh (新增)
├── demo-stress.sh (新增)
├── test-ajax-logs.sh (新增)
├── STRESS_TEST_GUIDE.md (新增)
├── AJAX_LOG_UPDATE_GUIDE.md (新增)
└── STRESS_FEATURE_SUMMARY.md (本文档)
```

## 使用流程

### 1. 启动控制平台
```bash
cd fault-control-service
mvn spring-boot:run
```

### 2. 访问压测页面
```
http://localhost:8080/stress
```

### 3. 配置压测参数
- 选择压测场景
- 设置并发数和总请求数
- 点击"启动压测"

### 4. 监控任务状态
- 实时查看任务运行状态
- 查看输出日志
- 必要时停止任务

## 测试和验证

### 1. 功能测试
```bash
./test-stress.sh
```

### 2. 演示脚本
```bash
./demo-stress.sh
```

### 3. AJAX日志更新测试
```bash
./test-ajax-logs.sh
```

### 4. 手动测试
- 访问Web界面进行交互测试
- 验证各种压测场景
- 测试任务管理功能
- 验证实时日志更新功能

## 配置要求

### 1. Python环境
- Python 3.8+
- uv包管理器
- 压测程序依赖安装

### 2. 系统要求
- Java 8+
- Maven 3.6+
- 网络连接到Train-Ticket系统

### 3. 路径配置
- 确保 `application.yml` 中的Python路径正确
- 验证Python虚拟环境可访问

## 优势特点

### 1. 用户友好
- 图形化界面操作
- 无需命令行知识
- 直观的参数配置

### 2. 功能完整
- 支持多种压测场景
- 实时监控和日志查看
- 任务生命周期管理
- AJAX实时日志更新

### 3. 技术先进
- 异步任务执行
- 实时状态更新
- AJAX实时日志轮询
- 完善的错误处理

### 4. 易于维护
- 模块化设计
- 清晰的代码结构
- 完善的文档

## 后续扩展

### 1. 功能增强
- 压测结果统计和分析
- 历史任务记录
- 压测报告生成

### 2. 性能优化
- 任务队列管理
- 资源使用监控
- 并发任务限制

### 3. 用户体验
- 更丰富的可视化图表
- 压测进度条显示
- 移动端适配

## 总结

成功实现了在控制平台页面上触发压测服务的完整功能，包括：

1. ✅ **后端服务**: 压测任务管理和Python程序调用
2. ✅ **前端界面**: 现代化的Web控制界面
3. ✅ **API接口**: 完整的RESTful API设计
4. ✅ **配置管理**: 灵活的配置选项
5. ✅ **文档支持**: 详细的使用指南和测试脚本
6. ✅ **错误处理**: 完善的异常处理和用户提示

该功能为Train-Ticket系统提供了便捷的压测管理工具，大大简化了压力测试的操作流程。 