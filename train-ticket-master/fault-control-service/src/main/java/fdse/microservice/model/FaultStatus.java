package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

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
    @Builder.Default
    private boolean reachable = false;
    @Builder.Default
    private List<FaultState> faults = new ArrayList<>();
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FaultState {
        private String id;
        @Builder.Default
        private boolean enabled = false;
        private Long delayMs;
        private Integer errorCode;
        private Double probability;
    }
} 