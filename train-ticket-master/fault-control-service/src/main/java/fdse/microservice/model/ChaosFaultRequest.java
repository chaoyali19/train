package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Chaos Mesh 故障注入请求模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChaosFaultRequest {
    private String serviceName;
    private String faultType;
    private Integer delaySeconds;
    private Integer durationMinutes;
    private String targetService; // 目标服务（被延迟的服务）
    private String sourceService; // 源服务（发起延迟的服务）
    private Boolean delayUpstream; // 是否延迟上游流量
} 