package playground.kturner.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author kturner
 *
 * Copy a directory with its files
 * 
 * This class bases on code found here: 
 * https://www.java-forum.org/thema/ordner-samt-dateien-kopieren.157759/page-2
 * 
 ***/


public class CopyDirVisitor extends SimpleFileVisitor<Path> {
    private Path fromPath;
    private Path toPath;
    private StandardCopyOption copyOption;

    /**
     * For copying directory use following command
     * Files.walkFileTree(startingDir, new CopyDirVisitor(startingDir, destDir, copyOption));
     * 
     * @param fromPath 
     * 			Source path from were files and directories should be copied from.
     * @param toPath
     * 			Destination path to were files and directories should be copied to.
     * @param copyOption
     * 			Defines the copyOption.
     * 
     */
    public CopyDirVisitor(Path fromPath, Path toPath, StandardCopyOption copyOption) {
        this.fromPath = fromPath;
        this.toPath = toPath;
        this.copyOption = copyOption;
        
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        toPath.getFileName().toFile().mkdirs();
    	Path targetPath = toPath.resolve(fromPath.relativize(dir));
        if (!Files.exists(targetPath)) {
            Files.createDirectory(targetPath);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        System.out.println(file);
        Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
        return FileVisitResult.CONTINUE;
    }
    
}
