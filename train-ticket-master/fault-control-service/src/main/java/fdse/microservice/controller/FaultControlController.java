package fdse.microservice.controller;

import fdse.microservice.model.FaultControlRequest;
import fdse.microservice.model.FaultControlResponse;
import fdse.microservice.model.FaultServiceInfo;
import fdse.microservice.model.FaultStatus;
import fdse.microservice.model.FaultInfo;
import fdse.microservice.model.TopologyInfo;
import fdse.microservice.model.ImageUpdateRequest;
import fdse.microservice.service.FaultControlService;
import fdse.microservice.service.FaultDiscoveryService;
import fdse.microservice.service.ImageManagementService;
import fdse.microservice.service.KubernetesService;
import fdse.microservice.service.StressTestService;
import fdse.microservice.service.TopologyService;
import fdse.microservice.service.ChaosMeshService;
import fdse.microservice.service.JvmFaultService;
import fdse.microservice.service.ServiceDependencyService;
import fdse.microservice.model.TopologyGraph;
import fdse.microservice.model.ChaosFaultRequest;
import fdse.microservice.model.ChaosFaultResponse;
import fdse.microservice.model.JvmFaultRequest;
import fdse.microservice.model.JvmLatencyFaultRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 故障控制控制器
 * 提供Web界面和API接口来管理故障
 */
@Slf4j
@Controller
public class FaultControlController {

    @Autowired
    private FaultDiscoveryService faultDiscoveryService;

    @Autowired
    private FaultControlService faultControlService;

    @Autowired
    private ImageManagementService imageManagementService;

    @Autowired
    private StressTestService stressTestService;

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private ChaosMeshService chaosMeshService;

    @Autowired
    private JvmFaultService jvmFaultService;

    @Autowired
    private ServiceDependencyService serviceDependencyService;

    /**
     * 主页面 - 显示所有服务的故障状态和控制界面
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/topology";
    }

    /**
     * 故障列表页面 - 显示所有服务的故障状态和控制界面
     */
    @GetMapping("/index")
    public String index(Model model) {
        try {
            List<FaultStatus> faultStatusList = faultDiscoveryService.getAllServiceStatus();
            List<FaultServiceInfo> serviceInfoList = faultDiscoveryService.getAllServiceInfo();
            // 打印 faultStatusList
            log.info("faultStatusList: {}", faultStatusList);
            model.addAttribute("faultStatusList", faultStatusList);
            model.addAttribute("serviceInfoList", serviceInfoList);
            
            return "index";
        } catch (Exception e) {
            log.error("获取故障状态失败", e);
            model.addAttribute("error", "Failed to get fault status: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 刷新故障状态
     */
    @GetMapping("/refresh")
    public String refresh(Model model) {
        faultDiscoveryService.refreshAllServices();
        return index(model);
    }

    /**
     * 控制故障开关 - 表单提交
     */
    @PostMapping("/control")
    public String controlFault(@RequestParam String serviceId,
                              @RequestParam String faultId,
                              @RequestParam boolean enable,
                              @RequestParam(required = false) Long delayMs,
                              @RequestParam(required = false) Integer errorCode,
                              @RequestParam(required = false) Double probability,
                              Model model) {
        try {
            FaultControlRequest request = new FaultControlRequest();
            request.setFaultId(faultId);
            request.setEnable(enable);
            request.setDelayMs(delayMs);
            request.setErrorCode(errorCode);
            request.setProbability(probability);
            
            FaultControlResponse response = faultControlService.controlFault(serviceId, request);
            
            if (response.isSuccess()) {
                model.addAttribute("message", "Operation successful: " + response.getMessage());
            } else {
                model.addAttribute("error", "Operation failed: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("控制故障失败", e);
            model.addAttribute("error", "Operation failed: " + e.getMessage());
        }
        
        return index(model);
    }

    /**
     * 更新服务镜像 - 表单提交
     */
    @PostMapping("/update-image")
    public String updateServiceImage(@RequestParam String serviceName,
                                   @RequestParam String imageUrl,
                                   @RequestParam(required = false, defaultValue = "default") String namespace,
                                   Model model) {
        try {
            log.info("收到镜像更新请求: service={}, image={}, namespace={}", 
                    serviceName, imageUrl, namespace);

            // 验证镜像地址格式
            if (!imageManagementService.validateImageUrl(imageUrl)) {
                model.addAttribute("error", "Invalid image URL format, please use format: registry/repository:tag");
                return index(model);
            }

            // 创建更新请求
            ImageUpdateRequest request = new ImageUpdateRequest();
            request.setServiceName(serviceName);
            request.setImageUrl(imageUrl);
            request.setNamespace(namespace);

            // 执行更新
            boolean success = imageManagementService.updateServiceImage(request);
            
            if (success) {
                model.addAttribute("message", "Image update successful: " + serviceName + " -> " + imageUrl);
            } else {
                model.addAttribute("error", "Image update failed: " + serviceName);
            }
        } catch (Exception e) {
            log.error("更新服务镜像失败", e);
            model.addAttribute("error", "Image update failed: " + e.getMessage());
        }
        
        return index(model);
    }

    /**
     * API接口 - 获取所有故障状态
     */
    @GetMapping("/api/status")
    @ResponseBody
    public List<FaultStatus> getFaultStatus() {
        return faultDiscoveryService.getAllServiceStatus();
    }

    /**
     * API接口 - 获取所有服务信息
     */
    @GetMapping("/api/info")
    @ResponseBody
    public List<FaultServiceInfo> getServiceInfo() {
        return faultDiscoveryService.getAllServiceInfo();
    }

    /**
     * API接口 - 控制故障
     */
    @PostMapping("/api/control")
    @ResponseBody
    public FaultControlResponse controlFaultApi(@RequestParam String serviceId,
                                               @RequestParam String faultId,
                                               @RequestParam String enable,
                                               @RequestParam(required = false) Long delayMs,
                                               @RequestParam(required = false) Integer errorCode,
                                               @RequestParam(required = false) Double probability) {
        try {
            log.info("收到故障控制API请求: serviceId={}, faultId={}, enable={}, delayMs={}, errorCode={}, probability={}", 
                    serviceId, faultId, enable, delayMs, errorCode, probability);
            
            // 验证必需参数
            if (serviceId == null || serviceId.trim().isEmpty()) {
                log.error("serviceId参数为空");
                FaultControlResponse response = new FaultControlResponse();
                response.setSuccess(false);
                response.setMessage("serviceId参数不能为空");
                return response;
            }
            
            if (faultId == null || faultId.trim().isEmpty()) {
                log.error("faultId参数为空");
                FaultControlResponse response = new FaultControlResponse();
                response.setSuccess(false);
                response.setMessage("faultId参数不能为空");
                return response;
            }
            
            // 解析enable参数
            boolean enableFlag;
            try {
                enableFlag = Boolean.parseBoolean(enable);
            } catch (Exception e) {
                log.error("enable参数解析失败: {}", enable, e);
                FaultControlResponse response = new FaultControlResponse();
                response.setSuccess(false);
                response.setMessage("enable参数格式错误，应为true或false");
                return response;
            }
            
            FaultControlRequest request = new FaultControlRequest();
            request.setFaultId(faultId);
            request.setEnable(enableFlag);
            request.setDelayMs(delayMs);
            request.setErrorCode(errorCode);
            request.setProbability(probability);
            
            log.info("调用故障控制服务: serviceId={}, request={}", serviceId, request);
            FaultControlResponse response = faultControlService.controlFault(serviceId, request);
            log.info("故障控制服务返回: {}", response);
            
            return response;
        } catch (Exception e) {
            log.error("API控制故障失败", e);
            FaultControlResponse response = new FaultControlResponse();
            response.setSuccess(false);
            response.setMessage("操作失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * API接口 - 更新服务镜像
     */
    @PostMapping("/api/update-image")
    @ResponseBody
    public String updateServiceImageApi(@RequestBody ImageUpdateRequest request) {
        try {
            if (!imageManagementService.validateImageUrl(request.getImageUrl())) {
                return "Invalid image URL format, please use format: registry/repository:tag";
            }

            boolean success = imageManagementService.updateServiceImage(request);
            return success ? "Image update successful" : "Image update failed";
        } catch (Exception e) {
            log.error("API镜像更新失败", e);
            return "Image update failed: " + e.getMessage();
        }
    }

    /**
     * API接口 - 获取服务当前镜像
     */
    @GetMapping("/api/service-image")
    @ResponseBody
    public String getServiceImageApi(@RequestParam String serviceName,
                                   @RequestParam(required = false, defaultValue = "default") String namespace) {
        return imageManagementService.getServiceImage(serviceName, namespace);
    }

    /**
     * API接口 - 获取K8s服务列表
     */
    @GetMapping("/api/k8s-services")
    @ResponseBody
    public List<KubernetesService.K8sServiceInfo> getK8sServicesApi(@RequestParam(required = false) String namespace) {
        return imageManagementService.getK8sServices(namespace);
    }

    /**
     * API接口 - 获取指定命名空间的服务列表
     */
    @GetMapping("/api/k8s-services/{namespace}")
    @ResponseBody
    public List<KubernetesService.K8sServiceInfo> getK8sServicesByNamespaceApi(@PathVariable String namespace) {
        return imageManagementService.getServicesByNamespace(namespace);
    }

    /**
     * API接口 - 刷新服务状态
     */
    @PostMapping("/api/refresh")
    @ResponseBody
    public String refreshServiceStatus(@RequestParam(required = false) String serviceId) {
        if (serviceId != null) {
            faultDiscoveryService.refreshService(serviceId);
            return "Service " + serviceId + " status refreshed";
        } else {
            faultDiscoveryService.refreshAllServices();
            return "All service status refreshed";
        }
    }

    /**
     * 错误页面
     */
    @GetMapping("/error")
    public String error(@RequestParam(required = false) String message, Model model) {
        if (message != null) {
            model.addAttribute("error", message);
        }
        return "error";
    }

    // ==================== 压测相关接口 ====================

    /**
     * 压测控制页面
     */
    @GetMapping("/stress")
    public String stressTestPage(Model model) {
        try {
            model.addAttribute("scenarios", stressTestService.getAvailableScenarios());
            model.addAttribute("runningTasks", stressTestService.getRunningTasks());
            return "stress";
        } catch (Exception e) {
            log.error("获取压测页面失败", e);
            model.addAttribute("error", "Failed to get stress test page: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 启动压测任务
     */
    @PostMapping("/stress/start")
    @ResponseBody
    public StressTestResponse startStressTest(@RequestParam String scenario,
                                            @RequestParam(defaultValue = "10") int concurrent,
                                            @RequestParam(defaultValue = "100") int count) {
        try {
            StressTestService.StressTestTask task = stressTestService.startStressTest(scenario, concurrent, count);
            return StressTestResponse.builder()
                    .success(true)
                    .message("压测任务已启动")
                    .taskId(task.getTaskId())
                    .build();
        } catch (Exception e) {
            log.error("启动压测任务失败", e);
            return StressTestResponse.builder()
                    .success(false)
                    .message("启动压测任务失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 停止压测任务
     */
    @PostMapping("/stress/stop")
    @ResponseBody
    public StressTestResponse stopStressTest(@RequestParam String taskId) {
        try {
            boolean stopped = stressTestService.stopStressTest(taskId);
            if (stopped) {
                return StressTestResponse.builder()
                        .success(true)
                        .message("压测任务已停止")
                        .taskId(taskId)
                        .build();
            } else {
                return StressTestResponse.builder()
                        .success(false)
                        .message("停止压测任务失败：任务不存在或已停止")
                        .taskId(taskId)
                        .build();
            }
        } catch (Exception e) {
            log.error("停止压测任务失败", e);
            return StressTestResponse.builder()
                    .success(false)
                    .message("停止压测任务失败: " + e.getMessage())
                    .taskId(taskId)
                    .build();
        }
    }

    /**
     * 获取压测任务状态
     */
    @GetMapping("/stress/status")
    @ResponseBody
    public StressTestResponse getStressTestStatus(@RequestParam(required = false) String taskId) {
        try {
            if (taskId != null) {
                // 获取特定任务状态
                StressTestService.StressTestTask task = stressTestService.getTaskStatus(taskId);
                if (task != null) {
                    return StressTestResponse.builder()
                            .success(true)
                            .taskId(taskId)
                            .status(task.getStatus())
                            .output(task.getOutput())
                            .error(task.getError())
                            .build();
                } else {
                    return StressTestResponse.builder()
                            .success(false)
                            .message("任务不存在")
                            .taskId(taskId)
                            .build();
                }
            } else {
                // 获取所有运行中的任务
                Map<String, StressTestService.StressTestTask> tasks = stressTestService.getRunningTasks();
                return StressTestResponse.builder()
                        .success(true)
                        .runningTasks(tasks)
                        .build();
            }
        } catch (Exception e) {
            log.error("获取压测任务状态失败", e);
            return StressTestResponse.builder()
                    .success(false)
                    .message("获取压测任务状态失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取可用的压测场景
     */
    @GetMapping("/stress/scenarios")
    @ResponseBody
    public String[] getAvailableScenarios() {
        return stressTestService.getAvailableScenarios();
    }

    /**
     * 压测响应类
     */
    public static class StressTestResponse {
        private boolean success;
        private String message;
        private String taskId;
        private String status;
        private String output;
        private String error;
        private Map<String, StressTestService.StressTestTask> runningTasks;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private StressTestResponse response = new StressTestResponse();

            public Builder success(boolean success) {
                response.success = success;
                return this;
            }

            public Builder message(String message) {
                response.message = message;
                return this;
            }

            public Builder taskId(String taskId) {
                response.taskId = taskId;
                return this;
            }

            public Builder status(String status) {
                response.status = status;
                return this;
            }

            public Builder output(String output) {
                response.output = output;
                return this;
            }

            public Builder error(String error) {
                response.error = error;
                return this;
            }

            public Builder runningTasks(Map<String, StressTestService.StressTestTask> runningTasks) {
                response.runningTasks = runningTasks;
                return this;
            }

            public StressTestResponse build() {
                return response;
            }
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public Map<String, StressTestService.StressTestTask> getRunningTasks() { return runningTasks; }
    }

    // ==================== 拓扑图相关接口 ====================

    /**
     * 拓扑图页面
     */
    @GetMapping("/topology")
    public String topologyPage(Model model) {
        try {
            // 初始化默认拓扑图
            topologyService.initializeDefaultTopologies();
            
            List<String> topologyNames = topologyService.getAllTopologyNames();
            model.addAttribute("topologyNames", topologyNames);
            
            if (!topologyNames.isEmpty()) {
                String firstTopology = topologyNames.get(0);
                TopologyGraph graph = topologyService.getTopologyGraph(firstTopology);
                model.addAttribute("currentTopology", firstTopology);
                model.addAttribute("topologyGraph", graph);
            }
            
            return "topology";
        } catch (Exception e) {
            log.error("获取拓扑图页面失败", e);
            model.addAttribute("error", "Failed to get topology page: " + e.getMessage());
            return "error";
        }
    }

    /**
     * API接口 - 获取所有拓扑图信息
     */
    @GetMapping("/api/topology/names")
    @ResponseBody
    public List<TopologyInfo> getTopologyNames() {
        topologyService.initializeDefaultTopologies();
        List<String> names = topologyService.getAllTopologyNames();
        List<TopologyInfo> topologyInfos = new ArrayList<>();
        
        for (String name : names) {
            TopologyGraph graph = topologyService.getTopologyGraph(name);
            TopologyInfo info = new TopologyInfo();
            info.setName(name);
            info.setDisplayName(graph.getName() != null ? graph.getName() : name);
            info.setDescription(graph.getDescription() != null ? graph.getDescription() : "");
            topologyInfos.add(info);
        }
        
        return topologyInfos;
    }

    /**
     * API接口 - 获取指定拓扑图数据
     */
    @GetMapping("/api/topology/graph")
    @ResponseBody
    public TopologyGraph getTopologyGraph(@RequestParam String name) {
        return topologyService.getTopologyGraph(name);
    }

    /**
     * API接口 - 保存拓扑图
     */
    @PostMapping("/api/topology/save")
    @ResponseBody
    public String saveTopologyGraph(@RequestParam String name, @RequestBody TopologyGraph graph) {
        try {
            topologyService.saveTopologyGraph(name, graph);
            return "Topology saved successfully";
        } catch (Exception e) {
            log.error("保存拓扑图失败", e);
            return "Failed to save topology: " + e.getMessage();
        }
    }

    /**
     * API接口 - 删除拓扑图
     */
    @PostMapping("/api/topology/delete")
    @ResponseBody
    public String deleteTopologyGraph(@RequestParam String name) {
        try {
            boolean deleted = topologyService.deleteTopologyGraph(name);
            if (deleted) {
                return "Topology deleted successfully";
            } else {
                return "Topology does not exist";
            }
        } catch (Exception e) {
            log.error("删除拓扑图失败", e);
            return "Failed to delete topology: " + e.getMessage();
        }
    }

    // ==================== Chaos Mesh 故障注入相关接口 ====================

    /**
     * API接口 - 应用网络延迟故障
     */
    @PostMapping("/api/chaos/apply")
    @ResponseBody
    public ChaosFaultResponse applyChaosFault(@RequestBody ChaosFaultRequest request) {
        try {
            if ("network-delay".equals(request.getFaultType())) {
                String result = chaosMeshService.applyNetworkDelay(
                    request.getServiceName(), 
                    request.getDelaySeconds(), 
                    request.getDurationMinutes(),
                    request.getTargetService(),
                    request.getSourceService(),
                    request.getDelayUpstream()
                );
                
                if (result.contains("成功") || result.contains("success")) {
                    return ChaosFaultResponse.builder()
                            .success(true)
                            .message(result)
                            .serviceName(request.getServiceName())
                            .build();
                } else {
                    return ChaosFaultResponse.builder()
                            .success(false)
                            .message(result)
                            .serviceName(request.getServiceName())
                            .build();
                }
            } else {
                return ChaosFaultResponse.builder()
                        .success(false)
                        .message("不支持的故障类型: " + request.getFaultType())
                        .serviceName(request.getServiceName())
                        .build();
            }
        } catch (Exception e) {
            log.error("应用故障注入失败", e);
            return ChaosFaultResponse.builder()
                    .success(false)
                    .message("应用故障注入失败: " + e.getMessage())
                    .serviceName(request.getServiceName())
                    .build();
        }
    }

    /**
     * API接口 - 停止故障注入
     */
    @PostMapping("/api/chaos/stop")
    @ResponseBody
    public ChaosFaultResponse stopChaosFault(@RequestParam String chaosName) {
        try {
            String result = chaosMeshService.stopFault(chaosName);
            
            if (result.contains("成功") || result.contains("success")) {
                return ChaosFaultResponse.builder()
                        .success(true)
                        .message(result)
                        .chaosName(chaosName)
                        .build();
            } else {
                return ChaosFaultResponse.builder()
                        .success(false)
                        .message(result)
                        .chaosName(chaosName)
                        .build();
            }
        } catch (Exception e) {
            log.error("停止故障注入失败", e);
            return ChaosFaultResponse.builder()
                    .success(false)
                    .message("停止故障注入失败: " + e.getMessage())
                    .chaosName(chaosName)
                    .build();
        }
    }

    /**
     * API接口 - 停止所有故障注入
     */
    @PostMapping("/api/chaos/stop-all")
    @ResponseBody
    public ChaosFaultResponse stopAllChaosFaults() {
        try {
            // 停止 Chaos Mesh 故障
            String chaosResult = chaosMeshService.stopAllFaults();
            
            // 停止 JVM 故障
            String jvmResult = jvmFaultService.stopAllFaults();
            
            // 合并结果
            StringBuilder combinedResult = new StringBuilder();
            combinedResult.append("Chaos Mesh: ").append(chaosResult).append("; ");
            combinedResult.append("JVM: ").append(jvmResult);
            
            // 如果两个都成功，返回成功响应
            if ((chaosResult.contains("成功") || chaosResult.contains("success")) && (jvmResult.contains("成功") || jvmResult.contains("success"))) {
                return ChaosFaultResponse.builder()
                        .success(true)
                        .message(combinedResult.toString())
                        .build();
            } else {
                return ChaosFaultResponse.builder()
                        .success(false)
                        .message(combinedResult.toString())
                        .build();
            }
        } catch (Exception e) {
            log.error("停止所有故障注入失败", e);
            return ChaosFaultResponse.builder()
                    .success(false)
                    .message("停止所有故障注入失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * GET接口 - 停止所有故障注入（确认页面）
     */
    @GetMapping("/stop-all-faults")
    public String stopAllFaultsConfirmPage() {
        return "stop-all-confirm";
    }

    @GetMapping("/rollback")
    public String stopAllFaultsConfirmPageRollback() {
        return "stop-all-confirm";
    }

    /**
     * GET接口 - 直接停止所有故障注入（无需确认）
     */
    @GetMapping("/stop-all-faults-direct")
    public String stopAllFaultsDirect(Model model) {
        try {
            // 停止 Chaos Mesh 故障
            String chaosResult = chaosMeshService.stopAllFaults();
            
            // 停止 JVM 故障
            String jvmResult = jvmFaultService.stopAllFaults();
            
            // 合并结果
            StringBuilder combinedResult = new StringBuilder();
            combinedResult.append("Chaos Mesh: ").append(chaosResult).append("; ");
            combinedResult.append("JVM: ").append(jvmResult);
            
            if ((chaosResult.contains("成功") || chaosResult.contains("success")) && (jvmResult.contains("成功") || jvmResult.contains("success"))) {
                model.addAttribute("message", "All faults stopped successfully: " + combinedResult.toString());
                
                // 尝试解析停止的故障数量
                try {
                    int totalStoppedCount = 0;
                    
                    // 解析 Chaos Mesh 故障数量
                    if (chaosResult.contains("停止") || chaosResult.contains("stopped")) {
                        String[] parts = chaosResult.split("停止");
                        if (parts.length > 1) {
                            String countPart = parts[1].split("个")[0].trim();
                            int stoppedCount = Integer.parseInt(countPart);
                            totalStoppedCount += stoppedCount;
                        }
                    }
                    
                    // 解析 JVM 故障数量
                    if (jvmResult.contains("停止") || jvmResult.contains("stopped")) {
                        String[] parts = jvmResult.split("停止");
                        if (parts.length > 1) {
                            String countPart = parts[1].split("个")[0].trim();
                            int stoppedCount = Integer.parseInt(countPart);
                            totalStoppedCount += stoppedCount;
                        }
                    }
                    
                    model.addAttribute("stoppedCount", totalStoppedCount);
                } catch (Exception e) {
                    // 解析失败不影响主流程
                    log.debug("解析停止故障数量失败", e);
                }
                
            } else {
                model.addAttribute("error", "Failed to stop faults: " + combinedResult.toString());
            }
            
        } catch (Exception e) {
            log.error("停止所有故障注入失败", e);
            model.addAttribute("error", "Exception stopping all fault injections: " + e.getMessage());
        }
        
        // 返回成功页面
        return "stop-all-success";
    }

    /**
     * API接口 - 获取活跃的故障列表
     */
    @GetMapping("/api/chaos/active")
    @ResponseBody
    public List<FaultInfo> getActiveFaults() {
        List<FaultInfo> allFaults = new ArrayList<>();
        allFaults.addAll(chaosMeshService.getActiveFaults());
        allFaults.addAll(jvmFaultService.getActiveJvmFaults());
        return allFaults;
    }

    // ==================== JVM故障注入相关接口 ====================

    /**
     * API接口 - 获取服务接口列表
     */
    @GetMapping("/api/service/interfaces")
    @ResponseBody
    public List<ServiceDependencyService.ServiceInterface> getServiceInterfaces(@RequestParam String serviceName) {
        return serviceDependencyService.getServiceInterfaces(serviceName);
    }

    /**
     * API接口 - 根据直接上游服务获取服务接口列表
     */
    @GetMapping("/api/service/interfaces/filtered")
    @ResponseBody
    public List<ServiceDependencyService.ServiceInterface> getServiceInterfacesFiltered(
            @RequestParam String serviceName,
            @RequestParam(required = false) List<String> directUpstreamServices) {
        if (directUpstreamServices == null || directUpstreamServices.isEmpty()) {
            return serviceDependencyService.getServiceInterfaces(serviceName);
        }
        return serviceDependencyService.getServiceInterfacesFilteredByDirectUpstream(serviceName, directUpstreamServices);
    }

    /**
     * API接口 - 应用JVM故障注入
     */
    @PostMapping("/api/jvm/apply")
    @ResponseBody
    public ChaosFaultResponse applyJvmFault(@RequestBody JvmFaultRequest request) {
        try {
            String result = jvmFaultService.applyJvmFault(request);
            
            if (result.contains("成功") || result.contains("success")) {
                return ChaosFaultResponse.builder()
                        .success(true)
                        .message(result)
                        .serviceName(request.getServiceName())
                        .build();
            } else {
                return ChaosFaultResponse.builder()
                        .success(false)
                        .message(result)
                        .serviceName(request.getServiceName())
                        .build();
            }
        } catch (Exception e) {
            log.error("应用JVM故障注入失败", e);
            return ChaosFaultResponse.builder()
                    .success(false)
                    .message("应用JVM故障注入失败: " + e.getMessage())
                    .serviceName(request.getServiceName())
                    .build();
        }
    }

    /**
     * API接口 - 停止JVM故障注入
     */
    @PostMapping("/api/jvm/stop")
    @ResponseBody
    public ChaosFaultResponse stopJvmFault(@RequestParam String chaosName) {
        try {
            String result = jvmFaultService.stopJvmFault(chaosName);
            
            if (result.contains("成功") || result.contains("success")) {
                return ChaosFaultResponse.builder()
                        .success(true)
                        .message(result)
                        .chaosName(chaosName)
                        .build();
            } else {
                return ChaosFaultResponse.builder()
                        .success(false)
                        .message(result)
                        .chaosName(chaosName)
                        .build();
            }
        } catch (Exception e) {
            log.error("停止JVM故障注入失败", e);
            return ChaosFaultResponse.builder()
                    .success(false)
                    .message("停止JVM故障注入失败: " + e.getMessage())
                    .chaosName(chaosName)
                    .build();
        }
    }

    // ==================== JVM延时故障注入相关接口 ====================

    /**
     * API接口 - 应用JVM延时故障注入
     */
    @PostMapping("/api/jvm/latency/apply")
    @ResponseBody
    public ChaosFaultResponse applyJvmLatencyFault(@RequestBody JvmLatencyFaultRequest request) {
        try {
            String result = jvmFaultService.applyJvmLatencyFault(request);
            
            if (result.contains("成功") || result.contains("success")) {
                return ChaosFaultResponse.builder()
                        .success(true)
                        .message(result)
                        .serviceName(request.getServiceName())
                        .build();
            } else {
                return ChaosFaultResponse.builder()
                        .success(false)
                        .message(result)
                        .serviceName(request.getServiceName())
                        .build();
            }
        } catch (Exception e) {
            log.error("应用JVM延时故障注入失败", e);
            return ChaosFaultResponse.builder()
                    .success(false)
                    .message("应用JVM延时故障注入失败: " + e.getMessage())
                    .serviceName(request.getServiceName())
                    .build();
        }
    }

    /**
     * API接口 - 停止JVM延时故障注入
     */
    @PostMapping("/api/jvm/latency/stop")
    @ResponseBody
    public ChaosFaultResponse stopJvmLatencyFault(@RequestParam String chaosName) {
        try {
            String result = jvmFaultService.stopJvmFault(chaosName);
            
            if (result.contains("成功") || result.contains("success")) {
                return ChaosFaultResponse.builder()
                        .success(true)
                        .message(result)
                        .chaosName(chaosName)
                        .build();
            } else {
                return ChaosFaultResponse.builder()
                        .success(false)
                        .message(result)
                        .chaosName(chaosName)
                        .build();
            }
        } catch (Exception e) {
            log.error("停止JVM延时故障注入失败", e);
            return ChaosFaultResponse.builder()
                    .success(false)
                    .message("停止JVM延时故障注入失败: " + e.getMessage())
                    .chaosName(chaosName)
                    .build();
        }
    }

    /**
     * API接口 - 获取 Kubernetes 连接状态
     */
    @GetMapping("/api/kubernetes/status")
    @ResponseBody
    public Map<String, String> getKubernetesStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "使用 kubectl 命令模式");
        return status;
    }
} 