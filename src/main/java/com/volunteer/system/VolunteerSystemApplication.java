package com.volunteer.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动入口。
 *
 * 注意 @EnableScheduling 不可省略：RankTask 上的 @Scheduled 只有在开启定时任务支持后
 * 才会被注册；缺少该注解时，每日排名快照任务会静默地「从不执行」。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.volunteer.system.mapper")
public class VolunteerSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(VolunteerSystemApplication.class, args);
    }
}