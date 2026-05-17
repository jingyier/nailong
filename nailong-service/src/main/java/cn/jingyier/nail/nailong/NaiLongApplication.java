package cn.jingyier.nail.nailong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NaiLongApplication {

    public static void main(String[] args) {
        SpringApplication.run(NaiLongApplication.class, args);
    }

}