package fdse.microservice.service;

import fdse.microservice.model.ImageUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 镜像管理服务
 * 负责处理镜像更新相关的业务逻辑
 */
@Slf4j
@Service
public class ImageManagementService {

    @Autowired
    private KubernetesService kubernetesService;

    /**
     * 更新服务镜像
     * @param request 镜像更新请求
     * @return 操作结果
     */
    public boolean updateServiceImage(ImageUpdateRequest request) {
        try {
            log.info("开始更新服务镜像: service={}, image={}", 
                    request.getServiceName(), request.getImageUrl());

            // 验证请求参数
            if (request.getServiceName() == null || request.getServiceName().trim().isEmpty()) {
                log.error("服务名称不能为空");
                return false;
            }

            if (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
                log.error("镜像地址不能为空");
                return false;
            }

            // 确定部署名称
            String deploymentName = request.getDeploymentName();
            if (deploymentName == null || deploymentName.trim().isEmpty()) {
                deploymentName = request.getServiceName();
            }

            // 确定命名空间
            String namespace = request.getNamespace();
            if (namespace == null || namespace.trim().isEmpty()) {
                namespace = "default";
            }

            // 检查部署是否存在
            if (!kubernetesService.deploymentExists(namespace, deploymentName)) {
                log.error("部署不存在: namespace={}, deployment={}", namespace, deploymentName);
                return false;
            }

            // 获取当前镜像
            String currentImage = kubernetesService.getDeploymentImage(namespace, deploymentName);
            log.info("当前镜像: {}", currentImage);

            // 更新镜像
            boolean success = kubernetesService.updateDeploymentImage(namespace, deploymentName, request.getImageUrl());
            
            if (success) {
                log.info("服务镜像更新成功: service={}, oldImage={}, newImage={}", 
                        request.getServiceName(), currentImage, request.getImageUrl());
            } else {
                log.error("服务镜像更新失败: service={}, image={}", 
                        request.getServiceName(), request.getImageUrl());
            }

            return success;

        } catch (Exception e) {
            log.error("更新服务镜像时发生异常: service={}, image={}", 
                    request.getServiceName(), request.getImageUrl(), e);
            return false;
        }
    }

    /**
     * 获取服务当前镜像
     * @param serviceName 服务名称
     * @param namespace 命名空间
     * @return 当前镜像地址
     */
    public String getServiceImage(String serviceName, String namespace) {
        try {
            if (namespace == null || namespace.trim().isEmpty()) {
                namespace = "default";
            }

            return kubernetesService.getDeploymentImage(namespace, serviceName);
        } catch (Exception e) {
            log.error("获取服务镜像失败: service={}, namespace={}", serviceName, namespace, e);
            return null;
        }
    }

    /**
     * 验证镜像地址格式
     * @param imageUrl 镜像地址
     * @return 是否有效
     */
    public boolean validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }

        // 简单的镜像地址格式验证
        // 格式: registry/repository:tag 或 repository:tag
        String trimmedUrl = imageUrl.trim();
        
        // 检查是否包含冒号（标签）
        if (!trimmedUrl.contains(":")) {
            return false;
        }

        // 检查是否包含斜杠（仓库路径）
        if (!trimmedUrl.contains("/")) {
            return false;
        }

        return true;
    }

    /**
     * 获取K8s服务列表
     * @param namespace 命名空间，如果为null则获取所有命名空间的服务
     * @return K8s服务列表
     */
    public List<KubernetesService.K8sServiceInfo> getK8sServices(String namespace) {
        try {
            if (namespace == null || namespace.trim().isEmpty()) {
                return kubernetesService.getAllServices();
            } else {
                return kubernetesService.getServices(namespace);
            }
        } catch (Exception e) {
            log.error("获取K8s服务列表失败: namespace={}", namespace, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取指定命名空间的服务列表
     * @param namespace 命名空间
     * @return 服务列表
     */
    public List<KubernetesService.K8sServiceInfo> getServicesByNamespace(String namespace) {
        return getK8sServices(namespace);
    }
} 