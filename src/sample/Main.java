package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Core;

public class Main extends Application {

    static int brightnessPercentChange = 5;
    static int blackThreshold = 10;
    static int blurrThreshold = 40;

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String str = System.getProperty("java.library.path");
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        loadLibraryOpenCV();
        launch(args);
    }

    private static void loadLibraryOpenCV() {
        try {

            InputStream in = null;
            File fileOut = null;//new File(System.getProperty("java.library.path"));
            String osName = System.getProperty("os.name");
            //System.out.println(VideoEncodingChecker.class, osName);
            if(osName.startsWith("Windows")){
                int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
                if(bitness == 32){
                    //Utils.out.println(Main.class, "32 bit detected");
                    in = Main.class.getResourceAsStream("/OpenCVDistrib/x86/opencv_java343.dll");
                    fileOut = File.createTempFile("lib", ".dll");
                }
                else if (bitness == 64){
                    //Utils.out.println(VideoEncodingChecker.class, "64 bit detected");
                    in = Main.class.getResourceAsStream("/OpenCVDistrib/x64/opencv_java343.dll");
                    fileOut = File.createTempFile("lib", ".dll");
                }
                else{
                    //Utils.out.println(VideoEncodingChecker.class, "Unknown bit detected - trying with 32 bit");
                    in = Main.class.getResourceAsStream("/OpenCVDistrib/x86/opencv_java343.dll");
                    fileOut = File.createTempFile("lib", ".dll");
                }
            }
            else if(osName.equals("Mac OS X")){
                in = Main.class.getResourceAsStream("/OpenCVDistrib/mac/libopencv_java343.dylib");
                fileOut = File.createTempFile("lib", ".dylib");
            }
            OutputStream out = FileUtils.openOutputStream(fileOut);
            IOUtils.copy(in, out);
            in.close();
            out.close();
            System.load(fileOut.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load opencv native library", e);
        }
    }
}
