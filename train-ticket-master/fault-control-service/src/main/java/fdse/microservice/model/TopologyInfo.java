package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 拓扑信息模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopologyInfo {
    private String name;
    private String displayName;
    private String description;
} 