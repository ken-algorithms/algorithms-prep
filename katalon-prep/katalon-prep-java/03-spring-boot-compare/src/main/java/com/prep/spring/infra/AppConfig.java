package com.prep.spring.infra;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /**
     * Clock la bean -> test co the thay bang Clock.fixed(...) de kiem soat thoi gian.
     * Neu goi Instant.now() truc tiep trong service thi khong test duoc, va se phai
     * dung Thread.sleep - nguon goc cua test cham va flaky.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
