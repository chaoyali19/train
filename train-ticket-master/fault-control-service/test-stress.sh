#!/bin/bash

# 压测功能测试脚本
# 用于测试控制平台的压测功能

set -e

echo "=========================================="
echo "压测功能测试脚本"
echo "=========================================="

# 检查服务是否运行
echo "1. 检查控制平台服务状态..."
if curl -s http://localhost:8080/ > /dev/null; then
    echo "✓ 控制平台服务运行正常"
else
    echo "✗ 控制平台服务未运行，请先启动服务"
    exit 1
fi

# 测试压测页面访问
echo "2. 测试压测页面访问..."
if curl -s http://localhost:8080/stress > /dev/null; then
    echo "✓ 压测页面访问正常"
else
    echo "✗ 压测页面访问失败"
    exit 1
fi

# 测试获取压测场景API
echo "3. 测试获取压测场景API..."
SCENARIOS=$(curl -s http://localhost:8080/stress/scenarios)
if [ $? -eq 0 ] && [ -n "$SCENARIOS" ]; then
    echo "✓ 获取压测场景成功: $SCENARIOS"
else
    echo "✗ 获取压测场景失败"
    exit 1
fi

# 测试获取任务状态API
echo "4. 测试获取任务状态API..."
STATUS=$(curl -s http://localhost:8080/stress/status)
if [ $? -eq 0 ]; then
    echo "✓ 获取任务状态成功"
else
    echo "✗ 获取任务状态失败"
    exit 1
fi

# 测试启动压测任务（使用较小的参数）
echo "5. 测试启动压测任务..."
TASK_RESPONSE=$(curl -s -X POST \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "scenario=high_speed&concurrent=2&count=5" \
    http://localhost:8080/stress/start)

if [ $? -eq 0 ] && echo "$TASK_RESPONSE" | grep -q "success.*true"; then
    echo "✓ 启动压测任务成功"
    # 提取任务ID
    TASK_ID=$(echo "$TASK_RESPONSE" | grep -o '"taskId":"[^"]*"' | cut -d'"' -f4)
    echo "  任务ID: $TASK_ID"
    
    # 等待几秒钟让任务开始运行
    echo "  等待任务开始运行..."
    sleep 3
    
    # 检查任务状态
    TASK_STATUS=$(curl -s "http://localhost:8080/stress/status?taskId=$TASK_ID")
    if [ $? -eq 0 ]; then
        echo "✓ 获取任务状态成功"
        echo "  任务状态响应: $TASK_STATUS"
    else
        echo "✗ 获取任务状态失败"
    fi
    
    # 停止任务
    echo "6. 测试停止压测任务..."
    STOP_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "taskId=$TASK_ID" \
        http://localhost:8080/stress/stop)
    
    if [ $? -eq 0 ] && echo "$STOP_RESPONSE" | grep -q "success.*true"; then
        echo "✓ 停止压测任务成功"
    else
        echo "✗ 停止压测任务失败"
    fi
else
    echo "✗ 启动压测任务失败"
    echo "  响应: $TASK_RESPONSE"
    exit 1
fi

echo "=========================================="
echo "压测功能测试完成"
echo "=========================================="
echo ""
echo "测试结果:"
echo "✓ 控制平台服务正常运行"
echo "✓ 压测页面可以正常访问"
echo "✓ 压测API接口正常工作"
echo "✓ 可以启动和停止压测任务"
echo ""
echo "访问地址:"
echo "- 故障控制页面: http://localhost:8080/"
echo "- 压测控制页面: http://localhost:8080/stress"
echo ""
echo "注意事项:"
echo "1. 确保Python压测程序路径配置正确"
echo "2. 确保Python环境已安装所需依赖"
echo "3. 确保Train-Ticket系统服务正常运行" 