#!/bin/bash

# ts-station-service 故障场景部署脚本
# 用于演示不同故障场景的部署

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

# 检查kubectl是否可用
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl 未安装或不在PATH中"
        exit 1
    fi
    
    if ! kubectl cluster-info &> /dev/null; then
        print_error "无法连接到Kubernetes集群"
        exit 1
    fi
}

# 构建Docker镜像
build_image() {
    print_info "构建Docker镜像..."
    
    # 检查是否存在Dockerfile
    if [ ! -f "Dockerfile" ]; then
        print_error "Dockerfile 不存在"
        exit 1
    fi
    
    # 构建镜像
    docker build -t ts-station-service:latest .
    print_success "Docker镜像构建完成"
}

# 部署正常版本（无故障）
deploy_normal() {
    print_info "部署正常版本（无故障）..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ts-station-service-normal
  labels:
    app: ts-station-service
    version: normal
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ts-station-service
  template:
    metadata:
      labels:
        app: ts-station-service
        version: normal
    spec:
      containers:
      - name: ts-station-service
        image: ts-station-service:latest
        ports:
        - containerPort: 12345
        env:
        - name: FAULT_EMPTY_STATION_QUERY
          value: "false"
        - name: FAULT_STATION_QUERY_DELAY
          value: "false"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: ts-station-service-normal
spec:
  selector:
    app: ts-station-service
  ports:
  - port: 12345
    targetPort: 12345
    protocol: TCP
  type: ClusterIP
EOF
    
    print_success "正常版本部署完成"
}

# 部署空数据故障版本
deploy_empty_data_fault() {
    print_info "部署空数据故障版本..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ts-station-service-empty-fault
  labels:
    app: ts-station-service
    version: empty-fault
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ts-station-service
  template:
    metadata:
      labels:
        app: ts-station-service
        version: empty-fault
    spec:
      containers:
      - name: ts-station-service
        image: ts-station-service:latest
        ports:
        - containerPort: 12345
        env:
        - name: FAULT_EMPTY_STATION_QUERY
          value: "true"
        - name: FAULT_STATION_QUERY_DELAY
          value: "false"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: ts-station-service-empty-fault
spec:
  selector:
    app: ts-station-service
  ports:
  - port: 12345
    targetPort: 12345
    protocol: TCP
  type: ClusterIP
EOF
    
    print_success "空数据故障版本部署完成"
}

# 部署延迟故障版本
deploy_delay_fault() {
    local delay_ms=${1:-10000}
    print_info "部署延迟故障版本（延迟: ${delay_ms}ms）..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ts-station-service-delay-fault
  labels:
    app: ts-station-service
    version: delay-fault
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ts-station-service
  template:
    metadata:
      labels:
        app: ts-station-service
        version: delay-fault
    spec:
      containers:
      - name: ts-station-service
        image: ts-station-service:latest
        ports:
        - containerPort: 12345
        env:
        - name: FAULT_EMPTY_STATION_QUERY
          value: "false"
        - name: FAULT_STATION_QUERY_DELAY
          value: "true"
        - name: FAULT_DELAY_TIME_MS
          value: "${delay_ms}"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: ts-station-service-delay-fault
spec:
  selector:
    app: ts-station-service
  ports:
  - port: 12345
    targetPort: 12345
    protocol: TCP
  type: ClusterIP
EOF
    
    print_success "延迟故障版本部署完成"
}

# 清理所有部署
cleanup() {
    print_info "清理所有故障部署..."
    
    kubectl delete deployment ts-station-service-normal --ignore-not-found=true
    kubectl delete deployment ts-station-service-empty-fault --ignore-not-found=true
    kubectl delete deployment ts-station-service-delay-fault --ignore-not-found=true
    
    kubectl delete service ts-station-service-normal --ignore-not-found=true
    kubectl delete service ts-station-service-empty-fault --ignore-not-found=true
    kubectl delete service ts-station-service-delay-fault --ignore-not-found=true
    
    print_success "清理完成"
}

# 显示部署状态
show_status() {
    print_info "当前部署状态:"
    echo ""
    kubectl get deployments -l app=ts-station-service
    echo ""
    kubectl get services -l app=ts-station-service
}

# 显示使用帮助
show_help() {
    echo "ts-station-service 故障场景部署脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  build                   构建Docker镜像"
    echo "  normal                  部署正常版本（无故障）"
    echo "  empty-fault             部署空数据故障版本"
    echo "  delay-fault [延迟ms]     部署延迟故障版本（默认10秒）"
    echo "  cleanup                 清理所有部署"
    echo "  status                  显示部署状态"
    echo "  help                    显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 build                构建镜像"
    echo "  $0 normal               部署正常版本"
    echo "  $0 empty-fault          部署空数据故障版本"
    echo "  $0 delay-fault 15000    部署15秒延迟故障版本"
    echo "  $0 cleanup              清理所有部署"
}

# 主函数
main() {
    case "${1:-help}" in
        "build")
            check_kubectl
            build_image
            ;;
        "normal")
            check_kubectl
            deploy_normal
            ;;
        "empty-fault")
            check_kubectl
            deploy_empty_data_fault
            ;;
        "delay-fault")
            check_kubectl
            deploy_delay_fault "$2"
            ;;
        "cleanup")
            check_kubectl
            cleanup
            ;;
        "status")
            check_kubectl
            show_status
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 执行主函数
main "$@" 