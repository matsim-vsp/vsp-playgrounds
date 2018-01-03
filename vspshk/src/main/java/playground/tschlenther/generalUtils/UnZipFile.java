package playground.tschlenther.generalUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

public class UnZipFile {
	
	Logger log = Logger.getLogger(UnZipFile.class);

 public static void main(String[] args) {
	  UnZipFile uzf = new UnZipFile();
	  try {
		  uzf.unZipFile(new File("C:/Users/Work/VSP/Nemo/Verkehrszaehlung_2015/nemo-master-ebd8d0a92610eb3fde0fd80bfe89fa12f1a68c31/data/input/counts/verkehrszaehlung_2015/2015/41071422.zip"));
	  } catch (ZipException e) {
		  e.printStackTrace();
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
 }

 public File unZipFile(File src) throws ZipException, IOException {
	 log.info("trying to unzip file " + src.getName());
	  String newPath = "";
	  String filePath = src.getAbsolutePath();
	  ZipFile zFile = new ZipFile(src);
	  File unzippedFolder = new File(filePath.substring(0, filePath.lastIndexOf('.')));
	  unzippedFolder.mkdir();
	  newPath = filePath.substring(0,filePath.lastIndexOf('.'));
	  
	  Enumeration<? extends ZipEntry> entries = zFile.entries();
	  boolean hasUnzippedOneTextFile = false;
	  while(entries.hasMoreElements()) {
	   
		  ZipEntry anEntry = entries.nextElement();
		  
		  if(!anEntry.isDirectory() && ( (anEntry.getName().endsWith("txt") && !hasUnzippedOneTextFile) || anEntry.getName().endsWith("xls")) ) {    
			  	saveEntry(zFile,anEntry,newPath);
			  	if(anEntry.getName().endsWith("txt")) hasUnzippedOneTextFile = true;
		  } else if(anEntry.isDirectory()){ 
			  newPath += File.separator+anEntry.getName();
		  }
	  }
	  
	  log.info("done unzipping");
	  return unzippedFolder;
 }

 private void saveEntry(ZipFile zFile, ZipEntry anEntry, String newPath) throws IOException {
	  InputStream in = null;
	  BufferedOutputStream fos = null;
	   
	  File aFile = null;
	  try{
		   aFile = new File(newPath +"/" + anEntry.getName());
		   aFile.getParentFile().mkdirs();
		   in = zFile.getInputStream(anEntry);
		   fos = new BufferedOutputStream(new FileOutputStream(aFile));
		   byte[] buffer = new byte[1024];
		   int length;
		   while((length = in.read(buffer)) > 0) {
		    fos.write(buffer,0,length);
		   }
	  } finally {
		  if(in != null) {
			  in.close();
		  }
		  if(fos != null) {
			  fos.close();
		  }
	  }  	
 	
 }
 
}