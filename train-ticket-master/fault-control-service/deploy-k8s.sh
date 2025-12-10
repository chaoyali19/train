#!/bin/bash

# 故障控制服务 Kubernetes 部署脚本
# 使用方法: ./deploy-k8s.sh [build|deploy|all|clean]

set -e

# 配置变量
NAMESPACE="chaos"
SERVICE_NAME="fault-control-service"
IMAGE_NAME="harbor.cloudwise.com/noname/fault-control-service"
IMAGE_TAG="aa555050-fault-v1"
NODEPORT="30081"

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

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装或不在 PATH 中"
        exit 1
    fi
    
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安装或不在 PATH 中"
        exit 1
    fi
    
    if ! kubectl cluster-info &> /dev/null; then
        log_error "无法连接到 Kubernetes 集群"
        exit 1
    fi
    
    log_success "依赖检查通过"
}

# 构建 Docker 镜像
build_image() {
    log_info "构建 Docker 镜像..."
    
    # 检查 Dockerfile 是否存在
    if [ ! -f "Dockerfile" ]; then
        log_error "Dockerfile 不存在"
        exit 1
    fi
    
    # 构建镜像
    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
    
    if [ $? -eq 0 ]; then
        log_success "镜像构建成功: ${IMAGE_NAME}:${IMAGE_TAG}"
    else
        log_error "镜像构建失败"
        exit 1
    fi
}

# 部署到 Kubernetes
deploy_to_k8s() {
    log_info "部署到 Kubernetes..."
    
    # 检查 YAML 文件是否存在
    if [ ! -f "k8s-deployment.yaml" ]; then
        log_error "k8s-deployment.yaml 不存在"
        exit 1
    fi
    
    # 应用 YAML 文件
    kubectl apply -f k8s-deployment.yaml
    
    if [ $? -eq 0 ]; then
        log_success "Kubernetes 资源创建成功"
    else
        log_error "Kubernetes 资源创建失败"
        exit 1
    fi
    
    # 等待部署完成
    log_info "等待部署完成..."
    kubectl wait --for=condition=available --timeout=300s deployment/${SERVICE_NAME} -n ${NAMESPACE}
    
    if [ $? -eq 0 ]; then
        log_success "部署完成"
    else
        log_error "部署超时或失败"
        exit 1
    fi
}

# 显示服务信息
show_service_info() {
    log_info "服务信息:"
    echo "----------------------------------------"
    echo "命名空间: ${NAMESPACE}"
    echo "服务名称: ${SERVICE_NAME}"
    echo "NodePort: ${NODEPORT}"
    echo "镜像: ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    
    # 获取节点信息
    log_info "获取节点信息..."
    NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
    
    if [ -n "$NODE_IP" ]; then
        echo "访问地址:"
        echo "  - NodePort: http://${NODE_IP}:${NODEPORT}"
        echo ""
    fi
    
    # 显示 Pod 状态
    log_info "Pod 状态:"
    kubectl get pods -n ${NAMESPACE} -l app=${SERVICE_NAME}
    echo ""
    
    # 显示服务状态
    log_info "服务状态:"
    kubectl get svc -n ${NAMESPACE} -l app=${SERVICE_NAME}
    echo ""
}

# 清理资源
cleanup() {
    log_info "清理 Kubernetes 资源..."
    
    if [ -f "k8s-deployment.yaml" ]; then
        kubectl delete -f k8s-deployment.yaml --ignore-not-found=true
        log_success "Kubernetes 资源已清理"
    fi
    
    log_info "清理 Docker 镜像..."
    docker rmi ${IMAGE_NAME}:${IMAGE_TAG} --force 2>/dev/null || true
    log_success "Docker 镜像已清理"
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    # 等待服务启动
    sleep 10
    
    # 获取 Pod 名称
    POD_NAME=$(kubectl get pods -n ${NAMESPACE} -l app=${SERVICE_NAME} -o jsonpath='{.items[0].metadata.name}')
    
    if [ -z "$POD_NAME" ]; then
        log_error "无法获取 Pod 名称"
        return 1
    fi
    
    # 检查 Pod 状态
    POD_STATUS=$(kubectl get pod ${POD_NAME} -n ${NAMESPACE} -o jsonpath='{.status.phase}')
    
    if [ "$POD_STATUS" = "Running" ]; then
        log_success "Pod 状态正常: ${POD_STATUS}"
    else
        log_error "Pod 状态异常: ${POD_STATUS}"
        return 1
    fi
    
    # 检查服务端口
    NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
    
    if [ -n "$NODE_IP" ]; then
        log_info "测试服务连接: http://${NODE_IP}:${NODEPORT}"
        
        # 使用 curl 测试连接（如果可用）
        if command -v curl &> /dev/null; then
            if curl -s -o /dev/null -w "%{http_code}" "http://${NODE_IP}:${NODEPORT}/actuator/health" | grep -q "200"; then
                log_success "服务健康检查通过"
            else
                log_warning "服务健康检查失败，可能需要更多时间启动"
            fi
        else
            log_warning "curl 不可用，跳过健康检查"
        fi
    fi
}

# 显示日志
show_logs() {
    log_info "显示服务日志..."
    
    POD_NAME=$(kubectl get pods -n ${NAMESPACE} -l app=${SERVICE_NAME} -o jsonpath='{.items[0].metadata.name}')
    
    if [ -n "$POD_NAME" ]; then
        kubectl logs -f ${POD_NAME} -n ${NAMESPACE}
    else
        log_error "无法获取 Pod 名称"
    fi
}

# 主函数
main() {
    case "$1" in
        "build")
            check_dependencies
            build_image
            ;;
        "deploy")
            check_dependencies
            deploy_to_k8s
            show_service_info
            health_check
            ;;
        "all")
            check_dependencies
            build_image
            deploy_to_k8s
            show_service_info
            health_check
            ;;
        "clean")
            cleanup
            ;;
        "logs")
            check_dependencies
            show_logs
            ;;
        "status")
            check_dependencies
            show_service_info
            ;;
        *)
            echo "使用方法: $0 [build|deploy|all|clean|logs|status]"
            echo ""
            echo "命令说明:"
            echo "  build   - 构建 Docker 镜像"
            echo "  deploy  - 部署到 Kubernetes"
            echo "  all     - 构建镜像并部署"
            echo "  clean   - 清理所有资源"
            echo "  logs    - 显示服务日志"
            echo "  status  - 显示服务状态"
            echo ""
            echo "示例:"
            echo "  $0 all     # 完整部署"
            echo "  $0 deploy  # 仅部署（假设镜像已存在）"
            echo "  $0 logs    # 查看日志"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@" 