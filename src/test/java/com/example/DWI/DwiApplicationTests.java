package com.example.DWI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.DWI.service.ApiCloudService;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DwiApplicationTests {

    @Autowired
    private ApiCloudService apiCloudService;

    @Test
    void contextLoads() {
        assertNotNull(apiCloudService);
    }
}

