package de.upb.spl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Defines methods to access files reading and writing their content. Use this
 * class when you don't want to bother with checked IO-exceptions.
 * 
 * @author aminfaez
 *
 */
public class FileUtil {

	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

	/**
	 * Returns the content of the file located at the given file path as a string.
	 * 
	 * @param filePath
	 *            path to the file to be read from
	 * @return Content of the file as a string
	 *
	 * @throws UncheckedIOException
	 *             if an I/O error occurs reading from the file
	 */
	public static String readFileAsString(String filePath) {
		byte[] encoded;
		try {
			encoded = Files.readAllBytes(Paths.get(Objects.requireNonNull(filePath)));
			String fileContent = new String(encoded, Charset.defaultCharset());
			return fileContent.replaceAll("\r\n", "\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static File getResourceFile(String resourceFilePath) {
		File resourceFile;
		ClassLoader classLoader = FileUtil.class.getClassLoader();
		try{
			resourceFile = new File(classLoader.getResource(resourceFilePath).getFile());
		} catch(NullPointerException ex) {
			throw new RuntimeException("Resource not found: " + resourceFilePath);
		}
		return resourceFile;
	}

	public static String getPathOfResource(String resourceFilePath) {
		String path = getResourceFile(resourceFilePath).getPath();
		if(resourceFilePath.endsWith("/") && !path.endsWith("/")) {
			return path + "/";
		} else {
			return path;
		}
	}

	public static String readResourceAsString(String resourceFilePath) {
		//Get file from resources folder
		ClassLoader classLoader = FileUtil.class.getClassLoader();
		try (InputStream inputStream = classLoader.getResourceAsStream(resourceFilePath)) {
			return Streams.InReadString(inputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String nextFreeFile(String path, String ending) {
		if(ending == null) {
			ending = "";
		}
		String filePath = path + "_%02d" + ending;
		int i = 0;
		File file;
		do {
			file = new File(String.format(filePath, i));
			i++;
		} while(file.exists());
		return String.format(filePath, i);
	}

	/**
	 * Write the given content to the given file. Creates the directory and the file
	 * if it doesn't exist. Deletes (overwrites) the old content if the file already
	 * exists
	 * 
	 * Uses WriteFileRequest.
	 * 
	 * @param filePath
	 *            path to the file to be written into
	 * @param fileContent
	 *            content that will be written to the file
	 * @return true, if the writing was successful
	 * 
	 * @throws UncheckedIOException
	 *             if an I/O error occurs writing to the file
	 */
	public static boolean writeStringToFile(String filePath, String fileContent) {
		File file = new File(filePath).getAbsoluteFile();
		/*
		 * Create directories up to the file.
		 */
		File parentFile = file.getParentFile();
		if(parentFile != null) {
			parentFile.mkdirs();
		}

		/*
		 * return output stream
		 */
		try (OutputStream outputStream = new FileOutputStream(file)){
			outputStream.write(fileContent.getBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return true;
	}

	/**
	 * Creates a list of all file names in the given directory that match the given
	 * regex. The list will not contain sub-directories even if their name match the
	 * given regex.
	 * 
	 * @param dirPath
	 *            path to the Directory that is looked into
	 * @param regex
	 *            regular expression used as a filter over the files. If null all
	 *            files in the directory are returned.
	 * @return list of all files
	 */
	public static List<String> listAllFilesInDir(String dirPath, String regex, boolean fileOnly) {

		File directory = new File(Objects.requireNonNull(dirPath)).getAbsoluteFile();
		if (!directory.isDirectory()) {
			throw new RuntimeException("The given path \"" + dirPath + "\" is not a directory.");
		}

		/*
		 * prepare the regular expression filter:
		 */
		Pattern filterPattern;
		if (regex != null) {
			filterPattern = Pattern.compile(regex);
		} else {
			filterPattern = Pattern.compile("(.*?)"); // match all
		}

		/*
		 * iterate over files in the given directory:
		 */
		List<String> listedFiles = new ArrayList<>();
		File[] files = directory.listFiles();
		for (File file : files) {
			if (filterPattern.matcher(file.getName()).matches() && (!fileOnly || file.isFile())) {
				listedFiles.add(file.getName());
			}
		}
		return listedFiles;
	}

	public static String getDirPath(String dir) {
		return dir.endsWith("/")? dir.substring(0, dir.length()-1) : dir;
	}




	protected static void mkDir(File file) {
		if (!file.exists()) {
			logger.info("Creating directory: \"" + file.getAbsolutePath() + "\"");
			file.mkdir();
		}
	}

	protected static String getFileExtension(String filename) throws IOException {
		try {
			return filename.substring(filename.lastIndexOf("."));
		} catch (IndexOutOfBoundsException e) {
			logger.debug("File: \"" + filename + "\" does not have a file extension.", e);
			return "";
		}
	}

	public void save(byte[] data, String filePath) {
		try {
			File destination = new File(filePath);
			if (destination.exists())
				System.out.println("Overwriting file: " + destination.getName());
			BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(destination));
			output.write(data, 0, data.length);
			output.close();
			System.out.println(">>> recv file " + filePath + " (" + (data.length >> 10) + " kiB)");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}