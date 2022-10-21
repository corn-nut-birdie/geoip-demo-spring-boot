package top.sdrkyj.geoipdemospringboot.geoip.qqwry.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class IndexRecord {
    String currentIp;
    String nextIp;
    private Integer offset;
}
