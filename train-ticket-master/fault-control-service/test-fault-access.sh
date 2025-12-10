#!/bin/bash

# 测试故障接口访问权限的脚本
# 使用方法: ./test-fault-access.sh [station-service-host]

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
    local description="$2"
    
    log_info "测试: $description"
    echo "  端点: GET $BASE_URL$endpoint"
    
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint")
    
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
    log_info "开始测试故障接口访问权限"
    log_info "目标服务: $BASE_URL"
    echo "=================================="
    
    # 测试1: 获取服务信息
    test_endpoint "/fault/info" "获取服务信息"
    
    # 测试2: 获取故障状态
    test_endpoint "/fault/status" "获取故障状态"
    
    # 测试3: 测试业务接口（对比）
    log_info "测试业务接口（对比）"
    echo "  测试站点查询接口..."
    
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
    
    if [ "$http_code" = "200" ]; then
        log_success "故障接口访问权限修复成功！"
        echo ""
        log_info "现在可以正常访问故障接口："
        echo "  - 服务信息: $BASE_URL/fault/info"
        echo "  - 故障状态: $BASE_URL/fault/status"
        echo "  - 故障控制: $BASE_URL/fault/control"
    else
        log_error "故障接口访问权限仍有问题"
        echo "请检查Spring Security配置和JWTFilter设置"
    fi
}

# 检查依赖
if ! command -v curl &> /dev/null; then
    log_error "curl 未安装，请先安装 curl"
    exit 1
fi

# 执行主函数
main "$@" 