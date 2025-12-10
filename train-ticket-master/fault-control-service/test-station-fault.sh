#!/bin/bash

# 测试station服务故障接口的脚本
# 使用方法: ./test-station-fault.sh [station-service-host]

set -e

# 默认配置
STATION_HOST="${1:-ts-station-service}"
STATION_PORT="8080"
BASE_URL="http://${STATION_HOST}:${STATION_PORT}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 测试函数
test_endpoint() {
    local endpoint="$1"
    local method="${2:-GET}"
    local data="${3:-}"
    local description="$4"
    
    log_info "测试: $description"
    echo "  端点: $method $BASE_URL$endpoint"
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        echo "  数据: $data"
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint")
    fi
    
    # 分离响应体和状态码
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "200" ]; then
        log_success "HTTP 200 - 成功"
        echo "  响应: $response_body" | head -c 200
        if [ ${#response_body} -gt 200 ]; then
            echo "..."
        fi
    else
        log_error "HTTP $http_code - 失败"
        echo "  响应: $response_body"
    fi
    
    echo ""
}

# 主测试流程
main() {
    log_info "开始测试station服务故障接口"
    log_info "目标服务: $BASE_URL"
    echo "=================================="
    
    # 测试1: 获取服务信息
    test_endpoint "/fault/info" "GET" "" "获取服务信息"
    
    # 测试2: 获取故障状态
    test_endpoint "/fault/status" "GET" "" "获取故障状态"
    
    # 测试3: 启用空数据故障
    test_endpoint "/fault/control" "POST" '{"faultId":"empty-station-query","enable":true}' "启用空数据故障"
    
    # 测试4: 再次获取故障状态（验证启用成功）
    test_endpoint "/fault/status" "GET" "" "验证故障状态更新"
    
    # 测试5: 禁用空数据故障
    test_endpoint "/fault/control" "POST" '{"faultId":"empty-station-query","enable":false}' "禁用空数据故障"
    
    # 测试6: 启用延迟故障
    test_endpoint "/fault/control" "POST" '{"faultId":"station-query-delay","enable":true,"delayMs":5000}' "启用延迟故障(5秒)"
    
    # 测试7: 启用500错误故障
    test_endpoint "/fault/control" "POST" '{"faultId":"station-query-500-error","enable":true}' "启用500错误故障"
    
    # 测试8: 启用随机500错误故障
    test_endpoint "/fault/control" "POST" '{"faultId":"station-query-random-500-error","enable":true,"probability":0.3}' "启用随机500错误故障(30%概率)"
    
    # 测试9: 启用响应结构错误故障
    test_endpoint "/fault/control" "POST" '{"faultId":"station-query-response-structure-error","enable":true}' "启用响应结构错误故障"
    
    # 测试10: 最终状态检查
    test_endpoint "/fault/status" "GET" "" "最终故障状态检查"
    
    # 测试11: 测试业务接口（验证故障注入）
    log_info "测试业务接口故障注入效果"
    echo "  测试站点查询接口..."
    
    # 测试正常的站点查询
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v1/stationservice/stations")
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "200" ]; then
        log_success "站点查询接口正常"
    else
        log_warning "站点查询接口异常 (HTTP $http_code)"
    fi
    
    echo ""
    log_info "测试完成"
    echo "=================================="
    log_info "故障接口测试总结:"
    echo "  - 服务信息接口: /fault/info"
    echo "  - 状态查询接口: /fault/status"
    echo "  - 故障控制接口: /fault/control"
    echo ""
    log_info "可用的故障类型:"
    echo "  - empty-station-query: 空数据故障"
    echo "  - station-query-delay: 延迟故障"
    echo "  - station-query-500-error: 500错误故障"
    echo "  - station-query-random-500-error: 随机500错误故障"
    echo "  - station-query-response-structure-error: 响应结构错误故障"
}

# 检查依赖
if ! command -v curl &> /dev/null; then
    log_error "curl 未安装，请先安装 curl"
    exit 1
fi

# 执行主函数
main "$@" 