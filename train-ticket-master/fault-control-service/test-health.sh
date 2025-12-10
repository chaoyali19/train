#!/bin/bash

# 测试健康检查端点的脚本
# 使用方法: ./test-health.sh [service-host] [service-port]

set -e

# 默认配置
SERVICE_HOST="${1:-localhost}"
SERVICE_PORT="${2:-8080}"
BASE_URL="http://${SERVICE_HOST}:${SERVICE_PORT}"

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
        echo "  响应: $response_body"
    else
        log_error "HTTP $http_code - 失败"
        echo "  响应: $response_body"
    fi
    
    echo ""
}

# 主测试流程
main() {
    log_info "开始测试健康检查端点"
    log_info "目标服务: $BASE_URL"
    echo "=================================="
    
    # 测试1: 健康检查端点
    test_endpoint "/actuator/health" "健康检查端点"
    
    # 测试2: 信息端点
    test_endpoint "/actuator/info" "信息端点"
    
    # 测试3: 指标端点
    test_endpoint "/actuator/metrics" "指标端点"
    
    # 测试4: 主页面
    test_endpoint "/" "主页面"
    
    # 测试5: 故障状态API
    test_endpoint "/api/fault/status" "故障状态API"
    
    echo "=================================="
    log_info "测试完成"
    
    # 总结
    if [ "$http_code" = "200" ]; then
        log_success "所有端点测试通过！"
        echo ""
        log_info "服务端点信息："
        echo "  - 健康检查: $BASE_URL/actuator/health"
        echo "  - 主页面: $BASE_URL/"
        echo "  - 故障状态: $BASE_URL/api/fault/status"
        echo ""
        log_info "Kubernetes健康检查应该能正常工作"
    else
        log_error "部分端点测试失败"
        echo "请检查服务配置和启动状态"
    fi
}

# 检查依赖
if ! command -v curl &> /dev/null; then
    log_error "curl 未安装，请先安装 curl"
    exit 1
fi

# 执行主函数
main "$@" 