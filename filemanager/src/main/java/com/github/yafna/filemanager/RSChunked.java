package com.github.yafna.filemanager;

import com.backblaze.erasure.ReedSolomon;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RSChunked {

    // buffer size used for writing
    private static final int BUFFER_SIZE = 8192;

    public void decode(RSDecodeSetup setup) throws IOException {
        validateDecodeSetup(setup);
        List<ByteChannel> channels = new ArrayList<>();
        try {
            boolean[] shardPresent = new boolean[setup.getEncodeSetup().getTotalShardCount()];
            for (int i = 0; i < setup.getEncodeSetup().getTotalShardCount(); i++) {
                Path shardFile = setup.getShardByIndex(i);
                if (shardFile.toFile().exists()) {
                    channels.add(Files.newByteChannel(shardFile, StandardOpenOption.READ));
                    shardPresent[i] = true;
                }
            }
            int actualSize = setup.getEncodeSetup().getShardSize();
            while (actualSize > 0) {
                byte[][] shards = new byte[setup.getEncodeSetup().getTotalShardCount()][];
                int shardFillingIndex = 0;
                for (int i = 0; i < channels.size(); i++) {
                    ByteBuffer buffer = ByteBuffer.allocate(setup.getEncodeSetup().getShardSize());
                    actualSize = channels.get(i).read(buffer);
                    if (actualSize == -1) {
                        return;
                    }
                    while (!shardPresent[shardFillingIndex]) {
                        shardFillingIndex++;
                    }
                    shards[shardFillingIndex] = new byte[actualSize];
                    System.arraycopy(buffer.array(), 0, shards[shardFillingIndex], 0, actualSize);
                    shardFillingIndex++;
                }
                for (int i = 0; i < setup.getEncodeSetup().getTotalShardCount(); i++) {
                    if (!shardPresent[i]) {
                        shards[i] = new byte[actualSize];
                    }
                }

                ReedSolomon reedSolomon = ReedSolomon.create(setup.getEncodeSetup().getDataShardsCount(), setup.getEncodeSetup().getParityShardsCount());
                reedSolomon.decodeMissing(shards, shardPresent, 0, actualSize);

                // get chunk actual length and write to output
                int fileSize = ((shards[0][0] << 24) | ((shards[0][1] & 0xff) << 16) | ((shards[0][2] & 0xff) << 8) | ((shards[0][3] & 0xff)));
                if (!setup.getDestFile().toFile().exists()) {
                    setup.getDestFile().toFile().createNewFile();
                }
                write(setup.getDestFile(), shards[0], 4, fileSize < (shards[0].length - 4) ? fileSize : (shards[0].length - 4));
                fileSize -= (shards[0].length - 4);
                int i = 1;
                while (fileSize > 0) {
                    write(setup.getDestFile(), shards[i], 0, fileSize < shards[i].length ? fileSize : shards[i].length);
                    fileSize -= shards[i].length;
                    i++;
                }
            }
        } finally {
            for (ByteChannel channel : channels) {
                try {
                    channel.close();
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    public void encode(RSEncodeSetup setup) throws IOException {
        try (ByteChannel br = Files.newByteChannel(setup.getSourceFile(), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(setup.getReadBufferSize());
            int noOfBytesRead = br.read(buffer);
            while (noOfBytesRead != -1) {
                byte[][] shards = doencoding(setup, buffer, noOfBytesRead);
                for (int i = 0; i < setup.getTotalShardCount(); i++) {
                    Path shard = setup.getShardByIndex(i);
                    if (!shard.toFile().exists()) {
                        shard.toFile().createNewFile();
                    }
                    write(shard, shards[i], 0, shards[i].length);
                }
                buffer.clear();
                noOfBytesRead = br.read(buffer);
            }
        }
    }

    private byte[][] doencoding(RSEncodeSetup setup, ByteBuffer data, int noOfBytesRead) {
        // Create a buffer holding the file size, followed by the contents of the file.
        byte[] allBytes = new byte[setup.getAllBufferSize()];
        allBytes[0] = (byte) (noOfBytesRead >> 24);
        allBytes[1] = (byte) (noOfBytesRead >> 16);
        allBytes[2] = (byte) (noOfBytesRead >> 8);
        allBytes[3] = (byte) (noOfBytesRead);
        System.arraycopy(data.array(), 0, allBytes, 4, setup.getReadBufferSize());
        // Make the buffers to hold the shards.
        byte[][] shards = new byte[setup.getTotalShardCount()][setup.getShardSize()];
        // Fill in the data shards
        for (int i = 0; i < setup.getDataShardsCount(); i++) {
            System.arraycopy(allBytes, i * setup.getShardSize(), shards[i], 0, setup.getShardSize());
        }

        // Use Reed-Solomon to calculate the parity.
        ReedSolomon reedSolomon = ReedSolomon.create(setup.getDataShardsCount(), setup.getParityShardsCount());
        reedSolomon.encodeParity(shards, 0, setup.getShardSize());
        return shards;
    }

    private void validateDecodeSetup(RSDecodeSetup setup) throws IOException {
        if (!setup.getSource().toFile().exists()) {
            throw new FileNotFoundException("File is not found " + setup.getSource().toString());
        }
        long shardSize = 0L;
        int shardsPresent = 0;
        for (int i = 0; i < setup.getEncodeSetup().getTotalShardCount(); i++) {
            Path shardFile = setup.getSource().getParent().resolve(setup.getFileCoreName() + "." + i);
            if (shardFile.toFile().exists()) {
                if (shardSize > 0) {
                    if (shardSize != shardFile.toFile().length()) {
                        throw new IOException("Shards expected to be same size, probably data was corrupted");
                    }
                } else {
                    shardSize = shardFile.toFile().length();
                }
                shardsPresent++;
            }
        }
        if (shardsPresent < setup.getEncodeSetup().getDataShardsCount()) {
            throw new IOException("Not enough shards present expected at least " + setup.getEncodeSetup().getDataShardsCount() + " shards");
        }
    }

    private void write(Path path, byte[] bytes, int offset, int length) throws IOException {
        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.APPEND)) {
            int rem = length;
            while (rem > 0) {
                int n = Math.min(rem, BUFFER_SIZE);
                out.write(bytes, offset + (length - rem), n);
                rem -= n;
            }
        }
    }
}
