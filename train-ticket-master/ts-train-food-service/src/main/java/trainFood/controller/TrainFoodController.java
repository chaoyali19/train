package trainFood.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import trainFood.service.TrainFoodService;
import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/trainfoodservice")
public class TrainFoodController {

    @Autowired
    TrainFoodService trainFoodService;

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainFoodController.class);
    
    // 通过环境变量控制内存消耗大小（单位：MB）
    @Value("${memory.consumption.mb:5}")
    private int memoryConsumptionMB;
    
    // 通过环境变量控制内存块数量
    @Value("${memory.chunks.count:3}")
    private int memoryChunksCount;
    
    // 存储每个请求的内存块，用于确保内存不被GC回收
    private final ConcurrentHashMap<String, List<byte[]>> requestMemoryMap = new ConcurrentHashMap<>();
    
    // 通过环境变量控制内存清理阈值
    @Value("${memory.cleanup.threshold:50}")
    private int memoryCleanupThreshold;
    
    // 通过环境变量控制保留的请求数量
    @Value("${memory.retain.count:20}")
    private int memoryRetainCount;
    
    // 通过环境变量控制内存块保留时间（秒）
    @Value("${memory.retention.seconds:30}")
    private int memoryRetentionSeconds;
    
    // 定时任务执行器，用于定期清理内存
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // 存储请求ID和创建时间的映射
    private final ConcurrentHashMap<String, Long> requestTimestamps = new ConcurrentHashMap<>();

    @GetMapping(path = "/trainfoods/welcome")
    public String home() {
        return "Welcome to [ Train Food Service ] !";
    }
    
    /**
     * 初始化定时清理任务
     */
    @PostConstruct
    public void initMemoryCleanup() {
        // 每10秒执行一次内存清理
        scheduler.scheduleAtFixedRate(this::cleanupExpiredMemoryChunks, 10L, 10L, TimeUnit.SECONDS);
        LOGGER.info("[Memory Simulation] Scheduled memory cleanup task every 10 seconds");
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/trainfoods")
    public HttpEntity getAllTrainFood(@RequestHeader HttpHeaders headers) {
        TrainFoodController.LOGGER.info("[Food Map Service][Get All TrainFoods]");
        
        // 模拟稳定的内存消耗操作
        String requestId = "all-trainfoods-" + System.currentTimeMillis();
        simulateStableMemoryConsumption(requestId);
        
        return ok(trainFoodService.listTrainFood(headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/trainfoods/{tripId}")
    public HttpEntity getTrainFoodOfTrip(@PathVariable String tripId, @RequestHeader HttpHeaders headers) {
        TrainFoodController.LOGGER.info("[Food Map Service][Get TrainFoods By TripId]");
        
        // 模拟稳定的内存消耗操作
        String requestId = "trip-" + tripId + "-" + System.currentTimeMillis();
        simulateStableMemoryConsumption(requestId);
        
        return ok(trainFoodService.listTrainFoodByTripId(tripId, headers));
    }
    
    /**
     * 模拟稳定的内存消耗操作
     * 每个请求都会消耗固定大小的内存，通过环境变量控制
     * 在高并发情况下会导致内存不足
     */
    private void simulateStableMemoryConsumption(String requestId) {
        try {
            LOGGER.info("[Memory Simulation] Starting stable memory consumption for requestId: {}, " +
                       "memoryConsumptionMB: {}, chunksCount: {}", 
                       requestId, memoryConsumptionMB, memoryChunksCount);
            
            // 计算每个内存块的大小
            int chunkSizeBytes = (memoryConsumptionMB * 1024 * 1024) / memoryChunksCount;
            
            // 创建固定数量的内存块
            List<byte[]> memoryChunks = new ArrayList<>();
            Random random = new Random();
            
            for (int i = 0; i < memoryChunksCount; i++) {
                // 创建固定大小的内存块
                byte[] chunk = new byte[chunkSizeBytes];
                
                // 填充随机数据，防止 JVM 优化
                random.nextBytes(chunk);
                memoryChunks.add(chunk);
                
                // 添加少量延迟，模拟真实的内存分配
                Thread.sleep(5);
            }
            
            // 将内存块存储到全局Map中，确保不被GC回收
            requestMemoryMap.put(requestId, memoryChunks);
            
            // 记录请求时间戳
            requestTimestamps.put(requestId, System.currentTimeMillis());
            
            // 定期清理旧的内存块，防止无限增长
            if (requestMemoryMap.size() > memoryCleanupThreshold) {
                cleanupOldMemoryChunks();
            }
            
            LOGGER.info("[Memory Simulation] Created {} memory chunks ({} MB total) for requestId: {}, " +
                       "total active requests: {}", 
                       memoryChunks.size(), memoryConsumptionMB, requestId, requestMemoryMap.size());
            
        } catch (InterruptedException e) {
            LOGGER.error("[Memory Simulation] Interrupted during memory operation", e);
            Thread.currentThread().interrupt();
        } catch (OutOfMemoryError e) {
            LOGGER.error("[Memory Simulation] OutOfMemoryError occurred for requestId: {}", requestId, e);
            // 重新抛出 OOM 错误，让服务崩溃
            throw e;
        } catch (Exception e) {
            LOGGER.error("[Memory Simulation] Error during memory operation for requestId: {}", requestId, e);
        }
    }
    
    /**
     * 清理旧的内存块，防止内存无限增长
     */
    private void cleanupOldMemoryChunks() {
        try {
            // 保留最新的指定数量的请求内存块
            if (requestMemoryMap.size() > memoryRetainCount) {
                int toRemove = requestMemoryMap.size() - memoryRetainCount;
                int removed = 0;
                
                // 使用迭代器安全地删除旧的内存块
                for (String key : requestMemoryMap.keySet()) {
                    if (removed >= toRemove) {
                        break;
                    }
                    requestMemoryMap.remove(key);
                    requestTimestamps.remove(key);
                    removed++;
                }
                
                LOGGER.info("[Memory Simulation] Cleaned up {} old memory chunks, remaining: {}", 
                           removed, requestMemoryMap.size());
            }
        } catch (Exception e) {
            LOGGER.error("[Memory Simulation] Error during cleanup", e);
        }
    }
    
    /**
     * 清理过期的内存块（基于时间）
     */
    private void cleanupExpiredMemoryChunks() {
        try {
            long currentTime = System.currentTimeMillis();
            long expirationTime = currentTime - (memoryRetentionSeconds * 1000L);
            
            int removed = 0;
            for (String requestId : requestTimestamps.keySet()) {
                Long timestamp = requestTimestamps.get(requestId);
                if (timestamp != null && timestamp < expirationTime) {
                    requestMemoryMap.remove(requestId);
                    requestTimestamps.remove(requestId);
                    removed++;
                }
            }
            
            if (removed > 0) {
                LOGGER.info("[Memory Simulation] Cleaned up {} expired memory chunks, remaining: {}", 
                           removed, requestMemoryMap.size());
            }
        } catch (Exception e) {
            LOGGER.error("[Memory Simulation] Error during expired cleanup", e);
        }
    }
}
