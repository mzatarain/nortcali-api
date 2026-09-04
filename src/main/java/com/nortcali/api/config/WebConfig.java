package com.nortcali.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RestaurantAccessInterceptor restaurantAccessInterceptor;

    public WebConfig(RestaurantAccessInterceptor restaurantAccessInterceptor) {
        this.restaurantAccessInterceptor = restaurantAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restaurantAccessInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
