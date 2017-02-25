package il.ac.idc.yonatan.causality;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Created by ygraber on 1/28/17.
 */
@SpringBootApplication
public class Main {

    @Bean
    public Module guavaModule() {
        return new GuavaModule();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
