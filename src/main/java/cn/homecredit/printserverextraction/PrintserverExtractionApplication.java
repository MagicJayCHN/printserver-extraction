package cn.homecredit.printserverextraction;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableApolloConfig
public class PrintserverExtractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrintserverExtractionApplication.class, args);
    }

}
