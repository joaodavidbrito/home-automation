package pt.joaobrito.ha.homeautomation;

import java.io.IOException;

import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import pt.joaobrito.ha.homeautomation.opencv.NativeUtils;

@EnableScheduling
@SpringBootApplication
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class})
public class HomeAutomationApplication {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, IOException {
        loadLibrary();
        SpringApplication.run(HomeAutomationApplication.class, args);
    }

    private static void loadLibrary() throws NoSuchFieldException, IllegalAccessException, IOException {
        String path = null;
        String os = System.getProperty("os.name");
        String bitness = System.getProperty("sun.arch.data.model");
        if (os.toUpperCase().contains("WINDOWS")) {
            if (bitness.endsWith("64")) {
                path = new String("/lib/x64/" + System.mapLibraryName(Core.NATIVE_LIBRARY_NAME));
            } else {
                path = new String("/lib/x86/" + System.mapLibraryName(Core.NATIVE_LIBRARY_NAME));
            }
        } else {
            return;
        }
        NativeUtils.loadLibrary(path);
    }

}
