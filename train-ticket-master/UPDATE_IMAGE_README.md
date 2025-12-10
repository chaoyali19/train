# 使用 kubectl 更新 ts-station-service 镜像

## 概述

本方案使用 `kubectl patch` 命令直接更新 `ts-station-service` 的镜像标签，不会影响其他Deployment配置。

## 方法1: 使用 YAML 文件（推荐）

### 1. 创建更新文件

文件 `update-station-service-image.yaml` 已经创建，内容如下：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ts-station-service
  namespace: chaos
spec:
  template:
    spec:
      containers:
      - name: ts-station-service
        image: harbor.cloudwise.com/noname/ts-station-service:aa555050-fault
```

### 2. 执行更新

```bash
# 应用更新
kubectl apply -f update-station-service-image.yaml

# 或者使用 patch 命令
kubectl patch deployment ts-station-service -n chaos --patch-file update-station-service-image.yaml
```

### 3. 验证更新

```bash
# 检查镜像是否更新
kubectl get deployment ts-station-service -n chaos -o jsonpath='{.spec.template.spec.containers[0].image}'

# 检查 Pod 状态
kubectl get pods -n chaos -l app=ts-station-service

# 等待部署完成
kubectl rollout status deployment/ts-station-service -n chaos
```

## 方法2: 使用 kubectl patch 命令

### 1. 直接使用 patch 命令

```bash
# 更新镜像标签
kubectl patch deployment ts-station-service -n chaos \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"ts-station-service","image":"harbor.cloudwise.com/noname/ts-station-service:aa555050-fault"}]}}}}'
```

### 2. 使用 JSON patch

```bash
# 创建 patch 文件
cat > patch.json << EOF
{
  "spec": {
    "template": {
      "spec": {
        "containers": [
          {
            "name": "ts-station-service",
            "image": "harbor.cloudwise.com/noname/ts-station-service:aa555050-fault"
          }
        ]
      }
    }
  }
}
EOF

# 应用 patch
kubectl patch deployment ts-station-service -n chaos --patch-file patch.json
```

## 方法3: 使用 kubectl set image 命令

```bash
# 最简单的方式
kubectl set image deployment/ts-station-service ts-station-service=harbor.cloudwise.com/noname/ts-station-service:aa555050-fault -n chaos
```

## 验证和监控

### 1. 检查更新状态

```bash
# 查看部署状态
kubectl rollout status deployment/ts-station-service -n chaos

# 查看部署历史
kubectl rollout history deployment/ts-station-service -n chaos

# 查看 Pod 事件
kubectl describe pod -n chaos -l app=ts-station-service
```

### 2. 验证镜像版本

```bash
# 检查当前镜像
kubectl get deployment ts-station-service -n chaos -o jsonpath='{.spec.template.spec.containers[0].image}'

# 检查其他服务镜像（应该保持不变）
kubectl get deployment ts-order-service -n chaos -o jsonpath='{.spec.template.spec.containers[0].image}'
```

### 3. 验证故障注入功能

```bash
# 获取服务地址
SERVICE_IP=$(kubectl get service ts-station-service -n chaos -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
SERVICE_PORT=$(kubectl get service ts-station-service -n chaos -o jsonpath='{.spec.ports[0].port}')

# 检查故障控制接口
curl "http://$SERVICE_IP:$SERVICE_PORT/api/v1/stationservice/fault/status"

# 启用空数据故障
curl -X POST "http://$SERVICE_IP:$SERVICE_PORT/api/v1/stationservice/fault/enableEmptyStationQuery?enable=true"

# 测试站点查询接口
curl "http://$SERVICE_IP:$SERVICE_PORT/api/v1/stationservice/stations"
```

## 回滚操作

如果需要回滚到之前的版本：

```bash
# 查看部署历史
kubectl rollout history deployment/ts-station-service -n chaos

# 回滚到上一个版本
kubectl rollout undo deployment/ts-station-service -n chaos

# 回滚到指定版本
kubectl rollout undo deployment/ts-station-service -n chaos --to-revision=2
```

## 批量更新脚本

创建一个批量更新脚本 `update-image.sh`：

```bash
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
```

使用方法：

```bash
chmod +x update-image.sh
./update-image.sh
```

## 注意事项

1. **镜像存在性**: 确保新的镜像标签 `aa555050-fault` 已经构建并推送到镜像仓库
2. **命名空间**: 确保在正确的命名空间 `chaos` 中执行命令
3. **权限**: 确保有足够的权限更新 Deployment
4. **影响范围**: 此操作只会更新 `ts-station-service`，不会影响其他服务
5. **回滚准备**: 建议在更新前记录当前的镜像版本，以便需要时回滚

## 故障排查

### 常见问题

1. **镜像拉取失败**
   ```bash
   # 检查 Pod 事件
   kubectl describe pod -n chaos -l app=ts-station-service
   
   # 检查镜像是否存在
   docker pull harbor.cloudwise.com/noname/ts-station-service:aa555050-fault
   ```

2. **部署卡住**
   ```bash
   # 查看部署状态
   kubectl rollout status deployment/ts-station-service -n chaos
   
   # 查看 Pod 日志
   kubectl logs -n chaos -l app=ts-station-service
   ```

3. **权限问题**
   ```bash
   # 检查当前用户权限
   kubectl auth can-i patch deployment -n chaos
   
   # 检查 RBAC 配置
   kubectl get rolebinding -n chaos
   ```

### 调试命令

```bash
# 查看完整的 Deployment 配置
kubectl get deployment ts-station-service -n chaos -o yaml

# 查看 Pod 详细信息
kubectl describe pod -n chaos -l app=ts-station-service

# 查看服务日志
kubectl logs -n chaos -l app=ts-station-service --tail=100 -f
``` 