package com.example.sfe4j;

import com.example.sfe4j.core.service.Sfe4jProperties;
import com.example.sfe4j.sftp.config.SftpConnectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({Sfe4jProperties.class, SftpConnectionProperties.class})
public class Sfe4jSftpApplication {

    public static void main(String[] args) {
        SpringApplication.run(Sfe4jSftpApplication.class, args);
    }
}
