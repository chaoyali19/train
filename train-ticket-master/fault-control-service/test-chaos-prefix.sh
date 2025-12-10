#!/bin/bash

# 测试 chaos_ 前缀处理功能

echo "=== 测试 chaos_ 前缀处理功能 ==="

# 测试服务名称处理
echo ""
echo "=== 测试服务名称处理 ==="

# 测试带有 chaos_ 前缀的服务
echo "测试带有 chaos_ 前缀的服务..."
curl -X POST http://localhost:8090/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "chaos_ts-user-service",
    "faultType": "network-delay",
    "delaySeconds": 2,
    "durationMinutes": 1
  }'

echo ""
echo ""

# 等待一下
sleep 2

# 测试不带 chaos_ 前缀的服务
echo "测试不带 chaos_ 前缀的服务..."
curl -X POST http://localhost:8090/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-user-service",
    "faultType": "network-delay",
    "delaySeconds": 2,
    "durationMinutes": 1
  }'

echo ""
echo ""

# 获取活跃故障列表
echo "获取活跃故障列表..."
curl http://localhost:8090/api/chaos/active

echo ""
echo ""

# 等待故障生效
echo "等待 5 秒让故障生效..."
sleep 5

# 再次获取活跃故障列表
echo "再次获取活跃故障列表..."
curl http://localhost:8090/api/chaos/active

echo ""
echo "=== 测试完成 ==="
echo ""
echo "注意: 检查生成的故障名称是否去掉了 chaos_ 前缀" 