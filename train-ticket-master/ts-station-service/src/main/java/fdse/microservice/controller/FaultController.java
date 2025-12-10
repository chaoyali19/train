package fdse.microservice.controller;

import fdse.microservice.model.FaultControlRequest;
import fdse.microservice.model.FaultControlResponse;
import fdse.microservice.model.FaultInfo;
import fdse.microservice.model.FaultServiceInfo;
import fdse.microservice.model.FaultStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 故障控制控制器
 * 实现故障服务规范接口
 */
@RestController
@RequestMapping("/fault")
public class FaultController {
    
    // 站点ID查询返回空数据故障开关
    private static volatile boolean emptyStationQueryEnabled = false;
    
    // 站点ID查询延迟故障开关
    private static volatile boolean stationQueryDelayEnabled = false;
    
    // 站点ID查询500错误故障开关
    private static volatile boolean stationQuery500ErrorEnabled = false;
    
    // 站点ID查询随机500错误故障开关
    private static volatile boolean stationQueryRandom500ErrorEnabled = false;
    
    // 站点ID查询响应结构错误故障开关
    private static volatile boolean stationQueryResponseStructureErrorEnabled = false;
    
    // 延迟时间（毫秒）
    private static volatile long delayTime = 10000;
    
    // 随机500错误概率
    private static volatile double random500ErrorProbability = 0.5;
    
    // 静态初始化块，从环境变量读取默认配置
    static {
        // 从环境变量读取故障配置
        String emptyFaultEnv = System.getenv("FAULT_EMPTY_STATION_QUERY");
        String delayFaultEnv = System.getenv("FAULT_STATION_QUERY_DELAY");
        String delayTimeEnv = System.getenv("FAULT_DELAY_TIME_MS");
        String error500FaultEnv = System.getenv("FAULT_STATION_QUERY_500_ERROR");
        String random500FaultEnv = System.getenv("FAULT_STATION_QUERY_RANDOM_500_ERROR");
        String responseStructureErrorEnv = System.getenv("FAULT_STATION_QUERY_RESPONSE_STRUCTURE_ERROR");
        
        // 如果环境变量设置为true，则默认启用空数据故障
        if ("true".equalsIgnoreCase(emptyFaultEnv)) {
            emptyStationQueryEnabled = true;
            System.out.println("[FaultController] 环境变量启用站点ID查询空数据故障: FAULT_EMPTY_STATION_QUERY=true");
        }
        
        // 如果环境变量设置为true，则默认启用延迟故障
        if ("true".equalsIgnoreCase(delayFaultEnv)) {
            stationQueryDelayEnabled = true;
            System.out.println("[FaultController] 环境变量启用站点ID查询延迟故障: FAULT_STATION_QUERY_DELAY=true");
        }
        
        // 如果环境变量设置为true，则默认启用500错误故障
        if ("true".equalsIgnoreCase(error500FaultEnv)) {
            stationQuery500ErrorEnabled = true;
            System.out.println("[FaultController] 环境变量启用站点ID查询500错误故障: FAULT_STATION_QUERY_500_ERROR=true");
        }
        
        // 如果环境变量设置为true，则默认启用随机500错误故障
        if ("true".equalsIgnoreCase(random500FaultEnv)) {
            stationQueryRandom500ErrorEnabled = true;
            System.out.println("[FaultController] 环境变量启用站点ID查询随机500错误故障: FAULT_STATION_QUERY_RANDOM_500_ERROR=true");
        }
        
        // 如果环境变量设置为true，则默认启用响应结构错误故障
        if ("true".equalsIgnoreCase(responseStructureErrorEnv)) {
            stationQueryResponseStructureErrorEnabled = true;
            System.out.println("[FaultController] 环境变量启用站点ID查询响应结构错误故障: FAULT_STATION_QUERY_RESPONSE_STRUCTURE_ERROR=true");
        }
        
        // 如果设置了延迟时间环境变量，则使用该值
        if (delayTimeEnv != null && !delayTimeEnv.trim().isEmpty()) {
            try {
                delayTime = Long.parseLong(delayTimeEnv);
                System.out.println("[FaultController] 环境变量设置延迟时间: FAULT_DELAY_TIME_MS=" + delayTime + "ms");
            } catch (NumberFormatException e) {
                System.err.println("[FaultController] 环境变量延迟时间格式错误: " + delayTimeEnv);
            }
        }
    }
    
    /**
     * 启用/禁用站点ID查询返回空数据故障
     */
    @PostMapping("/enableEmptyStationQuery")
    public String enableEmptyStationQuery(@RequestParam boolean enable) {
        emptyStationQueryEnabled = enable;
        return "Empty station ID query fault " + (enable ? "enabled" : "disabled");
    }
    
    /**
     * 启用/禁用站点ID查询延迟故障
     */
    @PostMapping("/enableStationQueryDelay")
    public String enableStationQueryDelay(@RequestParam boolean enable, 
                                        @RequestParam(defaultValue = "10000") long delayMs) {
        stationQueryDelayEnabled = enable;
        delayTime = delayMs;
        return "Station ID query delay fault " + (enable ? "enabled" : "disabled") + 
               (enable ? " with delay " + delayMs + "ms" : "");
    }
    
    /**
     * 启用/禁用站点ID查询500错误故障
     */
    @PostMapping("/enableStationQuery500Error")
    public String enableStationQuery500Error(@RequestParam boolean enable) {
        stationQuery500ErrorEnabled = enable;
        return "Station ID query 500 error fault " + (enable ? "enabled" : "disabled");
    }
    
    /**
     * 启用/禁用站点ID查询随机500错误故障
     */
    @PostMapping("/enableStationQueryRandom500Error")
    public String enableStationQueryRandom500Error(@RequestParam boolean enable) {
        stationQueryRandom500ErrorEnabled = enable;
        return "Station ID query random 500 error fault " + (enable ? "enabled" : "disabled");
    }
    
    /**
     * 启用/禁用站点ID查询响应结构错误故障
     */
    @PostMapping("/enableStationQueryResponseStructureError")
    public String enableStationQueryResponseStructureError(@RequestParam boolean enable) {
        stationQueryResponseStructureErrorEnabled = enable;
        return "Station ID query response structure error fault " + (enable ? "enabled" : "disabled");
    }
    
    /**
     * 获取服务信息
     */
    @GetMapping("/info")
    public FaultServiceInfo getServiceInfo() {
        return FaultServiceInfo.builder()
                .serviceName("站点服务")
                .serviceId("ts-station-service")
                .description("站点查询服务，提供站点信息查询功能")
                .version("1.0.0")
                .faults(Arrays.asList(
                    FaultInfo.builder()
                        .id("empty-station-query")
                        .name("空数据故障")
                        .description("站点查询接口返回空数据")
                        .type("boolean")
                        .build(),
                    FaultInfo.builder()
                        .id("station-query-delay")
                        .name("延迟故障")
                        .description("站点查询接口响应延迟")
                        .type("delay")
                        .defaultDelay(10000L)
                        .build(),
                    FaultInfo.builder()
                        .id("station-query-500-error")
                        .name("500错误故障")
                        .description("站点查询接口返回500错误")
                        .type("error")
                        .defaultErrorCode(500)
                        .build(),
                    FaultInfo.builder()
                        .id("station-query-random-500-error")
                        .name("随机500错误故障")
                        .description("站点查询接口随机返回500错误")
                        .type("random")
                        .defaultProbability(0.5)
                        .build(),
                    FaultInfo.builder()
                        .id("station-query-response-structure-error")
                        .name("响应结构错误故障")
                        .description("站点查询接口返回错误的响应结构")
                        .type("boolean")
                        .build()
                ))
                .build();
    }
    
    /**
     * 获取故障状态
     */
    @GetMapping("/status")
    public FaultStatus getFaultStatus() {
        return FaultStatus.builder()
                .serviceId("ts-station-service")
                .serviceName("站点服务")
                .status("正常")
                .timestamp(System.currentTimeMillis())
                .reachable(true)
                .faults(Arrays.asList(
                    FaultStatus.FaultState.builder()
                        .id("empty-station-query")
                        .enabled(emptyStationQueryEnabled)
                        .build(),
                    FaultStatus.FaultState.builder()
                        .id("station-query-delay")
                        .enabled(stationQueryDelayEnabled)
                        .delayMs(delayTime)
                        .build(),
                    FaultStatus.FaultState.builder()
                        .id("station-query-500-error")
                        .enabled(stationQuery500ErrorEnabled)
                        .errorCode(500)
                        .build(),
                    FaultStatus.FaultState.builder()
                        .id("station-query-random-500-error")
                        .enabled(stationQueryRandom500ErrorEnabled)
                        .probability(random500ErrorProbability)
                        .build(),
                    FaultStatus.FaultState.builder()
                        .id("station-query-response-structure-error")
                        .enabled(stationQueryResponseStructureErrorEnabled)
                        .build()
                ))
                .build();
    }
    
    /**
     * 控制故障
     */
    @PostMapping("/control")
    public FaultControlResponse controlFault(@RequestBody FaultControlRequest request) {
        String faultId = request.getFaultId();
        boolean enable = request.isEnable();
        
        switch (faultId) {
            case "empty-station-query":
                emptyStationQueryEnabled = enable;
                return FaultControlResponse.builder()
                        .success(true)
                        .message("Empty station query fault " + (enable ? "enabled" : "disabled"))
                        .faultId(faultId)
                        .enabled(enable)
                        .build();
                        
            case "station-query-delay":
                stationQueryDelayEnabled = enable;
                if (request.getDelayMs() != null) {
                    delayTime = request.getDelayMs();
                }
                return FaultControlResponse.builder()
                        .success(true)
                        .message("Station query delay fault " + (enable ? "enabled" : "disabled") + 
                                (enable ? " with delay " + delayTime + "ms" : ""))
                        .faultId(faultId)
                        .enabled(enable)
                        .build();
                        
            case "station-query-500-error":
                stationQuery500ErrorEnabled = enable;
                return FaultControlResponse.builder()
                        .success(true)
                        .message("Station query 500 error fault " + (enable ? "enabled" : "disabled"))
                        .faultId(faultId)
                        .enabled(enable)
                        .build();
                        
            case "station-query-random-500-error":
                stationQueryRandom500ErrorEnabled = enable;
                if (request.getProbability() != null) {
                    random500ErrorProbability = request.getProbability();
                }
                return FaultControlResponse.builder()
                        .success(true)
                        .message("Station query random 500 error fault " + (enable ? "enabled" : "disabled") +
                                (enable ? " with probability " + random500ErrorProbability : ""))
                        .faultId(faultId)
                        .enabled(enable)
                        .build();
                        
            case "station-query-response-structure-error":
                stationQueryResponseStructureErrorEnabled = enable;
                return FaultControlResponse.builder()
                        .success(true)
                        .message("Station query response structure error fault " + (enable ? "enabled" : "disabled"))
                        .faultId(faultId)
                        .enabled(enable)
                        .build();
                        
            default:
                return FaultControlResponse.builder()
                        .success(false)
                        .message("Unknown fault: " + faultId)
                        .faultId(faultId)
                        .enabled(false)
                        .build();
        }
    }
    

    
    // Getter方法供Service层使用
    public static boolean isEmptyStationQueryEnabled() {
        return emptyStationQueryEnabled;
    }
    
    public static boolean isStationQueryDelayEnabled() {
        return stationQueryDelayEnabled;
    }
    
    public static long getDelayTime() {
        return delayTime;
    }
    
    public static boolean isStationQuery500ErrorEnabled() {
        return stationQuery500ErrorEnabled;
    }
    
    public static boolean isStationQueryRandom500ErrorEnabled() {
        return stationQueryRandom500ErrorEnabled;
    }
    
    public static boolean isStationQueryResponseStructureErrorEnabled() {
        return stationQueryResponseStructureErrorEnabled;
    }
} 