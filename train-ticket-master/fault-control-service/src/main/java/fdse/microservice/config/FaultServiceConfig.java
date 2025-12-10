package fdse.microservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 故障服务配置类
 * 从application.yml中读取故障服务配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "fault")
public class FaultServiceConfig {

    private DiscoveryConfig discovery;
    private List<ServiceConfig> services;

    @Data
    public static class DiscoveryConfig {
        private boolean enabled = true;
        private long scanInterval = 30000;
        private long timeout = 5000;
    }

    @Data
    public static class ServiceConfig {
        private String id;
        private String name;
    }
} 