package some.transport;

import net.i2p.router.Router;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

public class GreetingTest {
    private static Router r;

    @BeforeClass
    public static void setUp() {
        Properties p = new Properties();
        // add your configuration settings, directories, etc.
        // where to find the I2P installation files
        p.put("i2p'.dir.base", "~/tmp");
        // where to find the I2P data files
        p.put("i2p.dir.config", "~/tmp");
        // bandwidth limits in K bytes per second
        p.put("i2np.inboundKBytesPerSecond", "50");
        p.put("i2np.outboundKBytesPerSecond", "50");
        p.put("router.sharePercentage", "80");
        p.put("foo", "bar");
        r = new Router(p);
        // don't call exit() when the router stops
        r.setKillVMOnEnd(true);

        r.runRouter();
//
        System.out.println("p = " + p);
        try {
            Thread.sleep(3000);
            System.out.println("sleeps end ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() {
        r.shutdown(0);
    }

    @Test
    public void test() throws IOException, ClassNotFoundException {
        System.out.println("test start");
        Server server = new Server();
        String sname = server.start();
        System.out.println("sname = " + sname);
        Client client = new Client();
        client.start(sname);
        System.out.println("client start");
        String s = "TESTTEST";
        String echoS = client.sendEcho(s);
        System.out.println("assert");
        Assert.assertEquals(s, echoS);
    }
}
