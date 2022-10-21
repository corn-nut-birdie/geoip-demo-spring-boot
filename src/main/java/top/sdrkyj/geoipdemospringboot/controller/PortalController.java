package top.sdrkyj.geoipdemospringboot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("details")
@Slf4j
public class PortalController {
    @GetMapping(value = "headers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public MultiValueMap<String, Object> showHeaders(@RequestHeader MultiValueMap<String, Object> headers) {
        log.info("showHeaders entered");
        log.info("headers, {}", headers);
        return headers;
    }
}
