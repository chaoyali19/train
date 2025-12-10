package fdse.microservice.model;

import lombok.Data;

/**
 * 镜像更新请求模型
 */
@Data
public class ImageUpdateRequest {
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 新的镜像地址
     */
    private String imageUrl;
    
    /**
     * 命名空间（可选，默认为default）
     */
    private String namespace = "default";
    
    /**
     * 部署名称（可选，如果不提供则使用服务名称）
     */
    private String deploymentName;
} 