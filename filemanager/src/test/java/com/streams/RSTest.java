package com.streams;

import com.github.yafna.filemanager.RSChunked;
import com.github.yafna.filemanager.RSDecodeSetup;
import com.github.yafna.filemanager.RSEncodeSetup;
import org.junit.Assert;
import org.junit.Assume;
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

public class RSTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testEncodeDecodeConsistency() throws IOException {
        String fileName = "wallp.jpg";
        Path testItem = new File(getClass().getClassLoader().getResource(fileName).getFile()).toPath();
        Path outputDir = folder.newFolder().toPath();

        RSEncodeSetup encodeSetup = new RSEncodeSetup(testItem, outputDir);
        RSChunked m = new RSChunked();
        m.encode(encodeSetup);

        RSDecodeSetup decodeSetup = new RSDecodeSetup(encodeSetup, outputDir.resolve(testItem.getFileName() + ".0"), outputDir);
        m.decode(decodeSetup);

        try (ByteChannel brSrc = Files.newByteChannel(testItem, StandardOpenOption.READ);
             ByteChannel brDst = Files.newByteChannel(outputDir.resolve(testItem.getFileName()), StandardOpenOption.READ)) {
            ByteBuffer bufferSrc = ByteBuffer.allocate(10000);
            ByteBuffer bufferDst = ByteBuffer.allocate(10000);
            int noOfBytesReadSrc = brSrc.read(bufferSrc);
            int noOfBytesReadDst = brDst.read(bufferDst);
            while (noOfBytesReadSrc > 0) {
                Assert.assertEquals(noOfBytesReadDst, noOfBytesReadSrc);
                for (int i = 0; i < noOfBytesReadSrc; ++i) {
                    Assert.assertEquals(bufferSrc.array()[i], bufferDst.array()[i]);
                }
                bufferSrc.clear();
                bufferDst.clear();
                noOfBytesReadSrc = brSrc.read(bufferSrc);
                noOfBytesReadDst = brDst.read(bufferDst);
            }
        }
    }

    @Test
    public void testEncodeDecodeWithPartialData() throws IOException {
        String fileName = "wallp.jpg";
        Path testItem = new File(getClass().getClassLoader().getResource(fileName).getFile()).toPath();
        Path outputDir = folder.newFolder().toPath();

        RSEncodeSetup encodeSetup = new RSEncodeSetup(testItem, outputDir, 3, 1);
        RSChunked m = new RSChunked();
        m.encode(encodeSetup);

        Assume.assumeTrue(outputDir.resolve(testItem.getFileName() + ".0").toFile().delete());

        RSDecodeSetup decodeSetup = new RSDecodeSetup(encodeSetup, outputDir.resolve(testItem.getFileName() + ".1"), outputDir);
        m.decode(decodeSetup);

        try (ByteChannel brSrc = Files.newByteChannel(testItem, StandardOpenOption.READ);
             ByteChannel brDst = Files.newByteChannel(outputDir.resolve(testItem.getFileName()), StandardOpenOption.READ)) {
            ByteBuffer bufferSrc = ByteBuffer.allocate(10000);
            ByteBuffer bufferDst = ByteBuffer.allocate(10000);
            int noOfBytesReadSrc = brSrc.read(bufferSrc);
            int noOfBytesReadDst = brDst.read(bufferDst);
            while (noOfBytesReadSrc > 0) {
                Assert.assertEquals(noOfBytesReadDst, noOfBytesReadSrc);
                for (int i = 0; i < noOfBytesReadSrc; ++i) {
                    Assert.assertEquals(bufferSrc.array()[i], bufferDst.array()[i]);
                }
                bufferSrc.clear();
                bufferDst.clear();
                noOfBytesReadSrc = brSrc.read(bufferSrc);
                noOfBytesReadDst = brDst.read(bufferDst);
            }
        }
    }
}
