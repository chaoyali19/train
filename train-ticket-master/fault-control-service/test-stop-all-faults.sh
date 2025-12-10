#!/bin/bash

# 测试停止所有故障的脚本
# 使用方法: ./test-stop-all-faults.sh

set -e

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

# 配置
API_BASE_URL="http://localhost:8090"
SERVICE_NAME="ts-config-service"

# 检查应用是否运行
check_app_running() {
    log_info "检查应用是否运行..."
    
    if curl -s "$API_BASE_URL/api/chaos/active" > /dev/null 2>&1; then
        log_success "应用正在运行"
        return 0
    else
        log_error "应用未运行，请先启动应用"
        exit 1
    fi
}

# 创建测试故障
create_test_faults() {
    log_info "创建测试故障..."
    
    # 创建第一个故障
    log_info "创建第一个故障..."
    response1=$(curl -s -X POST "$API_BASE_URL/api/chaos/apply" \
        -H "Content-Type: application/json" \
        -d "{
            \"serviceName\": \"$SERVICE_NAME\",
            \"delaySeconds\": 1,
            \"durationMinutes\": 5,
            \"targetService\": \"$SERVICE_NAME\",
            \"sourceService\": \"$SERVICE_NAME\",
            \"delayUpstream\": false
        }")
    
    echo "第一个故障响应: $response1"
    
    # 创建第二个故障
    log_info "创建第二个故障..."
    response2=$(curl -s -X POST "$API_BASE_URL/api/chaos/apply" \
        -H "Content-Type: application/json" \
        -d "{
            \"serviceName\": \"ts-station-service\",
            \"delaySeconds\": 2,
            \"durationMinutes\": 5,
            \"targetService\": \"ts-station-service\",
            \"sourceService\": \"ts-station-service\",
            \"delayUpstream\": false
        }")
    
    echo "第二个故障响应: $response2"
    
    # 等待一下让故障创建完成
    sleep 3
}

# 检查活跃故障
check_active_faults() {
    log_info "检查活跃故障..."
    
    response=$(curl -s "$API_BASE_URL/api/chaos/active")
    echo "活跃故障列表: $response"
    
    # 解析 JSON 获取故障数量
    fault_count=$(echo "$response" | grep -o '"chaosName"' | wc -l)
    log_info "当前活跃故障数量: $fault_count"
    
    return $fault_count
}

# 停止所有故障
stop_all_faults() {
    log_info "停止所有故障..."
    
    response=$(curl -s -X POST "$API_BASE_URL/api/chaos/stop-all" \
        -H "Content-Type: application/json")
    
    echo "停止所有故障响应: $response"
    
    if echo "$response" | grep -q '"success":true'; then
        log_success "停止所有故障成功"
        return 0
    else
        log_error "停止所有故障失败"
        return 1
    fi
}

# 验证故障是否已停止
verify_faults_stopped() {
    log_info "验证故障是否已停止..."
    
    # 等待一下让停止操作完成
    sleep 2
    
    response=$(curl -s "$API_BASE_URL/api/chaos/active")
    echo "停止后的活跃故障列表: $response"
    
    # 解析 JSON 获取故障数量
    fault_count=$(echo "$response" | grep -o '"chaosName"' | wc -l)
    log_info "停止后活跃故障数量: $fault_count"
    
    if [ "$fault_count" -eq 0 ]; then
        log_success "所有故障已成功停止"
        return 0
    else
        log_error "仍有 $fault_count 个故障未停止"
        return 1
    fi
}

# 检查 Kubernetes 中的 NetworkChaos 资源
check_k8s_resources() {
    log_info "检查 Kubernetes 中的 NetworkChaos 资源..."
    
    # 获取 chaos 命名空间中的所有 NetworkChaos
    networkchaos_list=$(kubectl get networkchaos -n chaos -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
    
    if [ -n "$networkchaos_list" ]; then
        log_warning "Kubernetes 中仍有 NetworkChaos 资源: $networkchaos_list"
        return 1
    else
        log_success "Kubernetes 中没有 NetworkChaos 资源"
        return 0
    fi
}

# 清理测试资源
cleanup() {
    log_info "清理测试资源..."
    
    # 确保所有故障都已停止
    stop_all_faults > /dev/null 2>&1
    
    # 清理 Kubernetes 中的资源
    kubectl delete networkchaos --all -n chaos > /dev/null 2>&1 || true
    
    log_success "清理完成"
}

# 主函数
main() {
    log_info "开始测试停止所有故障功能..."
    
    # 清理之前的测试资源
    cleanup
    
    # 检查应用状态
    check_app_running
    
    # 创建测试故障
    create_test_faults
    
    # 检查活跃故障
    initial_fault_count=$(check_active_faults)
    
    if [ "$initial_fault_count" -eq 0 ]; then
        log_warning "没有创建到活跃故障，跳过停止测试"
        return 0
    fi
    
    # 停止所有故障
    if stop_all_faults; then
        # 验证故障是否已停止
        if verify_faults_stopped; then
            # 检查 Kubernetes 资源
            if check_k8s_resources; then
                log_success "所有测试通过！停止所有故障功能正常工作"
                return 0
            else
                log_error "Kubernetes 资源清理失败"
                return 1
            fi
        else
            log_error "故障停止验证失败"
            return 1
        fi
    else
        log_error "停止所有故障失败"
        return 1
    fi
}

# 执行主函数
main "$@" 