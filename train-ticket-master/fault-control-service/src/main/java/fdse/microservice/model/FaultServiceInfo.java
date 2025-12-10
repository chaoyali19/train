package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 故障服务信息模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaultServiceInfo {
    private String serviceName;
    private String serviceId;
    private String description;
    private String version;
    private List<FaultInfo> faults;
} 