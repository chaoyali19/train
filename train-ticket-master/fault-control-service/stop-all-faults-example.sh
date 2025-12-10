#!/bin/bash

# 停止所有 Chaos Mesh 故障的简单示例
# 使用方法: ./stop-all-faults-example.sh

echo "=== 停止所有 Chaos Mesh 故障示例 ==="

# 配置
API_URL="http://localhost:8090"

echo "1. 检查当前活跃故障..."
ACTIVE_FAULTS=$(curl -s "$API_URL/api/chaos/active")
echo "活跃故障: $ACTIVE_FAULTS"

echo ""
echo "2. 停止所有故障..."
STOP_RESPONSE=$(curl -s -X POST "$API_URL/api/chaos/stop-all" \
    -H "Content-Type: application/json")
echo "停止响应: $STOP_RESPONSE"

echo ""
echo "3. 再次检查活跃故障..."
AFTER_STOP=$(curl -s "$API_URL/api/chaos/active")
echo "停止后活跃故障: $AFTER_STOP"

echo ""
echo "4. 检查 Kubernetes 中的 NetworkChaos 资源..."
K8S_RESOURCES=$(kubectl get networkchaos -n chaos 2>/dev/null || echo "没有找到 NetworkChaos 资源")
echo "Kubernetes 资源: $K8S_RESOURCES"

echo ""
echo "=== 操作完成 ===" 