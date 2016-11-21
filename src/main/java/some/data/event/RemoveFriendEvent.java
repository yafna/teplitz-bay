package some.data.event;

import lombok.Getter;

import java.util.Date;

@Getter
public class RemoveFriendEvent implements GenericEvent {
    private String friendId;
    private Date time;

    public RemoveFriendEvent(String friendId) {
        this.friendId = friendId;
        this.time = new Date();
    }
}
