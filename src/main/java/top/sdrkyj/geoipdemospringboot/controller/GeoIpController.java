package top.sdrkyj.geoipdemospringboot.controller;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import top.sdrkyj.geoipdemospringboot.geoip.GeoParser;
import top.sdrkyj.geoipdemospringboot.geoip.entity.GeoIpHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@CrossOrigin
@Slf4j
public class GeoIpController {
    private final GeoParser geoParser;

    public GeoIpController(GeoParser geoParser) {
        this.geoParser = geoParser;
    }

    @GetMapping
    @ResponseBody
    public Map<String, Object> geoIp(HttpServletRequest request,
                                     @RequestHeader(value = "x-forwarded-for", required = false) String xForwardedFor) throws IOException, GeoIp2Exception {
        log.debug("geoIp entered");

        String addr = Optional.ofNullable(xForwardedFor).orElse(request.getRemoteAddr());

        GeoIpHolder holder = geoParser.parse(addr);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("address", addr);
        map.put("parser-1", holder.getGeoRecord());
        map.put("parser-2", holder.getCityResponse());

        return map;
    }

    @GetMapping("slim")
    @ResponseBody
    public Map<String, Object> geoIpSlim(HttpServletRequest request,
                                         @RequestHeader(value = "x-forwarded-for", required = false) String xForwardedFor) throws IOException, GeoIp2Exception {
        log.debug("geoIpSlim entered");

        String addr = Optional.ofNullable(xForwardedFor).orElse(request.getRemoteAddr());
        GeoIpHolder holder = geoParser.parse(addr);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("address", addr);
        map.put("parser-1", holder.getGeoRecord());

        if (holder.getCityResponse() != null) {
            Map<String, Object> geoip2 = new LinkedHashMap<>();
            geoip2.put("continent-code", holder.getCityResponse().getContinent().getCode());
            geoip2.put("continent-name", holder.getCityResponse().getContinent().getNames().get("zh-CN"));
            geoip2.put("country-iso-code", holder.getCityResponse().getCountry().getIsoCode());
            geoip2.put("country-name", holder.getCityResponse().getCountry().getNames().getOrDefault("zh-CN", holder.getCityResponse().getCountry().getName()));
            geoip2.put("city-name", holder.getCityResponse().getCity().getNames().getOrDefault("zh-CN", holder.getCityResponse().getCity().getName()));
            geoip2.put("postal", holder.getCityResponse().getPostal());

            map.put("parser-2", geoip2);
        }

        return map;
    }

    @GetMapping("query")
    @ResponseBody
    public Map<String, Object> query(String ip) throws IOException, GeoIp2Exception {
        log.debug("query entered");

        String addr = InetAddress.getByName(ip).getHostAddress();
        GeoIpHolder holder = geoParser.parse(addr);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("address", addr);
        map.put("parser-1", holder.getGeoRecord());
        map.put("parser-2", holder.getCityResponse());

        return map;
    }
}
