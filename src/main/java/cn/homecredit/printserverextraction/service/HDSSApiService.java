package cn.homecredit.printserverextraction.service;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class HDSSApiService {


    private RateLimiter rateLimiter;

    private final RestTemplate restTemplate;

    public Queue<Long> getRequestTimestamps() {
        return requestTimestamps;
    }

    private final Queue<Long> requestTimestamps;

/*
PROD
    private static final String BASE_URL = "https://homegw.homecreditcfc.cn/hdss/v1/documents";

    private static final String AUTH_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjIxIn0.eyJhdWQiOiJIRFNTIiwic3ViIjoiMjQiLCJpYXQiOjE1ODU2OTIxMTksIm5iZiI6MTU4NTY5MjExOSwiZXhwIjoxOTAxMjI0OTE5LCJoZHNzX3YxIjp7ImciOlt7ImR0IjpbIioiXSwiYWMiOlsiKiJdLCJwYyI6WyIqIl19XX19.LmQbb59iCn-CLh1uZiSbrx-wgf_yT2Q0LSU0gIoTN3RbTPb29rMHPoQBwP8EkInbjr1VaF4dmMhL1adl0JeUAw";
    //10.26.215.182:/FPG2/miniplum/miniplum/
    private static final String NAS_PATH = "/FPG2/miniplum/miniplum/";
*/



    /*UAT
        GET https://home-gw.cn00c1.cn.infra/hdss/v1/documents?contractNumber=160414575&docType=TC&from=minip
        Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjYxIn0.eyJhdWQiOiJIRFNTIiwic3ViIjoiMjQiLCJpYXQiOjE1ODMwNTYzMzksIm5iZiI6MTU4MzA1NjMzOSwiZXhwIjoxOTAxMjI0OTE5LCJoZHNzX3YxIjp7ImciOlt7ImR0IjpbIioiXSwiYWMiOlsiKiJdLCJwYyI6WyIqIl19XX19.1MWhhk2tmTNsfCNNDZTch-kWUDPAvMYs9_9HuM_7LPdYAOIXuWIbB9tifrSYE1SFQyF9blis-s8bmmi48KiiKw
        Accept: application/pdf
    */
    @Value("${ps.immig.hdss.baseurl}")
    private  String BASE_URL;
    @Value("${ps.immig.hdss.authtoken}")
    private  String AUTH_TOKEN ;
    //10.26.215.182:/FPG2/miniplum/miniplum/
    @Value("${ps.immig.nas.path}")
    private  String NAS_PATH;

    @Value("${ps.immig.hdss.ratelimit:100}")
    private int rateLimit;

    @PostConstruct
    private void init() {
        this.rateLimiter = RateLimiter.create(rateLimit);
    }

    public HDSSApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.requestTimestamps = new ConcurrentLinkedQueue<>();
        this.restTemplate.setInterceptors(Collections.singletonList(new BearerTokenInterceptor()));
    }



    private class BearerTokenInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, AUTH_TOKEN);
            request.getHeaders().set(HttpHeaders.ACCEPT, "application/pdf");
            return execution.execute(request, body);
        }
    }


    public File callExternalApi(Long contractNumber, String docType)  {
        // 尝试获取令牌，如果没有可用的令牌，则会阻塞直到获取到令牌
        rateLimiter.acquire();

        long currentTime1 = System.currentTimeMillis();
        requestTimestamps.add(currentTime1);
        while (!requestTimestamps.isEmpty() && currentTime1 - requestTimestamps.peek() > TimeUnit.MINUTES.toMillis(1)) {
            requestTimestamps.poll();
        }


        // 调用外部 HDSS API 的具体逻辑
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("contractNumber", contractNumber)
                .queryParam("docType", docType)
                .queryParam("from", "minip")
                .toUriString();


            File file= restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
                byte[] buffer = new byte[1024];
                int bytesRead;

                String uniqueFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase();
                File f = new File(NAS_PATH + uniqueFileName + ".pdf");
                try (FileOutputStream outputStream = new FileOutputStream(f)) {
                    while ((bytesRead = clientHttpResponse.getBody().read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                return f;
            });

        return file;

    }
}
