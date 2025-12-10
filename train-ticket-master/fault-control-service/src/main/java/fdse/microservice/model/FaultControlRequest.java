package fdse.microservice.model;

import lombok.Data;

/**
 * 故障控制请求模型
 */
@Data
public class FaultControlRequest {
    private String faultId;
    private boolean enable;
    private Long delayMs;
    private Integer errorCode;
    private Double probability;
} 