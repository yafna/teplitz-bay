package some.data.provider;

import some.data.event.GenericEvent;
import some.dto.Friend;

import java.nio.file.Path;
import java.util.List;

public interface CustomDataSource {
    void saveEvent(GenericEvent event);

    void updateMemoryModel(Path path);

    void reloadMemoryModel();

    List<Friend> getAllFriends();
}
