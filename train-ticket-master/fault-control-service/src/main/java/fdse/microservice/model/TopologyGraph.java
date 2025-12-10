package fdse.microservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 拓扑图完整模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopologyGraph {
    private String name;
    private String description;
    private List<TopologyNode> nodes;
    private List<TopologyEdge> edges;
} 