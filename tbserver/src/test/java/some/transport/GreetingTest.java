package some.transport;

import net.i2p.client.streaming.I2PSocketManagerFactory;
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
        if (null == I2PSocketManagerFactory.createManager()) {

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
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @AfterClass
    public static void tearDown() {
        if (r != null) r.shutdown(0);
    }

    @Test
    public void test() throws IOException, ClassNotFoundException {
        Server server = new Server();
        String sname = server.getMyDestination();
        Server client = new Server();
        client.connectTo(sname);
        String s = "TESTTEST";
        String echoS = client.sendEcho(sname, s);
        Assert.assertEquals(s, echoS);
    }
}
