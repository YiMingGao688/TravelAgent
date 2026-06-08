package com.itheima.travelagent2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class TravelAgent2Application {
    public static void main(String[] args) {
        SpringApplication.run(TravelAgent2Application.class, args);
    }
}
