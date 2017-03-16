package com.github.yafna.filemanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RSDecodeSetup {
    private RSEncodeSetup encodeSetup;
    private Path source;
    private Path destDir;
}
