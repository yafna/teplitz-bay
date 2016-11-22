package some.data.provider.filesystem;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import some.data.event.AddFriendEvent;
import some.data.event.GenericEvent;
import some.data.event.RemoveFriendEvent;
import some.data.provider.CustomDataSource;
import some.dto.Friend;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Slf4j
public class FileSystemDataSource implements CustomDataSource {
    private List<Friend> friends = new ArrayList<>();
    private String folder;
    private Path path;

    public FileSystemDataSource() {
        this(System.getProperty("folder.to.save", System.getProperty("java.io.tmpdir")));
    }

    public FileSystemDataSource(String folderToSave) {
        this.folder = folderToSave;
        this.path = Paths.get(folder);
        if (!path.toFile().exists()) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
    }


    @Override
    public void saveEvent(GenericEvent event) {
//        String evntName = fileName.format(new Date());
        String evntName = Long.toString(new Date().getTime());
        Path path = Paths.get(folder).resolve(evntName);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             FileOutputStream fos = new FileOutputStream(path.toFile())) {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(event);
            oos.flush();
            oos.close();
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            IOUtils.copy(is, fos);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        updateMemoryModel(path);
    }

    @Override
    public void updateMemoryModel(Path path) {
        try (FileInputStream fis = new FileInputStream(path.toFile());
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            GenericEvent genericEvent = (GenericEvent) ois.readObject();
            if (genericEvent instanceof AddFriendEvent) {
                friends.add(((AddFriendEvent) genericEvent).getFriend());
            }
            if (genericEvent instanceof RemoveFriendEvent) {
                String idToRemove = ((RemoveFriendEvent) genericEvent).getFriendId();
                Friend friendToRemove = null;
                for (Friend f : friends) {
                    if (f.getId().equals(idToRemove)) {
                        friendToRemove = f;
                    }
                }
                if (friendToRemove != null) {
                    friends.remove(friendToRemove);
                } else {
                    log.error("Inconsistency - nothing to remove for " + idToRemove);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void reloadMemoryModel() {
        friends = new ArrayList<>();
        Path path = Paths.get(folder);

        File[] files = path.toFile().listFiles();
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    try {
                        BasicFileAttributes attr1 = Files.readAttributes(o1.toPath(), BasicFileAttributes.class);
                        BasicFileAttributes attr2 = Files.readAttributes(o2.toPath(), BasicFileAttributes.class);
                        return (int) (attr1.creationTime().toMillis() - attr2.creationTime().toMillis());
                    } catch (IOException e) {
                        log.error(e.getLocalizedMessage(), e);
                    }
                    return 0;
                }
            });
            for (final File fileEntry : files) {
                if (Files.isRegularFile(fileEntry.toPath())) {
                    updateMemoryModel(fileEntry.toPath());
                }
            }
        }
// no java 8 for now
//        try (Stream<Path> paths = Files.walk(path)) {
//            paths.forEach(filePath -> {
//                if (Files.isRegularFile(filePath)) {
//                    updateMemoryModel(filePath);
//                }
//            });
//        } catch (IOException e) {
//            log.error(e.getLocalizedMessage(), e);
//        }
    }

    @Override
    public List<Friend> getAllFriends() {
        return friends;
    }
}
