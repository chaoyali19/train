#!/bin/bash

# 设置参数
NAMESPACE="chaos"
SERVICE_NAME="ts-station-service"
NEW_IMAGE="harbor.cloudwise.com/noname/ts-station-service:aa555050-fault"

echo "开始更新 $SERVICE_NAME 镜像..."

# 检查部署是否存在
if ! kubectl get deployment $SERVICE_NAME -n $NAMESPACE &> /dev/null; then
    echo "错误: 部署 $SERVICE_NAME 在命名空间 $NAMESPACE 中不存在"
    exit 1
fi

# 获取当前镜像
CURRENT_IMAGE=$(kubectl get deployment $SERVICE_NAME -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}')
echo "当前镜像: $CURRENT_IMAGE"
echo "目标镜像: $NEW_IMAGE"

# 检查是否需要更新
if [[ "$CURRENT_IMAGE" == "$NEW_IMAGE" ]]; then
    echo "镜像已经是目标版本，无需更新"
    exit 0
fi

# 更新镜像
echo "正在更新镜像..."
kubectl set image deployment/$SERVICE_NAME $SERVICE_NAME=$NEW_IMAGE -n $NAMESPACE

# 等待部署完成
echo "等待部署完成..."
kubectl rollout status deployment/$SERVICE_NAME -n $NAMESPACE --timeout=300s

if [[ $? -eq 0 ]]; then
    echo "✅ 镜像更新成功"
    
    # 验证更新结果
    UPDATED_IMAGE=$(kubectl get deployment $SERVICE_NAME -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}')
    echo "更新后镜像: $UPDATED_IMAGE"
    
    # 显示 Pod 状态
    echo "Pod 状态:"
    kubectl get pods -n $NAMESPACE -l app=$SERVICE_NAME
else
    echo "❌ 镜像更新失败"
    exit 1
fi 