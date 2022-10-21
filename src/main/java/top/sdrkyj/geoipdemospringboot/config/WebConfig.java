package top.sdrkyj.geoipdemospringboot.config;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
@Slf4j
public class WebConfig {
    @Bean
    public DatabaseReader geoDatabaseReader(@Value("GeoLite2-City.mmdb") ClassPathResource resource
    ) throws IOException {
        log.info("geoDatabaseReader entered");

        return new DatabaseReader.Builder(resource.getFile()).withCache(new CHMCache()).build();
    }
}
