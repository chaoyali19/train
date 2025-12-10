package fdse.microservice.service;

import fdse.microservice.config.ChaosMeshConfig;
import fdse.microservice.model.FaultInfo;
import fdse.microservice.model.JvmFaultRequest;
import fdse.microservice.model.JvmLatencyFaultRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * JVM故障注入服务
 * 负责管理JVM故障注入操作
 */
@Slf4j
@Service
public class JvmFaultService {

    @Autowired
    private ServiceDependencyService serviceDependencyService;
    
    @Autowired
    private ChaosMeshConfig chaosMeshConfig;

    // 存储当前活跃的JVM故障注入
    private final Map<String, FaultInfo> activeJvmFaults = new ConcurrentHashMap<>();

    /**
     * 应用JVM故障注入
     */
    public String applyJvmFault(JvmFaultRequest request) {
        try {
            // 验证服务是否存在
            List<String> allServices = serviceDependencyService.getAllServiceNames();
            if (!allServices.contains(request.getServiceName())) {
                return "服务不存在: " + request.getServiceName();
            }

            // 生成故障名称
            String chaosName = "jvm-" + request.getServiceName().replace("ts-", "") + "-" + System.currentTimeMillis();
            
            // 生成JVM故障配置
            String jvmChaosYaml = generateJvmChaosYaml(chaosName, request);
            
            // 应用故障配置
            boolean success = applyJvmChaosWithKubectl(jvmChaosYaml);
            
            if (success) {
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = startTime.plusMinutes(request.getDurationMinutes());
                
                FaultInfo faultInfo = FaultInfo.builder()
                    .chaosName(chaosName)
                    .serviceName(request.getServiceName())
                    .faultType("jvm-500-error")
                    .delaySeconds(0)
                    .durationMinutes(request.getDurationMinutes())
                    .startTime(startTime)
                    .endTime(endTime)
                    .status("active")
                    .build();
                
                activeJvmFaults.put(chaosName, faultInfo);
                log.info("成功应用JVM故障注入 - 服务: {}, 方法: {}, 持续时间: {}m, 结束时间: {}", 
                        request.getServiceName(), request.getMethodName(), request.getDurationMinutes(), endTime);
                return "JVM fault injection successfully";
            } else {
                log.error("应用JVM故障注入失败 - 服务: {}", request.getServiceName());
                return "JVM fault injection failed";
            }
            
        } catch (Exception e) {
            log.error("应用JVM故障注入异常 - 服务: {}", request.getServiceName(), e);
            return "JVM故障注入异常: " + e.getMessage();
        }
    }

    /**
     * 停止JVM故障注入
     */
    public String stopJvmFault(String chaosName) {
        try {
            boolean success = deleteJvmChaosWithKubectl(chaosName);
            
            if (success) {
                FaultInfo faultInfo = activeJvmFaults.get(chaosName);
                if (faultInfo != null) {
                    faultInfo.setStatus("stopped");
                    activeJvmFaults.remove(chaosName);
                }
                log.info("成功停止JVM故障注入 - 故障名称: {}", chaosName);
                return "JVM failure stopped successfully";
            } else {
                log.error("停止JVM故障注入失败 - 故障名称: {}", chaosName);
                return "JVM failure stop failure";
            }
            
        } catch (Exception e) {
            log.error("停止JVM故障注入异常 - 故障名称: {}", chaosName, e);
            return "JVM故障停止异常: " + e.getMessage();
        }
    }

    /**
     * 获取活跃的JVM故障列表
     */
    public List<FaultInfo> getActiveJvmFaults() {
        return new ArrayList<>(activeJvmFaults.values());
    }

    /**
     * 停止所有JVM故障注入
     */
    public String stopAllFaults() {
        try {
            log.info("开始停止所有 JVM 故障...");
            
            // 1. 停止内存中记录的活跃故障
            int stoppedCount = 0;
            for (String chaosName : new ArrayList<>(activeJvmFaults.keySet())) {
                try {
                    FaultInfo faultInfo = activeJvmFaults.get(chaosName);
                    if (faultInfo != null) {
                        faultInfo.setStatus("stopped");
                        activeJvmFaults.remove(chaosName);
                        stoppedCount++;
                        log.info("已停止JVM故障: {} - 服务: {}", chaosName, faultInfo.getServiceName());
                    }
                } catch (Exception e) {
                    log.error("停止JVM故障失败: {}", chaosName, e);
                }
            }
            
            // 2. 清理 Kubernetes 中的所有 JVMChaos 资源
            int cleanedCount = cleanupAllJvmChaos();
            
            log.info("停止所有JVM故障完成 - 内存中停止: {}, Kubernetes 清理: {}", stoppedCount, cleanedCount);
            return String.format("成功停止所有JVM故障 - 内存中停止: %d, Kubernetes 清理: %d", stoppedCount, cleanedCount);
            
        } catch (Exception e) {
            log.error("停止所有JVM故障异常", e);
            return "停止所有JVM故障异常: " + e.getMessage();
        }
    }

    /**
     * 清理所有 JVMChaos 资源
     */
    private int cleanupAllJvmChaos() {
        try {
            // 获取 chaos 命名空间中的所有 JVMChaos 资源
            ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "jvmchaos", "-n", chaosMeshConfig.getNamespace(), "-o", "jsonpath='{.items[*].metadata.name}'");
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
                    log.info("JVMChaos 需要删除: {}", chaosNames);
                    for (String name : names) {
                        name = name.trim();
                        log.info("JVMChaos 正在删除: {}", name);
                        if (!name.isEmpty()) {
                            try {
                                if (deleteJvmChaosWithKubectl(name)) {
                                    deletedCount++;
                                    log.info("已删除 JVMChaos: {}", name);
                                }
                            } catch (Exception e) {
                                log.error("删除 JVMChaos 失败: {}", name, e);
                            }
                        }
                    }
                    
                    return deletedCount;
                } else {
                    log.info("{} 命名空间中没有 JVMChaos 资源", chaosMeshConfig.getNamespace());
                    return 0;
                }
            } else {
                log.warn("获取 JVMChaos 列表失败，退出码: {}, 输出: {}", exitCode, output.toString().trim());
                return 0;
            }
            
        } catch (Exception e) {
            log.error("清理所有 JVMChaos 异常", e);
            return 0;
        }
    }

    /**
     * 定时清理已过期的JVM故障
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒
    public void cleanupExpiredFaults() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredFaults = new ArrayList<>();
        
        for (Map.Entry<String, FaultInfo> entry : activeJvmFaults.entrySet()) {
            FaultInfo faultInfo = entry.getValue();
            if (faultInfo.getEndTime() != null && now.isAfter(faultInfo.getEndTime())) {
                expiredFaults.add(entry.getKey());
                log.info("发现过期JVM故障: {} - 服务: {}, 结束时间: {}", 
                        faultInfo.getChaosName(), faultInfo.getServiceName(), faultInfo.getEndTime());
            }
        }
        
        // 清理过期故障
        for (String chaosName : expiredFaults) {
            try {
                FaultInfo faultInfo = activeJvmFaults.get(chaosName);
                if (faultInfo != null) {
                    faultInfo.setStatus("expired");
                    activeJvmFaults.remove(chaosName);
                    log.info("已清理过期JVM故障: {} - 服务: {}", faultInfo.getChaosName(), faultInfo.getServiceName());
                }
            } catch (Exception e) {
                log.error("清理过期JVM故障失败: {}", chaosName, e);
            }
        }
        
        if (!expiredFaults.isEmpty()) {
            log.info("本次清理了 {} 个过期JVM故障", expiredFaults.size());
        }
    }

    /**
     * 生成JVM故障注入YAML配置
     */
    private String generateJvmChaosYaml(String chaosName, JvmFaultRequest request) {
        // 优先使用请求中的className，如果没有则从配置文件获取
        String className = request.getClassName();
        
        if (className == null || className.isEmpty()) {
            // 获取接口信息以获取class_name
            List<ServiceDependencyService.ServiceInterface> interfaces = serviceDependencyService.getServiceInterfaces(request.getServiceName());
            
            for (ServiceDependencyService.ServiceInterface iface : interfaces) {
                if (iface.getInterfaceName().equals(request.getInterfaceName()) && 
                    iface.getMethodName().equals(request.getMethodName())) {
                    className = iface.getClassName();
                    break;
                }
            }
            
            // 如果没有找到class_name，使用默认的包名映射
            if (className == null || className.isEmpty()) {
                className = getPackageName(request.getServiceName()) + ".controller." + getControllerName(request.getServiceName()) + "Controller";
            }
        }
        
        // 构建规则数据，确保正确的缩进
        String ruleData = String.format(
            "    RULE Inject500Error\n" +
            "    CLASS %s\n" +
            "    METHOD %s\n" +
            "    AT ENTRY\n" +
            "    IF true\n" +
            "        DO throw new RuntimeException(\"Simulated server error for %s\");\n" +
            "    ENDRULE",
            className,
            request.getMethodName(),
            request.getInterfaceName()
        );

        return String.format(
            "kind: JVMChaos\n" +
            "apiVersion: chaos-mesh.org/v1alpha1\n" +
            "metadata:\n" +
            "  namespace: %s\n" +
            "  name: %s\n" +
            "  annotations:\n" +
            "    experiment.chaos-mesh.org/pause: 'false'\n" +
            "spec:\n" +
            "  selector:\n" +
            "    namespaces:\n" +
            "      - %s\n" +
            "    labelSelectors:\n" +
            "      app: %s\n" +
            "  mode: all\n" +
            "  duration: %dm\n" +
            "  action: ruleData\n" +
            "  port: %d\n" +
            "  name: '--ruleData-%d'\n" +
            "  value: ''\n" +
            "  exception: ''\n" +
            "  latency: 0\n" +
            "  ruleData: |-\n" +
            "%s",
            chaosMeshConfig.getNamespace(),
            chaosName,
            chaosMeshConfig.getNamespace(),
            request.getServiceName(),
            request.getDurationMinutes(),
            chaosMeshConfig.getJvmPort(),
            System.currentTimeMillis(),
            ruleData
        );
    }

    /**
     * 根据服务名称获取包名
     */
    private String getPackageName(String serviceName) {
        // 根据服务名称映射到包名
        switch (serviceName) {
            case "ts-seat-service":
                return "seat";
            case "ts-order-service":
                return "order";
            case "ts-user-service":
                return "user";
            case "ts-auth-service":
                return "auth";
            case "ts-travel-service":
                return "travel";
            case "ts-travel2-service":
                return "travel2";
            case "ts-station-service":
                return "station";
            case "ts-train-service":
                return "train";
            case "ts-config-service":
                return "config";
            case "ts-price-service":
                return "price";
            case "ts-payment-service":
                return "payment";
            case "ts-inside-payment-service":
                return "inside-payment";
            case "ts-execute-service":
                return "execute";
            case "ts-cancel-service":
                return "cancel";
            case "ts-rebook-service":
                return "rebook";
            case "ts-route-service":
                return "route";
            case "ts-route-plan-service":
                return "route-plan";
            case "ts-travel-plan-service":
                return "travel-plan";
            case "ts-assurance-service":
                return "assurance";
            case "ts-consign-service":
                return "consign";
            case "ts-consign-price-service":
                return "consign-price";
            case "ts-food-service":
                return "food";
            case "ts-food-delivery-service":
                return "food-delivery";
            case "ts-station-food-service":
                return "station-food";
            case "ts-train-food-service":
                return "train-food";
            case "ts-notification-service":
                return "notification";
            case "ts-security-service":
                return "security";
            case "ts-verification-code-service":
                return "verification-code";
            case "ts-contacts-service":
                return "contacts";
            case "ts-admin-user-service":
                return "admin-user";
            case "ts-admin-travel-service":
                return "admin-travel";
            case "ts-admin-route-service":
                return "admin-route";
            case "ts-admin-basic-info-service":
                return "admin-basic-info";
            case "ts-admin-order-service":
                return "admin-order";
            case "ts-basic-service":
                return "basic";
            case "ts-preserve-service":
                return "preserve";
            case "ts-preserve-other-service":
                return "preserve-other";
            case "ts-order-other-service":
                return "order-other";
            case "ts-avatar-service":
                return "avatar";
            case "ts-news-service":
                return "news";
            case "ts-ticket-office-service":
                return "ticket-office";
            case "ts-voucher-service":
                return "voucher";
            case "ts-wait-order-service":
                return "wait-order";
            case "ts-delivery-service":
                return "delivery";
            default:
                return serviceName.replace("ts-", "").replace("-service", "");
        }
    }

    /**
     * 根据服务名称获取控制器名称
     */
    private String getControllerName(String serviceName) {
        String packageName = getPackageName(serviceName);
        // 将包名转换为首字母大写的控制器名
        return packageName.substring(0, 1).toUpperCase() + packageName.substring(1);
    }

    /**
     * 使用kubectl应用JVM故障配置
     */
    private boolean applyJvmChaosWithKubectl(String yamlContent) {
        try {
            ProcessBuilder pb = new ProcessBuilder("kubectl", "apply", "-f", "-");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 写入YAML内容
            process.getOutputStream().write(yamlContent.getBytes());
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
                log.info("JVM故障配置应用成功: {}", output.toString().trim());
                return true;
            } else {
                log.error("JVM故障配置应用失败，退出码: {}, 输出: {}", exitCode, output.toString().trim());
                return false;
            }
            
        } catch (Exception e) {
            log.error("应用JVM故障配置异常", e);
            return false;
        }
    }

    /**
     * 使用kubectl删除JVM故障配置
     */
    private boolean deleteJvmChaosWithKubectl(String chaosName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("kubectl", "delete", "jvmchaos", chaosName, "-n", chaosMeshConfig.getNamespace());
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
                log.info("JVM fault configuration deleted successfully: {}", output.toString().trim());
                return true;
            } else {
                log.error("JVM故障配置删除失败，退出码: {}, 输出: {}", exitCode, output.toString().trim());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Delete JVM fault configuration exception", e);
            return false;
        }
    }

    /**
     * 应用JVM延时故障注入
     */
    public String applyJvmLatencyFault(JvmLatencyFaultRequest request) {
        try {
            // 验证服务是否存在
            List<String> allServices = serviceDependencyService.getAllServiceNames();
            if (!allServices.contains(request.getServiceName())) {
                return "服务不存在: " + request.getServiceName();
            }

            // 生成故障名称
            String chaosName = "jvm-latency-" + request.getServiceName().replace("ts-", "") + "-" + System.currentTimeMillis();
            
            // 生成JVM延时故障配置
            String jvmChaosYaml = generateJvmLatencyChaosYaml(chaosName, request);
            
            // 应用故障配置
            boolean success = applyJvmChaosWithKubectl(jvmChaosYaml);
            
            if (success) {
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = startTime.plusMinutes(request.getDurationMinutes());
                
                FaultInfo faultInfo = FaultInfo.builder()
                    .chaosName(chaosName)
                    .serviceName(request.getServiceName())
                    .faultType("jvm-latency")
                    .delaySeconds(request.getLatencyMs() / 1000) // 转换为秒
                    .durationMinutes(request.getDurationMinutes())
                    .startTime(startTime)
                    .endTime(endTime)
                    .status("active")
                    .build();
                
                activeJvmFaults.put(chaosName, faultInfo);
                log.info("成功应用JVM延时故障注入 - 服务: {}, 方法: {}, 延时: {}ms, 持续时间: {}m, 结束时间: {}", 
                        request.getServiceName(), request.getMethodName(), request.getLatencyMs(), request.getDurationMinutes(), endTime);
                return "JVM delay fault injection is successfully";
            } else {
                log.error("应用JVM延时故障注入失败 - 服务: {}", request.getServiceName());
                return "JVM delay fault injection failed";
            }
            
        } catch (Exception e) {
            log.error("应用JVM延时故障注入异常 - 服务: {}", request.getServiceName(), e);
            return "JVM delay fault injection exception: " + e.getMessage();
        }
    }

    /**
     * 生成JVM延时故障注入YAML配置
     */
    private String generateJvmLatencyChaosYaml(String chaosName, JvmLatencyFaultRequest request) {
        // 优先使用请求中的className，如果没有则从配置文件获取
        String className = request.getClassName();
        
        if (className == null || className.isEmpty()) {
            // 获取接口信息以获取class_name
            List<ServiceDependencyService.ServiceInterface> interfaces = serviceDependencyService.getServiceInterfaces(request.getServiceName());
            
            for (ServiceDependencyService.ServiceInterface iface : interfaces) {
                if (iface.getInterfaceName().equals(request.getInterfaceName()) && 
                    iface.getMethodName().equals(request.getMethodName())) {
                    className = iface.getClassName();
                    break;
                }
            }
            
            // 如果没有找到class_name，使用默认的包名映射
            if (className == null || className.isEmpty()) {
                className = getPackageName(request.getServiceName()) + ".controller." + getControllerName(request.getServiceName()) + "Controller";
            }
        }

        return String.format(
            "kind: JVMChaos\n" +
            "apiVersion: chaos-mesh.org/v1alpha1\n" +
            "metadata:\n" +
            "  namespace: %s\n" +
            "  name: %s\n" +
            "spec:\n" +
            "  selector:\n" +
            "    namespaces:\n" +
            "      - %s\n" +
            "    labelSelectors:\n" +
            "      app: %s\n" +
            "  mode: all\n" +
            "  duration: %dm\n" +
            "  action: latency\n" +
            "  port: %d\n" +
            "  class: %s\n" +
            "  method: %s\n" +
            "  name: %s-latency-%d\n" +
            "  value: ''\n" +
            "  exception: ''\n" +
            "  latency: %d\n" +
            "  ruleData: ''",
            chaosMeshConfig.getNamespace(),
            chaosName,
            chaosMeshConfig.getNamespace(),
            request.getServiceName(),
            request.getDurationMinutes(),
            chaosMeshConfig.getJvmPort(),
            className,
            request.getMethodName(),
            className,
            System.currentTimeMillis(),
            request.getLatencyMs()
        );
    }
} 