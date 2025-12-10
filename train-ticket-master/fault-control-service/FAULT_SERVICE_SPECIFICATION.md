# 故障服务规范

## 概述

本文档定义了故障控制服务与各个业务服务之间的接口规范，用于实现自动化的故障发现和管理。

## 服务发现规范

### 1. 服务注册接口

每个支持故障注入的服务必须提供以下接口：

#### 1.1 获取服务信息
```http
GET /fault/info
```

响应格式：
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
      "type": "boolean",
      "defaultDelay": null
    },
    {
      "id": "station-query-delay",
      "name": "延迟故障",
      "description": "站点查询接口响应延迟",
      "type": "delay",
      "defaultDelay": 10000
    }
  ]
}
```

#### 1.2 获取故障状态
```http
GET /fault/status
```

响应格式：
```json
{
  "serviceId": "station-service",
  "serviceName": "站点服务",
  "status": "正常",
  "faults": [
    {
      "id": "empty-station-query",
      "enabled": false,
      "delayMs": null
    },
    {
      "id": "station-query-delay",
      "enabled": true,
      "delayMs": 15000
    }
  ],
  "timestamp": 1640995200000
}
```

#### 1.3 控制故障
```http
POST /fault/control
Content-Type: application/json

{
  "faultId": "empty-station-query",
  "enable": true,
  "delayMs": 5000
}
```

响应格式：
```json
{
  "success": true,
  "message": "Empty station query fault enabled",
  "faultId": "empty-station-query",
  "enabled": true
}
```

## 故障类型定义

### 1. boolean类型
- 简单的开关型故障
- 只有启用/禁用两种状态
- 不需要额外参数

### 2. delay类型
- 延迟型故障
- 需要指定延迟时间（毫秒）
- 参数：delayMs

### 3. error类型
- 错误型故障
- 返回指定的HTTP错误码
- 参数：errorCode

### 4. random类型
- 随机型故障
- 按概率触发故障
- 参数：probability（0-1之间的小数）

## 服务命名规范

### 1. 服务ID命名
- 使用kebab-case命名法
- 格式：`{service-name}-service`
- 示例：`station-service`, `user-service`, `order-service`

### 2. 端口规范
- 每个服务使用固定端口：8080
- 服务访问地址：`http://{service-id}:8080`

## 实现示例

### Spring Boot服务实现

```java
@RestController
@RequestMapping("/fault")
public class FaultController {
    
    @GetMapping("/info")
    public FaultServiceInfo getServiceInfo() {
        return FaultServiceInfo.builder()
            .serviceName("站点服务")
            .serviceId("station-service")
            .description("站点查询服务")
            .version("1.0.0")
            .faults(Arrays.asList(
                FaultInfo.builder()
                    .id("empty-station-query")
                    .name("空数据故障")
                    .description("站点查询接口返回空数据")
                    .type("boolean")
                    .build(),
                FaultInfo.builder()
                    .id("station-query-delay")
                    .name("延迟故障")
                    .description("站点查询接口响应延迟")
                    .type("delay")
                    .defaultDelay(10000L)
                    .build()
            ))
            .build();
    }
    
    @GetMapping("/status")
    public FaultStatus getFaultStatus() {
        // 返回当前故障状态
    }
    
    @PostMapping("/control")
    public FaultControlResponse controlFault(@RequestBody FaultControlRequest request) {
        // 控制故障开关
    }
}
```

## 故障控制服务配置

### 简化的配置文件
```yaml
fault:
  discovery:
    enabled: true
    scan-interval: 30000  # 扫描间隔（毫秒）
  services:
    - id: "station-service"
      name: "站点服务"
    - id: "user-service" 
      name: "用户服务"
    - id: "order-service"
      name: "订单服务"
```

## 错误处理

### 1. 服务不可达
- 返回HTTP 503状态码
- 在故障控制界面显示"离线"状态

### 2. 接口不存在
- 返回HTTP 404状态码
- 跳过该服务的故障管理

### 3. 接口错误
- 返回HTTP 500状态码
- 记录错误日志，显示错误信息

## 安全考虑

### 1. 访问控制
- 建议在生产环境中添加认证机制
- 使用API密钥或JWT令牌

### 2. 网络隔离
- 故障控制接口仅在内部网络可访问
- 不对外暴露故障控制功能

## 监控和日志

### 1. 服务发现日志
- 记录服务发现过程
- 记录故障状态变化

### 2. 操作审计
- 记录所有故障控制操作
- 记录操作人和时间戳

## 扩展性

### 1. 新增故障类型
- 在规范中添加新的故障类型定义
- 更新故障控制界面

### 2. 新增服务
- 只需在配置文件中添加服务ID
- 故障控制服务自动发现故障信息 