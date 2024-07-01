package cn.homecredit.printserverextraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableApolloConfig
public class printserverExtractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(printserverExtractionApplication.class, args);
    }

}
