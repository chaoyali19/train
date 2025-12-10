package order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 故障控制响应模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaultControlResponse {
    private boolean success;
    private String message;
    private String faultId;
    private boolean enabled;
} 