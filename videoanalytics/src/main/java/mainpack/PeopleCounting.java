package mainpack;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import helperspack.Detection;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Task:
 * Определение количества человек на наблюдаемом видеопотоке, подсчет количества.
 */
public class PeopleCounting {

    private static final String URL = "some_url";
    private static final String XML_FILE = "haarcascade_fullbody.xml";
    private static final String TXT_FILE = "file.txt";

    public static void countPeople() throws FileNotFoundException, UnsupportedEncodingException {
        System.load("/opt/share/OpenCV/java/libopencv_java342.so");

        VideoCapture capture = new VideoCapture();
        Mat frame = new Mat();
        CascadeClassifier classifier = new CascadeClassifier(XML_FILE);
        PrintWriter printWriter = new PrintWriter(TXT_FILE, "UTF-8");
        int numOfPeople = -1;

        if (capture.open(URL)) {
            System.out.println("Началось чтение видеопотока.");
        } else {
            System.out.println("Ошибка чтения видеопотока.");
        }

        while (capture.isOpened()) {
            capture.read(frame);
            if (!frame.empty()) {
                capture.read(frame);
                Rect[] peopleRect = Detection.detect(frame, classifier);
                int currentNumOfPeople = peopleRect.length;

                if (numOfPeople != currentNumOfPeople && numOfPeople != 0) {
                    numOfPeople = currentNumOfPeople;
                    printWriter.println("Количество людей в кадре: " + currentNumOfPeople);
                }
            } else {
                capture.release();
            }
        }
        printWriter.close();
    }

}
