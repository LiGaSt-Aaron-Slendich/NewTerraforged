package com.terraforged.mod.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

public class FileUtil {
   public static void write(Path path, FileUtil.IOConsumer<BufferedWriter> consumer) {
      write(path, null, consumer);
   }

   public static <T> void write(Path path, T context, FileUtil.IOBiConsumer<BufferedWriter, T> consumer) {
      path = path.toAbsolutePath();
      Path pathx = path.getParent();
      if (!Files.exists(pathx)) {
         try {
            Files.createDirectories(pathx);
         } catch (IOException ioexception) {
            ioexception.printStackTrace();
            return;
         }
      }

      try (BufferedWriter bufferedwriter = Files.newBufferedWriter(path)) {
         consumer.accept(bufferedwriter, context);
      } catch (IOException ioexception1) {
         ioexception1.printStackTrace();
      }
   }

   public static void walk(Path root, String path, FileUtil.FileSystemVisitor visitor) throws IOException {
      if (Files.isDirectory(root)) {
         walkDir(root, path, visitor);
      } else {
         walkSystem(root, path, visitor);
      }
   }

   public static void walkDir(Path root, String path, FileUtil.FileSystemVisitor visitor) throws IOException {
      root = root.resolve(path);
      walk(FileSystems.getDefault(), root, root, visitor);
   }

   public static void walkSystem(Path root, String path, FileUtil.FileSystemVisitor visitor) throws IOException {
      try (FileSystem filesystem = FileSystems.newFileSystem(root)) {
         root = filesystem.getPath(path);
         walk(filesystem, root, root, visitor);
      }
   }

   public static void walk(FileSystem fs, Path root, Path path, FileUtil.FileSystemVisitor visitor) throws IOException {
      Path pathx = fs.getPath(path.toString());
      if (Files.isDirectory(pathx)) {
         try (DirectoryStream<Path> directorystream = fs.provider().newDirectoryStream(pathx, entry -> true)) {
            directorystream.forEach(f -> {
               try {
                  walk(fs, root, f, visitor);
               } catch (IOException ioexception) {
                  throw new Error(ioexception);
               }
            });
         }
      } else {
         visitor.visit(fs, root, pathx);
      }
   }

   public static void createDirCopy(Path fromRoot, String fromPath, Path to) throws IOException {
      walk(fromRoot, fromPath, (fs, root, file) -> {
         Path path = root.relativize(file);
         Path path1 = resolve(to, path);
         if (!Files.exists(path1) && !Files.isDirectory(file)) {
            Path path2 = path1.getParent();
            if (!Files.exists(path2)) {
               Files.createDirectories(path2);
            }

            Files.copy(file, path1);
         }
      });
   }

   public static void createZipCopy(Path from, String path, Path to) throws IOException {
      try (ZipOutputStream zipoutputstream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(to)))) {
         walk(from, path, (fs, root, file) -> {
            String s = root.relativize(file).toString().replace('\\', '/');
            if (Files.isDirectory(file)) {
               if (!s.endsWith("/")) {
                  s = s + "'/";
               }

               ZipEntry zipentry = new ZipEntry(s);
               zipentry.setTime(System.currentTimeMillis());
               zipoutputstream.putNextEntry(zipentry);
               zipoutputstream.closeEntry();
            } else {
               ZipEntry zipentry1 = new ZipEntry(s);
               zipentry1.setTime(System.currentTimeMillis());
               zipoutputstream.putNextEntry(zipentry1);

               try (InputStreamReader inputstreamreader = new InputStreamReader(fs.provider().newInputStream(file))) {
                  IOUtils.copy(inputstreamreader, zipoutputstream, Charset.defaultCharset());
               }

               zipoutputstream.closeEntry();
            }
         });
         zipoutputstream.finish();
         zipoutputstream.flush();
      }
   }

   public static void delete(Path path) {
      iterate(path, file -> {
         try {
            Files.deleteIfExists(file);
         } catch (IOException ioexception) {
            ioexception.printStackTrace();
         }
      });
   }

   public static void iterate(Path path, Consumer<Path> consumer) {
      if (Files.isDirectory(path)) {
         try (Stream<Path> stream = Files.list(path)) {
            stream.forEach(file -> iterate(file, consumer));
         } catch (IOException ioexception) {
            ioexception.printStackTrace();
         }
      }

      consumer.accept(path);
   }

   public static Path resolve(Path base, Path path) {
      Path pathx = base;

      for (Path path1 : path) {
         pathx = pathx.resolve(path1.getFileName().toString());
      }

      return pathx;
   }

   public interface FileSystemVisitor {
      void visit(FileSystem var1, Path var2, Path var3) throws IOException;
   }

   public interface IOBiConsumer<A, B> {
      void accept(A var1, B var2) throws IOException;
   }

   public interface IOConsumer<T> extends FileUtil.IOBiConsumer<T, Void> {
      void accept(T var1) throws IOException;

      default void accept(T t, Void unused) throws IOException {
         this.accept(t);
      }
   }
}
