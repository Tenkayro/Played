package de.tenkayro.client;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Writes a complete replacement file without ever truncating the current file first. */
public final class AtomicFileWriter {
	private AtomicFileWriter() {
	}

	public static void write(Path file, byte[] contents) throws IOException {
		Files.createDirectories(file.getParent());
		Path temporaryFile = temporaryFile(file);
		try {
			Files.write(temporaryFile, contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			try (FileChannel channel = FileChannel.open(temporaryFile, StandardOpenOption.WRITE)) {
				channel.force(true);
			}

			if (Files.isRegularFile(file)) {
				Files.copy(file, backupFile(file), StandardCopyOption.REPLACE_EXISTING);
			}
			moveIntoPlace(temporaryFile, file);
		} finally {
			Files.deleteIfExists(temporaryFile);
		}
	}

	public static Path backupFile(Path file) {
		return file.resolveSibling(file.getFileName() + ".bak");
	}

	private static Path temporaryFile(Path file) {
		return file.resolveSibling(file.getFileName() + ".tmp");
	}

	private static void moveIntoPlace(Path temporaryFile, Path file) throws IOException {
		try {
			Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
