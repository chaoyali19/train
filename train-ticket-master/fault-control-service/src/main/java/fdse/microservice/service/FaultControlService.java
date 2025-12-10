package fdse.microservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fdse.microservice.config.FaultServiceConfig;
import fdse.microservice.model.FaultControlRequest;
import fdse.microservice.model.FaultControlResponse;
import fdse.microservice.model.FaultStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 故障控制服务
 * 负责与各个故障服务进行通信
 */
@Slf4j
@Service
public class FaultControlService {

    @Autowired
    private FaultServiceConfig faultServiceConfig;

    @Autowired
    private FaultDiscoveryService faultDiscoveryService;

    private final HttpClient httpClient = HttpClients.createDefault();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有服务的故障状态
     */
    public List<FaultStatus> getAllFaultStatus() {
        // 使用FaultDiscoveryService来获取状态
        return faultDiscoveryService.getAllServiceStatus();
    }

    /**
     * 获取指定服务的故障状态
     */
    public FaultStatus getFaultStatus(String serviceId) {
        return faultDiscoveryService.getServiceStatus(serviceId);
    }

    /**
     * 控制故障开关
     */
    public FaultControlResponse controlFault(String serviceId, FaultControlRequest request) {
        try {
            String baseUrl = "http://" + serviceId + ":8080";
            String controlUrl = baseUrl + "/fault/control";
            
            HttpPost httpRequest = new HttpPost(controlUrl);
            httpRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(request), StandardCharsets.UTF_8));
            httpRequest.setHeader("Content-Type", "application/json");
            
            HttpResponse response = httpClient.execute(httpRequest);
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                FaultControlResponse controlResponse = objectMapper.readValue(responseBody, FaultControlResponse.class);
                
                // 刷新服务状态
                faultDiscoveryService.refreshService(serviceId);
                
                log.info("控制故障成功 - 服务: {}, 故障: {}, 启用: {}", 
                        serviceId, request.getFaultId(), request.isEnable());
                
                return controlResponse;
            } else {
                log.error("控制故障失败 - 服务: {}, HTTP状态: {}", serviceId, response.getStatusLine().getStatusCode());
                return FaultControlResponse.builder()
                        .success(false)
                        .message("HTTP错误: " + response.getStatusLine().getStatusCode())
                        .faultId(request.getFaultId())
                        .enabled(false)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("控制故障失败 - 服务: {}, 故障: {}", serviceId, request.getFaultId(), e);
            return FaultControlResponse.builder()
                    .success(false)
                    .message("控制失败: " + e.getMessage())
                    .faultId(request.getFaultId())
                    .enabled(false)
                    .build();
        }
    }

    /**
     * 刷新服务状态
     */
    public void refreshServiceStatus(String serviceId) {
        faultDiscoveryService.refreshService(serviceId);
    }

    /**
     * 刷新所有服务状态
     */
    public void refreshAllServiceStatus() {
        faultDiscoveryService.refreshAllServices();
    }

    /**
     * 获取服务配置
     */
    public FaultServiceConfig.ServiceConfig getServiceConfig(String serviceId) {
        return faultServiceConfig.getServices().stream()
                .filter(s -> s.getId().equals(serviceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有服务配置
     */
    public List<FaultServiceConfig.ServiceConfig> getAllServiceConfigs() {
        return faultServiceConfig.getServices();
    }
} 