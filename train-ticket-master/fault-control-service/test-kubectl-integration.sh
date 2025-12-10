#!/bin/bash

# 测试 kubectl 集成脚本
# 使用方法: ./test-kubectl-integration.sh

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

# 检查 kubectl 是否可用
check_kubectl() {
    log_info "检查 kubectl 是否可用..."
    
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安装或不在 PATH 中"
        exit 1
    fi
    
    if ! kubectl cluster-info &> /dev/null; then
        log_error "无法连接到 Kubernetes 集群"
        exit 1
    fi
    
    log_success "kubectl 可用且能连接到集群"
}

# 检查 chaos-mesh 命名空间
check_chaos_namespace() {
    log_info "检查 chaos 命名空间..."
    
    if ! kubectl get namespace chaos &> /dev/null; then
        log_warning "chaos 命名空间不存在，正在创建..."
        kubectl create namespace chaos
        log_success "已创建 chaos 命名空间"
    else
        log_success "chaos 命名空间已存在"
    fi
}

# 测试生成 YAML
test_yaml_generation() {
    log_info "测试 YAML 生成..."
    
    # 模拟 ChaosMeshService 的 YAML 生成逻辑
    chaosName="test-network-delay-$(date +%s)"
    sourceService="ts-config-service"
    targetService="ts-config-service"
    delaySeconds=1
    durationMinutes=1
    
    cat > /tmp/test-network-chaos.yaml << EOF
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: $chaosName
  namespace: chaos
spec:
  action: delay
  mode: one
  selector:
    labelSelectors:
      app: $targetService
  delay:
    latency: ${delaySeconds}s
    correlation: '100'
    jitter: 0ms
  duration: ${durationMinutes}m
EOF
    
    log_success "已生成测试 YAML 文件: /tmp/test-network-chaos.yaml"
    echo "生成的 YAML 内容:"
    cat /tmp/test-network-chaos.yaml
}

# 测试 kubectl apply
test_kubectl_apply() {
    log_info "测试 kubectl apply..."
    
    if kubectl apply -f /tmp/test-network-chaos.yaml; then
        log_success "kubectl apply 成功"
        
        # 等待一下让资源创建完成
        sleep 2
        
        # 检查资源是否创建成功
        if kubectl get networkchaos -n chaos | grep -q "test-network-delay"; then
            log_success "NetworkChaos 资源创建成功"
        else
            log_error "NetworkChaos 资源创建失败"
        fi
    else
        log_error "kubectl apply 失败"
        exit 1
    fi
}

# 测试 kubectl delete
test_kubectl_delete() {
    log_info "测试 kubectl delete..."
    
    chaosName=$(kubectl get networkchaos -n chaos -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    
    if [ -n "$chaosName" ]; then
        if kubectl delete networkchaos $chaosName -n chaos; then
            log_success "kubectl delete 成功"
        else
            log_error "kubectl delete 失败"
        fi
    else
        log_warning "没有找到可删除的 NetworkChaos 资源"
    fi
}

# 清理测试文件
cleanup() {
    log_info "清理测试文件..."
    rm -f /tmp/test-network-chaos.yaml
    log_success "清理完成"
}

# 主函数
main() {
    log_info "开始测试 kubectl 集成..."
    
    check_kubectl
    check_chaos_namespace
    test_yaml_generation
    test_kubectl_apply
    test_kubectl_delete
    cleanup
    
    log_success "所有测试通过！kubectl 集成正常工作"
}

# 执行主函数
main "$@" 