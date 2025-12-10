#!/bin/bash

# Chaos Mesh 故障注入测试脚本

echo "=== Chaos Mesh 故障注入测试 ==="

# 检查 kubectl 是否可用
if ! command -v kubectl &> /dev/null; then
    echo "错误: kubectl 未安装或不在 PATH 中"
    exit 1
fi

# 检查 chaos-mesh 是否已安装
if ! kubectl get crd networkchaos.chaos-mesh.org &> /dev/null; then
    echo "错误: Chaos Mesh 未安装"
    echo "请先安装 Chaos Mesh:"
    echo "helm repo add chaos-mesh https://charts.chaos-mesh.org"
    echo "helm install chaos-mesh chaos-mesh/chaos-mesh --namespace chaos-mesh --create-namespace"
    exit 1
fi

echo "✓ Chaos Mesh 已安装"

# 测试故障注入 API
echo ""
echo "=== 测试故障注入 API ==="

# 测试应用网络延迟故障
echo "测试应用网络延迟故障..."
curl -X POST http://localhost:8090/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-user-service",
    "faultType": "network-delay",
    "delaySeconds": 3,
    "durationMinutes": 2
  }'

echo ""
echo ""

# 等待一下
sleep 2

# 获取活跃故障列表
echo "获取活跃故障列表..."
curl http://localhost:8090/api/chaos/active

echo ""
echo ""

# 等待故障生效
echo "等待 10 秒让故障生效..."
sleep 10

# 再次获取活跃故障列表
echo "再次获取活跃故障列表..."
curl http://localhost:8090/api/chaos/active

echo ""
echo "=== 测试完成 ==="
echo ""
echo "注意: 故障会在指定时间后自动停止，或者可以通过 API 手动停止" 