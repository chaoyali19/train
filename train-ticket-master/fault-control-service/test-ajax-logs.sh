#!/bin/bash

# 测试AJAX实时日志更新功能
echo "=== 测试AJAX实时日志更新功能 ==="

# 检查服务是否运行
echo "1. 检查控制服务状态..."
if curl -s http://localhost:8090/health > /dev/null; then
    echo "✓ 控制服务运行正常"
else
    echo "✗ 控制服务未运行，请先启动服务"
    exit 1
fi

# 启动一个压测任务
echo "2. 启动压测任务..."
TASK_RESPONSE=$(curl -s -X POST http://localhost:8090/stress/start \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "scenario=high_speed&concurrent=5&count=50")

echo "响应: $TASK_RESPONSE"

# 提取任务ID
TASK_ID=$(echo $TASK_RESPONSE | grep -o '"taskId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TASK_ID" ]; then
    echo "✓ 任务启动成功，任务ID: $TASK_ID"
else
    echo "✗ 任务启动失败"
    exit 1
fi

# 等待几秒钟让任务开始运行
echo "3. 等待任务开始运行..."
sleep 3

# 测试AJAX日志更新
echo "4. 测试AJAX日志更新功能..."

# 模拟AJAX请求获取日志
for i in {1..10}; do
    echo "第 $i 次获取日志..."
    
    LOG_RESPONSE=$(curl -s "http://localhost:8090/stress/status?taskId=$TASK_ID")
    echo "日志响应: $LOG_RESPONSE"
    
    # 检查是否有日志输出
    if echo "$LOG_RESPONSE" | grep -q "output"; then
        echo "✓ 检测到日志输出"
    else
        echo "- 暂无日志输出"
    fi
    
    sleep 1
done

# 停止任务
echo "5. 停止压测任务..."
STOP_RESPONSE=$(curl -s -X POST http://localhost:8090/stress/stop \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "taskId=$TASK_ID")

echo "停止响应: $STOP_RESPONSE"

# 最终状态检查
echo "6. 最终状态检查..."
FINAL_STATUS=$(curl -s http://localhost:8090/stress/status)
echo "最终状态: $FINAL_STATUS"

echo ""
echo "=== AJAX实时日志更新测试完成 ==="
echo ""
echo "测试说明："
echo "1. 页面会自动每1秒更新一次日志"
echo "2. 页面会自动每1秒更新一次任务状态"
echo "3. 日志会自动滚动到底部显示最新内容"
echo "4. 无需手动刷新页面即可看到实时更新"
echo ""
echo "访问 http://localhost:8090/stress 查看实时效果" 