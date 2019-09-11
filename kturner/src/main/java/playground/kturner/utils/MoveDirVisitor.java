package playground.kturner.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.apache.log4j.Logger;

/**
 * @author kturner
 *
 * Move a directory with its files and directories
 * 
 * This class bases on code found here: 
 * https://www.java-forum.org/thema/ordner-samt-dateien-kopieren.157759/page-2
 * 
 * 
 ***/

public class MoveDirVisitor extends SimpleFileVisitor<Path> {
	
	private static final Logger log = Logger.getLogger(MoveDirVisitor.class) ;
	
    private Path fromPath;
    private Path toPath;
    private StandardCopyOption copyOption;

    /**
     * For moving directory use following command
     * Files.walkFileTree(startingDir, new MoveDirVisitor(startingDir, destDir, copyOption));
     * 
     * Remaining empty directories in fromPath after moving files and directories will be deleted.
     * 
     * @param fromPath 
     * 			Source path from were files and directories should be copied from.
     * @param toPath
     * 			Destination path to were files and directories should be copied to.
     * @param copyOption
     * 			Defines the copyOption.
     * @throws IOException 
     * 
     */
    public MoveDirVisitor(Path fromPath, Path toPath, StandardCopyOption copyOption) throws IOException {
        this.fromPath = fromPath;
        this.toPath = toPath;
        this.copyOption = copyOption;
        
        if (!Files.exists(toPath)){
        	Files.createDirectory(toPath);
        	log.info("Created directory: " + toPath.toString());
        }
        
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    	Path targetPath = toPath.resolve(fromPath.relativize(dir));
        if (!Files.exists(targetPath)) {
            Files.createDirectory(targetPath);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//        System.out.println(file);
        Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }
    
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
    {
    	if (Files.exists(dir) && Files.isDirectory(dir)){
    		if (dir.toFile().list().length == 0) {	//directory is empty
    			Files.delete(dir);
    		} else {
		    		log.warn("Something goes wrong with moving files from: " + dir.getFileName());
		    	}
    	}
    	
        Objects.requireNonNull(dir);
        if (exc != null)
            throw exc;
        return FileVisitResult.CONTINUE;
    }
    
}
