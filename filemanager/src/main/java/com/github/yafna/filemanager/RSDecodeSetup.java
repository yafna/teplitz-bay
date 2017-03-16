package com.github.yafna.filemanager;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

@Data
@AllArgsConstructor
public class RSDecodeSetup {
    private RSEncodeSetup encodeSetup;
    private Path source;
    private Path destDir;

    public Path getFileCoreName() {
        return encodeSetup.getSourceFile().getFileName();
    }

    public Path getDestFile() {
      return destDir.resolve( encodeSetup.getSourceFile().getFileName());
    }

    public Path getShardByIndex(int index){
        return  getSource().getParent().resolve(getFileCoreName() + "." + index);
    }
}
