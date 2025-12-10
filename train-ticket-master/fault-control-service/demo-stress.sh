#!/bin/bash

# 压测功能演示脚本
# 展示如何使用控制平台的压测功能

set -e

echo "=========================================="
echo "压测功能演示脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查服务状态
check_service() {
    echo -e "${BLUE}1. 检查控制平台服务状态...${NC}"
    if curl -s http://localhost:8080/ > /dev/null; then
        echo -e "${GREEN}✓ 控制平台服务运行正常${NC}"
        return 0
    else
        echo -e "${RED}✗ 控制平台服务未运行${NC}"
        echo -e "${YELLOW}请先启动控制平台服务:${NC}"
        echo "  cd fault-control-service"
        echo "  mvn spring-boot:run"
        return 1
    fi
}

# 演示压测页面访问
demo_stress_page() {
    echo -e "${BLUE}2. 演示压测页面访问...${NC}"
    echo -e "${YELLOW}访问地址: http://localhost:8080/stress${NC}"
    echo -e "${YELLOW}页面功能:${NC}"
    echo "  - 压测场景选择"
    echo "  - 并发数和请求数配置"
    echo "  - 任务启动和监控"
    echo "  - 实时日志查看"
}

# 演示API接口
demo_api() {
    echo -e "${BLUE}3. 演示API接口...${NC}"
    
    echo -e "${YELLOW}获取可用场景:${NC}"
    SCENARIOS=$(curl -s http://localhost:8080/stress/scenarios)
    echo "  响应: $SCENARIOS"
    
    echo -e "${YELLOW}获取任务状态:${NC}"
    STATUS=$(curl -s http://localhost:8080/stress/status)
    echo "  响应: $STATUS"
}

# 演示压测任务
demo_stress_task() {
    echo -e "${BLUE}4. 演示压测任务...${NC}"
    
    echo -e "${YELLOW}启动一个小规模压测任务:${NC}"
    echo "  场景: high_speed"
    echo "  并发数: 2"
    echo "  总请求数: 5"
    
    TASK_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "scenario=high_speed&concurrent=2&count=5" \
        http://localhost:8080/stress/start)
    
    if echo "$TASK_RESPONSE" | grep -q "success.*true"; then
        echo -e "${GREEN}✓ 压测任务启动成功${NC}"
        
        # 提取任务ID
        TASK_ID=$(echo "$TASK_RESPONSE" | grep -o '"taskId":"[^"]*"' | cut -d'"' -f4)
        echo "  任务ID: $TASK_ID"
        
        # 等待任务运行
        echo -e "${YELLOW}等待任务运行...${NC}"
        sleep 5
        
        # 检查任务状态
        echo -e "${YELLOW}检查任务状态:${NC}"
        TASK_STATUS=$(curl -s "http://localhost:8080/stress/status?taskId=$TASK_ID")
        echo "  状态: $TASK_STATUS"
        
        # 停止任务
        echo -e "${YELLOW}停止压测任务...${NC}"
        STOP_RESPONSE=$(curl -s -X POST \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "taskId=$TASK_ID" \
            http://localhost:8080/stress/stop)
        
        if echo "$STOP_RESPONSE" | grep -q "success.*true"; then
            echo -e "${GREEN}✓ 压测任务停止成功${NC}"
        else
            echo -e "${RED}✗ 压测任务停止失败${NC}"
        fi
    else
        echo -e "${RED}✗ 压测任务启动失败${NC}"
        echo "  响应: $TASK_RESPONSE"
    fi
}

# 显示使用说明
show_usage() {
    echo -e "${BLUE}5. 使用说明...${NC}"
    echo ""
    echo -e "${GREEN}=== 压测功能使用指南 ===${NC}"
    echo ""
    echo "1. 访问压测页面:"
    echo "   http://localhost:8080/stress"
    echo ""
    echo "2. 配置压测参数:"
    echo "   - 选择压测场景 (high_speed, normal, food, parallel, pay, cancel, consign)"
    echo "   - 设置并发数 (1-100)"
    echo "   - 设置总请求数 (1-10000)"
    echo ""
    echo "3. 启动压测:"
    echo "   - 点击'启动压测'按钮"
    echo "   - 系统会异步执行压测任务"
    echo "   - 在任务监控区域查看状态"
    echo ""
    echo "4. 监控任务:"
    echo "   - 实时查看任务运行状态"
    echo "   - 查看输出日志和错误信息"
    echo "   - 可以随时停止运行中的任务"
    echo ""
    echo "5. 支持的压测场景:"
    echo "   - high_speed: 高铁票查询"
    echo "   - normal: 普通列车票查询"
    echo "   - food: 食品查询"
    echo "   - parallel: 并行车票查询"
    echo "   - pay: 查询并支付订单"
    echo "   - cancel: 查询并取消订单"
    echo "   - consign: 查询并添加托运信息"
    echo ""
    echo "6. 建议的压测参数:"
    echo "   - 小规模测试: 并发5-10，请求数50-100"
    echo "   - 中等规模测试: 并发10-50，请求数100-1000"
    echo "   - 大规模测试: 并发50-100，请求数1000-10000"
    echo ""
    echo "7. 注意事项:"
    echo "   - 确保Python压测程序环境配置正确"
    echo "   - 确保Train-Ticket系统服务正常运行"
    echo "   - 避免设置过高的并发数，防止系统过载"
    echo ""
    echo "8. 故障排除:"
    echo "   - 查看应用日志: tail -f logs/application.log"
    echo "   - 测试脚本: ./test-stress.sh"
    echo "   - 详细文档: STRESS_TEST_GUIDE.md"
}

# 主函数
main() {
    echo -e "${GREEN}开始压测功能演示...${NC}"
    echo ""
    
    # 检查服务状态
    if ! check_service; then
        exit 1
    fi
    
    # 演示各个功能
    demo_stress_page
    echo ""
    
    demo_api
    echo ""
    
    demo_stress_task
    echo ""
    
    show_usage
    echo ""
    
    echo -e "${GREEN}==========================================${NC}"
    echo -e "${GREEN}压测功能演示完成${NC}"
    echo -e "${GREEN}==========================================${NC}"
    echo ""
    echo -e "${BLUE}相关文档:${NC}"
    echo "  - 使用指南: STRESS_TEST_GUIDE.md"
    echo "  - 测试脚本: test-stress.sh"
    echo "  - 项目文档: README.md"
    echo ""
    echo -e "${BLUE}访问地址:${NC}"
    echo "  - 故障控制: http://localhost:8080/"
    echo "  - 压测控制: http://localhost:8080/stress"
}

# 运行主函数
main 