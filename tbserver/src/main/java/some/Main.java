package some;

import some.transport.Server;

import java.util.Scanner;

public class Main {
    private Server server = null;

    public static void main(String... args) {
        Main main = new Main();
        Scanner scanner = new Scanner(System.in);
        CmdCommands command = null;
        while (command != CmdCommands.EXIT) {
            System.out.print("Enter command: ");
            String input = scanner.next();
            command = CmdCommands.getByName(input);
            if (command != null)
                switch (command) {
                    case HELP: {
                        System.out.println(CmdCommands.listCommands());
                        break;
                    }
                    case GET_MY_DESTINATION: {
                        System.out.println(main.getServer().getMyDestination());
                        break;
                    }
                    case CONNECT: {
                        System.out.println("Enter destination id: ");
                        String dest = scanner.next();
                        main.getServer().connectTo(dest);
                        System.out.println("connected to" + dest);
                        break;
                    }
                    case DISCONNECT: {
                        System.out.println("Enter destination id: ");
                        String dest = scanner.next();
                        main.getServer().connectTo(dest);
                        System.out.println("disconnected with" + dest);
                        break;
                    }
                    case SEND_ECHO: {
                        System.out.println("Enter destination id: ");
                        String dest = scanner.next();
                        System.out.println("Enter echo string: ");
                        String text = scanner.next();
                        String response = main.getServer().sendEcho(dest, text);
                        System.out.println("received: " + response);
                        break;
                    }
                    default: {
                        System.out.println("Not recognized command:  " + input + "  .\n Type help to see available options");
                    }
            } else {
                System.out.println("Commands list: \n " + CmdCommands.listCommands());
            }
        }
    }

    public Server getServer() {
        if (server == null) {
            server = new Server();
        }
        return server;
    }
}
