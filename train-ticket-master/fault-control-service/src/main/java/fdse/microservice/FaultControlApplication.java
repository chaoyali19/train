package fdse.microservice;

import fdse.microservice.service.FaultDiscoveryService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 故障控制服务主应用类
 * 提供Web界面来管理和控制各种故障注入
 */
@SpringBootApplication
@EnableScheduling
public class FaultControlApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(FaultControlApplication.class, args);
        
        // 启动服务发现
        FaultDiscoveryService discoveryService = context.getBean(FaultDiscoveryService.class);
        discoveryService.startDiscovery();
        
        System.out.println("故障控制服务启动成功！");
        System.out.println("访问地址: http://localhost:8080/fault-control");
    }
} 