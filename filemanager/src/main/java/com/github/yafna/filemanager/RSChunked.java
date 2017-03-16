package com.github.yafna.filemanager;

import com.backblaze.erasure.ReedSolomon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class RSChunked {

    public void decode(RSDecodeSetup setup) {
        if (!setup.getSource().toFile().exists()) {
            System.out.println("Cannot read input file: " + setup.getSource().toString());
            return;
        }
        String coreName = setup.getSource().getFileName().toString().substring(0, setup.getSource().getFileName().toString().lastIndexOf("."));

        // Read in any of the shards that are present.
        final boolean[] shardPresent = new boolean[setup.getEncodeSetup().getTotalShardCount()];
        long shardSize = 0L;
        List<ByteChannel> channels = new ArrayList<>();
        try {
            for (int i = 0; i < setup.getEncodeSetup().getTotalShardCount(); i++) {
                Path shardFile = setup.getSource().getParent().resolve(coreName + "." + i);
                if (shardFile.toFile().exists()) {
                    if (shardSize > 0) {
                        if (shardSize != shardFile.toFile().length()) {
                            System.out.println("Shards expected to be same size");
                        }
                    } else {
                        shardSize = shardFile.toFile().length();
                    }
                    channels.add(Files.newByteChannel(setup.getSource().getParent().resolve(coreName + "." + i), StandardOpenOption.READ));
                    shardPresent[i] = true;
                }
            }
            if (channels.size() < setup.getEncodeSetup().getDataShardsCount()) {
                System.out.println("Not enough shards present");
                return;
            }
            int actualSize = setup.getEncodeSetup().getShardSize();
            while (shardSize > 0 && actualSize > 0) {
                byte[][] shards = new byte[setup.getEncodeSetup().getTotalShardCount()][];
                for (int i = 0; i < channels.size(); i++) {
                    ByteBuffer buffer = ByteBuffer.allocate(setup.getEncodeSetup().getShardSize());
                    actualSize = channels.get(i).read(buffer);
                    shards[i] = new byte[actualSize];
                    System.arraycopy(buffer.array(), 0, shards[i], 0, actualSize);
                }
                shardSize -= actualSize;

                // Make empty buffers for the missing shards.
                for (int i = channels.size(); i < setup.getEncodeSetup().getTotalShardCount(); i++) {
                    if (!shardPresent[i]) {
                        shards[i] = new byte[actualSize];
                    }
                }

                // Use Reed-Solomon to fill in the missing shards
                ReedSolomon reedSolomon = ReedSolomon.create(setup.getEncodeSetup().getDataShardsCount(), setup.getEncodeSetup().getParityShardsCount());
                reedSolomon.decodeMissing(shards, shardPresent, 0, actualSize);

                // Combine the data shards into one buffer for convenience. (This is not efficient, but it is convenient.)
                byte[] allBytes = new byte[actualSize * setup.getEncodeSetup().getDataShardsCount()];
                for (int i = 0; i < setup.getEncodeSetup().getDataShardsCount(); i++) {
                    System.arraycopy(shards[i], 0, allBytes, actualSize * i, actualSize);
                }

                // get chunk actual length
                int fileSize = ((allBytes[0] << 24) | ((allBytes[1] & 0xff) << 16) | ((allBytes[2] & 0xff) << 8) | ((allBytes[3] & 0xff)));

                byte[] toWrite = new byte[fileSize];
                System.arraycopy(allBytes, 4, toWrite, 0, fileSize);
                // Write the decoded file
                if (!setup.getDestDir().resolve(coreName).toFile().exists()) {
                    setup.getDestDir().resolve(coreName).toFile().createNewFile();
                }
                Files.write(setup.getDestDir().resolve(coreName), toWrite, StandardOpenOption.APPEND);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            for (ByteChannel channel : channels) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void encode(RSEncodeSetup setup) {
        try (ByteChannel br = Files.newByteChannel(setup.getSourceFile(), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(setup.getReadBufferSize());
            int noOfBytesRead = br.read(buffer);
            while (noOfBytesRead != -1) {
                byte[][] shards = doencoding(setup, buffer, noOfBytesRead);
                for (int i = 0; i < setup.getTotalShardCount(); i++) {
                    if (!setup.getShardsFolder().resolve(setup.getSourceFile().getFileName() + "." + i).toFile().exists()) {
                        setup.getShardsFolder().resolve(setup.getSourceFile().getFileName() + "." + i).toFile().createNewFile();
                    }
                    Files.write(setup.getShardsFolder().resolve(setup.getSourceFile().getFileName() + "." + i), shards[i], StandardOpenOption.APPEND);
                }
                buffer = ByteBuffer.allocate(setup.getReadBufferSize());
                noOfBytesRead = br.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}
