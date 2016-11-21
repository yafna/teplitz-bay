package some.data;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import some.data.event.AddFriendEvent;
import some.data.event.RemoveFriendEvent;
import some.data.provider.filesystem.FileSystemDataSource;
import some.dto.Friend;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystemDataSourceTest {
    private String s = System.getProperty("java.io.tmpdir");
    private Path path = Paths.get(s).resolve("ttt");

    @After
    public void cleanUp() {
        try {
            FileUtils.cleanDirectory(path.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSaveAddFriend() {
        FileSystemDataSource fileSystemDataSource = new FileSystemDataSource(path.toString());
        int startnum = fileSystemDataSource.getAllFriends().size();
        fileSystemDataSource.saveEvent(new AddFriendEvent(new Friend("friendid1")));
        int endnum = fileSystemDataSource.getAllFriends().size();
        Assert.assertEquals(startnum + 1, endnum);
    }

    @Test
    public void testSaveRemoveFriend() {
        FileSystemDataSource fileSystemDataSource = new FileSystemDataSource(path.toString());
        String friendId = "friendid1";
        int startnum = fileSystemDataSource.getAllFriends().size();
        fileSystemDataSource.saveEvent(new AddFriendEvent(new Friend(friendId)));
        int endnum = fileSystemDataSource.getAllFriends().size();
        Assume.assumeTrue(startnum + 1 == endnum);

        fileSystemDataSource.saveEvent(new RemoveFriendEvent(friendId));
        int endnum2 = fileSystemDataSource.getAllFriends().size();
        Assert.assertEquals(startnum, endnum2);
    }

    @Test
    public void testReload() {
        FileSystemDataSource fileSystemDataSource = new FileSystemDataSource(path.toString());
        fileSystemDataSource.saveEvent(new AddFriendEvent(new Friend("friendid1")));
        fileSystemDataSource.saveEvent(new AddFriendEvent(new Friend("friendid2")));
        fileSystemDataSource.saveEvent(new RemoveFriendEvent("friendid2"));
        fileSystemDataSource.saveEvent(new AddFriendEvent(new Friend("friendid3")));
        int startnum = fileSystemDataSource.getAllFriends().size();
        fileSystemDataSource.reloadMemoryModel();
        int endnum = fileSystemDataSource.getAllFriends().size();

        Assert.assertEquals(startnum, endnum);
    }
}
