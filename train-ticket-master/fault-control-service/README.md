# 故障控制服务 (Fault Control Service)

一个用于管理和控制各种微服务故障注入的Web应用，支持自动发现服务、故障状态监控和Kubernetes镜像管理。

## 功能特性

- 🔍 **自动服务发现**：自动发现和监控配置的微服务
- 🎛️ **故障控制**：通过Web界面控制各种故障注入（延迟、错误码、概率等）
- 🖼️ **镜像管理**：支持通过K8s更新服务镜像
- 📊 **实时监控**：实时显示服务状态和故障信息
- 🔄 **自动刷新**：定期自动刷新服务状态
- 🌐 **REST API**：提供完整的API接口
- 🚀 **动态Workload列表**：从K8s集群动态获取workload列表（Deployment、StatefulSet、DaemonSet）
- 🔍 **智能搜索**：支持在workload列表中搜索，快速定位目标workload

## 快速开始

### 前置要求

- Java 11+
- Maven 3.6+
- Kubernetes集群（用于镜像管理功能）
- kubeconfig文件（用于连接K8s集群）

### 1. 克隆项目

```bash
git clone <repository-url>
cd fault-control-service
```

### 2. 配置Kubernetes

#### 集群内运行（推荐）
如果应用以Pod方式运行在K8s集群内，系统会自动检测并使用in-cluster配置，无需额外配置。

#### 集群外运行
如果应用在集群外运行，将您的kubeconfig文件复制到项目根目录：

```bash
cp /path/to/your/kubeconfig kube.conf
```

### Kubernetes配置

在 `application.yml` 中配置Kubernetes相关设置：

```yaml
kubernetes:
  kubeconfig-path: /opt/github/train-ticket/fault-control-service/kube.conf  # kubeconfig文件路径
  namespace: chaos  # 默认命名空间
```

**注意**：kubeconfig-path配置仅在集群外运行时使用，集群内运行时会自动忽略此配置。

系统会自动检测运行环境：
- **集群内运行**：自动使用in-cluster配置（ServiceAccount token）
- **集群外运行**：使用application.yml中配置的kubeconfig文件或标准配置

### 3. 启动应用

```bash
mvn spring-boot:run
```

### 4. 访问Web界面

打开浏览器访问：http://localhost:8080

## 配置说明

### 服务配置

在 `application.yml` 中配置要监控的服务：

```yaml
fault:
  services:
    - id: "ts-station-service"
      name: "站点服务"
    - id: "ts-user-service"
      name: "用户服务"
    - id: "ts-order-service"
      name: "订单服务"
```

### Kubernetes配置

```yaml
kubernetes:
  kubeconfig-path: /opt/github/train-ticket/fault-control-service/kube.conf
  namespace: chaos
```

## 使用方法

### 故障控制

1. 访问Web界面
2. 在服务列表中找到要控制的服务
3. 选择故障类型（延迟、错误码、概率等）
4. 设置参数值
5. 点击"启用"或"禁用"按钮

### 镜像管理

1. 在"服务镜像管理"区域选择命名空间（默认选择chaos，也可选择default、kube-system等）
2. 系统会自动从K8s集群获取该命名空间的workload列表
3. 在workload下拉框中输入关键词进行搜索，快速找到目标workload
4. 从搜索结果中选择要更新的workload
5. 输入新的镜像地址（格式：`registry/repository:tag`）
6. 点击"更新镜像"按钮

## API接口

### 故障控制API

```bash
# 获取所有服务状态
GET /api/status

# 获取服务信息
GET /api/info

# 控制故障
POST /api/control
```

### 镜像管理API

```bash
# 获取指定命名空间的服务列表
GET /api/k8s-services/{namespace}

# 获取所有命名空间的服务列表
GET /api/k8s-services

# 获取服务当前镜像
GET /api/service-image?serviceName=ts-station-service&namespace=default

# 更新服务镜像
POST /api/update-image
Content-Type: application/json

{
  "serviceName": "ts-station-service",
  "imageUrl": "registry.example.com/ts-station-service:v1.2.3",
  "namespace": "default"
}
```

## 测试

运行测试脚本验证功能：

```bash
# 完整功能测试
chmod +x test-k8s-local.sh
./test-k8s-local.sh

# K8s服务列表功能测试
chmod +x test-k8s-services.sh
./test-k8s-services.sh
```

## 部署到Kubernetes

### 权限要求

故障控制服务需要以下Kubernetes权限来管理镜像更新：

**核心资源权限**：
- **nodes, namespaces, configmaps, services, pods, replicationcontrollers** - 读取权限
- **daemonsets, deployments, replicasets, statefulsets** - 读取和更新权限（用于镜像管理）

**网络资源权限**：
- **ingresses** - 读取权限
- **routes** (OpenShift) - 读取权限

这些权限通过ServiceAccount、ClusterRole和ClusterRoleBinding自动配置，遵循最小权限原则。

### 部署步骤

1. **应用RBAC配置**：
```bash
kubectl apply -f k8s-deployment.yaml
```

2. **验证部署**：
```bash
kubectl get pods -n chaos
kubectl get svc -n chaos
```

3. **访问服务**：
```bash
# 通过NodePort访问
kubectl get svc fault-control-service -n chaos
# 或者端口转发
kubectl port-forward svc/fault-control-service 8080:8080 -n chaos
```

## 故障排除

## 压测功能

### 概述

控制平台集成了压测功能，可以通过Web界面直接触发Python压测程序进行压力测试。

### 功能特点

- **可视化配置**: 通过Web界面配置压测参数
- **实时监控**: 实时查看压测任务状态和输出日志
- **多场景支持**: 支持多种压测场景（高铁票查询、普通列车查询等）
- **任务管理**: 可以启动、停止和监控多个压测任务

### 压测场景

支持以下压测场景：

- `high_speed`: 高铁票查询
- `normal`: 普通列车票查询
- `food`: 食品查询
- `parallel`: 并行车票查询
- `pay`: 查询并支付订单
- `cancel`: 查询并取消订单
- `consign`: 查询并添加托运信息

### 使用方法

1. **访问压测页面**
   ```
   http://localhost:8080/stress
   ```

2. **配置压测参数**
   - 选择压测场景
   - 设置并发数（1-100）
   - 设置总请求数（1-10000）

3. **启动压测**
   - 点击"启动压测"按钮
   - 系统会异步执行压测任务

4. **监控任务状态**
   - 实时查看任务运行状态
   - 查看输出日志和错误信息
   - 可以随时停止运行中的任务

### 配置说明

在 `application.yml` 中配置Python压测程序路径：

```yaml
stress:
  test:
    python:
      path: "../train-ticket-auto-query"  # Python项目路径
    venv:
      path: "../train-ticket-auto-query/.venv/bin/python"  # Python虚拟环境路径
```

### API接口

#### 1. 启动压测任务
```http
POST /stress/start
Content-Type: application/x-www-form-urlencoded

scenario=high_speed&concurrent=10&count=100
```

#### 2. 停止压测任务
```http
POST /stress/stop
Content-Type: application/x-www-form-urlencoded

taskId=task_1234567890_123
```

#### 3. 获取任务状态
```http
GET /stress/status
GET /stress/status?taskId=task_1234567890_123
```

#### 4. 获取可用场景
```http
GET /stress/scenarios
```

### 测试压测功能

使用提供的测试脚本验证压测功能：

```bash
./test-stress.sh
```

### 注意事项

1. **Python环境**: 确保Python压测程序环境配置正确
2. **依赖安装**: 确保Python项目已安装所需依赖
3. **服务状态**: 确保Train-Ticket系统服务正常运行
4. **路径配置**: 确保application.yml中的Python路径配置正确

### 2. 启用延迟故障
### K8s连接问题

1. **集群内运行检查**：
   - 确认Pod有正确的ServiceAccount权限
   - 检查RBAC配置是否正确
   - 验证ServiceAccount token是否有效

2. **集群外运行检查**：
   - 检查kubeconfig文件是否存在
   - 验证集群连接：`kubectl cluster-info`
   - 确认文件权限正确

3. **环境检测**：
   - 查看应用日志中的"集群环境检测结果"
   - 确认KUBERNETES_SERVICE_HOST和KUBERNETES_SERVICE_PORT环境变量
   - 检查ServiceAccount相关文件是否存在

### 服务连接问题

1. 检查服务是否正在运行
2. 验证网络连接
3. 查看应用日志

## 开发

### 构建项目

```bash
mvn clean package
```

### 运行测试

```bash
mvn test
```

## 许可证

[许可证信息]

## 贡献

欢迎提交Issue和Pull Request！ 