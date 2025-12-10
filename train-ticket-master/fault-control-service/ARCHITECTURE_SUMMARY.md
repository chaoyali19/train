# 故障控制服务架构总结

## 概述

基于您的需求，我重新设计了一个自动发现的故障控制服务架构。新架构不再需要在配置文件中硬编码每个故障，而是通过标准化的接口自动发现各个服务的故障信息。

## 架构设计

### 1. 自动发现机制

#### 服务发现流程
1. 故障控制服务启动时，读取配置的服务列表
2. 定时扫描每个服务（默认30秒间隔）
3. 通过标准接口获取服务信息和故障状态
4. 缓存服务信息，提供实时状态更新

#### 服务地址规范
- 服务ID: `{service-name}-service`（如：`station-service`）
- 访问地址: `http://{service-id}:8080`
- 故障接口路径: `/fault/*`

### 2. 标准化接口规范

每个支持故障注入的服务必须实现以下三个接口：

#### 2.1 获取服务信息
```http
GET /fault/info
```

返回服务的基本信息和支持的故障列表：
```json
{
  "serviceName": "站点服务",
  "serviceId": "station-service",
  "description": "站点查询服务",
  "version": "1.0.0",
  "faults": [
    {
      "id": "empty-station-query",
      "name": "空数据故障",
      "description": "站点查询接口返回空数据",
      "type": "boolean"
    }
  ]
}
```

#### 2.2 获取故障状态
```http
GET /fault/status
```

返回当前故障状态：
```json
{
  "serviceId": "station-service",
  "serviceName": "站点服务",
  "status": "正常",
  "timestamp": 1640995200000,
  "reachable": true,
  "faults": [
    {
      "id": "empty-station-query",
      "enabled": false
    }
  ]
}
```

#### 2.3 控制故障
```http
POST /fault/control
Content-Type: application/json

{
  "faultId": "empty-station-query",
  "enable": true,
  "delayMs": 5000
}
```

### 3. 故障类型支持

#### 3.1 boolean类型
- 简单的开关型故障
- 只有启用/禁用两种状态
- 不需要额外参数

#### 3.2 delay类型
- 延迟型故障
- 需要指定延迟时间（毫秒）
- 参数：`delayMs`

#### 3.3 error类型
- 错误型故障
- 返回指定的HTTP错误码
- 参数：`errorCode`

#### 3.4 random类型
- 随机型故障
- 按概率触发故障
- 参数：`probability`（0-1之间的小数）

## 实现对比

### 旧架构（硬编码配置）
```yaml
fault:
  services:
    station-service:
      name: "站点服务"
      base-url: "http://localhost:12345"
      faults:
        - id: "empty-station-query"
          name: "空数据故障"
          endpoint: "/api/v1/stationservice/fault/enableEmptyStationQuery"
          type: "boolean"
```

### 新架构（自动发现）
```yaml
fault:
  discovery:
    enabled: true
    scan-interval: 30000
  services:
    - id: "station-service"
      name: "站点服务"
```

## 优势

### 1. 配置简化
- 只需配置服务ID和名称
- 无需手动维护每个故障的详细信息
- 新增故障时无需修改配置文件

### 2. 自动发现
- 动态发现服务支持的故障类型
- 实时监控服务状态
- 自动适配新的故障类型

### 3. 标准化
- 统一的接口规范
- 支持多种故障类型
- 便于其他服务集成

### 4. 扩展性
- 新增服务只需在配置中添加一行
- 新增故障类型只需更新规范
- 支持复杂的故障参数

## 部署和使用

### 1. 启动故障控制服务
```bash
cd fault-control-service
./deploy.sh run
```

### 2. 访问Web界面
```
http://localhost:8080/fault-control
```

### 3. 测试功能
```bash
./test-fault-discovery.sh
```

## 服务实现示例

### Station服务实现
已经为station服务实现了新的故障控制接口：

1. **新增模型类**：
   - `FaultServiceInfo.java` - 服务信息模型
   - `FaultInfo.java` - 故障信息模型
   - `FaultStatus.java` - 故障状态模型
   - `FaultControlRequest.java` - 控制请求模型
   - `FaultControlResponse.java` - 控制响应模型

2. **更新FaultController**：
   - 实现 `/fault/info` 接口
   - 实现 `/fault/status` 接口
   - 实现 `/fault/control` 接口
   - 保留旧接口以兼容现有代码

3. **支持的故障类型**：
   - 空数据故障（boolean）
   - 延迟故障（delay）
   - 500错误故障（error）
   - 随机500错误故障（random）
   - 响应结构错误故障（boolean）

## 后续扩展

### 1. 新增服务
只需在配置文件中添加服务ID，故障控制服务会自动发现该服务的故障信息。

### 2. 新增故障类型
在规范中定义新的故障类型，更新前端界面即可支持。

### 3. 监控和告警
可以基于故障状态实现监控告警功能。

### 4. 权限控制
可以添加认证和授权机制，控制故障操作的权限。

## 总结

新的故障控制服务架构实现了您的要求：

1. ✅ **配置简化**：不再需要硬编码每个故障
2. ✅ **自动发现**：通过标准接口自动获取故障信息
3. ✅ **服务规范**：定义了统一的故障服务规范
4. ✅ **地址规范**：使用 `{service-id}:8080` 的地址格式
5. ✅ **Web界面**：提供直观的故障控制界面
6. ✅ **API接口**：提供RESTful API接口

这个架构具有良好的扩展性和维护性，可以轻松支持更多的服务和故障类型。 