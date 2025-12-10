package fdse.microservice.controller;

import fdse.microservice.service.FaultControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 测试控制器
 * 提供简化的测试页面
 */
@Controller
public class TestController {

    @Autowired
    private FaultControlService faultControlService;

    /**
     * 测试页面
     */
    @GetMapping("/test")
    public String testPage(Model model) {
        try {
            // 获取所有故障状态
            model.addAttribute("faultStatusList", faultControlService.getAllFaultStatus());
            
                    // 添加成功消息
        model.addAttribute("message", "Test page loaded successfully");
            
        } catch (Exception e) {
                    // 添加错误消息
        model.addAttribute("error", "Failed to load data: " + e.getMessage());
            model.addAttribute("faultStatusList", java.util.Collections.emptyList());
        }
        
        return "test";
    }
} 