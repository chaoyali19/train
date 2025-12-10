package fdse.microservice.config;

import fdse.microservice.model.TopologyGraph;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Topology configuration class
 * Used to manage topology graph configuration information
 */
@Data
@Component
@ConfigurationProperties(prefix = "topology")
public class TopologyConfig {

    /**
     * Topology graph configuration list
     */
    private List<TopologyDefinition> definitions;

    /**
     * Topology graph definition
     */
    @Data
    public static class TopologyDefinition {
        /**
         * Topology graph name
         */
        private String name;
        
        /**
         * Topology graph description
         */
        private String description;
        
        /**
         * Node configuration
         */
        private List<NodeConfig> nodes;
        
        /**
         * Edge configuration
         */
        private List<EdgeConfig> edges;
    }

    /**
     * Node configuration
     */
    @Data
    public static class NodeConfig {
        /**
         * Node ID
         */
        private String id;
        
        /**
         * Node name
         */
        private String name;
        
        /**
         * Node type (browser, java, MYSQL)
         */
        private String type;
        
        /**
         * Response time (milliseconds)
         */
        private Integer pureTime;
        
        /**
         * Proportion
         */
        private String pureRate;
        
        /**
         * Status (0: normal, 1: abnormal)
         */
        private Integer status = 0;
    }

    /**
     * Edge configuration
     */
    @Data
    public static class EdgeConfig {
        /**
         * Source node ID
         */
        private String source;
        
        /**
         * Target node ID
         */
        private String target;
    }
} 