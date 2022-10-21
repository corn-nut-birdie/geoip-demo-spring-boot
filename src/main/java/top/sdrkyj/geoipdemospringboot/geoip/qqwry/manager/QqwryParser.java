package top.sdrkyj.geoipdemospringboot.geoip.qqwry.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import top.sdrkyj.geoipdemospringboot.geoip.qqwry.entity.GeoRecord;
import top.sdrkyj.geoipdemospringboot.geoip.qqwry.entity.IndexRecord;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;

@Component
@Slf4j
public class QqwryParser {
    @Value("qqwry.dat")
    private ClassPathResource resource;

    public String[] versionInfo() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(resource.getFile(), "r")) {
            file.seek(4);
            int lastIndexRecordOffset = Integer.reverseBytes(file.readInt());

            log.trace("lastIndexRecordOffset, {}", lastIndexRecordOffset);

            file.seek(lastIndexRecordOffset + 4);
            file.seek(readOffset(file));
            return read2Strings(file);
        }
    }

    public GeoRecord searchGeoRecord(String searchIpStr) throws IOException {
        GeoRecord record = parseGeoRecord(searchIndexRecord(searchIpStr));
        record.setIp(searchIpStr);
        return record;
    }

    public IndexRecord searchIndexRecord(String searchIpStr) throws IOException {
        log.trace("searchIp entered");
        byte[] searchIp = Inet4Address.getByName(searchIpStr).getAddress();

        try (RandomAccessFile file = new RandomAccessFile(resource.getFile(), "r")) {
            // seek 0
            int firstIndexRecordOffset = Integer.reverseBytes(file.readInt());
            int lastIndexRecordOffset = Integer.reverseBytes(file.readInt());

            file.seek(firstIndexRecordOffset);
            byte[][] left = read2IpBytesWithOffset(file);
            if (Arrays.compareUnsigned(left[0], searchIp) <= 0 && Arrays.compareUnsigned(left[1], searchIp) >= 0) {
                // fetch left
                return new IndexRecord(
                        Inet4Address.getByAddress(left[0]).getHostAddress(),
                        Inet4Address.getByAddress(left[1]).getHostAddress(),
                        new BigInteger(left[2]).intValue()
                );
            }

            file.seek(lastIndexRecordOffset);
            byte[] right0 = readIpBytes(file);
            if (Arrays.compareUnsigned(right0, searchIp) <= 0) {
                // fetch right
                return new IndexRecord(
                        Inet4Address.getByAddress(right0).getHostAddress(),
                        null,
                        -1
                );
            }

            int leftRecordOffset = firstIndexRecordOffset;
            int rightRecordOffset = lastIndexRecordOffset;
            int midRecordOffset;
            byte[][] middle;
            while (true) {
                midRecordOffset = leftRecordOffset + (rightRecordOffset - leftRecordOffset) / 7 / 2 * 7;
                file.seek(midRecordOffset);
                middle = read2IpBytesWithOffset(file);
                if (Arrays.compareUnsigned(middle[0], searchIp) <= 0 && Arrays.compareUnsigned(middle[1], searchIp) >= 0) {
                    // fetch middle
                    return new IndexRecord(
                            Inet4Address.getByAddress(middle[0]).getHostAddress(),
                            Inet4Address.getByAddress(middle[1]).getHostAddress(),
                            new BigInteger(middle[2]).intValue()
                    );
                } else if (Arrays.compareUnsigned(middle[0], searchIp) > 0) {
                    log.trace("search left part");
                    rightRecordOffset = midRecordOffset;
                } else {
                    log.trace("search right part");
                    leftRecordOffset = midRecordOffset;
                }
            }
        } finally {
            log.trace("searchIp left");
        }
    }

    public GeoRecord parseGeoRecord(IndexRecord indexRecord) throws IOException {
        log.trace("parseRecord entered");

        if (indexRecord.getOffset() <= 0) {
            return new GeoRecord(indexRecord.getCurrentIp(), "255.255.255.255", "IANA保留地址", null, "0");
        }

        try (RandomAccessFile file = new RandomAccessFile(resource.getFile(), "r")) {
            file.seek(indexRecord.getOffset());

            InetAddress ipEnd = readIp(file);
            String recordA;
            String recordB;

            log.trace("ipEnd, {}", ipEnd.getHostAddress());

            byte mark;
            mark = file.readByte();
            log.trace("mark, {}", mark);

            String format;

            if (mark == 1) {
                //一次跳转
                file.seek(readOffset(file));
                mark = file.readByte();
                if (mark == 2) {
                    int offset = readOffset(file);
                    mark = file.readByte();
                    if (mark == 1 || mark == 2) {
                        // format 5
                        log.info("format 5");
                        format = "5";

                        int recordBOffset = readOffset(file);

                        file.seek(offset);
                        recordA = readString(file);

                        if (recordBOffset == 0) {
                            recordB = null;
                        } else {
                            file.seek(recordBOffset);
                            recordB = readString(file);
                        }
                    } else {
                        // format 4
                        log.info("format 4");
                        format = "4";

                        recordB = readString(file, mark);
                        file.seek(offset);
                        recordA = readString(file);
                    }
                } else {
                    // format 2
                    log.info("format 2");
                    format = "2";

                    recordA = readString(file, mark);

                    mark = file.readByte();
                    log.info("mark at b of format 2, {}", mark);
                    if (mark == 2) {
                        int offset = readOffset(file);
                        file.seek(offset);
                        recordB = readString(file);
                    } else {
                        recordB = readString(file, mark);
                    }
                }
            } else if (mark == 2) {
                // format 3
                log.info("format 3");
                format = "3";

                //二次跳转
                int recordAOffset = readOffset(file);
                //recordB = readString(file);

                mark = file.readByte();
                log.info("mark at b of format 3, {}", mark);
                if (mark == 2) {
                    int offset = readOffset(file);
                    file.seek(offset);
                    recordB = readString(file);
                } else {
                    recordB = readString(file, mark);
                }

                file.seek(recordAOffset);
                recordA = readString(file);
            } else {
                // format 1
                log.info("format 1");
                format = "1";

                //字符串
                String[] records = read2Strings(file, mark);
                recordA = records[0];
                recordB = records[1];
            }

            return new GeoRecord(indexRecord.getCurrentIp(), ipEnd.getHostAddress(), recordA,
                    Optional.ofNullable(recordB)
                            .filter(s -> !s.contains("CZ88.NET"))
                            .orElse(null)
                    , format);
        }
    }

    private byte[] readIpBytes(RandomAccessFile file) throws IOException {
        byte[] bytes = new byte[4];
        file.read(bytes);
        reverse(bytes);
        return bytes;
    }

    private byte[][] read2IpBytesWithOffset(RandomAccessFile file) throws IOException {
        byte[] ipBytes = new byte[11];
        file.readFully(ipBytes);
        reverse(ipBytes);
        return new byte[][]{
                Arrays.copyOfRange(ipBytes, 7, 11),
                Arrays.copyOfRange(ipBytes, 0, 4),
                Arrays.copyOfRange(ipBytes, 4, 7)
        };
    }

    private InetAddress readIp(RandomAccessFile file) throws IOException {
        byte[] ipBytes = new byte[4];
        file.readFully(ipBytes);
        reverse(ipBytes);
        return Inet4Address.getByAddress(ipBytes);
    }

    private String readString(RandomAccessFile file) throws IOException {
        byte[] buffer = new byte[2048];

        long offset = file.getFilePointer();
        file.readFully(buffer);
        int end1 = -1;

        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == 0) {
                end1 = i;
                break;
            }
        }
        file.seek(offset + end1);

        return new String(buffer, 0, end1, Charset.forName("GB18030"));
    }

    private String readString(RandomAccessFile file, byte firstByte) throws IOException {
        byte[] buffer = new byte[2048];
        buffer[0] = firstByte;

        long offset = file.getFilePointer();
        file.readFully(buffer, 1, buffer.length - 1);
        int end1 = -1;

        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == 0) {
                end1 = i;
                break;
            }
        }
        file.seek(offset + end1);

        return new String(buffer, 0, end1, Charset.forName("GB18030"));
    }

    private String[] read2Strings(RandomAccessFile file) throws IOException {
        byte[] buffer = new byte[4096];
        file.readFully(buffer);
        int end1 = -1, end2 = -1;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == 0) {
                if (end1 < 0) {
                    end1 = i;
                } else {
                    end2 = i;
                    break;
                }
            }
        }

        if (end1 < 0 || end2 <= 0) {
            return new String[2];
        } else {
            return new String[]{new String(buffer, 0, end1, Charset.forName("GB18030")),
                    new String(buffer, end1 + 1, end2 - end1 - 1, Charset.forName("GB18030"))};
        }
    }

    private String[] read2Strings(RandomAccessFile file, byte firstByte) throws IOException {
        log.info("read2Strings entered");

        byte[] buffer = new byte[4096];
        buffer[0] = firstByte;

        file.readFully(buffer, 1, buffer.length - 1);
        int end1 = -1, end2 = -1;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == 0) {
                if (end1 < 0) {
                    end1 = i;
                } else {
                    end2 = i;
                    break;
                }
            }
        }

        return new String[]{
                new String(buffer, 0, end1, Charset.forName("GB18030")),
                new String(buffer, end1 + 1, end2 - end1 - 1, Charset.forName("GB18030"))
        };
    }

    private int readOffset(RandomAccessFile file) throws IOException {
        byte[] bytes = new byte[4];
        file.read(bytes, 0, 3);
        reverse(bytes);

        //return Integer.reverseBytes(new BigInteger(bytes).intValue());
        return new BigInteger(bytes).intValue();
    }

    private void reverse(byte[] bytes) {
        byte t;
        for (int i = 0; i < bytes.length / 2; i++) {
            t = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = t;
        }
    }
}
