#!/bin/bash

echo "=== 测试基于拓扑关系的上游服务延迟功能 ==="

# 测试1: 获取拓扑信息
echo "1. 获取拓扑信息..."
curl -s http://localhost:8080/api/topology/user-login | jq '.nodes[] | select(.name != "browser") | .name' | head -5

# 测试2: 应用网络延迟故障（延迟上游流量）
echo -e "\n2. 应用网络延迟故障（延迟上游流量）..."
curl -X POST http://localhost:8080/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-user-service",
    "faultType": "network-delay",
    "delaySeconds": 2,
    "durationMinutes": 5,
    "targetService": "ts-user-service",
    "sourceService": "ts-auth-service",
    "delayUpstream": true
  }' | jq '.'

# 等待2秒
sleep 2

# 测试3: 查看活跃故障
echo -e "\n3. 查看活跃故障..."
curl -s http://localhost:8080/api/chaos/active | jq '.'

# 测试4: 应用网络延迟故障（延迟所有流量）
echo -e "\n4. 应用网络延迟故障（延迟所有流量）..."
curl -X POST http://localhost:8080/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-order-service",
    "faultType": "network-delay",
    "delaySeconds": 1,
    "durationMinutes": 3,
    "delayUpstream": false
  }' | jq '.'

# 等待2秒
sleep 2

# 测试5: 再次查看活跃故障
echo -e "\n5. 再次查看活跃故障..."
curl -s http://localhost:8080/api/chaos/active | jq '.'

# 测试6: 停止第一个故障
echo -e "\n6. 停止第一个故障..."
ACTIVE_FAULTS=$(curl -s http://localhost:8080/api/chaos/active)
FIRST_CHAOS_NAME=$(echo "$ACTIVE_FAULTS" | jq -r '.[0].chaosName // empty')

if [ ! -z "$FIRST_CHAOS_NAME" ]; then
    echo "停止故障: $FIRST_CHAOS_NAME"
    curl -X POST http://localhost:8080/api/chaos/stop \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "chaosName=$FIRST_CHAOS_NAME" | jq '.'
else
    echo "没有找到活跃故障"
fi

echo -e "\n=== 测试完成 ===" 