package com.terraforged.mod.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

public class FileUtil {
    public static void write(Path path, IOConsumer<BufferedWriter> consumer) {
        FileUtil.write(path, null, consumer);
    }

    public static <T> void write(Path path, T context, IOBiConsumer<BufferedWriter, T> consumer) {
        Path parent = (path = path.toAbsolutePath()).getParent();
        if (!Files.exists(parent, new LinkOption[0])) {
            try {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, new OpenOption[0]);){
            consumer.accept(writer, context);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void walk(Path root, String path, FileSystemVisitor visitor) throws IOException {
        if (Files.isDirectory(root, new LinkOption[0])) {
            FileUtil.walkDir(root, path, visitor);
        } else {
            FileUtil.walkSystem(root, path, visitor);
        }
    }

    public static void walkDir(Path root, String path, FileSystemVisitor visitor) throws IOException {
        root = root.resolve(path);
        FileUtil.walk(root.getFileSystem(), root, root, visitor);
    }

    public static void walkSystem(Path root, String path, FileSystemVisitor visitor) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(root);){
            root = fs.getPath(path, new String[0]);
            FileUtil.walk(fs, root, root, visitor);
        }
    }

    public static void walk(FileSystem fs, Path root, Path path, FileSystemVisitor visitor) throws IOException {
        Path file = fs.getPath(path.toString(), new String[0]);
        if (Files.isDirectory(file, new LinkOption[0])) {
            try (DirectoryStream<Path> stream = fs.provider().newDirectoryStream(file, entry -> true);){
                stream.forEach(f -> {
                    try {
                        FileUtil.walk(fs, root, f, visitor);
                    }
                    catch (IOException e) {
                        throw new Error(e);
                    }
                });
            }
        } else {
            visitor.visit(fs, root, file);
        }
    }

    public static void createDirCopy(Path fromRoot, String fromPath, Path to) throws IOException {
        FileUtil.walk(fromRoot, fromPath, (fs, root, file) -> {
            Path relative = root.relativize(file);
            Path dest = FileUtil.resolve(to, relative);
            if (Files.exists(dest, new LinkOption[0]) || Files.isDirectory(file, new LinkOption[0])) {
                return;
            }
            Path parent = dest.getParent();
            if (!Files.exists(parent, new LinkOption[0])) {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
            Files.copy(file, dest, new CopyOption[0]);
        });
    }

    public static void createZipCopy(Path from, String path, Path to) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(to, new OpenOption[0])));){
            FileUtil.walk(from, path, (fs, root, file) -> {
                Object name = root.relativize(file).toString().replace('\\', '/');
                if (Files.isDirectory(file, new LinkOption[0])) {
                    if (!((String)name).endsWith("/")) {
                        name = (String)name + "'/";
                    }
                    ZipEntry entry = new ZipEntry((String)name);
                    entry.setTime(System.currentTimeMillis());
                    output.putNextEntry(entry);
                } else {
                    ZipEntry entry = new ZipEntry((String)name);
                    entry.setTime(System.currentTimeMillis());
                    output.putNextEntry(entry);
                    try (InputStreamReader input = new InputStreamReader(fs.provider().newInputStream(file, new OpenOption[0]));){
                        IOUtils.copy((Reader)input, (OutputStream)output, (Charset)Charset.defaultCharset());
                    }
                }
                output.closeEntry();
            });
            output.finish();
            output.flush();
        }
    }

    public static void delete(Path path) {
        FileUtil.iterate(path, file -> {
            try {
                Files.deleteIfExists(file);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void iterate(Path path, Consumer<Path> consumer) {
        if (Files.isDirectory(path, new LinkOption[0])) {
            try (Stream<Path> files = Files.list(path);){
                files.forEach(file -> FileUtil.iterate(file, consumer));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        consumer.accept(path);
    }

    public static Path resolve(Path base, Path path) {
        Path result = base;
        for (Path part : path) {
            result = result.resolve(part.getFileName().toString());
        }
        return result;
    }

    public static interface IOBiConsumer<A, B> {
        public void accept(A var1, B var2) throws IOException;
    }

    public static interface FileSystemVisitor {
        public void visit(FileSystem var1, Path var2, Path var3) throws IOException;
    }

    public static interface IOConsumer<T>
    extends IOBiConsumer<T, Void> {
        public void accept(T var1) throws IOException;

        @Override
        default public void accept(T t, Void unused) throws IOException {
            this.accept(t);
        }
    }
}
