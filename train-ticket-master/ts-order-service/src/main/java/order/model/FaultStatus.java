package order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 故障状态模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaultStatus {
    private String serviceId;
    private String serviceName;
    private String status;
    private String details;
    private long timestamp;
    private boolean reachable;
    private List<FaultState> faults;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FaultState {
        private String id;
        private boolean enabled;
        private Long delayMs;
        private Integer errorCode;
        private Double probability;
    }
} 