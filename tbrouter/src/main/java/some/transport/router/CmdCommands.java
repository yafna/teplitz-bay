package some.transport.router;

import lombok.Getter;

public enum CmdCommands {
    EXIT("exit"),
    RUN_ROUTER("runrouter"),
    HELP("help");

    @Getter
    private String displayName;

    CmdCommands(String displayName) {
        this.displayName = displayName;
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
            sb.append(c.displayName).append("\n");
        }
        return sb.toString();
    }
}