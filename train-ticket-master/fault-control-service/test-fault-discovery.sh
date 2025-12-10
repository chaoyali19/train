#!/bin/bash

# 故障发现功能测试脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 测试配置
FAULT_CONTROL_URL="http://localhost:8080/fault-control"
STATION_SERVICE_URL="http://localhost:12345"

# 检查服务是否运行
check_service() {
    local url=$1
    local service_name=$2
    
    print_info "检查 $service_name 服务状态..."
    
    if curl -s --connect-timeout 5 "$url" > /dev/null 2>&1; then
        print_success "$service_name 服务正在运行"
        return 0
    else
        print_error "$service_name 服务未运行或不可访问"
        return 1
    fi
}

# 测试故障控制服务API
test_fault_control_api() {
    print_info "测试故障控制服务API..."
    
    # 测试获取服务信息
    print_info "测试获取服务信息接口..."
    if curl -s "$FAULT_CONTROL_URL/api/info" | jq . > /dev/null 2>&1; then
        print_success "获取服务信息接口正常"
    else
        print_error "获取服务信息接口异常"
        return 1
    fi
    
    # 测试获取故障状态
    print_info "测试获取故障状态接口..."
    if curl -s "$FAULT_CONTROL_URL/api/status" | jq . > /dev/null 2>&1; then
        print_success "获取故障状态接口正常"
    else
        print_error "获取故障状态接口异常"
        return 1
    fi
    
    # 测试刷新服务状态
    print_info "测试刷新服务状态接口..."
    if curl -s -X POST "$FAULT_CONTROL_URL/api/refresh" > /dev/null 2>&1; then
        print_success "刷新服务状态接口正常"
    else
        print_error "刷新服务状态接口异常"
        return 1
    fi
}

# 测试station服务故障接口
test_station_fault_api() {
    print_info "测试station服务故障接口..."
    
    # 测试获取服务信息
    print_info "测试获取station服务信息..."
    if curl -s "$STATION_SERVICE_URL/fault/info" | jq . > /dev/null 2>&1; then
        print_success "station服务信息接口正常"
    else
        print_error "station服务信息接口异常"
        return 1
    fi
    
    # 测试获取故障状态
    print_info "测试获取station故障状态..."
    if curl -s "$STATION_SERVICE_URL/fault/status" | jq . > /dev/null 2>&1; then
        print_success "station故障状态接口正常"
    else
        print_error "station故障状态接口异常"
        return 1
    fi
    
    # 测试控制故障
    print_info "测试控制station故障..."
    local response=$(curl -s -X POST "$STATION_SERVICE_URL/fault/control" \
        -H "Content-Type: application/json" \
        -d '{"faultId":"empty-station-query","enable":true}')
    
    if echo "$response" | jq . > /dev/null 2>&1; then
        print_success "station故障控制接口正常"
        print_info "响应: $response"
    else
        print_error "station故障控制接口异常"
        return 1
    fi
}

# 测试故障控制
test_fault_control() {
    print_info "测试故障控制功能..."
    
    # 启用空数据故障
    print_info "启用空数据故障..."
    local response=$(curl -s -X POST "$FAULT_CONTROL_URL/api/control?serviceId=station-service" \
        -H "Content-Type: application/json" \
        -d '{"faultId":"empty-station-query","enable":true}')
    
    if echo "$response" | jq -r '.success' 2>/dev/null | grep -q "true"; then
        print_success "启用空数据故障成功"
    else
        print_error "启用空数据故障失败"
        print_info "响应: $response"
        return 1
    fi
    
    # 等待一下让故障生效
    sleep 2
    
    # 验证故障是否生效
    print_info "验证故障是否生效..."
    local status_response=$(curl -s "$FAULT_CONTROL_URL/api/status")
    local fault_enabled=$(echo "$status_response" | jq -r '.[] | select(.serviceId=="station-service") | .faults[] | select(.id=="empty-station-query") | .enabled' 2>/dev/null)
    
    if [ "$fault_enabled" = "true" ]; then
        print_success "空数据故障已生效"
    else
        print_error "空数据故障未生效"
        return 1
    fi
    
    # 禁用空数据故障
    print_info "禁用空数据故障..."
    response=$(curl -s -X POST "$FAULT_CONTROL_URL/api/control?serviceId=station-service" \
        -H "Content-Type: application/json" \
        -d '{"faultId":"empty-station-query","enable":false}')
    
    if echo "$response" | jq -r '.success' 2>/dev/null | grep -q "true"; then
        print_success "禁用空数据故障成功"
    else
        print_error "禁用空数据故障失败"
        print_info "响应: $response"
        return 1
    fi
}

# 测试延迟故障
test_delay_fault() {
    print_info "测试延迟故障功能..."
    
    # 启用延迟故障
    print_info "启用延迟故障（5秒）..."
    local response=$(curl -s -X POST "$FAULT_CONTROL_URL/api/control?serviceId=station-service" \
        -H "Content-Type: application/json" \
        -d '{"faultId":"station-query-delay","enable":true,"delayMs":5000}')
    
    if echo "$response" | jq -r '.success' 2>/dev/null | grep -q "true"; then
        print_success "启用延迟故障成功"
    else
        print_error "启用延迟故障失败"
        print_info "响应: $response"
        return 1
    fi
    
    # 等待一下让故障生效
    sleep 2
    
    # 验证延迟故障是否生效
    print_info "验证延迟故障是否生效..."
    local status_response=$(curl -s "$FAULT_CONTROL_URL/api/status")
    local fault_enabled=$(echo "$status_response" | jq -r '.[] | select(.serviceId=="station-service") | .faults[] | select(.id=="station-query-delay") | .enabled' 2>/dev/null)
    local delay_ms=$(echo "$status_response" | jq -r '.[] | select(.serviceId=="station-service") | .faults[] | select(.id=="station-query-delay") | .delayMs' 2>/dev/null)
    
    if [ "$fault_enabled" = "true" ] && [ "$delay_ms" = "5000" ]; then
        print_success "延迟故障已生效，延迟时间: ${delay_ms}ms"
    else
        print_error "延迟故障未生效"
        return 1
    fi
    
    # 禁用延迟故障
    print_info "禁用延迟故障..."
    response=$(curl -s -X POST "$FAULT_CONTROL_URL/api/control?serviceId=station-service" \
        -H "Content-Type: application/json" \
        -d '{"faultId":"station-query-delay","enable":false}')
    
    if echo "$response" | jq -r '.success' 2>/dev/null | grep -q "true"; then
        print_success "禁用延迟故障成功"
    else
        print_error "禁用延迟故障失败"
        print_info "响应: $response"
        return 1
    fi
}

# 显示测试结果
show_test_results() {
    print_info "测试完成！"
    print_info "故障控制服务地址: $FAULT_CONTROL_URL"
    print_info "station服务地址: $STATION_SERVICE_URL"
    print_info ""
    print_info "可以访问以下地址查看故障控制界面："
    print_info "  http://localhost:8080/fault-control"
}

# 主函数
main() {
    print_info "开始故障发现功能测试..."
    print_info "故障控制服务地址: $FAULT_CONTROL_URL"
    print_info "station服务地址: $STATION_SERVICE_URL"
    print_info ""
    
    # 检查依赖
    if ! command -v jq &> /dev/null; then
        print_error "jq 未安装，请先安装 jq"
        exit 1
    fi
    
    if ! command -v curl &> /dev/null; then
        print_error "curl 未安装，请先安装 curl"
        exit 1
    fi
    
    # 检查服务状态
    check_service "$FAULT_CONTROL_URL" "故障控制服务" || exit 1
    check_service "$STATION_SERVICE_URL" "station服务" || exit 1
    
    print_info ""
    
    # 执行测试
    test_fault_control_api || exit 1
    test_station_fault_api || exit 1
    test_fault_control || exit 1
    test_delay_fault || exit 1
    
    print_info ""
    show_test_results
}

# 执行主函数
main "$@" 