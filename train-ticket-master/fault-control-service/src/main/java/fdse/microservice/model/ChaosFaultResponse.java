package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Chaos Mesh 故障注入响应模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChaosFaultResponse {
    private boolean success;
    private String message;
    private String chaosName;
    private String serviceName;
} 