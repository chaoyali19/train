package fdse.microservice.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * 压测服务
 * 负责管理压测任务和与Python压测程序交互
 */
@Slf4j
@Service
public class StressTestService {

    @Value("${stress.test.python.path:../train-ticket-auto-query}")
    private String pythonProjectPath;

    @Value("${stress.test.venv.path:../train-ticket-auto-query/.venv/bin/python}")
    private String pythonExecutable;

    private final Map<String, StressTestTask> runningTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdCounter = new AtomicLong(1);

    /**
     * 压测任务类
     */
    public static class StressTestTask {
        private final String taskId;
        private final String scenario;
        private final int concurrent;
        private final int count;
        private final long startTime;
        @JsonIgnore
        private Process process;
        private String status;
        private String output;
        private String error;

        public StressTestTask(String scenario, int concurrent, int count) {
            this.taskId = "task_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
            this.scenario = scenario;
            this.concurrent = concurrent;
            this.count = count;
            this.startTime = System.currentTimeMillis();
            this.status = "starting";
            this.output = "";
            this.error = "";
        }

        // Getters
        public String getTaskId() { return taskId; }
        public String getScenario() { return scenario; }
        public int getConcurrent() { return concurrent; }
        public int getCount() { return count; }
        public long getStartTime() { return startTime; }
        public Process getProcess() { return process; }
        public String getStatus() { return status; }
        public String getOutput() { return output; }
        public String getError() { return error; }

        // Setters
        public void setProcess(Process process) { this.process = process; }
        public void setStatus(String status) { this.status = status; }
        public void setOutput(String output) { this.output = output; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * 启动压测任务
     */
    public StressTestTask startStressTest(String scenario, int concurrent, int count) {
        try {
            // 创建压测任务
            StressTestTask task = new StressTestTask(scenario, concurrent, count);
            runningTasks.put(task.getTaskId(), task);

            // 异步执行压测
            CompletableFuture.runAsync(() -> {
                executeStressTest(task);
            });

            log.info("启动压测任务: {} - 场景: {}, 并发: {}, 请求数: {}", 
                    task.getTaskId(), scenario, concurrent, count);

            return task;

        } catch (Exception e) {
            log.error("启动压测任务失败", e);
            throw new RuntimeException("启动压测任务失败: " + e.getMessage());
        }
    }

    /**
     * 执行压测任务
     */
    private void executeStressTest(StressTestTask task) {
        try {
            task.setStatus("running");

            // 构建命令
            Path projectPath = Paths.get(pythonProjectPath).toAbsolutePath();
            String[] command = {
                pythonExecutable,
                "-m", "src.stress",
                "--scenario", task.getScenario(),
                "--concurrent", String.valueOf(task.getConcurrent()),
                "--count", String.valueOf(task.getCount())
            };

            log.info("执行压测命令: {}", String.join(" ", command));

            // 启动进程
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(projectPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            task.setProcess(process);

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    task.setOutput(output.toString());
                    
                    // 检查是否包含完成信息
                    if (line.contains("压测成功完成") || line.contains("压测失败")) {
                        break;
                    }
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                task.setStatus("completed");
                log.info("压测任务完成: {}", task.getTaskId());
            } else if (exitCode == 143) {
                // 退出码143表示进程被SIGTERM信号终止，这是正常的停止操作
                task.setStatus("stopped");
                log.info("压测任务已停止: {} - 退出码: {}", task.getTaskId(), exitCode);
            } else {
                task.setStatus("failed");
                task.setError("进程退出码: " + exitCode);
                log.error("压测任务失败: {} - 退出码: {}", task.getTaskId(), exitCode);
            }

        } catch (Exception e) {
            task.setStatus("failed");
            task.setError(e.getMessage());
            log.error("压测任务执行异常: {}", task.getTaskId(), e);
        } finally {
            // 清理任务
            runningTasks.remove(task.getTaskId());
        }
    }

    /**
     * 停止压测任务
     */
    public boolean stopStressTest(String taskId) {
        StressTestTask task = runningTasks.get(taskId);
        if (task != null && task.getProcess() != null) {
            try {
                task.getProcess().destroy();
                task.setStatus("stopped");
                log.info("停止压测任务: {}", taskId);
                return true;
            } catch (Exception e) {
                log.error("停止压测任务失败: {}", taskId, e);
                return false;
            }
        }
        return false;
    }

    /**
     * 获取压测任务状态
     */
    public StressTestTask getTaskStatus(String taskId) {
        return runningTasks.get(taskId);
    }

    /**
     * 获取所有运行中的任务
     */
    public Map<String, StressTestTask> getRunningTasks() {
        return new ConcurrentHashMap<>(runningTasks);
    }

    /**
     * 获取可用的压测场景
     */
    public String[] getAvailableScenarios() {
        return new String[]{
            "high_speed",    // 高铁票查询
            "normal",        // 普通列车票查询
            "food",          // 食品查询
            "parallel",      // 并行车票查询
            "pay",           // 查询并支付订单
            "cancel",        // 查询并取消订单
            "consign"        // 查询并添加托运信息
        };
    }
} 