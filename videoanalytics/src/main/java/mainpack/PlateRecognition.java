package mainpack;

import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import helperspack.Detection;
import helperspack.Sort;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Task:
 * Определение автомобильных номеров, используя алгоритмы обучения, при обнаружении - создание
 * скриншота, запись в логгируемый файл предполагаемый номер.
 */
public class PlateRecognition {

    private static final String URL = "some_url";
    private static final String XML_FILE = "/files/haarcascade_russian_plate_number.xml";
    private static final String NN_NUM = "/files/my_mod/model_num2all";
    private static final String NN_LETT = "/files/model_lett2all";
    private static final String TXT_FILE = "file.txt";

    public static void recognizePlateNumber() throws UnsupportedKerasConfigurationException, IOException, InvalidKerasConfigurationException {
        System.load("/opt/share/OpenCV/java/libopencv_java342.so");

        VideoCapture capture = new VideoCapture();
        Mat frameBGR = new Mat();
        Mat frameMOG = new Mat();
        CascadeClassifier classifier = new CascadeClassifier(XML_FILE);
        PrintWriter printWriter = new PrintWriter(TXT_FILE, "UTF-8");
        ArrayList<Mat[]> listOfPlatesMat = new ArrayList<>();
        ArrayList<String> allRecognizedPlates = new ArrayList<>();
        BackgroundSubtractorMOG2 pMOG2 = Video.createBackgroundSubtractorMOG2();
        MultiLayerNetwork networkNum = KerasModelImport.importKerasSequentialModelAndWeights(NN_NUM);
        MultiLayerNetwork networkLett = KerasModelImport.importKerasSequentialModelAndWeights(NN_LETT);

        if (capture.open(URL)) {
            System.out.println("Началось чтение видеопотока.");
        } else {
            System.out.println("Ошибка чтения видеопотока.");
        }

        while (capture.isOpened()) {
            capture.read(frameBGR);
            if (!frameBGR.empty()) {
                pMOG2.apply(frameBGR, frameMOG);

                double avr = Core.mean(frameMOG).val[0];
                // если есть движение
                if (avr > 10) {
                    Rect[] platesRect = Detection.detect(frameBGR, classifier);
                    Sort.sortRectsByX(platesRect); // нужно ли ? они вроде уже отсортированы на выходе из detectMultiScale, но это не точно

                    ArrayList<Mat> platesMat = Detection.getRotatedPlates(frameBGR, platesRect);

                    for (Mat plateMat : platesMat) {
                        Imgproc.resize(plateMat, plateMat, new Size(250, 50),
                                0, 0, Imgproc.INTER_LINEAR);

                        listOfPlatesMat.add(Detection.getSymbols(plateMat));
                    }

                    for (int i = 0; i < listOfPlatesMat.size(); i++) {
                        ArrayList<String> resultArrLst = Detection.getResults(listOfPlatesMat.get(i), networkNum, networkLett);
                        String resultStr;
                        if (resultArrLst.size() == 0) {
                            resultStr = "не распознан";
                        } else {
                            resultStr = String.join("", resultArrLst);
                        }
                        Imgproc.putText(frameBGR, resultStr, new Point(platesRect[i].x, platesRect[i].y - 10),
                                Core.FONT_HERSHEY_DUPLEX, 0.7, new Scalar(255, 255, 0), 1);


                        // на питоне запись производится по лучшему принципу :)
                        if (!allRecognizedPlates.contains(resultStr)) {
                            allRecognizedPlates.add(resultStr);
                            printWriter.println("Автомобильный номер: " + resultStr + "; время: "
                                    + new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Calendar.getInstance().getTime()));
                            Imgcodecs.imwrite(String.format("file%d.jpg", allRecognizedPlates.size()), frameBGR);
                        }
                    }

                    for (Rect rect : platesRect) {
                        Imgproc.rectangle(frameBGR, new Point(rect.x, rect.y),
                                new Point(rect.x + rect.width, rect.y + rect.height),
                                new Scalar(255, 0, 255), 2);
                    }
                    // new Scalar(44, 24, 143)
                    // new Scalar(143, 44, 24)
                }
            } else {
                capture.release();
            }
        }

        printWriter.close();

    }
}
