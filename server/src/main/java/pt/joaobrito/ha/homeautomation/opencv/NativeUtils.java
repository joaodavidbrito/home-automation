package pt.joaobrito.ha.homeautomation.opencv;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NativeUtils.class);

    public static void loadLibrary(String path) throws IOException {
        LOG.info("path =" + System.getProperty("java.library.path"));

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("The path must to be absolute (start with '/').");
        }

        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;

        // Split filename to prexif and suffix (extension)
        String prefix = "";
        String suffix = null;
        if (filename != null) {
            parts = filename.split("\\.", 2);
            prefix = parts[0];
            suffix = (parts.length > 1) ? "." + parts[parts.length - 1] : null; // Thanks, davs! :-)
        }

        // Check if the filename is okay
        if (filename == null || prefix.length() < 3) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }

        // Prepare temporary file
        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();

        if (!temp.exists()) {
            throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
        }

        // Prepare buffer for data copying
        byte[] buffer = new byte[1024];
        int readBytes;

        // Open and check input stream
        InputStream is = NativeUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }

        // Open output stream and copy data between source file in JAR and the temporary file
        OutputStream os = new FileOutputStream(temp);
        try {
            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        } finally {
            // If read/write fails, close streams safely before throwing an exception
            os.close();
            is.close();
        }

        // Finally, load the library
        System.load(temp.getAbsolutePath());

        final String libraryPrefix = prefix;
        final String lockSuffix = ".lock";

        // create lock file
        final File lock = new File(temp.getAbsolutePath() + lockSuffix);
        lock.createNewFile();
        lock.deleteOnExit();

        // file filter for library file (without .lock files)
        FileFilter tmpDirFilter =
                new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.getName().startsWith(libraryPrefix) && !pathname.getName().endsWith(lockSuffix);
                    }
                };

        // get all library files from temp folder
        String tmpDirName = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(tmpDirName);
        File[] tmpFiles = tmpDir.listFiles(tmpDirFilter);

        // delete all files which don't have n accompanying lock file
        for (int i = 0; i < tmpFiles.length; i++) {
            // Create a file to represent the lock and test.
            File lockFile = new File(tmpFiles[i].getAbsolutePath() + lockSuffix);
            if (!lockFile.exists()) {
                System.out.println("deleting: " + tmpFiles[i].getAbsolutePath());
                tmpFiles[i].delete();
            }
        }
    }

}
