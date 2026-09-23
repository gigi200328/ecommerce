package com.ojt.ecommerce.backofficeinventory.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        // URL Path (/uploads/product-images/**)  Local System Folder (file:uploads/product-images/) 
        registry.addResourceHandler("/uploads/product-images/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}