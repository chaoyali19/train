#!/bin/bash

# 测试故障控制AJAX实时刷新功能
echo "=== 测试故障控制AJAX实时刷新功能 ==="

# 检查服务是否运行
echo "1. 检查控制服务状态..."
if curl -s http://localhost:8090/health > /dev/null; then
    echo "✓ 控制服务运行正常"
else
    echo "✗ 控制服务未运行，请先启动服务"
    exit 1
fi

# 测试API状态接口
echo "2. 测试API状态接口..."
STATUS_RESPONSE=$(curl -s http://localhost:8090/api/status)
echo "API响应: $STATUS_RESPONSE"

# 检查响应格式
if echo "$STATUS_RESPONSE" | grep -q '"success"'; then
    echo "✓ API状态接口正常"
else
    echo "✗ API状态接口异常"
    exit 1
fi

# 模拟AJAX请求
echo "3. 模拟AJAX状态刷新..."
for i in {1..5}; do
    echo "第 $i 次AJAX请求..."
    
    AJAX_RESPONSE=$(curl -s http://localhost:8090/api/status)
    
    # 检查响应
    if echo "$AJAX_RESPONSE" | grep -q '"success":true'; then
        echo "✓ AJAX请求成功"
        
        # 提取服务数量
        SERVICE_COUNT=$(echo "$AJAX_RESPONSE" | grep -o '"faultStatusList":\[.*\]' | grep -o '\[.*\]' | jq 'length' 2>/dev/null || echo "未知")
        echo "   服务数量: $SERVICE_COUNT"
        
        # 检查在线服务
        ONLINE_COUNT=$(echo "$AJAX_RESPONSE" | grep -o '"reachable":true' | wc -l)
        echo "   在线服务: $ONLINE_COUNT"
        
    else
        echo "✗ AJAX请求失败"
    fi
    
    sleep 2
done

echo ""
echo "4. 功能说明："
echo "   - 页面每5秒自动刷新服务状态"
echo "   - 无需刷新整个页面"
echo "   - 实时更新服务在线/离线状态"
echo "   - 实时更新故障控制状态"
echo "   - 保持用户操作状态"

echo ""
echo "=== 故障控制AJAX功能测试完成 ==="
echo ""
echo "访问 http://localhost:8090/ 查看实时效果"
echo "页面将自动每5秒更新一次服务状态" 