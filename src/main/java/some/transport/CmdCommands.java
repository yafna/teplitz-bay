package some.transport;

import lombok.Getter;

public enum CmdCommands {
    EXIT("exit", 0),
    GET_MY_DESTINATION("mydestination", 0),
    CONNECT("connect", 1),
    DISCONNECT("disconnect", 1),
    SEND_ECHO("echo", 2),
    RUN_ROUTER("runrouter", 0),
    HELP("help", 0);

    @Getter
    private int argsNum;
    @Getter
    private String displayName;

    CmdCommands(String displayName,int argsNum) {
        this.displayName = displayName;
        this.argsNum = argsNum;
    }

    public static CmdCommands getByName(String name) {
        for (CmdCommands c : values()) {
            if (c.displayName.equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    public static String listCommands() {
        StringBuilder sb = new StringBuilder();
        for (CmdCommands c : values()) {
            sb.append(c.displayName);
            for(int i = 0; i < c.argsNum; ++i) {
                sb.append(" <parameter>");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
