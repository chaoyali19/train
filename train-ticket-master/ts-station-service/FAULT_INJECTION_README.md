# ts-station-service 故障注入功能说明

## 概述

本服务实现了故障注入功能，用于模拟生产环境中的各种故障场景，帮助测试系统的容错能力和故障恢复机制。

## 故障类型

### 1. 空数据故障 (Empty Data Fault)
- **描述**: 站点查询接口 `/api/v1/stationservice/stations` 返回空数据
- **影响**: 所有依赖站点信息的服务无法正常工作
- **模拟场景**: 新版本上线后业务逻辑错误导致数据查询失败

### 2. 延迟故障 (Delay Fault)
- **描述**: 站点查询接口响应延迟
- **影响**: 调用方服务超时，影响用户体验
- **模拟场景**: 数据库查询缓慢或网络延迟

## 环境变量配置

### 故障开关环境变量

| 环境变量名 | 默认值 | 说明 |
|-----------|--------|------|
| `FAULT_EMPTY_STATION_QUERY` | `false` | 启用/禁用空数据故障 |
| `FAULT_STATION_QUERY_DELAY` | `false` | 启用/禁用延迟故障 |
| `FAULT_DELAY_TIME_MS` | `10000` | 延迟时间（毫秒） |

### 环境变量示例

```bash
# 启用空数据故障
export FAULT_EMPTY_STATION_QUERY=true

# 启用延迟故障，设置15秒延迟
export FAULT_STATION_QUERY_DELAY=true
export FAULT_DELAY_TIME_MS=15000

# 同时启用两种故障
export FAULT_EMPTY_STATION_QUERY=true
export FAULT_STATION_QUERY_DELAY=true
export FAULT_DELAY_TIME_MS=8000
```

## 部署方式

### 1. Docker 部署

#### 构建镜像
```bash
docker build -t ts-station-service:latest .
```

#### 运行容器（正常版本）
```bash
docker run -d -p 12345:12345 --name ts-station-service ts-station-service:latest
```

#### 运行容器（启用空数据故障）
```bash
docker run -d -p 12345:12345 \
  -e FAULT_EMPTY_STATION_QUERY=true \
  --name ts-station-service-fault \
  ts-station-service:latest
```

#### 运行容器（启用延迟故障）
```bash
docker run -d -p 12345:12345 \
  -e FAULT_STATION_QUERY_DELAY=true \
  -e FAULT_DELAY_TIME_MS=15000 \
  --name ts-station-service-delay \
  ts-station-service:latest
```

### 2. Kubernetes 部署

#### 使用部署脚本
```bash
# 构建镜像
./deploy-fault-scenarios.sh build

# 部署正常版本
./deploy-fault-scenarios.sh normal

# 部署空数据故障版本
./deploy-fault-scenarios.sh empty-fault

# 部署延迟故障版本（15秒延迟）
./deploy-fault-scenarios.sh delay-fault 15000

# 查看部署状态
./deploy-fault-scenarios.sh status

# 清理所有部署
./deploy-fault-scenarios.sh cleanup
```

#### 手动部署
```bash
# 部署空数据故障版本
kubectl apply -f k8s-fault-deployment.yaml
```

## 运行时控制

### 故障控制接口

服务启动后，可以通过以下HTTP接口动态控制故障：

#### 1. 启用/禁用空数据故障
```bash
# 启用空数据故障
curl -X POST "http://localhost:12345/api/v1/stationservice/fault/enableEmptyStationQuery?enable=true"

# 禁用空数据故障
curl -X POST "http://localhost:12345/api/v1/stationservice/fault/enableEmptyStationQuery?enable=false"
```

#### 2. 启用/禁用延迟故障
```bash
# 启用延迟故障（默认10秒）
curl -X POST "http://localhost:12345/api/v1/stationservice/fault/enableStationQueryDelay?enable=true"

# 启用延迟故障（自定义延迟时间）
curl -X POST "http://localhost:12345/api/v1/stationservice/fault/enableStationQueryDelay?enable=true&delayMs=15000"

# 禁用延迟故障
curl -X POST "http://localhost:12345/api/v1/stationservice/fault/enableStationQueryDelay?enable=false"
```

#### 3. 查看故障状态
```bash
curl "http://localhost:12345/api/v1/stationservice/fault/status"
```

### 测试脚本

使用提供的测试脚本进行自动化测试：

```bash
# 修改脚本中的服务地址
SERVICE_URL="http://your-service-address:12345"

# 运行测试
./fault-test.sh
```

## 故障场景示例

### 场景1: 新版本上线故障
```bash
# 1. 部署启用空数据故障的版本
./deploy-fault-scenarios.sh empty-fault

# 2. 验证故障效果
curl "http://localhost:12345/api/v1/stationservice/stations"

# 3. 通过接口动态修复
curl -X POST "http://localhost:12345/api/v1/stationservice/fault/enableEmptyStationQuery?enable=false"

# 4. 验证修复效果
curl "http://localhost:12345/api/v1/stationservice/stations"
```

### 场景2: 性能问题模拟
```bash
# 1. 部署启用延迟故障的版本
./deploy-fault-scenarios.sh delay-fault 15000

# 2. 测试接口响应时间
time curl "http://localhost:12345/api/v1/stationservice/stations"

# 3. 动态调整延迟时间
curl -X POST "http://localhost:12345/api/v1/stationservice/fault/enableStationQueryDelay?enable=true&delayMs=5000"
```

## 监控和日志

### 日志输出
故障注入会在日志中输出相关信息：

```
[FaultController] 环境变量启用空数据故障: FAULT_EMPTY_STATION_QUERY=true
[FaultController] 环境变量启用延迟故障: FAULT_STATION_QUERY_DELAY=true
[FaultController] 环境变量设置延迟时间: FAULT_DELAY_TIME_MS=15000ms
```

### 业务日志
在业务方法中会记录故障注入的详细信息：

```
[query][Query stations][Fault injection: return empty list]
[query][Query stations][Fault injection: delay 15000ms]
```

## 注意事项

1. **生产环境谨慎使用**: 故障注入功能仅用于测试环境，生产环境请确保所有故障开关为 `false`
2. **资源限制**: 延迟故障会增加系统资源消耗，请合理设置延迟时间
3. **依赖服务影响**: 故障会影响依赖此服务的其他服务，请提前通知相关团队
4. **监控告警**: 建议在测试环境中关闭相关监控告警，避免误报

## 故障排查

### 常见问题

1. **故障未生效**
   - 检查环境变量是否正确设置
   - 确认服务是否重启
   - 查看启动日志中的故障配置信息

2. **接口无响应**
   - 检查延迟时间是否设置过长
   - 确认服务是否正常运行
   - 查看服务日志

3. **依赖服务异常**
   - 检查调用方服务的超时配置
   - 确认故障影响范围
   - 及时关闭故障开关

### 日志查看
```bash
# 查看服务日志
kubectl logs -f deployment/ts-station-service-fault

# 查看特定故障相关的日志
kubectl logs deployment/ts-station-service-fault | grep "Fault injection"
``` 