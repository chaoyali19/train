package fdse.microservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ChaosMesh 配置类
 * 管理故障注入相关的配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "chaos.mesh")
public class ChaosMeshConfig {
    
    /**
     * Kubernetes 命名空间
     */
    private String namespace = "chaos";
    
    /**
     * JVM 故障注入端口
     */
    private Integer jvmPort = 9277;
    
    /**
     * 故障注入超时时间（毫秒）
     */
    private Long timeout = 30000L;
    
    /**
     * 是否启用故障注入
     */
    private Boolean enabled = true;
} 