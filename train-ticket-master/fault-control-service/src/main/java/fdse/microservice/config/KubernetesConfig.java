package fdse.microservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kubernetes配置类
 * 从application.yml中读取Kubernetes相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "kubernetes")
public class KubernetesConfig {
    
    /**
     * kubeconfig文件路径
     */
    private String kubeconfigPath;
    
    /**
     * 默认命名空间
     */
    private String namespace;
} 