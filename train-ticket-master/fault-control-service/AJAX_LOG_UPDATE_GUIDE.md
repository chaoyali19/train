# AJAX实时日志更新功能说明

## 功能概述

压测控制中心现已支持使用AJAX技术实现实时日志更新，无需刷新页面即可看到压测任务的实时输出和状态变化。

## 主要特性

### 1. 自动实时更新
- **日志更新频率**: 每1秒自动更新一次日志内容
- **状态更新频率**: 每1秒自动更新一次任务状态
- **无需手动刷新**: 页面会自动获取最新数据并更新显示

### 2. 智能日志管理
- **自动滚动**: 日志内容会自动滚动到底部，显示最新输出
- **格式保持**: 保持日志的原始格式和换行
- **等待提示**: 当暂无日志时显示"等待输出..."提示

### 3. 任务生命周期管理
- **启动时**: 自动开始日志和状态轮询
- **运行中**: 持续更新日志和状态
- **停止时**: 自动停止轮询，清理资源

## 技术实现

### 前端实现
```javascript
// 日志轮询间隔
const LOG_INTERVAL = 1000; // 1s

// 状态轮询间隔  
const STATUS_INTERVAL = 1000; // 1s

// 实时日志更新函数
function updateTaskLogs() {
    if (!currentTaskId) return;
    
    fetch(`/stress/status?taskId=${currentTaskId}`)
    .then(response => response.json())
    .then(result => {
        if (result.success && result.output) {
            const logContainer = document.querySelector(`[data-task-id="${currentTaskId}"] .output-log`);
            if (logContainer) {
                logContainer.textContent = result.output;
                // 自动滚动到底部
                logContainer.scrollTop = logContainer.scrollHeight;
            }
        }
    });
}

// 开始日志轮询
function startLogPolling() {
    logInterval = setInterval(updateTaskLogs, LOG_INTERVAL);
}
```

### 后端支持
- 现有的`/stress/status`接口已支持按任务ID查询
- 返回包含实时日志输出的JSON响应
- 支持多任务并发监控

## 使用方法

### 1. 启动压测任务
1. 访问 `http://localhost:8090/stress`
2. 选择压测场景和参数
3. 点击"启动压测"按钮

### 2. 监控实时日志
- 任务启动后，日志区域会自动开始更新
- 每1秒获取一次最新日志
- 日志会自动滚动显示最新内容

### 3. 查看任务状态
- 任务状态每1秒自动更新
- 包括运行时间、状态变化等
- 支持实时停止任务

## 性能优化

### 1. 轮询控制
- 只在有活动任务时进行轮询
- 任务停止后自动停止轮询
- 避免无意义的网络请求

### 2. 资源管理
- 使用`clearInterval`清理定时器
- 防止内存泄漏
- 优化网络带宽使用

### 3. 错误处理
- 网络请求失败时不会影响页面
- 自动重试机制
- 友好的错误提示

## 浏览器兼容性

- **现代浏览器**: Chrome, Firefox, Safari, Edge
- **移动端**: 支持响应式设计
- **JavaScript**: ES6+语法，需要现代浏览器支持

## 测试验证

### 手动测试
1. 启动控制服务: `./deploy.sh`
2. 访问压测页面: `http://localhost:8090/stress`
3. 启动一个压测任务
4. 观察日志是否每1秒自动更新

### 自动化测试
```bash
# 运行AJAX日志更新测试
./test-ajax-logs.sh
```

## 故障排除

### 1. 日志不更新
- 检查浏览器控制台是否有错误
- 确认网络连接正常
- 验证后端服务状态

### 2. 页面卡顿
- 检查是否有大量日志输出
- 考虑调整轮询频率
- 检查浏览器性能

### 3. 内存使用过高
- 检查定时器是否正确清理
- 监控浏览器内存使用
- 必要时刷新页面

## 未来改进

### 1. WebSocket支持
- 考虑使用WebSocket替代轮询
- 实现真正的实时通信
- 减少网络开销

### 2. 日志过滤
- 添加日志级别过滤
- 支持关键词搜索
- 提供日志导出功能

### 3. 性能监控
- 添加网络请求统计
- 监控页面性能指标
- 提供性能优化建议

## 总结

AJAX实时日志更新功能提供了良好的用户体验，让用户能够实时监控压测任务的执行情况，无需手动刷新页面。该功能通过智能的轮询机制和资源管理，确保了系统的稳定性和性能。 