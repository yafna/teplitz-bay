package some.data.event;

import lombok.Getter;
import some.dto.Friend;

import java.util.Date;

@Getter
public class AddFriendEvent implements GenericEvent {
    private Friend friend;
    private Date time;

    public AddFriendEvent(Friend friend) {
        this.friend = friend;
        this.time = new Date();
    }
}
