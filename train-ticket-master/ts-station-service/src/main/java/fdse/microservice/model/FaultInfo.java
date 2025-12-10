package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 故障信息模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaultInfo {
    private String id;
    private String name;
    private String description;
    private String type;
    private Long defaultDelay;
    private Integer defaultErrorCode;
    private Double defaultProbability;
} 