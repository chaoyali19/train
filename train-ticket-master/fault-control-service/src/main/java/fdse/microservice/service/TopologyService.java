package fdse.microservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fdse.microservice.config.TopologyConfig;
import fdse.microservice.model.TopologyGraph;
import fdse.microservice.model.TopologyNode;
import fdse.microservice.model.TopologyEdge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拓扑图服务
 * 管理拓扑图数据和配置
 */
@Slf4j
@Service
public class TopologyService {

    @Autowired
    private TopologyConfig topologyConfig;

    @Autowired
    private ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, TopologyGraph> topologyGraphs = new ConcurrentHashMap<>();

    /**
     * 获取所有拓扑图名称
     */
    public List<String> getAllTopologyNames() {
        return new ArrayList<>(topologyGraphs.keySet());
    }

    /**
     * 获取指定拓扑图
     */
    public TopologyGraph getTopologyGraph(String name) {
        return topologyGraphs.get(name);
    }

    /**
     * 保存拓扑图
     */
    public void saveTopologyGraph(String name, TopologyGraph graph) {
        topologyGraphs.put(name, graph);
        log.info("保存拓扑图: {}, 节点数: {}, 边数: {}", name, 
                graph.getNodes() != null ? graph.getNodes().size() : 0,
                graph.getEdges() != null ? graph.getEdges().size() : 0);
    }

    /**
     * 删除拓扑图
     */
    public boolean deleteTopologyGraph(String name) {
        TopologyGraph removed = topologyGraphs.remove(name);
        if (removed != null) {
            log.info("删除拓扑图: {}", name);
            return true;
        }
        return false;
    }

    /**
     * 创建示例拓扑图
     */
    public void createSampleTopology() {
        // 创建示例拓扑图数据
        List<TopologyNode> nodes = new ArrayList<>();
        List<TopologyEdge> edges = new ArrayList<>();

        // 添加节点
        nodes.add(TopologyNode.builder()
                .id("ffffffff-aab8-bce5-ffff-ffffc52b8dcf")
                .name("chaos_ts-food-service")
                .pureTime(2986.0)
                .pureRate("0.0825")
                .status(0)
                .type("java")
                .build());

        nodes.add(TopologyNode.builder()
                .id("ffffffff-fcc6-3abb-0000-000074fd8c25")
                .name("chaos_ts-travel-service")
                .pureTime(1104.0)
                .pureRate("0.0305")
                .status(0)
                .type("java")
                .build());

        nodes.add(TopologyNode.builder()
                .id("00000000-1f14-e1f7-ffff-ffffe4462a32")
                .name("chaos_ts-route-service")
                .pureTime(1028.0)
                .pureRate("0.0284")
                .status(0)
                .type("java")
                .build());

        nodes.add(TopologyNode.builder()
                .id("00000000-23fa-1d1b-0000-00007a7950ed")
                .name("MYSQL:mysql:3306")
                .pureTime(560.0)
                .pureRate("0.0154")
                .status(0)
                .type("MYSQL")
                .build());

        nodes.add(TopologyNode.builder()
                .id("ffffffff-8eb4-a2f5-ffff-ffffd7056d83")
                .name("MYSQL:mysql:3306")
                .pureTime(213.0)
                .pureRate("0.0059")
                .status(0)
                .type("MYSQL")
                .build());

        nodes.add(TopologyNode.builder()
                .id("00000000-0bde-017e-0000-000016d4d176")
                .name("chaos_ts-station-food-service")
                .pureTime(1213.0)
                .pureRate("0.0335")
                .status(0)
                .type("java")
                .build());

        nodes.add(TopologyNode.builder()
                .id("00000000-2af9-486c-0000-00006f0391ad")
                .name("MYSQL:mysql:3306")
                .pureTime(983.0)
                .pureRate("0.0272")
                .status(0)
                .type("MYSQL")
                .build());

        nodes.add(TopologyNode.builder()
                .id("ffffffff-c61a-76d9-ffff-ffffe412ac9f")
                .name("chaos_ts-train-food-service")
                .pureTime(26155.0)
                .pureRate("0.7226")
                .status(0)
                .type("java")
                .build());

        nodes.add(TopologyNode.builder()
                .id("00000000-1bcf-bcfc-0000-000014e866b6")
                .name("MYSQL:mysql:3306")
                .pureTime(369.0)
                .pureRate("0.0102")
                .status(0)
                .type("MYSQL")
                .build());

        nodes.add(TopologyNode.builder()
                .id("browser")
                .name("browser")
                .pureTime(null)
                .pureRate(null)
                .status(0)
                .type("browser")
                .build());

        // 添加边
        edges.add(TopologyEdge.builder()
                .source("00000000-1f14-e1f7-ffff-ffffe4462a32")
                .target("00000000-23fa-1d1b-0000-00007a7950ed")
                .build());

        edges.add(TopologyEdge.builder()
                .source("ffffffff-fcc6-3abb-0000-000074fd8c25")
                .target("00000000-1f14-e1f7-ffff-ffffe4462a32")
                .build());

        edges.add(TopologyEdge.builder()
                .source("ffffffff-fcc6-3abb-0000-000074fd8c25")
                .target("ffffffff-8eb4-a2f5-ffff-ffffd7056d83")
                .build());

        edges.add(TopologyEdge.builder()
                .source("ffffffff-aab8-bce5-ffff-ffffc52b8dcf")
                .target("ffffffff-fcc6-3abb-0000-000074fd8c25")
                .build());

        edges.add(TopologyEdge.builder()
                .source("ffffffff-aab8-bce5-ffff-ffffc52b8dcf")
                .target("00000000-0bde-017e-0000-000016d4d176")
                .build());

        edges.add(TopologyEdge.builder()
                .source("00000000-0bde-017e-0000-000016d4d176")
                .target("00000000-2af9-486c-0000-00006f0391ad")
                .build());

        edges.add(TopologyEdge.builder()
                .source("ffffffff-c61a-76d9-ffff-ffffe412ac9f")
                .target("00000000-1bcf-bcfc-0000-000014e866b6")
                .build());

        edges.add(TopologyEdge.builder()
                .source("ffffffff-aab8-bce5-ffff-ffffc52b8dcf")
                .target("ffffffff-c61a-76d9-ffff-ffffe412ac9f")
                .build());

        edges.add(TopologyEdge.builder()
                .source("browser")
                .target("ffffffff-aab8-bce5-ffff-ffffc52b8dcf")
                .build());

        TopologyGraph graph = TopologyGraph.builder()
                .nodes(nodes)
                .edges(edges)
                .build();

        saveTopologyGraph("示例拓扑图", graph);
    }

    /**
     * 初始化默认拓扑图
     */
    public void initializeDefaultTopologies() {
        if (topologyGraphs.isEmpty()) {
            loadTopologiesFromConfig();
        }
    }

    /**
     * 从配置文件加载拓扑图
     */
    private void loadTopologiesFromConfig() {
        try {
            // 尝试从JSON文件加载拓扑
            loadTopologiesFromJsonFiles();
            
            // 如果没有加载到任何拓扑，尝试从YAML配置加载
            if (topologyGraphs.isEmpty() && topologyConfig.getDefinitions() != null) {
                for (TopologyConfig.TopologyDefinition definition : topologyConfig.getDefinitions()) {
                    try {
                        TopologyGraph graph = convertDefinitionToGraph(definition);
                        saveTopologyGraph(definition.getName(), graph);
                        log.info("成功加载拓扑图: {}", definition.getName());
                    } catch (Exception e) {
                        log.error("加载拓扑图失败: {}", definition.getName(), e);
                    }
                }
            }
            
            // 如果仍然没有拓扑，使用示例拓扑
            if (topologyGraphs.isEmpty()) {
                log.warn("没有找到拓扑配置，使用示例拓扑");
                createSampleTopology();
            }
        } catch (Exception e) {
            log.error("加载拓扑配置失败", e);
            createSampleTopology();
        }
    }

    /**
     * 从JSON文件加载拓扑图
     */
    private void loadTopologiesFromJsonFiles() {
        try {
            // 使用ClassPathResource来加载JAR包中的资源
            Resource topologiesDir = resourceLoader.getResource("classpath:topologies/");
            
            // 检查是否是JAR包中的资源
            if (topologiesDir.getURL().getProtocol().equals("jar")) {
                // JAR包中的资源，使用不同的加载方式
                loadTopologiesFromJar(topologiesDir);
            } else if (topologiesDir.exists() && topologiesDir.getFile().isDirectory()) {
                // 文件系统中的资源
                File[] files = topologiesDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File file : files) {
                        try {
                            TopologyGraph graph = objectMapper.readValue(file, TopologyGraph.class);
                            String name = file.getName().replace(".json", "");
                            saveTopologyGraph(name, graph);
                            log.info("成功加载拓扑图文件: {}", file.getName());
                        } catch (Exception e) {
                            log.error("加载拓扑图文件失败: {}", file.getName(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("无法从JSON文件加载拓扑图: {}", e.getMessage());
        }
    }

    /**
     * 从JAR包中加载拓扑图文件
     */
    private void loadTopologiesFromJar(Resource topologiesDir) {
        try {
            // 使用ClassPathResource来获取classpath下的资源
            org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver = 
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver(resourceLoader);
            
            Resource[] resources = resolver.getResources("classpath:topologies/*.json");
            
            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename != null && filename.endsWith(".json")) {
                        TopologyGraph graph = objectMapper.readValue(resource.getInputStream(), TopologyGraph.class);
                        String name = filename.replace(".json", "");
                        saveTopologyGraph(name, graph);
                        log.info("成功加载拓扑图文件: {}", filename);
                    }
                } catch (Exception e) {
                    log.error("加载拓扑图文件失败: {}", resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.warn("从JAR包加载拓扑图失败: {}", e.getMessage());
        }
    }

    /**
     * 将配置定义转换为拓扑图
     */
    private TopologyGraph convertDefinitionToGraph(TopologyConfig.TopologyDefinition definition) {
        List<TopologyNode> nodes = new ArrayList<>();
        List<TopologyEdge> edges = new ArrayList<>();

        // 转换节点
        if (definition.getNodes() != null) {
            for (TopologyConfig.NodeConfig nodeConfig : definition.getNodes()) {
                TopologyNode node = TopologyNode.builder()
                        .id(nodeConfig.getId())
                        .name(nodeConfig.getName())
                        .type(nodeConfig.getType())
                        .pureTime(nodeConfig.getPureTime() != null ? nodeConfig.getPureTime().doubleValue() : null)
                        .pureRate(nodeConfig.getPureRate())
                        .status(nodeConfig.getStatus())
                        .build();
                nodes.add(node);
            }
        }

        // 转换边
        if (definition.getEdges() != null) {
            for (TopologyConfig.EdgeConfig edgeConfig : definition.getEdges()) {
                TopologyEdge edge = TopologyEdge.builder()
                        .source(edgeConfig.getSource())
                        .target(edgeConfig.getTarget())
                        .build();
                edges.add(edge);
            }
        }

        return TopologyGraph.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }
} 