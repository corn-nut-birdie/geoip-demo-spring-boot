package top.sdrkyj.geoipdemospringboot.geoip.qqwry.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class GeoRecord {
    String ip;
    String ipStart;
    String ipEnd;
    String recordA;
    String recordB;
    String format;

    public GeoRecord(String ipStart, String ipEnd, String recordA, String recordB, String format) {
        this.ipStart = ipStart;
        this.ipEnd = ipEnd;
        this.recordA = recordA;
        this.recordB = recordB;
        this.format = format;
    }
}
