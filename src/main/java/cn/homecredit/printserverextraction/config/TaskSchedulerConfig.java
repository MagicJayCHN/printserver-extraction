package cn.homecredit.printserverextraction.config;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.boot.web.client.RestTemplateBuilder;

import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;

@Configuration
public class TaskSchedulerConfig {

    @Value("${ps.immig.scheduler.poolsize}")
    private int poolSize;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("ContractProcessor--");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();

            HttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            return builder
                    .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

