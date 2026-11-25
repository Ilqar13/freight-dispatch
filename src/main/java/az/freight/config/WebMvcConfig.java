package az.freight.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final DriverAuthInterceptor driverAuth;
    private final CustomerAuthInterceptor customerAuth;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(driverAuth)
                .addPathPatterns("/api/driver/**")
                .excludePathPatterns("/api/driver/login");
        registry.addInterceptor(customerAuth)
                .addPathPatterns("/api/customer/**")
                .excludePathPatterns("/api/customer/login", "/api/customer/register");
    }
}
