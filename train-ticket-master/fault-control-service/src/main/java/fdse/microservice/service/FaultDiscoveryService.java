package fdse.microservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fdse.microservice.config.FaultServiceConfig;
import fdse.microservice.model.FaultInfo;
import fdse.microservice.model.FaultServiceInfo;
import fdse.microservice.model.FaultStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 故障发现服务
 * 负责自动发现各个服务的故障信息
 */
@Slf4j
@Service
public class FaultDiscoveryService {

    @Autowired
    private FaultServiceConfig faultServiceConfig;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    
    // 缓存服务信息
    private final ConcurrentHashMap<String, FaultServiceInfo> serviceInfoCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FaultStatus> statusCache = new ConcurrentHashMap<>();

    public FaultDiscoveryService() {
        // 配置HTTP客户端
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * 启动服务发现
     */
    public void startDiscovery() {
        if (!faultServiceConfig.getDiscovery().isEnabled()) {
            log.info("服务发现已禁用");
            return;
        }

        log.info("启动故障服务发现，扫描间隔: {}ms", faultServiceConfig.getDiscovery().getScanInterval());
        
        // 立即执行一次发现
        discoverServices();
        
        // 再次执行一次发现，确保获取到所有服务状态
        try {
            Thread.sleep(2000); // 等待2秒
            discoverServices();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 定时执行发现
        scheduler.scheduleAtFixedRate(
            this::discoverServices,
            faultServiceConfig.getDiscovery().getScanInterval(),
            faultServiceConfig.getDiscovery().getScanInterval(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 停止服务发现
     */
    public void stopDiscovery() {
        scheduler.shutdown();
        log.info("故障服务发现已停止");
    }

    /**
     * 发现所有服务
     */
    private void discoverServices() {
        log.debug("开始发现故障服务...");
        
        for (FaultServiceConfig.ServiceConfig serviceConfig : faultServiceConfig.getServices()) {
            try {
                discoverService(serviceConfig);
            } catch (Exception e) {
                log.error("发现服务失败: {}", serviceConfig.getId(), e);
            }
        }
    }

    /**
     * 发现单个服务
     */
    private void discoverService(FaultServiceConfig.ServiceConfig serviceConfig) {
        String serviceId = serviceConfig.getId();
        String baseUrl = "http://" + serviceId + ":8080";
        
        try {
            // 获取服务信息
            FaultServiceInfo serviceInfo = fetchServiceInfo(baseUrl);
            if (serviceInfo != null) {
                serviceInfoCache.put(serviceId, serviceInfo);
                log.debug("发现服务信息: {}", serviceId);
            }
            
            // 获取服务状态
            FaultStatus status = fetchServiceStatus(baseUrl, serviceConfig.getName());
            if (status != null) {
                statusCache.put(serviceId, status);
                log.debug("获取服务状态: {}", serviceId);
            } else {
                // 如果获取状态失败，创建一个不可达的状态对象
                FaultStatus offlineStatus = FaultStatus.builder()
                        .serviceId(serviceId)
                        .serviceName(serviceConfig.getName())
                        .status("离线")
                        .details("获取服务状态失败")
                        .timestamp(System.currentTimeMillis())
                        .reachable(false)
                        .faults(new ArrayList<>())
                        .build();
                
                statusCache.put(serviceId, offlineStatus);
                log.debug("创建离线状态: {}", serviceId);
            }
            
        } catch (Exception e) {
            log.warn("服务不可达: {} ({})", serviceId, e.getMessage());
            
            // 标记服务为不可达，但保留服务名称信息
            FaultStatus offlineStatus = FaultStatus.builder()
                    .serviceId(serviceId)
                    .serviceName(serviceConfig.getName())
                    .status("离线")
                    .details("连接失败: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .reachable(false)
                    .faults(new ArrayList<>())
                    .build();
            
            statusCache.put(serviceId, offlineStatus);
            
            // 如果缓存中已有该服务的信息，尝试保留故障列表
            FaultServiceInfo cachedInfo = serviceInfoCache.get(serviceId);
            if (cachedInfo != null && cachedInfo.getFaults() != null) {
                List<FaultStatus.FaultState> defaultFaults = new ArrayList<>();
                for (FaultInfo faultInfo : cachedInfo.getFaults()) {
                    FaultStatus.FaultState faultState = FaultStatus.FaultState.builder()
                            .id(faultInfo.getId())
                            .enabled(false) // 默认禁用
                            .build();
                    defaultFaults.add(faultState);
                }
                offlineStatus.setFaults(defaultFaults);
            }
        }
    }

    /**
     * 获取服务信息
     */
    private FaultServiceInfo fetchServiceInfo(String baseUrl) throws Exception {
        String url = baseUrl + "/fault/info";
        HttpGet request = new HttpGet(url);
        
        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        
        if (response.getStatusLine().getStatusCode() == 200) {
            return objectMapper.readValue(responseBody, FaultServiceInfo.class);
        } else {
            log.warn("获取服务信息失败: {} (HTTP {})", url, response.getStatusLine().getStatusCode());
            return null;
        }
    }

    /**
     * 获取服务状态
     */
    private FaultStatus fetchServiceStatus(String baseUrl, String serviceName) throws Exception {
        String url = baseUrl + "/fault/status";
        HttpGet request = new HttpGet(url);
        
        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        
        if (response.getStatusLine().getStatusCode() == 200) {
            FaultStatus status = objectMapper.readValue(responseBody, FaultStatus.class);
            // 确保有默认值
            if (status.getFaults() == null) {
                status.setFaults(new ArrayList<>());
            }
            status.setReachable(true);
            return status;
        } else {
            log.warn("获取服务状态失败: {} (HTTP {})", url, response.getStatusLine().getStatusCode());
            return null;
        }
    }

    /**
     * 获取所有服务信息
     */
    public List<FaultServiceInfo> getAllServiceInfo() {
        return new ArrayList<>(serviceInfoCache.values());
    }

    /**
     * 获取所有服务状态
     */
    public List<FaultStatus> getAllServiceStatus() {
        return new ArrayList<>(statusCache.values());
    }

    /**
     * 获取指定服务信息
     */
    public FaultServiceInfo getServiceInfo(String serviceId) {
        return serviceInfoCache.get(serviceId);
    }

    /**
     * 获取指定服务状态
     */
    public FaultStatus getServiceStatus(String serviceId) {
        return statusCache.get(serviceId);
    }

    /**
     * 刷新指定服务
     */
    public void refreshService(String serviceId) {
        FaultServiceConfig.ServiceConfig serviceConfig = faultServiceConfig.getServices().stream()
                .filter(s -> s.getId().equals(serviceId))
                .findFirst()
                .orElse(null);
        
        if (serviceConfig != null) {
            discoverService(serviceConfig);
        }
    }

    /**
     * 刷新所有服务
     */
    public void refreshAllServices() {
        discoverServices();
    }
} 