package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String chaosName;
    private String serviceName;
    private String faultType;
    private int delaySeconds;
    private int durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // "active", "expired", "stopped"
} 