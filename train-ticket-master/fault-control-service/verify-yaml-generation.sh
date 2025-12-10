#!/bin/bash

echo "=== 验证 YAML 生成是否正确 ==="

# 测试1: 验证延迟上游流量的 YAML 生成
echo "1. 测试延迟上游流量的 YAML 生成..."
curl -X POST http://localhost:8080/api/chaos/apply \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ts-train-food-service",
    "faultType": "network-delay",
    "delaySeconds": 2,
    "durationMinutes": 5,
    "targetService": "ts-train-food-service",
    "sourceService": "ts-food-service",
    "delayUpstream": true
  }' | jq '.'

echo -e "\n2. 查看生成的 YAML 文件..."
ls -la /tmp/network-delay-ts-train-food-service-*.yaml 2>/dev/null | head -1

if [ -f /tmp/network-delay-ts-train-food-service-*.yaml ]; then
    echo -e "\n3. YAML 文件内容:"
    cat /tmp/network-delay-ts-train-food-service-*.yaml
else
    echo "没有找到生成的 YAML 文件"
fi

echo -e "\n=== 验证完成 ===" 