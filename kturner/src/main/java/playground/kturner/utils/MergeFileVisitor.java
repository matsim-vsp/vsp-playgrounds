package playground.kturner.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * @author kturner
 *
 * Merge files with same name from different subdirectories.
 *  
 ***/

//TODO: insert new constructor: separate destiniationFile and filename of files to merge.

public class MergeFileVisitor extends SimpleFileVisitor<Path> {
	private File destinationFile;
	private boolean removeMergedFiles;
	

	/** 
	 * Merge same-named files within subdirectories.
	 * To use: 
	 * Files.walkFileTree(startingDir, new MergeFileVisitor(destinationFile));
	 * with @param startingDir: Directory in which the search for the files to merge starts.
	 * 
	 * Remaining empty directories in startingDir after merging files and directories will be deleted.
	 * 
	 * @param destinationFile	Defines the file (incl path) to merge the Data to. Now the filename of this file also defines which files will be merged.
	 */
	public MergeFileVisitor(File destinationFile) {
		this.destinationFile = destinationFile;
		this.removeMergedFiles = false;
	}
	
                  
	/**
	 * Merge same-named files within subdirectories.
	 * To use: 
	 * Files.walkFileTree(startingDir, new MergeFileVisitor(destinationFile, true));
	 * with @param startingDir: Directory in which the search for the files to merge starts.
	 * 
	 * Remaining empty directories in startingDir after merging files and directories will be deleted.
	 * 
	 * @param destinationFile	Defines the file (incl path) to merge the Data to. Now the filename of this file also defines which files will be merged.
	 * @param removeMergedFiles	if <code>true</code>, then merged - and therefore not longer needed - files will be removed from filesystem.
	 */
	public MergeFileVisitor(File destinationFile, boolean removeMergedFiles) {
		this.destinationFile = destinationFile;
		this.removeMergedFiles = removeMergedFiles;
	}


	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (file.getFileName().equals(destinationFile.toPath().getFileName()) && file.toFile().getCanonicalPath() != destinationFile.getCanonicalPath()){ //avoid handeling destinationFile 
			try (
					FileReader reader = new FileReader(file.toString()); 
					FileWriter writer = new FileWriter(destinationFile, true); ){  
				int c = reader.read();
				while(c!=-1) {
					writer.write(c);
					c = reader.read();  
				}
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (removeMergedFiles){ 
				Files.delete(file); //Delete file after content was merged
			}
		}

		return FileVisitResult.CONTINUE;
	}

	/**
	 * Remove emtpy directories
	 */
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
	{
		if (Files.exists(dir) && Files.isDirectory(dir)){
			if (dir.toFile().list().length == 0) {	
				Files.delete(dir);					
			} 
		}

		Objects.requireNonNull(dir);
		if (exc != null)
			throw exc;
		return FileVisitResult.CONTINUE;
	}

}
