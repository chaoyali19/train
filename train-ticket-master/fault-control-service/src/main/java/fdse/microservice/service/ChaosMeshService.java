package fdse.microservice.service;

import fdse.microservice.config.ChaosMeshConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import fdse.microservice.model.FaultInfo;

/**
 * Chaos Mesh 故障注入服务
 * 负责管理 chaos-mesh 的故障注入操作
 */
@Slf4j
@Service
public class ChaosMeshService {

    @Autowired
    private ChaosMeshConfig chaosMeshConfig;

    // 存储当前活跃的故障注入
    private final Map<String, FaultInfo> activeFaults = new ConcurrentHashMap<>();

    /**
     * 应用网络延迟故障
     */
    public String applyNetworkDelay(String serviceName, int delaySeconds, int durationMinutes, 
                                   String targetService, String sourceService, Boolean delayUpstream) {
        try {
            // 去掉 chaos_ 前缀
            String cleanServiceName = serviceName.startsWith("chaos_") ? 
                serviceName.substring(6) : serviceName;
            
            String chaosName = "network-delay-" + cleanServiceName + "-" + System.currentTimeMillis();
            
            boolean success;
            
            if (delayUpstream != null && delayUpstream && targetService != null && sourceService != null) {
                // 清理目标服务和源服务的前缀
                String cleanTargetService = targetService.startsWith("chaos_") ? 
                    targetService.substring(6) : targetService;
                String cleanSourceService = sourceService.startsWith("chaos_") ? 
                    sourceService.substring(6) : sourceService;
                
                // 使用 kubectl 创建方向性延迟
                success = createNetworkChaosWithKubectl(chaosName, cleanSourceService, cleanTargetService, 
                                                      delaySeconds, durationMinutes);
            } else {
                // 使用 kubectl 创建普通延迟
                success = createNetworkChaosWithKubectl(chaosName, cleanServiceName, cleanServiceName, 
                                                      delaySeconds, durationMinutes);
            }
            
            if (success) {
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
                
                FaultInfo faultInfo = FaultInfo.builder()
                    .chaosName(chaosName)
                    .serviceName(serviceName)
                    .faultType("network-delay")
                    .delaySeconds(delaySeconds)
                    .durationMinutes(durationMinutes)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status("active")
                    .build();
                
                activeFaults.put(chaosName, faultInfo);
                log.info("成功应用网络延迟故障 - 服务: {} (清理后: {}), 延迟: {}s, 持续时间: {}m, 目标服务: {}, 源服务: {}, 延迟上游: {}, 结束时间: {}", 
                        serviceName, cleanServiceName, delaySeconds, durationMinutes, targetService, sourceService, delayUpstream, endTime);
                return "Fault injection successful";
            } else {
                log.error("应用网络延迟故障失败 - 服务: {} (清理后: {})", serviceName, cleanServiceName);
                return "Fault injection failed";
            }
            
        } catch (Exception e) {
            log.error("应用网络延迟故障异常 - 服务: {}", serviceName, e);
            return "故障注入异常: " + e.getMessage();
        }
    }

    /**
     * 停止故障注入
     */
    public String stopFault(String chaosName) {
        try {
            boolean success = deleteNetworkChaosWithKubectl(chaosName);
            
            if (success) {
                FaultInfo faultInfo = activeFaults.get(chaosName);
                if (faultInfo != null) {
                    faultInfo.setStatus("stopped");
                    activeFaults.remove(chaosName);
                }
                log.info("成功停止故障注入 - 故障名称: {}", chaosName);
                return "Failure stopped successfully";
            } else {
                log.error("停止故障注入失败 - 故障名称: {}", chaosName);
                return "Fault stop failed";
            }
            
        } catch (Exception e) {
            log.error("停止故障注入异常 - 故障名称: {}", chaosName, e);
            return "故障停止异常: " + e.getMessage();
        }
    }

    /**
     * 停止所有故障注入
     */
    public String stopAllFaults() {
        try {
            log.info("开始停止所有 Chaos Mesh 故障...");
            
            // 1. 停止内存中记录的活跃故障
            int stoppedCount = 0;
            for (String chaosName : new ArrayList<>(activeFaults.keySet())) {
                try {
                    FaultInfo faultInfo = activeFaults.get(chaosName);
                    if (faultInfo != null) {
                        faultInfo.setStatus("stopped");
                        activeFaults.remove(chaosName);
                        stoppedCount++;
                        log.info("已停止故障: {} - 服务: {}", chaosName, faultInfo.getServiceName());
                    }
                } catch (Exception e) {
                    log.error("停止故障失败: {}", chaosName, e);
                }
            }
            
            // 2. 清理 Kubernetes 中的所有 NetworkChaos 资源
            int cleanedCount = cleanupAllNetworkChaos();
            
            log.info("停止所有故障完成 - 内存中停止: {}, Kubernetes 清理: {}", stoppedCount, cleanedCount);
            return String.format("成功停止所有故障 - 内存中停止: %d, Kubernetes 清理: %d", stoppedCount, cleanedCount);
            
        } catch (Exception e) {
            log.error("停止所有故障异常", e);
            return "停止所有故障异常: " + e.getMessage();
        }
    }

    /**
     * 清理所有 NetworkChaos 资源
     */
    private int cleanupAllNetworkChaos() {
        try {
            // 获取 chaos 命名空间中的所有 NetworkChaos 资源
            ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "networkchaos", "-n", chaosMeshConfig.getNamespace(), "-o", "jsonpath='{.items[*].metadata.name}'");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                String chaosNames = output.toString().trim();
                if (!chaosNames.isEmpty()) {
                    // 分割资源名称并逐个删除
                    String[] names = chaosNames.split("\\s+");
                    int deletedCount = 0;
                    log.info("chaosNames 需要删除: {}", chaosNames);
                    for (String name : names) {
                        name = name.trim();
                        if (name.equals("network-delay-ts-station-food-service-1753871630123") || name.equals("network-delay-ts-station-food-service-1752821876831")) {
                            log.info("name 跳过: {}", name);
                            continue;
                        }
                        log.info("name 正在删除: {}", name);
                        if (!name.isEmpty()) {
                            try {
                                if (deleteNetworkChaosWithKubectl(name)) {
                                    deletedCount++;
                                    log.info("已删除 NetworkChaos: {}", name);
                                }
                            } catch (Exception e) {
                                log.error("删除 NetworkChaos 失败: {}", name, e);
                            }
                        }
                    }
                    
                    return deletedCount;
                } else {
                    log.info("{} 命名空间中没有 NetworkChaos 资源", chaosMeshConfig.getNamespace());
                    return 0;
                }
            } else {
                log.warn("获取 NetworkChaos 列表失败，退出码: {}, 输出: {}", exitCode, output.toString().trim());
                return 0;
            }
            
        } catch (Exception e) {
            log.error("清理所有 NetworkChaos 异常", e);
            return 0;
        }
    }

    /**
     * 获取活跃的故障列表
     */
    public List<FaultInfo> getActiveFaults() {
        return new ArrayList<>(activeFaults.values());
    }

    /**
     * 使用 kubectl 创建 NetworkChaos
     */
    private boolean createNetworkChaosWithKubectl(String chaosName, String sourceService, String targetService, 
                                                int delaySeconds, int durationMinutes) {
        try {
            String yaml = generateNetworkChaosYaml(chaosName, sourceService, targetService, delaySeconds, durationMinutes);
            
            // 创建临时文件并写入 YAML
            ProcessBuilder pb = new ProcessBuilder("kubectl", "apply", "-f", "-");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 写入 YAML 到进程
            process.getOutputStream().write(yaml.getBytes());
            process.getOutputStream().close();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("成功创建 NetworkChaos: {}, 输出: {}", chaosName, output.toString().trim());
                return true;
            } else {
                log.error("创建 NetworkChaos 失败: {}, 退出码: {}, 输出: {}", chaosName, exitCode, output.toString().trim());
                return false;
            }
            
        } catch (Exception e) {
            log.error("创建 NetworkChaos 异常: {}", chaosName, e);
            return false;
        }
    }

    /**
     * 使用 kubectl 删除 NetworkChaos
     */
    private boolean deleteNetworkChaosWithKubectl(String chaosName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("kubectl", "delete", "networkchaos", chaosName, "-n", chaosMeshConfig.getNamespace());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("成功删除 NetworkChaos: {}, 输出: {}", chaosName, output.toString().trim());
                return true;
            } else {
                // 如果资源不存在，也认为是成功的
                if (output.toString().contains("not found")) {
                    log.info("NetworkChaos 不存在，可能已经被删除: {}", chaosName);
                    return true;
                }
                log.error("删除 NetworkChaos 失败: {}, 退出码: {}, 输出: {}", chaosName, exitCode, output.toString().trim());
                return false;
            }
            
        } catch (Exception e) {
            log.error("删除 NetworkChaos 异常: {}", chaosName, e);
            return false;
        }
    }

    /**
     * 生成 NetworkChaos YAML
     */
    private String generateNetworkChaosYaml(String chaosName, String sourceService, String targetService, 
                                          int delaySeconds, int durationMinutes) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("apiVersion: chaos-mesh.org/v1alpha1\n");
        yaml.append("kind: NetworkChaos\n");
        yaml.append("metadata:\n");
        yaml.append("  name: ").append(chaosName).append("\n");
        yaml.append("  namespace: ").append(chaosMeshConfig.getNamespace()).append("\n");
        yaml.append("spec:\n");
        yaml.append("  action: delay\n");
        yaml.append("  mode: one\n");
        yaml.append("  selector:\n");
        yaml.append("    labelSelectors:\n");
        yaml.append("      app: ").append(targetService).append("\n");
        yaml.append("  delay:\n");
        yaml.append("    latency: ").append(delaySeconds).append("s\n");
        yaml.append("    correlation: '100'\n");
        yaml.append("    jitter: 0ms\n");
        yaml.append("  duration: ").append(durationMinutes).append("m\n");
        
        // 如果是方向性延迟，添加 target
        if (!sourceService.equals(targetService)) {
            yaml.append("  target:\n");
            yaml.append("    selector:\n");
            yaml.append("      labelSelectors:\n");
            yaml.append("        app: ").append(sourceService).append("\n");
            yaml.append("    mode: one\n");
        }
        
        return yaml.toString();
    }

    /**
     * 定时清理已过期的故障
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒
    public void cleanupExpiredFaults() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredFaults = new ArrayList<>();
        
        for (Map.Entry<String, FaultInfo> entry : activeFaults.entrySet()) {
            FaultInfo faultInfo = entry.getValue();
            if (faultInfo.getEndTime() != null && now.isAfter(faultInfo.getEndTime())) {
                expiredFaults.add(entry.getKey());
                log.info("发现过期故障: {} - 服务: {}, 结束时间: {}", 
                        faultInfo.getChaosName(), faultInfo.getServiceName(), faultInfo.getEndTime());
            }
        }
        
        // 清理过期故障
        for (String chaosName : expiredFaults) {
            try {
                FaultInfo faultInfo = activeFaults.get(chaosName);
                if (faultInfo != null) {
                    faultInfo.setStatus("expired");
                    activeFaults.remove(chaosName);
                    log.info("已清理过期故障: {} - 服务: {}", faultInfo.getChaosName(), faultInfo.getServiceName());
                }
            } catch (Exception e) {
                log.error("清理过期故障失败: {}", chaosName, e);
            }
        }
        
        if (!expiredFaults.isEmpty()) {
            log.info("本次清理了 {} 个过期故障", expiredFaults.size());
        }
    }
} 