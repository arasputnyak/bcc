package mainpack;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import helperspack.Detection;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Task: (сделано только для прямоугольника!)
 * Задание фигуры - линии/прямоугольника, при пересечении/вхождении в которую - информирование,
 * запись времени вхождения и отрезка видео на время нахождения за линией/в прямоугольнике.
 */
public class BorderCrossingDetection {

    private static final String URL = "some_url";
    private static final String XML_FILE = "haarcascade_fullbody.xml";
    private static final String TXT_FILE = "file.txt";

    public static void trackBorderCrossing(Point botLeft, Point topRight) throws IOException {
        // Point-s должны задаваться пользователем!
        System.load("/opt/share/OpenCV/java/libopencv_java342.so");

        VideoCapture capture = new VideoCapture();
        VideoWriter writer = new VideoWriter();
        Mat frameBGR = new Mat();
        Mat frameMOG = new Mat();
        CascadeClassifier cascadeClassifier = new CascadeClassifier(XML_FILE);
        PrintWriter printWriter = new PrintWriter(TXT_FILE, "UTF-8");
        BackgroundSubtractorMOG2 pMOG2 = Video.createBackgroundSubtractorMOG2();
        String currentTime = "";
        boolean f = false;
        int numOfPeople = -1;
        int fileNumber = 0;

        if (capture.open(URL)) {
            System.out.println("Началось чтение видеопотока.");
        } else {
            System.out.println("Ошибка чтения видеопотока.");
        }

        while (capture.isOpened()) {
            capture.read(frameBGR);
            if (!frameBGR.empty()) {
                capture.read(frameBGR);
                Imgproc.rectangle(frameBGR, botLeft, topRight, new Scalar(139, 34, 82), 4, 8, 0);

                pMOG2.apply(frameBGR, frameMOG);
                Rect rectROI = new Rect(botLeft, topRight);
                Mat frameROIbgr = frameBGR.submat(rectROI);
                Mat frameROImog = frameMOG.submat(rectROI);

                double avr = Core.mean(frameROImog).val[0];
                if (avr > 10) {
                    // движение есть -> кто-то зашел в выделенную область
                    Rect[] peopleRects = Detection.detect(frameROIbgr, cascadeClassifier);
                    for (Rect man : peopleRects) {
                        Imgproc.rectangle(frameBGR, new Point(man.x, man.y),
                                new Point(man.x + man.width, man.y + man.height),
                                new Scalar(255, 0, 255), 2);
                    }
                    int currentNumOfPeople = peopleRects.length;
                    if (!f) {
                        f = true;
                        /*currentTime = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Calendar.getInstance().getTime());
                        printWriter.println("В выделенной области замечено движение: " + currentTime);*/

                        printWriter.println("В выделенной области замечено движение: ");

                        if (writer.isOpened()) {
                            writer.release();
                        }
                        writer.open(String.format("file%d.avi", fileNumber), VideoWriter.fourcc('X', 'V', 'I', 'D'),
                                15, new Size(1280, 720), true);
                        fileNumber++;
                    }
                    writer.write(frameBGR);
                    if (numOfPeople != currentNumOfPeople && numOfPeople != 0) {
                        numOfPeople = currentNumOfPeople;
                        currentTime = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Calendar.getInstance().getTime());
                        printWriter.println("      людей в области: " + numOfPeople + "; время: " + currentTime);
                    }
                } else {
                    if (f) {
                        f = false;
                        currentTime = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Calendar.getInstance().getTime());
                        printWriter.println("Движение в выделенной области прекратилось; время: " + currentTime);
                        writer.release();
                    }
                }
            } else {
                capture.release();
            }
        }

        if (writer.isOpened()) writer.release();
        printWriter.close();
    }
}
