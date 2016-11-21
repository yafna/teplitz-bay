package some.data.provider;

import some.data.event.AddFriendEvent;
import some.data.event.RemoveFriendEvent;
import some.data.provider.filesystem.FileSystemDataSource;
import some.dto.Friend;

import java.util.List;

public class FriendsService {
    private CustomDataSource dataSource;

    public FriendsService() {
        dataSource = new FileSystemDataSource();
    }

    public List<Friend> getAll() {
        return dataSource.getAllFriends();
    }

    public void remove(String id) {
        dataSource.saveEvent(new RemoveFriendEvent(id));
    }

    public void add(String id) {
        dataSource.saveEvent(new AddFriendEvent(new Friend(id)));
    }
}
