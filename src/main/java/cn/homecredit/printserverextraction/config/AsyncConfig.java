//package cn.homecredit.printserverextraction.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.annotation.AsyncConfigurer;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import java.util.concurrent.Executor;
//
//@Configuration
//@EnableAsync
//public class AsyncConfig implements AsyncConfigurer {
//
//    @Override
//    public Executor getAsyncExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(10); // 核心线程数
//        executor.setMaxPoolSize(50);  // 最大线程数
//        executor.setQueueCapacity(100); // 队列容量
//        executor.setThreadNamePrefix("ContractProcessor-");
//        executor.initialize();
//        return executor;
//    }
//
//}