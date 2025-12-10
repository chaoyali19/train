package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 拓扑图节点模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopologyNode {
    private String id;
    private String name;
    private Double pureTime;
    private String pureRate;
    private Integer status;
    private String type;
} 