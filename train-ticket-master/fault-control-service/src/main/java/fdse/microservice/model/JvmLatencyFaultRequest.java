package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * JVM延时故障注入请求模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JvmLatencyFaultRequest {
    private String serviceName;
    private String interfaceName;
    private String methodName;
    private String httpMethod;
    private String className;
    private Integer durationMinutes;
    private Integer latencyMs;
    private Double probability;
} 