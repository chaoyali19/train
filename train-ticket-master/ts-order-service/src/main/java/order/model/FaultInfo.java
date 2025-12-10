package order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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