package pe.yuseok.kim.hwpconvert.config;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class ThymeleafConfig {
    
    @PostConstruct
    public void configureThymeleaf() {
        // Allow request, session, servletContext and response objects
        System.setProperty("org.thymeleaf.spring6.expression.objects.enableRequests", "true");
        System.setProperty("org.thymeleaf.spring6.expression.objects.enableSessions", "true");
        System.setProperty("org.thymeleaf.spring6.expression.objects.enableServletContext", "true");
        System.setProperty("org.thymeleaf.spring6.expression.objects.enableResponses", "true");
    }
} 