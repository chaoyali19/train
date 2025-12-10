package fdse.microservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ChaosMeshConfig 配置测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "chaos.mesh.namespace=test-chaos",
    "chaos.mesh.jvm-port=9288",
    "chaos.mesh.timeout=45000",
    "chaos.mesh.enabled=false"
})
public class ChaosMeshConfigTest {

    @Autowired
    private ChaosMeshConfig chaosMeshConfig;

    @Test
    public void testConfigLoading() {
        assertNotNull(chaosMeshConfig);
        assertEquals("test-chaos", chaosMeshConfig.getNamespace());
        assertEquals(9288, chaosMeshConfig.getJvmPort());
        assertEquals(45000L, chaosMeshConfig.getTimeout());
        assertFalse(chaosMeshConfig.getEnabled());
    }

    @Test
    public void testDefaultValues() {
        // 测试默认值
        ChaosMeshConfig defaultConfig = new ChaosMeshConfig();
        assertEquals("chaos", defaultConfig.getNamespace());
        assertEquals(9277, defaultConfig.getJvmPort());
        assertEquals(30000L, defaultConfig.getTimeout());
        assertTrue(defaultConfig.getEnabled());
    }
} 