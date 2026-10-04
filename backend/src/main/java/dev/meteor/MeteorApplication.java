package dev.meteor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({
        "dev.meteor.product.mapper",
        "dev.meteor.auth.mapper",
        "dev.meteor.order.mapper",
        "dev.meteor.payment.mapper",
        "dev.meteor.entitlement.mapper",
        "dev.meteor.delivery.mapper",
        "dev.meteor.download.mapper"
})
@SpringBootApplication
public class MeteorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeteorApplication.class, args);
    }
}
