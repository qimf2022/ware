package com.shiyu.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 后端服务启动入口。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ShiYuBackendApplication {

    /**
     * 应用主入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ShiYuBackendApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  诗语启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "_____   __     __ \n" +
                " / ____|  \\ \\   / /\n" +
                "| (___     \\ \\_/ / \n" +
                " \\___ \\     \\   /  \n" +
                " ____) |     | |   \n" +
                "|_____/      |_| ");
    }
}
