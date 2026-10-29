package com.minimart.config;

import com.minimart.service.DemoAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountInitializer.class);
    private final DemoAccountService demoAccountService;
    private final boolean enabled;
    private final String demoPassword;

    public DemoAccountInitializer(DemoAccountService demoAccountService,
                                  @Value("${app.bootstrap-demo.enabled:true}") boolean enabled,
                                  @Value("${app.bootstrap-demo.password}") String demoPassword) {
        this.demoAccountService = demoAccountService;
        this.enabled = enabled;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        int initializedAccounts = demoAccountService.initializePlaceholderAccounts(demoPassword);
        if (initializedAccounts > 0) {
            log.info("Đã mã hóa mật khẩu và kích hoạt {} tài khoản demo.", initializedAccounts);
        }
    }
}
