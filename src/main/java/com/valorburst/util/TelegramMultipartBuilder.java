package com.valorburst.util;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TelegramMultipartBuilder {

    private static final String LINE_FEED = "\r\n";

    public static HttpRequest.BodyPublisher buildMultipartFileUpload(
            String chatId,
            File file,
            String filename,
            String boundary
    ) throws IOException {

        List<byte[]> byteArrays = new ArrayList<>();

        // --- Part 1: chat_id field ---
        byteArrays.add(("--" + boundary + LINE_FEED).getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Disposition: form-data; name=\"chat_id\"" + LINE_FEED + LINE_FEED).getBytes(StandardCharsets.UTF_8));
        byteArrays.add((chatId + LINE_FEED).getBytes(StandardCharsets.UTF_8));

        // --- Part 2: document file field ---
        byteArrays.add(("--" + boundary + LINE_FEED).getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Disposition: form-data; name=\"document\"; filename=\"" + filename + "\"" + LINE_FEED).getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" + LINE_FEED + LINE_FEED).getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(file.toPath()));
        byteArrays.add(LINE_FEED.getBytes(StandardCharsets.UTF_8));

        // --- End boundary ---
        byteArrays.add(("--" + boundary + "--" + LINE_FEED).getBytes(StandardCharsets.UTF_8));

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}
