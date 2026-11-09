
package com.duoc.tickets_s3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(@Value("${aws.region}") String awsRegion) {

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}
