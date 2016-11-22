package some.transport.router;

import net.i2p.router.Router;

import java.util.Properties;
import java.util.Scanner;

public class Runner {
    private Router r;

    public static void main(String[] args) {
        final Runner main = new Runner();
        Scanner scanner = new Scanner(System.in);
        CmdCommands command = null;
        while (command != CmdCommands.EXIT) {
            System.out.print("Enter command: ");
            String input = scanner.next();
            command = CmdCommands.getByName(input);
            if (command != null) {
                switch (command) {
                    case HELP: {
                        System.out.println(CmdCommands.listCommands());
                        break;
                    }
//                        loosing sout here
                    case RUN_ROUTER: {
                        System.out.println("router starting.... ");
                        scanner.close();
                        main.runRouter();
                        scanner = new Scanner(System.in);
                        System.out.println("router started ");
                        break;
                    }
                    default: {
                        System.out.println("Not recognized command:  " + input + "  .\n Type help to see available options");
                    }
                }
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (main.getR() != null) {
                    main.getR().shutdown(2);
                }
            }
        });
    }

    public Router getR() {
        return r;
    }

    public void runRouter() {
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
    }
}
