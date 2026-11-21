package net.runelite.client.plugins.playerlogger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class PlayerHistoryStore {
    private static final Type VISITS = new TypeToken<ArrayList<WorldVisit>>() {}.getType();
    private final Gson gson = new Gson();
    private final Path directory;

    PlayerHistoryStore(Path directory) {
        this.directory = directory;
    }

    // A second client on the same computer can record a different visit at the same time.
    List<WorldVisit> update(Predicate<List<WorldVisit>> edit) throws IOException {
        Files.createDirectories(directory);
        Path history = directory.resolve("visits.json");
        try (FileChannel channel = FileChannel.open(directory.resolve("visits.lock"),
                 StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            List<WorldVisit> visits = new ArrayList<>();
            if (Files.exists(history)) {
                try (Reader reader = Files.newBufferedReader(history)) {
                    visits = gson.fromJson(reader, VISITS);
                    if (visits == null) {
                        throw new IOException("Empty Player Logger history");
                    }
                }
            }
            if (edit.test(visits)) {
                Path temp = Files.createTempFile(directory, "visits-", ".tmp");
                try {
                    try (Writer writer = Files.newBufferedWriter(temp)) {
                        gson.toJson(visits, VISITS, writer);
                    }
                    Files.move(temp, history, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
            return visits;
        }
    }
}