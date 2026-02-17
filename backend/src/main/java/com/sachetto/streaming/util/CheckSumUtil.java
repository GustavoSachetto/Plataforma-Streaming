package com.sachetto.streaming.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CheckSumUtil {

    private static final int BUFFER_8KB = 8192;
    private static final String ALGORITHM = "SHA-256";

    public static String calculateSingleFileHash(Path path) {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
            return calculateHash(List.of(is));
        } catch (Exception _) {
            throw new RuntimeException();
        }
    }
    
    public static boolean isValid(MultipartFile file, String clientChecksum) {
        if (clientChecksum == null || clientChecksum.isBlank()) return false;
        try (InputStream is = file.getInputStream()) {
            return calculateHash(List.of(is)).equalsIgnoreCase(clientChecksum);
        } catch (Exception _) {
            return false;
        }
    }

    public static boolean isValidFullFile(List<String> chunks, String expectedFullHash) {      
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_8KB];

            for (String chunkPathStr : chunks) {
                Path path = Paths.get(chunkPathStr);
                
                try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        digest.update(buffer, 0, bytesRead);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(expectedFullHash);
        } catch (Exception _) {
            throw new RuntimeException();
        }
    }

    private static String calculateHash(List<InputStream> streams) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        byte[] buffer = new byte[BUFFER_8KB];

        for (InputStream is : streams) {
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        return HexFormat.of().formatHex(digest.digest());
    }
}