package fdse.microservice.service;

import fdse.microservice.config.KubernetesConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1DaemonSetList;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kubernetes服务
 * 负责与K8s集群交互，管理部署的镜像更新
 */
@Slf4j
@Service
public class KubernetesService {

    private final AppsV1Api appsV1Api;
    private final CoreV1Api coreV1Api;
    private final KubernetesConfig kubernetesConfig;

    @Autowired
    public KubernetesService(KubernetesConfig kubernetesConfig) {
        this.kubernetesConfig = kubernetesConfig;
        AppsV1Api tempAppsApi = null;
        CoreV1Api tempCoreApi = null;
        
        try {
            // 检测是否在K8s集群内运行
            boolean isInCluster = isRunningInCluster();
            
            if (isInCluster) {
                log.info("检测到在K8s集群内运行，使用in-cluster配置");
                try {
                    ApiClient client = ClientBuilder.cluster().build();
                    Configuration.setDefaultApiClient(client);
                    tempAppsApi = new AppsV1Api(client);
                    tempCoreApi = new CoreV1Api(client);
                    log.info("使用in-cluster配置初始化Kubernetes客户端成功");
                } catch (Exception e) {
                    log.warn("in-cluster配置初始化失败: {}", e.getMessage());
                    // 回退到标准配置
                    fallbackToStandardConfig(tempAppsApi, tempCoreApi);
                }
            } else {
                log.info("检测到在集群外运行，尝试使用kubeconfig文件");
                // 尝试从配置的kubeconfig文件加载配置
                String kubeconfigPath = kubernetesConfig.getKubeconfigPath();
                File kubeconfigFile = new File(kubeconfigPath);
                
                if (kubeconfigFile.exists()) {
                    log.info("发现kubeconfig文件: {}", kubeconfigPath);
                    try (FileReader reader = new FileReader(kubeconfigFile)) {
                        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(reader)).build();
                        Configuration.setDefaultApiClient(client);
                        tempAppsApi = new AppsV1Api(client);
                        tempCoreApi = new CoreV1Api(client);
                        log.info("使用kubeconfig文件初始化Kubernetes客户端成功");
                    }
                } else {
                    log.info("未找到kubeconfig文件: {}，尝试使用标准配置", kubeconfigPath);
                    fallbackToStandardConfig(tempAppsApi, tempCoreApi);
                }
            }
        } catch (Exception e) {
            log.warn("初始化Kubernetes客户端失败，镜像管理功能将不可用: {}", e.getMessage());
        }
        
        this.appsV1Api = tempAppsApi;
        this.coreV1Api = tempCoreApi;
    }

    /**
     * 检测是否在K8s集群内运行
     * @return 是否在集群内
     */
    private boolean isRunningInCluster() {
        // 检查是否存在K8s集群内的标准环境变量和文件
        String kubeServiceHost = System.getenv("KUBERNETES_SERVICE_HOST");
        String kubeServicePort = System.getenv("KUBERNETES_SERVICE_PORT");
        
        // 检查是否存在ServiceAccount token文件
        File tokenFile = new File("/var/run/secrets/kubernetes.io/serviceaccount/token");
        File caFile = new File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");
        File namespaceFile = new File("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        
        boolean hasServiceHost = kubeServiceHost != null && !kubeServiceHost.isEmpty();
        boolean hasServicePort = kubeServicePort != null && !kubeServicePort.isEmpty();
        boolean hasTokenFile = tokenFile.exists() && tokenFile.canRead();
        boolean hasCaFile = caFile.exists() && caFile.canRead();
        boolean hasNamespaceFile = namespaceFile.exists() && namespaceFile.canRead();
        
        boolean inCluster = hasServiceHost && hasServicePort && hasTokenFile && hasCaFile && hasNamespaceFile;
        
        log.info("集群环境检测结果: KUBERNETES_SERVICE_HOST={}, KUBERNETES_SERVICE_PORT={}, " +
                "token文件存在={}, ca文件存在={}, namespace文件存在={}, 在集群内={}", 
                hasServiceHost, hasServicePort, hasTokenFile, hasCaFile, hasNamespaceFile, inCluster);
        
        return inCluster;
    }

    /**
     * 回退到标准配置
     * @param tempAppsApi 临时Apps API
     * @param tempCoreApi 临时Core API
     */
    private void fallbackToStandardConfig(AppsV1Api tempAppsApi, CoreV1Api tempCoreApi) {
        try {
            // 使用标准配置（从环境变量或默认位置加载）
            ApiClient client = ClientBuilder.standard().build();
            Configuration.setDefaultApiClient(client);
            tempAppsApi = new AppsV1Api(client);
            tempCoreApi = new CoreV1Api(client);
            log.info("使用标准配置初始化Kubernetes客户端成功");
        } catch (Exception e) {
            log.warn("标准配置初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 更新部署的镜像
     * @param namespace 命名空间
     * @param deploymentName 部署名称
     * @param imageName 新的镜像名称
     * @return 操作结果
     */
    public boolean updateDeploymentImage(String namespace, String deploymentName, String imageName) {
        if (appsV1Api == null) {
            log.error("Kubernetes客户端未初始化，无法更新镜像");
            return false;
        }

        try {
            log.info("开始更新部署镜像: namespace={}, deployment={}, image={}", 
                    namespace, deploymentName, imageName);

            // 获取当前部署
            V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                    deploymentName, namespace, null);

            if (deployment == null) {
                log.error("部署不存在: namespace={}, deployment={}", namespace, deploymentName);
                return false;
            }

            // 更新容器镜像
            V1DeploymentSpec spec = deployment.getSpec();
            if (spec != null) {
                V1PodTemplateSpec template = spec.getTemplate();
                if (template != null && template.getSpec() != null) {
                    List<V1Container> containers = template.getSpec().getContainers();
                    if (containers != null && !containers.isEmpty()) {
                        // 更新第一个容器的镜像
                        containers.get(0).setImage(imageName);
                        log.info("已更新容器镜像为: {}", imageName);
                    }
                }
            }

            // 应用更新
            appsV1Api.replaceNamespacedDeployment(
                    deploymentName, namespace, deployment, null, null, null, null);

            log.info("部署镜像更新成功: namespace={}, deployment={}, image={}", 
                    namespace, deploymentName, imageName);
            return true;

        } catch (ApiException e) {
            log.error("更新部署镜像失败: namespace={}, deployment={}, image={}, error={}", 
                    namespace, deploymentName, imageName, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("更新部署镜像时发生未知错误", e);
            return false;
        }
    }

    /**
     * 获取部署的当前镜像
     * @param namespace 命名空间
     * @param deploymentName 部署名称
     * @return 当前镜像名称，如果获取失败返回null
     */
    public String getDeploymentImage(String namespace, String deploymentName) {
        if (appsV1Api == null) {
            log.error("Kubernetes客户端未初始化，无法获取镜像信息");
            return null;
        }

        try {
            V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                    deploymentName, namespace, null);

            if (deployment != null && deployment.getSpec() != null) {
                V1PodTemplateSpec template = deployment.getSpec().getTemplate();
                if (template != null && template.getSpec() != null) {
                    List<V1Container> containers = template.getSpec().getContainers();
                    if (containers != null && !containers.isEmpty()) {
                        return containers.get(0).getImage();
                    }
                }
            }

            return null;
        } catch (ApiException e) {
            log.error("获取部署镜像失败: namespace={}, deployment={}, error={}", 
                    namespace, deploymentName, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("获取部署镜像时发生未知错误", e);
            return null;
        }
    }

    /**
     * 检查部署是否存在
     * @param namespace 命名空间
     * @param deploymentName 部署名称
     * @return 是否存在
     */
    public boolean deploymentExists(String namespace, String deploymentName) {
        if (appsV1Api == null) {
            log.error("Kubernetes客户端未初始化，无法检查部署存在性");
            return false;
        }

        try {
            appsV1Api.readNamespacedDeployment(deploymentName, namespace, null);
            return true;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            log.error("检查部署存在性失败: namespace={}, deployment={}, error={}", 
                    namespace, deploymentName, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("检查部署存在性时发生未知错误", e);
            return false;
        }
    }

    /**
     * 获取指定命名空间的workload列表
     * @param namespace 命名空间，如果为null则使用默认命名空间
     * @return workload列表
     */
    public List<K8sServiceInfo> getServices(String namespace) {
        if (appsV1Api == null) {
            log.error("Kubernetes客户端未初始化，无法获取workload列表");
            return new ArrayList<>();
        }

        if (namespace == null || namespace.trim().isEmpty()) {
            namespace = kubernetesConfig.getNamespace();
        }

        try {
            log.info("获取命名空间 {} 的workload列表", namespace);
            List<K8sServiceInfo> workloads = new ArrayList<>();
            
            // 获取Deployments
            V1DeploymentList deploymentList = appsV1Api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null, null);
            workloads.addAll(deploymentList.getItems().stream()
                    .map(this::convertDeploymentToK8sServiceInfo)
                    .collect(Collectors.toList()));
            
            // 获取StatefulSets
            V1StatefulSetList statefulSetList = appsV1Api.listNamespacedStatefulSet(namespace, null, null, null, null, null, null, null, null, null, null);
            workloads.addAll(statefulSetList.getItems().stream()
                    .map(this::convertStatefulSetToK8sServiceInfo)
                    .collect(Collectors.toList()));
            
            // 获取DaemonSets
            V1DaemonSetList daemonSetList = appsV1Api.listNamespacedDaemonSet(namespace, null, null, null, null, null, null, null, null, null, null);
            workloads.addAll(daemonSetList.getItems().stream()
                    .map(this::convertDaemonSetToK8sServiceInfo)
                    .collect(Collectors.toList()));
            
            return workloads;
                    
        } catch (ApiException e) {
            log.error("获取workload列表失败: namespace={}, error={}", namespace, e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取workload列表时发生未知错误", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有命名空间的workload列表
     * @return workload列表
     */
    public List<K8sServiceInfo> getAllServices() {
        if (appsV1Api == null) {
            log.error("Kubernetes客户端未初始化，无法获取workload列表");
            return new ArrayList<>();
        }

        try {
            log.info("获取所有命名空间的workload列表");
            List<K8sServiceInfo> workloads = new ArrayList<>();
            
            // 获取所有Deployments
            V1DeploymentList deploymentList = appsV1Api.listDeploymentForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
            workloads.addAll(deploymentList.getItems().stream()
                    .map(this::convertDeploymentToK8sServiceInfo)
                    .collect(Collectors.toList()));
            
            // 获取所有StatefulSets
            V1StatefulSetList statefulSetList = appsV1Api.listStatefulSetForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
            workloads.addAll(statefulSetList.getItems().stream()
                    .map(this::convertStatefulSetToK8sServiceInfo)
                    .collect(Collectors.toList()));
            
            // 获取所有DaemonSets
            V1DaemonSetList daemonSetList = appsV1Api.listDaemonSetForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
            workloads.addAll(daemonSetList.getItems().stream()
                    .map(this::convertDaemonSetToK8sServiceInfo)
                    .collect(Collectors.toList()));
            
            return workloads;
                    
        } catch (ApiException e) {
            log.error("获取所有workload列表失败: error={}", e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取所有workload列表时发生未知错误", e);
            return new ArrayList<>();
        }
    }

    /**
     * 将V1Service转换为K8sServiceInfo
     * @param service K8s服务对象
     * @return K8sServiceInfo对象
     */
    private K8sServiceInfo convertToK8sServiceInfo(V1Service service) {
        K8sServiceInfo serviceInfo = new K8sServiceInfo();
        serviceInfo.setName(service.getMetadata().getName());
        serviceInfo.setNamespace(service.getMetadata().getNamespace());
        serviceInfo.setServiceId(service.getMetadata().getName());
        serviceInfo.setServiceName(service.getMetadata().getName());
        
        // 获取服务类型
        if (service.getSpec() != null && service.getSpec().getType() != null) {
            serviceInfo.setType(service.getSpec().getType());
        }
        
        // 获取端口信息
        if (service.getSpec() != null && service.getSpec().getPorts() != null) {
            serviceInfo.setPorts(service.getSpec().getPorts().stream()
                    .map(port -> port.getPort() != null ? port.getPort().toString() : "unknown")
                    .collect(Collectors.joining(", ")));
        }
        
        return serviceInfo;
    }

    /**
     * 将V1Deployment转换为K8sServiceInfo
     * @param deployment V1Deployment对象
     * @return K8sServiceInfo对象
     */
    private K8sServiceInfo convertDeploymentToK8sServiceInfo(V1Deployment deployment) {
        K8sServiceInfo serviceInfo = new K8sServiceInfo();
        serviceInfo.setName(deployment.getMetadata().getName());
        serviceInfo.setNamespace(deployment.getMetadata().getNamespace());
        serviceInfo.setServiceId(deployment.getMetadata().getName());
        serviceInfo.setServiceName(deployment.getMetadata().getName());
        serviceInfo.setType("Deployment");
        return serviceInfo;
    }

    /**
     * 将V1StatefulSet转换为K8sServiceInfo
     * @param statefulSet V1StatefulSet对象
     * @return K8sServiceInfo对象
     */
    private K8sServiceInfo convertStatefulSetToK8sServiceInfo(V1StatefulSet statefulSet) {
        K8sServiceInfo serviceInfo = new K8sServiceInfo();
        serviceInfo.setName(statefulSet.getMetadata().getName());
        serviceInfo.setNamespace(statefulSet.getMetadata().getNamespace());
        serviceInfo.setServiceId(statefulSet.getMetadata().getName());
        serviceInfo.setServiceName(statefulSet.getMetadata().getName());
        serviceInfo.setType("StatefulSet");
        return serviceInfo;
    }

    /**
     * 将V1DaemonSet转换为K8sServiceInfo
     * @param daemonSet V1DaemonSet对象
     * @return K8sServiceInfo对象
     */
    private K8sServiceInfo convertDaemonSetToK8sServiceInfo(V1DaemonSet daemonSet) {
        K8sServiceInfo serviceInfo = new K8sServiceInfo();
        serviceInfo.setName(daemonSet.getMetadata().getName());
        serviceInfo.setNamespace(daemonSet.getMetadata().getNamespace());
        serviceInfo.setServiceId(daemonSet.getMetadata().getName());
        serviceInfo.setServiceName(daemonSet.getMetadata().getName());
        serviceInfo.setType("DaemonSet");
        return serviceInfo;
    }

    /**
     * K8s服务信息内部类
     */
    public static class K8sServiceInfo {
        private String name;
        private String namespace;
        private String serviceId;
        private String serviceName;
        private String type;
        private String ports;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        
        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getPorts() { return ports; }
        public void setPorts(String ports) { this.ports = ports; }
    }
} 