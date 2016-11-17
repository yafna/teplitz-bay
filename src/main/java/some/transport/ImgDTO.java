package some.transport;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ImgDTO implements Serializable{
    private String name;
    private byte[] data;
}
