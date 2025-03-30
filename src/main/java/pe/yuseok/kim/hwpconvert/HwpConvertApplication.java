package pe.yuseok.kim.hwpconvert;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import pe.yuseok.kim.hwpconvert.service.UserService;
import pe.yuseok.kim.hwpconvert.util.FileUtils;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class HwpConvertApplication {

    public static void main(String[] args) {
        SpringApplication.run(HwpConvertApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner initializeApplication(UserService userService, FileUtils fileUtils) {
        return args -> {
            // Create necessary directories
            fileUtils.createDirectories();
            
            // Create admin user if not exists
            userService.createAdminUserIfNotExists();
        };
    }
}
