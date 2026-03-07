package com.volunteer.volunteersystem;

import org.mybatis.spring.annotation.MapperScan; // 注意导入这个包
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.volunteer.volunteersystem.mapper") // 🚨 确保路径和你 mapper 包的路径完全一致
public class VolunteerSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(VolunteerSystemApplication.class, args);
    }
}