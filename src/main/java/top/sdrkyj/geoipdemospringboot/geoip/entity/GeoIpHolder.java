package top.sdrkyj.geoipdemospringboot.geoip.entity;

import com.maxmind.geoip2.model.CityResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.sdrkyj.geoipdemospringboot.geoip.qqwry.entity.GeoRecord;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class GeoIpHolder {
    private GeoRecord geoRecord;
    private CityResponse cityResponse;
}
