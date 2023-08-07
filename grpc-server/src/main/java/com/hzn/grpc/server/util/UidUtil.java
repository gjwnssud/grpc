package com.hzn.grpc.server.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class UidUtil {
    public static String getID(String type) {

        switch (type) {
            case "L" :
                return UUID.randomUUID().toString().toLowerCase();
            case "U" :
                return UUID.randomUUID().toString().toUpperCase();
            default:
                String uuid = UUID.randomUUID().toString().toUpperCase();

                Random rn = new Random();
                String[] arr = uuid.split("(?<!^)");
                for(int i=0; i<=16; i++) {
                    int num =rn.nextInt(35);
                    arr[num] = arr[num].toLowerCase();
                }
                return Arrays.stream(arr).collect(Collectors.joining());
        }

    }

    public static String getIDV1() {
        String uuid = generateType1UUID().toString();
        uuid = uuid.replaceAll("-", "");
        uuid = uuid.substring(0,20);
        return uuid;
    }

    private static long get64LeastSignificantBitsForVersion1() {
        Random random = new Random();
        long random63BitLong = random.nextLong() & 0x3FFFFFFFFFFFFFFFL;
        long variant3BitFlag = 0x8000000000000000L;
        return random63BitLong + variant3BitFlag;
    }

    private static long get64MostSignificantBitsForVersion1() {
        LocalDateTime start = LocalDateTime.of(1582, 10, 15, 0, 0, 0);
        Duration duration = Duration.between(start, LocalDateTime.now());
        long seconds = duration.getSeconds();
        long nanos = duration.getNano();
        long timeForUuidIn100Nanos = seconds * 10000000 + nanos * 100;
        long least12SignificatBitOfTime = (timeForUuidIn100Nanos & 0x000000000000FFFFL) >> 4;
        long version = 1 << 12;
        return
                (timeForUuidIn100Nanos & 0xFFFFFFFFFFFF0000L) + version + least12SignificatBitOfTime;
    }

    public static UUID generateType1UUID() {

        long most64SigBits = get64MostSignificantBitsForVersion1();
        long least64SigBits = get64LeastSignificantBitsForVersion1();

        return new UUID(least64SigBits, most64SigBits);
    }


}
