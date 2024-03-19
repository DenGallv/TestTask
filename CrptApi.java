package com.example.requestrc;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    TimeUnit timeUnit;
    int restLimit;
    RestClient restClient = RestClient.create();
    Semaphore semaphore;
    Logger logger = LoggerFactory.getLogger(CrptApi.class);

    public CrptApi(TimeUnit timeUnit, int restLimit) {
        this.timeUnit = timeUnit;
        this.restLimit = restLimit;
        this.semaphore = new Semaphore(restLimit);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                semaphore.release(restLimit - semaphore.availablePermits());
            }
        }, timeUnit.toMillis(1), timeUnit.toMillis(1));
    }

    public void createDocument(Document document) {
        try {
            semaphore.acquire();
            ResponseEntity<Document> response = restClient.post()
                    .uri("https://ismp.crpt.ru/api/v3/lk/documents/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(document)
                    .retrieve()
                    .toEntity(Document.class);

            Assertions.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
            logger.info("Документ создан успешно");
            Assertions.assertNotNull(response);
            logger.info("Документ c id:" + response.getBody().getDoc_id() + " прошел проверку на null");
        } catch (InterruptedException e) {
            logger.error("Превышено ограничение на количество запросов");
        }
    }
}
