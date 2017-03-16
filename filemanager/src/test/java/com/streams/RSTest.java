package com.streams;

import com.github.yafna.filemanager.RSChunked;
import com.github.yafna.filemanager.RSDecodeSetup;
import com.github.yafna.filemanager.RSEncodeSetup;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class RSTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss").withZone(ZoneId.systemDefault());

    @Test
    public void testEncodeDecodeConsistency() throws IOException {
        String fileName = "wallp.jpg";
        Path testItem = new File(getClass().getClassLoader().getResource(fileName).getFile()).toPath();
        Path outputDir = folder.newFolder(datetimeFormatter.format(Instant.now())).toPath();

        RSEncodeSetup encodeSetup = new RSEncodeSetup(testItem, outputDir);
        RSChunked m = new RSChunked();
        m.encode(encodeSetup);

        RSDecodeSetup decodeSetup=  new RSDecodeSetup(encodeSetup, outputDir.resolve(testItem.getFileName() + ".0"), outputDir);
        m.decode(decodeSetup);

        try (ByteChannel brSrc = Files.newByteChannel(testItem, StandardOpenOption.READ);
             ByteChannel brDst = Files.newByteChannel(outputDir.resolve(testItem.getFileName()), StandardOpenOption.READ)) {
            ByteBuffer bufferSrc = ByteBuffer.allocate(10000);
            ByteBuffer bufferDst = ByteBuffer.allocate(10000);
            int noOfBytesReadSrc = brSrc.read(bufferSrc);
            int noOfBytesReadDst = brDst.read(bufferDst);

            Assert.assertEquals(noOfBytesReadDst, noOfBytesReadSrc);
            for (int i = 0; i < noOfBytesReadSrc; ++i) {
                Assert.assertEquals(bufferSrc.array()[i], bufferDst.array()[i]);
            }
        }
    }
}
