package com.sachetto.streaming.util;

import java.util.concurrent.atomic.AtomicLong;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FixedProtocolGenerator {
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final String MACHINE_ID = "X1"; 

    private static String toBase36Padded(long value, int width) {
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.insert(0, CHARS.charAt((int) (value % 36)));
            value /= 36;
        }
        while (sb.length() < width) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    public static String generate() {
        long timestamp = System.currentTimeMillis();
        long seq = SEQUENCE.getAndIncrement() % 1296;

        String timePart = toBase36Padded(timestamp, 8);
        String seqPart = toBase36Padded(seq, 2);

        return (timePart + MACHINE_ID + seqPart).toUpperCase();
    }
}
