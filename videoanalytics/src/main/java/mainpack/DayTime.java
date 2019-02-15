package mainpack;

import org.joda.time.DateTime;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Task: (тут еще думать и думать)
 * Определение времени суток в зависимости от уровня освещения;
 * Основываясь на текущем времени, проверять уровень освещения;
 * при несовпадении (напр., темный экран в дневное время) - информирование;
 * Стабилизация изображения при уровне освещения ниже заданного порога.
 */
public class DayTime {

    private static final String URL = "some_url";

    public static void main() throws FileNotFoundException, UnsupportedEncodingException {
        System.load("/opt/share/OpenCV/java/libopencv_java342.so");

        VideoCapture capture = new VideoCapture();
        Mat frameBGR = new Mat();
        boolean f = false;

        if (capture.open(URL)) {
            System.out.println("Началось чтение видеопотока.");
        } else {
            System.out.println("Ошибка чтения видеопотока.");
        }

        while (capture.isOpened()) {
            capture.read(frameBGR);
            if (!frameBGR.empty()) {
                // первый вариант: бинаризация + сравнение среднего значения пикселей с порогом

                Mat frameBW = new Mat();
                Mat frameThresh = new Mat();
                Imgproc.cvtColor(frameBGR, frameBW, Imgproc.COLOR_BGR2GRAY);
                Imgproc.threshold(frameBW, frameThresh, 100, 255, Imgproc.THRESH_BINARY);

                double avr = Core.mean(frameThresh).val[0];

                // второй вариант (не дописан): перевод в монохромное + гистограмма по яркости
                // если преобладают темные пиксели -> темное время суток
                /*Mat frameHSV = new Mat();
                Mat hist = new Mat();
                Imgproc.cvtColor(frame, frameHSV, Imgproc.COLOR_BGR2HSV);
                List<Mat> smth = new ArrayList<>();
                Core.split(frameHSV, smth);
                MatOfFloat ranges = new MatOfFloat(0f, 256f);
                Imgproc.calcHist(smth, new MatOfInt(0), new Mat(), hist,
                new MatOfInt(256), ranges);*/
                // float brightness = кол-во пикселей с яркостью 0-50 делить на общее число пикселей
                // если brightness > порога в ночное время или
                // если brightness < порога в девное время
                // -> тревога

                // в зависимости от времени года (т.е. от того, когда светает/темнеет)
                // должны быть разные значения этих констант (хотя вообще константы - это зло :))
                int currentTime = new DateTime().getHourOfDay();
                if ((((currentTime >= 22) || (currentTime >= 0 && currentTime <=5)) && avr > 40) ||
                        ((currentTime > 5 && currentTime < 22) && avr < 40)) {
                    if (!f) {
                        f = true;
                        if (avr > 40) {
                            System.out.println("Уровень освещения не соответствует ночному времени суток, "
                                    + new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Calendar.getInstance().getTime()));
                        } else {
                            System.out.println("Уровень освещения не соответствует дневному времени суток, "
                                    + new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Calendar.getInstance().getTime()));
                        }
                    }
                    if (f) {
                        if (avr < 40) {
                            if (avr > 10) {
                                // стабилизация изображения (вообще эта фунция встроена в совр. камеры...?)
                                stabilizeImage1(frameBGR, 2, (int)(avr / 2));
                                // OR stabilizeImage2(frame, 0.5);
                            } else {
                                System.out.println("Необходимо включить источник света..");
                            }

                        }
                    }
                } else {
                    if (f) {
                        f = false;
                        System.out.println("Уровень освещения соответствует времени суток.");
                    }
                }
            } else {
                capture.release();
            }
        }
    }

    /**
     * такая себе стабилизация, просто сделала поярче и поконтрастнее
     * @param alpha - параметр контрастности
     * @param beta - параметр яркости
     */
    private static void stabilizeImage1(Mat image, double alpha, double beta) {
        image.convertTo(image, -1, alpha, beta);
    }

    /**
     * гамма-коррекция (надо тестить для подбора оптимального параметра)
     * @param gamma - должен быть меньше единицы для распознавания деталей на слабо освещённых участках
     */
    private static void stabilizeImage2(Mat image, double gamma) {
        Mat lut = new Mat(1, 256, CvType.CV_8UC1);
        lut.setTo(new Scalar(0));

        for (int i = 0; i < 256; i++) {
            lut.put(0, i, Math.pow(i/255, gamma) * 255);
        }
        Core.LUT(image, lut, image);
    }

}
