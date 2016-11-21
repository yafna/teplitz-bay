package some;

import lombok.Getter;
import net.i2p.router.Router;
import some.transport.CmdCommands;
import some.transport.Server;

import java.util.Properties;
import java.util.Scanner;

public class Main {

    private Server server = null;
    @Getter
    private Router r;

    public static void main(String... args) {
        Main main = new Main();
        Scanner scanner = new Scanner(System.in);
        CmdCommands command = null;
        while (command != CmdCommands.EXIT) {
            System.out.print("Enter command: ");
            String input = scanner.next();
            String[] inputStrs = input.trim().split(" +");
            if (inputStrs.length > 0) {
                command = CmdCommands.getByName(inputStrs[0]);
                if (command != null) {
                    switch (command) {
                        case HELP: {
                            System.out.println(CmdCommands.listCommands());
                            break;
                        }
//                        loosing sout here
//                        case RUN_ROUTER: {
//                            System.out.println("router starting.... ");
//                            scanner.close();
//                            main.runRouter();
//                            scanner = new Scanner(System.in);
//                            System.out.println("router started ");
//                            break;
//                        }
                        case GET_MY_DESTINATION: {
                            System.out.println(main.getServer().getMyDestination());
                            break;
                        }
                        case CONNECT: {
                            if (inputStrs.length != 2) {
                                System.out.println("Wrong arguments. Expected one string with server destination");
                            } else {
                                main.getServer().connectTo(inputStrs[1]);
                                System.out.println("connected to" + inputStrs[1]);
                            }
                            break;
                        }
                        case DISCONNECT: {
                            if (inputStrs.length != 2) {
                                System.out.println("Wrong arguments. Expected one string with server destination");
                            } else {
                                main.getServer().connectTo(inputStrs[1]);
                                System.out.println("disconnected with" + inputStrs[1]);
                            }
                            break;
                        }
                        case SEND_ECHO: {
                            if (inputStrs.length != 3) {
                                System.out.println("Wrong arguments. Expected string with server destination and echo text");
                            } else {
                                main.getServer().sendEcho(inputStrs[1], inputStrs[2]);
                                System.out.println("connected to" + inputStrs[1]);
                            }
                            break;
                        }
                    }
                } else {
                    System.out.println("Not recognized command:  " + input + "  .\n Type help to see available options");
                }
            } else {
                System.out.println("Commands list: \n " + CmdCommands.listCommands());
            }
        }
        if(main.getR() != null) {
            main.getR().shutdown(2);
        }
    }

    public Server getServer() {
        if (server == null) {
            server = new Server();
        }
        return server;
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
