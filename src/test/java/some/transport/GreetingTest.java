package some.transport;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.net.ServerSocket;

@Slf4j
public class GreetingTest {
    private final static String ip = "192.168.1.1";
    private String serverName = "testserver";

    // Locate server by name
    @Test
    public void greeting() throws IOException, ClassNotFoundException {
        int serverPort = randomPort();
        Server server = new Server(serverPort, serverPort);
        server.store(serverName, ip);

        Server client1 = new Server(serverPort, randomPort());
        Server client2 = new Server(serverPort, randomPort());

        Assert.assertEquals(ip, client1.get(serverName));
        Assert.assertEquals(ip, client2.get(serverName));
    }

    @Test
    public void send() throws IOException, ClassNotFoundException {
        int serverPort = randomPort();
        ImgDTO dto = new ImgDTO();
        dto.setName("2.jpg");
        InputStream stream = getClass().getResourceAsStream("/1.jpg");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(stream, bos);
        dto.setData(bos.toByteArray());
        stream.close();
        bos.close();

        Data data = new Data(dto);
        Server server = new Server(serverPort, serverPort);
        server.store(serverName, data);

        Server client1 = new Server(serverPort, randomPort());

        Data stream1 = client1.getData(serverName);
        File f1 = new File("new1.jpg");
        ImgDTO arr = (ImgDTO) stream1.getObject();
        FileOutputStream fos = new FileOutputStream(f1);
        ByteArrayInputStream bis = new ByteArrayInputStream(arr.getData());
        IOUtils.copy(bis, fos);
        fos.close();

        Assert.assertArrayEquals(dto.getData(), arr.getData());
    }

    @SneakyThrows
    private static int randomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            int port = socket.getLocalPort();
            log.info("Aquired random port: {}", port);
            return port;
        }
    }

}
