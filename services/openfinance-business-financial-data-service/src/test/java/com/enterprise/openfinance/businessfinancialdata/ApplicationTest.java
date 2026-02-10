package com.enterprise.openfinance.businessfinancialdata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "openfinance.businessfinancialdata.persistence.mode=inmemory",
        "openfinance.businessfinancialdata.cache.mode=inmemory"
})
class ApplicationTest {
    @Test
    void contextLoads() {
    }
}
