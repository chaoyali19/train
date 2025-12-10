#!/bin/bash

# 演示AJAX实时日志更新功能
echo "=== AJAX实时日志更新功能演示 ==="
echo ""

# 检查服务状态
echo "1. 检查控制服务状态..."
if curl -s http://localhost:8090/health > /dev/null; then
    echo "✓ 控制服务运行正常"
else
    echo "✗ 控制服务未运行，请先启动服务"
    exit 1
fi

echo ""
echo "2. 启动演示压测任务..."
echo "   场景: high_speed"
echo "   并发数: 3"
echo "   总请求数: 20"
echo ""

# 启动压测任务
TASK_RESPONSE=$(curl -s -X POST http://localhost:8090/stress/start \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "scenario=high_speed&concurrent=3&count=20")

TASK_ID=$(echo $TASK_RESPONSE | grep -o '"taskId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TASK_ID" ]; then
    echo "✓ 任务启动成功，任务ID: $TASK_ID"
    echo ""
    echo "3. 演示实时日志更新..."
    echo "   现在请打开浏览器访问: http://localhost:8090/stress"
    echo "   您将看到："
echo "   - 日志每1秒自动更新"
echo "   - 状态每1秒自动更新"
echo "   - 日志自动滚动到底部"
echo "   - 无需手动刷新页面"
    echo ""
    echo "4. 监控任务进度..."
    
    # 监控任务进度
    for i in {1..30}; do
        STATUS_RESPONSE=$(curl -s "http://localhost:8090/stress/status?taskId=$TASK_ID")
        
        # 检查任务是否还在运行
        if echo "$STATUS_RESPONSE" | grep -q '"success":false'; then
            echo "✓ 任务已完成"
            break
        fi
        
        # 显示进度
        if echo "$STATUS_RESPONSE" | grep -q "进度"; then
            PROGRESS=$(echo "$STATUS_RESPONSE" | grep -o "进度: [0-9.]*%" | tail -1)
            echo "   $PROGRESS"
        else
            echo "   任务运行中..."
        fi
        
        sleep 2
    done
    
    echo ""
    echo "5. 演示完成"
    echo "   在浏览器中您可以看到："
    echo "   ✓ 实时日志更新"
    echo "   ✓ 任务状态变化"
    echo "   ✓ 进度百分比显示"
    echo "   ✓ 自动滚动效果"
    echo ""
    echo "=== 演示完成 ==="
    echo ""
    echo "功能特点："
    echo "- 无需刷新页面"
    echo "- 实时监控压测进度"
    echo "- 自动显示最新日志"
    echo "- 友好的用户界面"
    
else
    echo "✗ 任务启动失败"
    echo "响应: $TASK_RESPONSE"
    exit 1
fi 