package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 拓扑图边模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopologyEdge {
    private String source;
    private String target;
} 