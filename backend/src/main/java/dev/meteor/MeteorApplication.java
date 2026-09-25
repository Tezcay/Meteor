package dev.meteor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("dev.meteor.product.mapper")
@SpringBootApplication
public class MeteorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeteorApplication.class, args);
    }
}
