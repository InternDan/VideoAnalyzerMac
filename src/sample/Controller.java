package sample;


import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.round;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import javafx.scene.media.MediaView;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Controller implements Initializable{
    MediaPlayer mediaPlayer;
    Media media;

    VideoCapture capture;
    String videoPath = "E:/Google Drive/NetBeansProjects/VideoEncodingChecker/barsandtone.flv";
    double fps;

    int brightnessPercentChange = 30;
    int blurrThreshold = 70;
    double blackThreshold = 0.001;
    double whiteThreshold = 0.999;
    double adobeThreshold = 0.85;

    final ArrayList<Button> brightnessButtons = new ArrayList<Button>();
    final ArrayList<Button> whiteButtons = new ArrayList<Button>();
    final ArrayList<Button> blackButtons = new ArrayList<Button>();
    final ArrayList<Button> sameButtons = new ArrayList<Button>();
    final ArrayList<Button> adobeButtons = new ArrayList<Button>();


    @FXML
    private ImageView iv = new ImageView();

    @FXML
    private BorderPane bp = new BorderPane();

    @FXML
    private StackPane sp = new StackPane();

    @FXML
    private VBox videoVB = new VBox();

    @FXML
    private ProgressBar progressBar = new ProgressBar();

    @FXML
    private AnchorPane progressBarAnchor = new AnchorPane();


    @FXML
    private Button buttonLoadVideo;


    @FXML
    private Button buttonFullAnalysis;

    @FXML
    private Button buttonAnalyzeAudio;

    @FXML
    private Button buttonAnalyzeVideo;

    @FXML
    private CheckBox audioSilence;

    @FXML
    private CheckBox audioWaveformSpike;

    @FXML
    private CheckBox audioDisparateLevels;

    @FXML
    private CheckBox videoWhiteFrame;

    @FXML
    private CheckBox videoBlackFrame;

    @FXML
    private CheckBox videoPremiereOffline;

    @FXML
    private CheckBox videoFrameRepeat;

    @FXML
    private CheckBox videoBrightnessFrame;

    ScrollBar scrollBar = new ScrollBar();

    @FXML
    private void onClick_load_video(){
        File file = PickAFile();
        videoPath = file.toString();
        capture = new VideoCapture(videoPath);
        fps = capture.get(Videoio.CAP_PROP_FPS);
        if (capture.isOpened()) {
            Mat frame = new Mat();
            capture.read(frame);
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            Image img = new Image(new ByteArrayInputStream(buffer.toArray()));
            iv.setImage(img);
            iv = ResizeImageViewToBorderPaneCenter(iv,bp);
        }
        scrollBar.setMin(0);
        scrollBar.setMax(capture.get(7));
        scrollBar.setOrientation(Orientation.HORIZONTAL);
        scrollBar.setPrefHeight(30);
        scrollBar.valueProperty().addListener((ObservableValue<? extends Number> ov, Number old_val, Number new_val) -> {
            GoToFrame((int) round(new_val.doubleValue()));
        });
        bp.setBottom(scrollBar);
        buttonLoadVideo.managedProperty().bind(buttonLoadVideo.visibleProperty());
        buttonLoadVideo.setVisible(false);
        StackPane.setAlignment(iv, Pos.CENTER);
    }

    @FXML
    private void onClick_full_analysis() throws InterruptedException{

        int bars = CountCheckedBoxes();
        ArrayList<ProgressBar> pBarArray = new ArrayList<ProgressBar>();

        GridPane progressBarGridPane = new GridPane();
        progressBarGridPane.setHgap(10);
        progressBarGridPane.prefWidthProperty().bind(progressBarAnchor.widthProperty());
        for (int i = 0; i < bars; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(100.0 / bars);
            progressBarGridPane.getColumnConstraints().add(colConst);
            progressBarGridPane.setMaxWidth(Double.MAX_VALUE);
            pBarArray.add(new ProgressBar());
            pBarArray.get(i).setMaxWidth(Double.MAX_VALUE);
            progressBarGridPane.add(pBarArray.get(i),i,0,1,1);
        }
        progressBarGridPane.getStyleClass().add("grid");
        progressBarAnchor.getChildren().add(progressBarGridPane);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        int pBarCount = 0;
        if (videoWhiteFrame.isSelected()){

            pBarArray.get(pBarCount).setProgress(0);
            Task copyWorker = whiteWorker();
            pBarArray.get(pBarCount).progressProperty().unbind();
            pBarArray.get(pBarCount).progressProperty().bind(copyWorker.progressProperty());
            copyWorker.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                System.out.println(newValue);
            });
            copyWorker.setOnSucceeded(e -> {
                for (int i = 0; i < whiteButtons.size(); i++){
                    videoVB.getChildren().add(whiteButtons.get(i));
                }
            });
            Thread thread = new Thread(copyWorker);
            executor.submit(thread);
            pBarCount++;
        }

        if (videoBlackFrame.isSelected()){
            pBarArray.get(pBarCount).setProgress(0);
            Task copyWorker = blackWorker();
            pBarArray.get(pBarCount).progressProperty().unbind();
            pBarArray.get(pBarCount).progressProperty().bind(copyWorker.progressProperty());
            copyWorker.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                System.out.println(newValue);
            });
            copyWorker.setOnSucceeded(e -> {
                for (int i = 0; i < blackButtons.size(); i++){
                    videoVB.getChildren().add(blackButtons.get(i));
                }
            });
            Thread thread = new Thread(copyWorker);
            executor.submit(thread);
            pBarCount++;
        }

        if (videoPremiereOffline.isSelected()){
            pBarArray.get(pBarCount).setProgress(0);
            Task copyWorker = adobePremiereWorker();
            pBarArray.get(pBarCount).progressProperty().unbind();
            pBarArray.get(pBarCount).progressProperty().bind(copyWorker.progressProperty());
            copyWorker.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                System.out.println(newValue);
            });
            copyWorker.setOnSucceeded(e -> {
                for (int i = 0; i < adobeButtons.size(); i++){
                    videoVB.getChildren().add(adobeButtons.get(i));
                }
            });
            Thread thread = new Thread(copyWorker);
            executor.submit(thread);
            pBarCount++;
        }

        if (videoFrameRepeat.isSelected()){
            pBarArray.get(pBarCount).setProgress(0);
            Task copyWorker = repeatWorker();
            pBarArray.get(pBarCount).progressProperty().unbind();
            pBarArray.get(pBarCount).progressProperty().bind(copyWorker.progressProperty());
            copyWorker.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                System.out.println(newValue);
            });
            copyWorker.setOnSucceeded(e -> {
                for (int i = 0; i < sameButtons.size(); i++){
                    videoVB.getChildren().add(sameButtons.get(i));
                }
            });
            Thread thread = new Thread(copyWorker);
            executor.submit(thread);
            pBarCount++;
        }

        if (videoBrightnessFrame.isSelected()){
            pBarArray.get(pBarCount).setProgress(0);
            Task copyWorker = brightnessWorker();
            pBarArray.get(pBarCount).progressProperty().unbind();
            pBarArray.get(pBarCount).progressProperty().bind(copyWorker.progressProperty());
            copyWorker.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                System.out.println(newValue);
            });
            copyWorker.setOnSucceeded(e -> {
                for (int i = 0; i < brightnessButtons.size(); i++){
                    videoVB.getChildren().add(brightnessButtons.get(i));
                }
            });
            Thread thread = new Thread(copyWorker);
            executor.submit(thread);
            pBarCount++;
        }


        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS); //waits until both runnables have finished.


    }

    @FXML
    private void onClick_analyze_audio(){

    }

    @FXML
    private void onClick_analyze_video(){

    }

    @Override
    public void initialize(URL url, ResourceBundle rb){
        //todo

    }

    //Tasks

    public Task brightnessWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                VideoCapture camera = new VideoCapture(videoPath);
                double frames = camera.get(7);
                ArrayList<Integer> framesBrightnessChange = new ArrayList<>();
                if (!camera.isOpened()) {
                    System.out.println("Error! Camera can't be opened!");
                    return framesBrightnessChange;
                }
                Mat frame = new Mat();
                double diff = 0;
                int f = 1;
                double dPrev = 0;
                while(true){
                    if (camera.read(frame)){
                        Mat grayFrame = convertToGray(frame);
                        MatOfDouble mu = new MatOfDouble();
                        MatOfDouble sigma = new MatOfDouble();
                        Core.meanStdDev(grayFrame, mu, sigma);
                        double d = mu.get(0,0)[0];
                        if (f != 0) {
                            diff = d - dPrev;
                            if (((diff / d) * 100) > brightnessPercentChange) {
                                Button btn = new Button();
                                btn.prefWidthProperty().set(350);
                                btn.wrapTextProperty().setValue(true);
                                double seconds = (double) f / fps;
                                int hours = (int) (seconds / 3600);
                                seconds = seconds % 3600;
                                int minutes = (int) seconds/60;
                                int sec = (int) seconds % 60;
                                //TODO: convert these correctly
                                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                                btn.setText("Brightness fr " + Integer.toString(f) + " at " + time);
                                btn.setStyle("-fx-background-color: transparent;");
                                final String vPath = videoPath;
                                final int f2 = f;
                                btn.setOnAction((ActionEvent e) -> {
                                    GoToFrameResults(f2);
                                });
                                brightnessButtons.add(btn);
                            }
                        }
                        f++;
                        updateProgress(f, frames);
                        dPrev = d;
                        grayFrame.release();
                        mu.release();
                        sigma.release();
                    }else {
                        camera.release();
                        return true;
                    }
                }
            }
        };
    }

    public Task whiteWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                VideoCapture camera = new VideoCapture(videoPath);
                double frames = camera.get(7);
                //get color depth
                ArrayList<Integer> whiteFrames = new ArrayList<>();
                if (!camera.isOpened()) {
                    System.out.println("Error! Camera can't be opened!");
                    return whiteFrames;
                }
                Mat frame = new Mat();
                int f = 1;
                while(true){
                    if (camera.read(frame)){
                        Mat grayFrame = convertToGray(frame);
                        double thresh = whiteThreshold * InterpretDepthInt(grayFrame.depth());
                        MatOfDouble mu = new MatOfDouble();
                        MatOfDouble sigma = new MatOfDouble();
                        Core.meanStdDev(grayFrame, mu, sigma);
                        double d = mu.get(0,0)[0];
                        if (d > thresh) {
                            Button btn = new Button();
                            btn.prefWidthProperty().set(350);
                            btn.wrapTextProperty().setValue(true);
                            double seconds = (double) f / fps;
                            int hours = (int) (seconds / 3600);
                            seconds = seconds % 3600;
                            int minutes = (int) seconds/60;
                            int sec = (int) seconds % 60;
                            //TODO: convert these correctly
                            String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                            btn.setText("Brightness fr " + Integer.toString(f) + " at " + time);
                            btn.setStyle("-fx-background-color: transparent;");
                            final String vPath = videoPath;
                            final int f2 = f;
                            btn.setOnAction((ActionEvent e) -> {
                                GoToFrameResults(f2);
                            });
                            whiteButtons.add(btn);
                        }
                        f++;
                        updateProgress(f, frames);
                        grayFrame.release();
                        mu.release();
                        sigma.release();
                    }else {
                        camera.release();
                        return whiteFrames;
                    }
                }
            }
        };
    }

    public Task blackWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                VideoCapture camera = new VideoCapture(videoPath);
                double frames = camera.get(7);
                //get color depth
                ArrayList<Integer> blackFrames = new ArrayList<>();
                if (!camera.isOpened()) {
                    System.out.println("Error! Camera can't be opened!");
                    return blackFrames;
                }
                Mat frame = new Mat();
                int f = 1;
                while(true){
                    if (camera.read(frame)){
                        Mat grayFrame = convertToGray(frame);
                        double thresh = blackThreshold * InterpretDepthInt(grayFrame.depth());
                        MatOfDouble mu = new MatOfDouble();
                        MatOfDouble sigma = new MatOfDouble();
                        Core.meanStdDev(grayFrame, mu, sigma);
                        double d = mu.get(0,0)[0];
                        if (d < thresh) {
                            Button btn = new Button();
                            btn.prefWidthProperty().set(350);
                            btn.wrapTextProperty().setValue(true);
                            double seconds = (double) f / fps;
                            int hours = (int) (seconds / 3600);
                            seconds = seconds % 3600;
                            int minutes = (int) seconds/60;
                            int sec = (int) seconds % 60;
                            //TODO: convert these correctly
                            String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                            btn.setText("Brightness fr " + Integer.toString(f) + " at " + time);
                            btn.setStyle("-fx-background-color: transparent;");
                            final String vPath = videoPath;
                            final int f2 = f;
                            btn.setOnAction((ActionEvent e) -> {
                                GoToFrameResults(f2);
                            });
                            blackButtons.add(btn);
                        }
                        f++;
                        updateProgress(f, frames);
                        grayFrame.release();
                        mu.release();
                        sigma.release();
                    }else {
                        camera.release();
                        return blackFrames;
                    }
                }
            }
        };
    }

    public Task repeatWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                VideoCapture camera = new VideoCapture(videoPath);
                double numFrames = camera.get(7);
                //get color depth
                ArrayList<Integer> sameFrames = new ArrayList<>();
                if (!camera.isOpened()) {
                    System.out.println("Error! Camera can't be opened!");
                    return sameFrames;
                }
                Mat frame1 = new Mat();
                Mat frame2 = new Mat();
                for (int i = 0; i < numFrames; i++){
                    if(i>0){
                        camera.set(1,i-1);
                        camera.read(frame1);
                        Mat grayFrame1 = convertToGray(frame1);
                        camera.set(1,i);
                        camera.read(frame2);
                        Mat grayFrame2 = convertToGray(frame2);
                        Mat diff = new Mat();
                        Core.compare(grayFrame1, grayFrame2,diff, Core.CMP_EQ);
                        int different = Core.countNonZero(diff);
                        if(different < 4){
                            Button btn = new Button();
                            btn.prefWidthProperty().set(350);
                            btn.wrapTextProperty().setValue(true);
                            double seconds = (double) i / fps;
                            int hours = (int) (seconds / 3600);
                            seconds = seconds % 3600;
                            int minutes = (int) seconds/60;
                            int sec = (int) seconds % 60;
                            //TODO: convert these correctly
                            String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                            btn.setText("Repeat fr " + Integer.toString(i) + " at " + time);
                            btn.setStyle("-fx-background-color: transparent;");
                            final String vPath = videoPath;
                            final int f2 = i;
                            btn.setOnAction((ActionEvent e) -> {
                                GoToFrameResults(f2);
                            });
                            sameButtons.add(btn);
                        }
                        grayFrame1.release();
                        grayFrame2.release();
                        updateProgress(i, numFrames);
                    }
                }
                return sameFrames;
            }
        };
    }

    public Task adobePremiereWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                VideoCapture camera = new VideoCapture(videoPath);
                double numFrames = camera.get(7);
                //get color depth
                ArrayList<Integer> adobeFrames = new ArrayList<>();
                if (!camera.isOpened()) {
                    System.out.println("Error! Camera can't be opened!");
                    return adobeFrames;
                }

                Mat tmp = readImage("CS6_media-offline.png", 1);



                //Mat tmp = Imgcodecs.imread(getClass().getClassLoader().getResource(pathToImageSortBy).toURI().toString());
                Mat adobeVerySmall = convertToGray(tmp);
                tmp = Imgcodecs.imread("c:\\users\\buffa\\desktop\\CS6_media-offline.tif");
                Mat adobeSmall = convertToGray(tmp);
                tmp = Imgcodecs.imread("c:\\users\\buffa\\desktop\\CS6_media-offline.tif");
                Mat adobeMedium = convertToGray(tmp);
                tmp = Imgcodecs.imread("c:\\users\\buffa\\desktop\\CS6_media-offline.tif");
                Mat adobeLarge = convertToGray(tmp);
                tmp = Imgcodecs.imread("c:\\users\\buffa\\desktop\\CS6_media-offline.tif");
                Mat adobeHD = convertToGray(tmp);
                tmp = Imgcodecs.imread("c:\\users\\buffa\\desktop\\CS6_media-offline.tif");
                Mat adobeUHD = convertToGray(tmp);

                Mat frame = new Mat();
                for (int i = 0; i < numFrames; i++){
                    camera.set(1,i);
                    camera.read(frame);
                    //frame = Imgcodecs.imread("c:\\users\\buffa\\desktop\\CS6_media-offline.tif");
                    Mat grayFrame = convertToGray(frame);
                    //Core.compare(grayFrame1, grayFrame2,diff, Core.CMP_EQ);
                    //int different = Core.countNonZero(diff);

                    for(int j = 0; j < 6; j++){
                        Mat temp1 = new Mat();
                        switch (j) {
                            case 0:
                                temp1 = adobeVerySmall;
                                break;
                            case 1:
                                temp1 = adobeSmall;
                                break;
                            case 2:
                                temp1 = adobeMedium;
                                break;
                            case 3:
                                temp1 = adobeLarge;
                                break;
                            case 4:
                                temp1 = adobeHD;
                                break;
                            case 5:
                                temp1 = adobeUHD;
                                break;
                            default:
                                break;
                        }
                        Mat result = new Mat();
                        int result_cols = frame.cols() - temp1.cols()+1;
                        int result_rows = frame.rows() - temp1.rows()+1;
                        result.create(result_rows, result_cols, CvType.CV_8UC3);
                        Imgproc.matchTemplate(grayFrame, temp1, result, Imgproc.TM_SQDIFF);
                        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
                        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
                        if(mmr.minVal < 4){
                            Button btn = new Button();
                            btn.prefWidthProperty().set(350);
                            btn.wrapTextProperty().setValue(true);
                            double seconds = (double) i / fps;
                            int hours = (int) (seconds / 3600);
                            seconds = seconds % 3600;
                            int minutes = (int) seconds/60;
                            int sec = (int) seconds % 60;
                            //TODO: convert these correctly
                            String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                            btn.setText("Adobe fr " + Integer.toString(i) + " at " + time);
                            btn.setStyle("-fx-background-color: transparent;");
                            final String vPath = videoPath;
                            final int f2 = i;
                            btn.setOnAction((ActionEvent e) -> {
                                GoToFrameResults(f2);
                            });
                            adobeButtons.add(btn);
                        }
                        result.release();
                        //temp1.release();
                    }

                    grayFrame.release();
                    updateProgress(i, numFrames);
                }

                return adobeFrames;
            }
        };
    }

    //helpers

    public static Mat convertToGray(Mat frame) {
        Mat gray = frame.clone();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);
        return gray;
    }

    public static void showWindow(BufferedImage img) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel(new ImageIcon(img)));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(img.getWidth(), img.getHeight() + 30);
        frame.setTitle("Image captured");
        frame.setVisible(true);
    }

    public static BufferedImage matToBufferedImage(Mat frame) {
        int type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
        WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);

        return image;
    }

    public static File PickAFile(){
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Videos", "mp4", "flv", "mov");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        else{
            return null;
        }
    }

    public static ImageView ResizeImageViewToBorderPaneCenter(ImageView iv, BorderPane bp){
        iv.fitWidthProperty().bind(bp.widthProperty());
        iv.fitHeightProperty().bind(bp.heightProperty());

        return iv;
    }

    public static ArrayList<Integer> ParseFrameNumbers(ArrayList<Integer> al){
        ArrayList<Integer> outList = new ArrayList<Integer>();
        for(int i = 0; i < al.size(); i++){
            if(i == 0){
                outList.add(al.get(i));
            }else{
                if(al.get(i)-al.get(i-1) > 1){
                    outList.add(al.get(i));
                }
            }
        }
        return outList;
    }

    public static double InterpretDepthInt(int i){
        if (i==0){
            return 255;
        }else if(i == 1){
            return 127;
        }else if(i == 2){
            return 65535;
        }else if(i == 3){
            return 32767;
        }else if(i == 4){
            return 2147483647;
        }else{
            return 0;
        }
    }

    public void SetResultsLinkButtons(ArrayList<Integer> frames, String arrayName){
        if(arrayName.equals("blurryFrames")){
            for(int i = 0; i < frames.size(); i++){
                Button btn = new Button();
                btn.prefWidthProperty().set(350);
                btn.wrapTextProperty().setValue(true);
                final int f = frames.get(i);
                double seconds = (double) f / fps;
                int hours = (int) (seconds / 3600);
                seconds = seconds % 3600;
                int minutes = (int) seconds/60;
                int sec = (int) seconds % 60;
                //TODO: convert these correctly
                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                btn.setText("Blurry fr " + Integer.toString(f) + " at " + time);
                btn.setStyle("-fx-background-color: transparent;");
                final String vPath = videoPath;
                btn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        GoToFrameResults(f);
                    }
                });
                videoVB.getChildren().add(btn);
            }
        }

        if(arrayName.equals("whiteFrames")){
            for(int i = 0; i < frames.size(); i++){
                Button btn = new Button();
                btn.prefWidthProperty().set(350);
                btn.wrapTextProperty().setValue(true);
                final int f = frames.get(i);
                double seconds = (double) f / fps;
                int hours = (int) (seconds / 3600);
                seconds = seconds % 3600;
                int minutes = (int) seconds/60;
                int sec = (int) seconds % 60;
                //TODO: convert these correctly
                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                btn.setText("White fr " + Integer.toString(f) + " at " + time);
                btn.setStyle("-fx-background-color: transparent;");
                final String vPath = videoPath;
                btn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        GoToFrameResults(f);
                    }
                });
                videoVB.getChildren().add(btn);
            }
        }

        if(arrayName.equals("duplicateFrames")){
            for(int i = 0; i < frames.size(); i++){
                Button btn = new Button();
                btn.prefWidthProperty().set(350);
                btn.wrapTextProperty().setValue(true);
                final int f = frames.get(i);
                double seconds = (double) f / fps;
                int hours = (int) (seconds / 3600);
                seconds = seconds % 3600;
                int minutes = (int) seconds/60;
                int sec = (int) seconds % 60;
                //TODO: convert these correctly
                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                btn.setText("Duplicate fr " + Integer.toString(f) + " at " + time);
                btn.setStyle("-fx-background-color: transparent;");
                final String vPath = videoPath;
                btn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        GoToFrameResults(f);
                    }
                });
                videoVB.getChildren().add(btn);
            }
        }

        if(arrayName.equals("blackFrames")){
            for(int i = 0; i < frames.size(); i++){
                Button btn = new Button();
                btn.prefWidthProperty().set(350);
                btn.wrapTextProperty().setValue(true);
                final int f = frames.get(i);
                double seconds = (double) f / fps;
                int hours = (int) (seconds / 3600);
                seconds = seconds % 3600;
                int minutes = (int) seconds/60;
                int sec = (int) seconds % 60;
                //TODO: convert these correctly
                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                btn.setText("Black fr " + Integer.toString(f) + " at " + time);
                btn.setStyle("-fx-background-color: transparent;");
                final String vPath = videoPath;
                btn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        GoToFrameResults(f);
                    }
                });
                videoVB.getChildren().add(btn);
            }
        }

        if(arrayName.equals("brightnessFrames")){
            for(int i = 0; i < frames.size(); i++){
                Button btn = new Button();
                btn.prefWidthProperty().set(350);
                btn.wrapTextProperty().setValue(true);
                final int f = frames.get(i);
                double seconds = (double) f / fps;
                int hours = (int) (seconds / 3600);
                seconds = seconds % 3600;
                int minutes = (int) seconds/60;
                int sec = (int) seconds % 60;
                //TODO: convert these correctly
                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, sec);
                btn.setText("Brightness fr " + Integer.toString(f) + " at " + time);
                btn.setStyle("-fx-background-color: transparent;");
                final String vPath = videoPath;
                btn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        GoToFrameResults(f);
                    }
                });
                videoVB.getChildren().add(btn);
            }
        }
    }

    public void GoToFrameResults(int i){
        if (capture.isOpened()) {
            capture.set(1,i);
            Mat frame = new Mat();
            capture.read(frame);
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            Image img = new Image(new ByteArrayInputStream(buffer.toArray()));
            iv.setImage(img);
            scrollBar.setValue((double)i+1);
            //iv = ResizeImageViewToBorderPaneCenter(iv,bp);
        }
    }

    public void GoToFrame(int i){
        if (capture.isOpened()) {
            capture.set(1,i);
            Mat frame = new Mat();
            capture.read(frame);
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            Image img = new Image(new ByteArrayInputStream(buffer.toArray()));
            iv.setImage(img);
        }
    }

    public int CountCheckedBoxes(){
        int count = 0;
        if (videoWhiteFrame.isSelected()){
            count++;
        }

        if (videoBlackFrame.isSelected()){
            count++;
        }

        if (videoPremiereOffline.isSelected()){
            count++;
        }

        if (videoFrameRepeat.isSelected()){
            count++;
        }

        if (videoBrightnessFrame.isSelected()){
            count++;
        }
        return count;
    }

    public static Mat readImage(String name, int flags) {
        URL url = name.getClass().getResource(name);

        // make sure the file exists
        if (url == null) {
            System.out.println("ResourceNotFound: " + name);
            return new Mat();
        }

        String path = url.getPath();

        // not sure why we (sometimes; while running unpacked from the IDE) end
        // up with the authority-part of the path (a single slash) as prefix,
        // ...anyways: Highgui.imread can't handle it, so that's why.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Mat image = Imgcodecs.imread(path, flags);

        // ...and if Highgui.imread() has failed, we simply assume that the file
        // is packed in a jar (i.e. Java should be able to read the image)
        if (image.empty()) {
            BufferedImage buf;

            try {
                buf = ImageIO.read(url);
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
                return image;
            }

            int height = buf.getHeight();
            int width = buf.getWidth();
            int rgb, type, channels;

            switch (flags) {
                case 1:
                    type = CvType.CV_8UC1;
                    channels = 1;
                    break;
                case 2:
                default:
                    type = CvType.CV_8UC3;
                    channels = 3;
                    break;
            }

            byte[] px = new byte[channels];
            image = new Mat(height, width, type);

            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    rgb = buf.getRGB(x, y);
                    px[0] = (byte)(rgb & 0xFF);
                    if (channels==3) {
                        px[1] = (byte)((rgb >> 8) & 0xFF);
                        px[2] = (byte)((rgb >> 16) & 0xFF);
                    }
                    image.put(y, x, px);
                }
            }
        }

        return image;
    }
}
