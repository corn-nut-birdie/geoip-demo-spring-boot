package top.sdrkyj.geoipdemospringboot.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.springframework.stereotype.Component;
import top.sdrkyj.geoipdemospringboot.geoip.entity.GeoIpHolder;
import top.sdrkyj.geoipdemospringboot.geoip.qqwry.manager.QqwryParser;

import java.io.IOException;
import java.net.InetAddress;

@Component
public class GeoParser {
    private final DatabaseReader databaseReader;
    private final QqwryParser qqwryParser;

    public GeoParser(DatabaseReader databaseReader, QqwryParser qqwryParser) {
        this.databaseReader = databaseReader;
        this.qqwryParser = qqwryParser;
    }

    public GeoIpHolder parse(String ipStr) throws IOException, GeoIp2Exception {
        return new GeoIpHolder(
                qqwryParser.searchGeoRecord(ipStr),
                databaseReader.tryCity(InetAddress.getByName(ipStr)).orElse(null)
        );
    }
}
