#!/bin/bash

# Train-Ticket 故障模拟测试脚本
# 用于模拟各种生产环境故障场景

set -e

NAMESPACE="chaos"
CHAOS_DIR="manifests/helm/trainticket/templates"

echo "🚀 开始 Train-Ticket 故障模拟测试..."

# 函数：检查 Chaos Mesh 是否已安装
check_chaos_mesh() {
    echo "📋 检查 Chaos Mesh 安装状态..."
    if ! kubectl get crd networkchaos.chaos-mesh.org >/dev/null 2>&1; then
        echo "❌ Chaos Mesh 未安装，请先安装 Chaos Mesh"
        echo "安装命令: helm install chaos-mesh chaos-mesh/chaos-mesh --namespace chaos-mesh --create-namespace"
        exit 1
    fi
    echo "✅ Chaos Mesh 已安装"
}

# 函数：故障1 - 大流量负载过高
test_high_load() {
    echo "🔥 模拟故障1: 异常大流量导致负载过高"
    kubectl apply -f ${CHAOS_DIR}/chaos-load-test.yaml
    echo "⏰ 等待 30 秒观察效果..."
    sleep 30
    echo "📊 检查服务状态:"
    kubectl get pods -n ${NAMESPACE} -l app=ts-order-service
    kubectl top pods -n ${NAMESPACE} -l app=ts-order-service
}

# 函数：故障2 - 外部API不可用
test_external_api_failure() {
    echo "🌐 模拟故障2: 依赖外部公共API服务不可用"
    kubectl apply -f ${CHAOS_DIR}/chaos-external-api-failure.yaml
    echo "⏰ 等待 30 秒观察效果..."
    sleep 30
    echo "📊 检查服务状态:"
    kubectl get pods -n ${NAMESPACE} -l app=ts-payment-service
    kubectl logs -n ${NAMESPACE} -l app=ts-payment-service --tail=10
}

# 函数：故障7 - 业务逻辑错误
test_business_logic_error() {
    echo "💥 模拟故障7: 业务逻辑实现错误"
    kubectl apply -f ${CHAOS_DIR}/chaos-business-logic-error.yaml
    echo "⏰ 等待 30 秒观察效果..."
    sleep 30
    echo "📊 检查服务状态:"
    kubectl get pods -n ${NAMESPACE} -l app=ts-order-service
    kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'
}

# 函数：故障8 - 镜像名称错误
test_image_error() {
    echo "🐳 模拟故障8: deployment 部署 image 名称拼写错误"
    kubectl apply -f ${CHAOS_DIR}/chaos-image-error.yaml
    echo "⏰ 等待 30 秒观察效果..."
    sleep 30
    echo "📊 检查服务状态:"
    kubectl get pods -n ${NAMESPACE} -l app=ts-order-service-broken
    kubectl describe pod -n ${NAMESPACE} -l app=ts-order-service-broken
}

# 函数：清理故障
cleanup_chaos() {
    echo "🧹 清理故障模拟..."
    kubectl delete -f ${CHAOS_DIR}/chaos-load-test.yaml --ignore-not-found
    kubectl delete -f ${CHAOS_DIR}/chaos-external-api-failure.yaml --ignore-not-found
    kubectl delete -f ${CHAOS_DIR}/chaos-business-logic-error.yaml --ignore-not-found
    kubectl delete -f ${CHAOS_DIR}/chaos-image-error.yaml --ignore-not-found
    echo "✅ 故障模拟已清理"
}

# 函数：显示帮助信息
show_help() {
    echo "Train-Ticket 故障模拟测试脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  1    模拟故障1: 异常大流量导致负载过高"
    echo "  2    模拟故障2: 依赖外部公共API服务不可用"
    echo "  7    模拟故障7: 业务逻辑实现错误"
    echo "  8    模拟故障8: deployment 部署 image 名称拼写错误"
    echo "  all  运行所有故障模拟"
    echo "  cleanup 清理所有故障模拟"
    echo "  help  显示此帮助信息"
    echo ""
}

# 主函数
main() {
    case "${1:-help}" in
        "1")
            check_chaos_mesh
            test_high_load
            ;;
        "2")
            check_chaos_mesh
            test_external_api_failure
            ;;
        "7")
            check_chaos_mesh
            test_business_logic_error
            ;;
        "8")
            test_image_error
            ;;
        "all")
            check_chaos_mesh
            echo "🔄 运行所有故障模拟..."
            test_high_load
            sleep 10
            test_external_api_failure
            sleep 10
            test_business_logic_error
            sleep 10
            test_image_error
            ;;
        "cleanup")
            cleanup_chaos
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 执行主函数
main "$@" 