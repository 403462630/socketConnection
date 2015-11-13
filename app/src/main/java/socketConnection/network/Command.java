package socketConnection.network;

/**
 * Created by Bruce on 12/3/14.
 */
public enum Command {
    PUMP((short) 3101);

    private short value;
    private Command(short value){
        this.value = value;
    }

    public short getValue() {
        return value;
    }

    public static Command fromValue(short value) {
        for (Command command : Command.values()) {
            if (command.getValue() == value) {
                return command;
            }
        }
        return UNKNOWN;
    }
}
