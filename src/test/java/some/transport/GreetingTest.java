package some.transport;

import net.tomp2p.storage.Data;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class GreetingTest {
    private int portToBootstrap = 6003;

    private int clentPort1 = 6001;
    private int clentPort2 = 6002;
    private int serverPort = 6003;
    private String serverName = "testserver";
    private String ip = "192.168.1.1";

    // Locate server by name
    @Test
    public void greeting() throws IOException, ClassNotFoundException {
        Server server = new Server(portToBootstrap, serverPort);
        server.store(serverName, ip);

        Server client1 = new Server(portToBootstrap, clentPort1);
        Server client2 = new Server(portToBootstrap, clentPort2);

        Assert.assertEquals(ip, client1.get(serverName));
        Assert.assertEquals(ip, client2.get(serverName));
    }

    @Test
    public void send() throws IOException, ClassNotFoundException {
        ImgDTO dto = new ImgDTO();
        dto.setName("2.jpg");
        InputStream stream = getClass().getResourceAsStream("/1.jpg");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(stream, bos);
        dto.setData(bos.toByteArray());
        stream.close();
        bos.close();

        Data data = new Data(dto);
        Server server = new Server(portToBootstrap, serverPort);
        server.store(serverName, data);


        Server client1 = new Server(portToBootstrap, clentPort1);

        Data stream1 = client1.getData(serverName);
        File f1 = new File("new1.jpg");
        ImgDTO arr = (ImgDTO) stream1.getObject();
        FileOutputStream fos = new FileOutputStream(f1);
        ByteArrayInputStream bis = new ByteArrayInputStream(arr.getData());
        IOUtils.copy(bis, fos);
        ((InputStream) stream1.getObject()).close();
        fos.close();

        Assert.assertEquals(dto.getData(), arr.getData());
    }
}
