package mainpack;

// import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import helperspack.MyMatOfPoint;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Task: (делала только для добавления новых предметов (исчезновение предметов не обрабатывала))
 * Взять некоторое изображение/отрезок видео некоторой территории как сравниваемое;
 * сравнивать с этим шаблоном другой видеопоток, на котором могут появляться новые предметы;
 * при появлении предметов - фиксировать их нахождение, время появления и исчезновения.
 */
public class LeftObjects {

    private static final String URL = "some_url";
    private static final String TXT_FILE = "file.txt";
    // МАССИВ ОСТАВЛЕННЫХ ОЪЕКТОВ НЕ ОГРАНИЧЕН И ВСЕ НОВЫЕ ОБЪЕКТЫ ДОБАВЛЯЮТСЯ В КОНЕЦ

    public static void trackLeftObjects() throws FileNotFoundException, UnsupportedEncodingException {
        System.load("/opt/share/OpenCV/java/libopencv_java342.so");

        VideoCapture capture = new VideoCapture();
        Mat frameBGR = new Mat(); // текущий кадр
        Mat frameBackground = new Mat(); // фон (обновляется, если объект оставлен больше, чем на 5 секунд)
        Mat frameMOG = new Mat(); // кадр движущихся объектов (бинарный)
        PrintWriter printWriter = new PrintWriter(TXT_FILE, "UTF-8");
        BackgroundSubtractorMOG2 pMOG2 = Video.createBackgroundSubtractorMOG2();
        boolean f = false; // если человек ушел, а предмет лежит меньше заданного времени, продолжаем алгоритм

        ArrayList<MyMatOfPoint> objects = new ArrayList<>();
        ArrayList<MatOfPoint> contours = new ArrayList<>();

        if (capture.open(URL)) {
            System.out.println("Началось чтение видеопотока.");
        } else {
            System.out.println("Ошибка чтения видеопотока.");
        }

        capture.read(frameBGR);
        frameBGR.copyTo(frameBackground);
        Mat frameObjectS = Mat.zeros(frameBGR.cols(), frameBGR.rows(), CvType.CV_8UC1); // кадр оставленных объектов (бинарный)
        double fps = capture.get(Videoio.CAP_PROP_FPS);

        while (capture.isOpened()) {
            capture.read(frameBGR);
            if (!frameBGR.empty()) {
                pMOG2.apply(frameBGR, frameMOG);

                double avr = Core.mean(frameMOG).val[0];

                // когда началось движение
                if (avr > 5 || f) {

                    for (int i = 0; i < frameBGR.rows(); i++) {
                        for (int j = 0; j < frameBGR.cols(); j++) {
                            // сравниваем кадр и фон в пикселях, где нет движения
                            if (frameMOG.get(i, j)[0] == 0) {
                                // если (почти) не отличаются (погрешность из-за шумов)
                                if ((frameBGR.get(i, j)[0] - frameBackground.get(i, j)[0] < 10) &&
                                    (frameBGR.get(i, j)[1] - frameBackground.get(i, j)[1] < 10) &&
                                    (frameBGR.get(i, j)[2] - frameBackground.get(i, j)[2] < 10)) {
                                    // рисуем черным (фон)
                                    frameObjectS.put(i, j, 0);
                                } else {
                                    // рисуем белым (оставленный предмет)
                                    frameObjectS.put(i, j, 255);
                                }
                            }
                        }
                    }

                    // убираем шумы
                    Imgproc.resize(frameObjectS, frameObjectS, new Size(8, 8), 0, 0, Imgproc.INTER_AREA);
                    Imgproc.resize(frameObjectS, frameObjectS, new Size(frameBGR.cols(), frameBGR.rows()), 0, 0, Imgproc.INTER_LINEAR);

                    Imgproc.threshold(frameObjectS, frameObjectS, 100, 255, Imgproc.THRESH_BINARY);

                    double avr2 = Core.mean(frameObjectS).val[0];

                    // если предмет оставлен
                    if (avr2 > 5 || hasNotNullContour(objects)) {
                        if (!f) {
                            f = true;
                        }

                        // находим конутры, добавляем в массив объектов
                        // в соотвт. массиве времени начинаем отсчет
                        Imgproc.findContours(frameObjectS, contours, new Mat(), Imgproc.RETR_TREE,
                                Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

                        if (objects.size() == 0 || !hasNotNullContour(objects)) {
                            // когда в objects нет контуров для сравнения с контурами из contours
                            for (int i = 0; i < contours.size(); i++) {
                                MatOfPoint contour = contours.get(i);
                                MyMatOfPoint myMatOfPoint = new MyMatOfPoint(contour);
                                myMatOfPoint.setContourArea(Imgproc.contourArea(contour));
                                myMatOfPoint.setCenter(Imgproc.moments(contour));

                                objects.add(myMatOfPoint);
                            }
                        } else {
                            // допустим в предыдущем кадре было два предмета, потом один из них остался, а второй забрали
                            // и в новом кадре на новое место положили новый предмет (поэтому сравнивается положение контуров в кадре с предыдущим)
                            int size = objects.size();
                            int sizeNotNull = howManyNotNullContours(objects);

                            for (int i = 0; i < contours.size(); i++) {
                                int c = 0;

                                MatOfPoint contour = contours.get(i);
                                MyMatOfPoint myMatOfPoint = new MyMatOfPoint(contour);
                                myMatOfPoint.setContourArea(Imgproc.contourArea(contour));
                                myMatOfPoint.setCenter(Imgproc.moments(contour));
                                for (int j = 0; j < size; j++) {
                                    if (objects.get(j).getContourArea() != 0) {

                                        if (objects.get(j).getCenter().x - myMatOfPoint.getCenter().x > 10 || objects.get(j).getCenter().y - myMatOfPoint.getCenter().y > 10) {
                                            // -> это разные объекты
                                            c++;
                                        }
                                    }
                                }
                                if (c == sizeNotNull) {
                                    // -> это новый объект
                                    objects.add(myMatOfPoint);

                                    // TODO
                                    // а что делать со старым? как его удалить из objects?? (если его забирают в момент когда положили новый)
                                }
                            }
                        }

                        for (int i = 0; i < objects.size(); i++) {
                            if (objects.get(i).getContourArea() > 0) {
                                if (objects.get(i).getTime() == 0) objects.get(i).setF2(true);
                                objects.get(i).setTime(objects.get(i).getTime() + 1);
                                int timeSec = (int)(objects.get(i).getTime() / fps);

                                if (timeSec > 5 && timeSec <= 8) {
                                    MatOfPoint2f approxCurve = new MatOfPoint2f();
                                    MatOfPoint2f contour2f = new MatOfPoint2f(objects.get(i).getMatOfPoint().toArray());
                                    double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                                    Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
                                    Rect rect = Imgproc.boundingRect(new MatOfPoint(approxCurve.toArray()));
                                    int indent = 3; // отступ рамок прямоугольника от предмета
                                    Imgproc.rectangle(frameBGR, new Point(rect.x - indent, rect.y - indent),
                                            new Point(rect.x + rect.width + 2 * indent, rect.y + rect.height + 2 * indent),
                                            new Scalar(255, 0, 0), 2);

                                    // обновление фона + запись в файл
                                    if (objects.get(i).isF2()) {
                                        Mat frameObject = Mat.zeros(frameBGR.cols(), frameBGR.rows(), CvType.CV_8UC1);
                                        ArrayList<MatOfPoint> cnt = new ArrayList<>(); // приходится создавать, т.к. в drawContours передается не отдельный конутр, а массив контуров :(
                                        cnt.add(objects.get(i).getMatOfPoint());
                                        Imgproc.drawContours(frameObject, cnt, -1, new Scalar(255, 255, 255), -1);
                                        for (int j = 0; j < frameBGR.rows(); j++) {
                                            for (int k = 0; k < frameBGR.cols(); k++) {
                                                if (frameObject.get(j, k)[0] == 255) {
                                                    frameBackground.put(j, k, frameBGR.get(j, k));
                                                }
                                            }
                                        }

                                        String currentTime = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Calendar.getInstance().getTime());
                                        printWriter.println("Оставлен предмет, время: " + currentTime);

                                        objects.get(i).setF2(false);
                                    }
                                }

                                if (timeSec > 8) {
                                    objects.get(i).setContourArea(0);
                                    // objects.get(i).setTime(0);
                                }
                            }
                        }
                    } else {
                        if (f) {
                            f = false;
                        }

                        for (int i = 0; i < objects.size(); i++) {
                            // objects.get(i).setF2(false);
                            // objects.get(i).setTime(0);
                            if (objects.get(i).getContourArea() == 0) objects.get(i).setContourArea(0);
                        }
                    }
                }
            } else {
                capture.release();
            }
        }

    }

    private static boolean hasNotNullContour(ArrayList<MyMatOfPoint> arrayList) {
        boolean f = false;
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).getContourArea() != 0) {
                f = true;
            }
        }
        return f;
    }

    private static int howManyNotNullContours(ArrayList<MyMatOfPoint> arrayList) {
        int size = 0;
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).getContourArea() != 0) {
                size++;
            }
        }
        return size;
    }
}
