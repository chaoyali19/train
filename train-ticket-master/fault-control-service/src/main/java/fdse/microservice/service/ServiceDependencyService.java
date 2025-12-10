package fdse.microservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 服务依赖配置服务
 * 负责读取和解析服务依赖配置文件
 */
@Slf4j
@Service
public class ServiceDependencyService {

    private final ObjectMapper objectMapper;
    private JsonNode serviceDependencies;

    public ServiceDependencyService() {
        this.objectMapper = new ObjectMapper();
        loadServiceDependencies();
    }

    /**
     * 加载服务依赖配置
     */
    private void loadServiceDependencies() {
        try {
            ClassPathResource resource = new ClassPathResource("service_dependencies_with_upstream.json");
            InputStream inputStream = resource.getInputStream();
            JsonNode root = objectMapper.readTree(inputStream);
            this.serviceDependencies = root.get("service_dependencies");
            log.info("成功加载服务依赖配置，共 {} 个服务", serviceDependencies.size());
        } catch (IOException e) {
            log.error("加载服务依赖配置文件失败", e);
            this.serviceDependencies = objectMapper.createArrayNode();
        }
    }

    /**
     * 获取所有服务名称
     */
    public List<String> getAllServiceNames() {
        List<String> serviceNames = new ArrayList<>();
        if (serviceDependencies != null && serviceDependencies.isArray()) {
            for (JsonNode service : serviceDependencies) {
                if (service.has("service_name")) {
                    serviceNames.add(service.get("service_name").asText());
                }
            }
        }
        return serviceNames;
    }

    /**
     * 获取指定服务的接口列表
     */
    public List<ServiceInterface> getServiceInterfaces(String serviceName) {
        List<ServiceInterface> interfaces = new ArrayList<>();
        
        if (serviceDependencies != null && serviceDependencies.isArray()) {
            for (JsonNode service : serviceDependencies) {
                if (service.has("service_name") && 
                    service.get("service_name").asText().equals(serviceName) &&
                    service.has("interfaces")) {
                    
                    JsonNode interfacesNode = service.get("interfaces");
                    if (interfacesNode.isArray()) {
                        for (JsonNode interfaceNode : interfacesNode) {
                            ServiceInterface serviceInterface = new ServiceInterface();
                            serviceInterface.setInterfaceName(interfaceNode.get("interface_name").asText());
                            serviceInterface.setMethodName(interfaceNode.get("method_name").asText());
                            serviceInterface.setHttpMethod(interfaceNode.get("http_method").asText());
                            
                            // 解析class_name
                            if (interfaceNode.has("class_name")) {
                                serviceInterface.setClassName(interfaceNode.get("class_name").asText());
                            }
                            
                            // 解析upstream_services
                            if (interfaceNode.has("upstream_services")) {
                                List<UpstreamService> upstreamServices = new ArrayList<>();
                                JsonNode upstreamServicesNode = interfaceNode.get("upstream_services");
                                if (upstreamServicesNode.isArray()) {
                                    for (JsonNode upstreamServiceNode : upstreamServicesNode) {
                                        UpstreamService upstreamService = new UpstreamService();
                                        upstreamService.setServiceName(upstreamServiceNode.get("service_name").asText());
                                        upstreamService.setInterfaceName(upstreamServiceNode.get("interface_name").asText());
                                        upstreamService.setMethodName(upstreamServiceNode.get("method_name").asText());
                                        upstreamServices.add(upstreamService);
                                    }
                                }
                                serviceInterface.setUpstreamServices(upstreamServices);
                            }
                            
                            interfaces.add(serviceInterface);
                        }
                    }
                    break;
                }
            }
        }
        
        return interfaces;
    }

    /**
     * 根据服务名称和接口名称查找方法名
     */
    public Optional<String> getMethodName(String serviceName, String interfaceName) {
        List<ServiceInterface> interfaces = getServiceInterfaces(serviceName);
        return interfaces.stream()
                .filter(iface -> iface.getInterfaceName().equals(interfaceName))
                .map(ServiceInterface::getMethodName)
                .findFirst();
    }

    /**
     * 根据服务名称和方法名查找接口名称
     */
    public Optional<String> getInterfaceName(String serviceName, String methodName) {
        List<ServiceInterface> interfaces = getServiceInterfaces(serviceName);
        return interfaces.stream()
                .filter(iface -> iface.getMethodName().equals(methodName))
                .map(ServiceInterface::getInterfaceName)
                .findFirst();
    }

    /**
     * 根据直接上游服务过滤接口列表
     */
    public List<ServiceInterface> getServiceInterfacesFilteredByDirectUpstream(String serviceName, List<String> directUpstreamServices) {
        List<ServiceInterface> allInterfaces = getServiceInterfaces(serviceName);
        List<ServiceInterface> filteredInterfaces = new ArrayList<>();
        
        for (ServiceInterface iface : allInterfaces) {
            // 如果接口有上游服务，检查是否在直接上游服务列表中
            if (iface.getUpstreamServices() != null && !iface.getUpstreamServices().isEmpty()) {
                boolean hasDirectUpstream = false;
                for (UpstreamService upstream : iface.getUpstreamServices()) {
                    if (directUpstreamServices.contains(upstream.getServiceName())) {
                        hasDirectUpstream = true;
                        break;
                    }
                }
                if (hasDirectUpstream) {
                    filteredInterfaces.add(iface);
                }
            }
        }
        
        return filteredInterfaces;
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        loadServiceDependencies();
    }

    /**
     * 服务接口信息
     */
    public static class ServiceInterface {
        private String interfaceName;
        private String methodName;
        private String httpMethod;
        private String className;
        private List<UpstreamService> upstreamServices;

        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public String getHttpMethod() { return httpMethod; }
        public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public List<UpstreamService> getUpstreamServices() { return upstreamServices; }
        public void setUpstreamServices(List<UpstreamService> upstreamServices) { this.upstreamServices = upstreamServices; }
    }

    /**
     * 上游服务信息
     */
    public static class UpstreamService {
        private String serviceName;
        private String interfaceName;
        private String methodName;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
    }
} 