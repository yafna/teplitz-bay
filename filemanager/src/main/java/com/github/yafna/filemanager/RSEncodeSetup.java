package com.github.yafna.filemanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
public class RSEncodeSetup {
    private static final int BYTES_IN_INT = 4;

    private int shardSize = 50000;
    private int dataShardsCount = 4;
    private int parityShardsCount = 2;
    private Path sourceFile;
    private Path shardsFolder;

    public RSEncodeSetup(Path sourceFile, Path shardsFolder, int dataShardsCount, int parityShardsCount){
        this.sourceFile = sourceFile;
        this.shardsFolder = shardsFolder;
        this.dataShardsCount = dataShardsCount;
        this.parityShardsCount = parityShardsCount;
    }

    public RSEncodeSetup(Path sourceFile, Path shardsFolder){
        this.sourceFile = sourceFile;
        this.shardsFolder = shardsFolder;
    }

    public int getTotalShardCount(){
        return dataShardsCount + parityShardsCount;
    }

    public int getBytesInInt(){
        return BYTES_IN_INT;
    }

    public int  getAllBufferSize(){
        return shardSize*dataShardsCount;
    }

    public int  getReadBufferSize(){
        return getAllBufferSize() - getBytesInInt();
    }

    public Path getShardByIndex(int index){
        return getShardsFolder().resolve(getSourceFile().getFileName() + "." + index);
    }
}
