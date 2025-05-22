package bit.bitgroundspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
public class BitGroundSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(BitGroundSpringApplication.class, args);
    }

}
