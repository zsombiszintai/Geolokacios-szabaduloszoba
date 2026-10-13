package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.config.MinioConfig;
import com.cityscape.geoszabaduloszobabackend.config.MinioProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MinioPropertiesTest {
    @Test void bindsExistingCamelCaseKeysAndBuildsClientWithoutNetworkCalls() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(MinioProperties.class, MinioConfig.class)
                .withPropertyValues("minio.url=https://storage.example.com", "minio.accessKey=access",
                        "minio.secretKey=secret", "minio.bucketName=avatars")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(MinioClient.class);
                    var properties = context.getBean(MinioProperties.class);
                    assertThat(properties.getUrl()).isEqualTo("https://storage.example.com");
                    assertThat(properties.getAccessKey()).isEqualTo("access");
                    assertThat(properties.getSecretKey()).isEqualTo("secret");
                    assertThat(properties.getBucketName()).isEqualTo("avatars");
                });
    }
}
