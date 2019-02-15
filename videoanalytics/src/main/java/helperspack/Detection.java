package helperspack;

import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;

public class Detection {
    private static String[] alphabet = "A,B,C,E,H,K,M,O,P,T,Y,X".split(",");

    /**
     * функция для обнаружения искомых объектов и удаления повторений (+)
     * @return - возвращает rects!! не mat
     */
    public static Rect[] detect(Mat image, CascadeClassifier classifier) {
        Mat imageGray = new Mat();
        MatOfRect detectedObjects = new MatOfRect();

        Imgproc.cvtColor(image, imageGray, Imgproc.COLOR_BGR2GRAY);
        /*classifier.detectMultiScale(grayMat, plates, 1.3, 10,
                0, new Size(30, 30), new Size(130, 130));*/
        classifier.detectMultiScale(imageGray, detectedObjects);
        Rect[] detectedObjectsRect = detectedObjects.toArray();

        // (+) иногда detectMultiScale один объект распознает дважды (как один внутри другого или как пересекающиеся)
        // -> один из них лишний -> удаляем
        ArrayList<Integer> inds = internalObjects(detectedObjectsRect);
        for (int i = 0; i < inds.size(); i++) {
            detectedObjectsRect = ArrayUtils.removeElement(detectedObjectsRect, inds.get(i));
        }
        return detectedObjectsRect;
    }

    /**
     * функция для нахождения индексов внутренних и пересекающихся контуров
     * считается, что одинаковых (расположенных один на другом) контуров (прямоугольников) нет)))
     * (не тестила)
     */
    private static ArrayList<Integer> internalObjects(Rect[] objects) {
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < objects.length; i++) {
            Rect cnt = objects[i];
            for (int j = 0; j < objects.length; j++) {
                if (i != j) {
                    // если один прямоугольник содержит в себе другой
                    if (cnt.x >= objects[j].x && cnt.x + cnt.width <= objects[j].x + objects[j].width &&
                        cnt.y >= objects[j].y && cnt.y + cnt.height <= objects[j].y + objects[j].height) {
                        indexes.add(j);
                    }

                    // если пересекаются (два случая)
                    int a = cnt.width * cnt.height;
                    int b = objects[j].width * objects[j].height;
                    int c = -1;  // площадь пересечения

                    if (cnt.x >= objects[j].x && cnt.x + cnt.width >= objects[j].x + objects[j].width &&
                        cnt.y <= objects[j].y && cnt.y + cnt.height <= objects[j].y + objects[j].height) {
                        c = (objects[j].y + objects[j].height - cnt.y) * (cnt.x + cnt.width - objects[j].x);
                    }

                    if (cnt.x >= objects[j].x && cnt.x + cnt.width >= objects[j].x + objects[j].width &&
                        cnt.y >= objects[j].y && cnt.y + cnt.height <= objects[j].y + objects[j].height) {
                        c = (objects[j].height) * (cnt.x + cnt.width - objects[j].x);
                    }

                    if (c > 0) {
                        // если пересекаются более чем на 0.8
                        if (c / a > 0.8 || c / b > 0.8) {
                            indexes.add(j);
                        }
                    }
                }
            }
        }
        return indexes;
    }

    /**
     * функция для выделения из исходного кадра изображения с областью с номером и оставляет от них ТОЛЬКО номер
     * @param image - полный кадр
     * @param rects
     * @return массив изображений с номером
     */
    public static ArrayList<Mat> getRotatedPlates(Mat image, Rect[] rects) {
        ArrayList<Mat> symbolsMat = new ArrayList<>();
        for (Rect rect : rects) {
            symbolsMat.add(image.submat(rect));
        }
        symbolsMat.forEach(Detection::rotateRectROI);
        return symbolsMat;

    }

    /**
     * функция для выделения из roi самого номера и поворота его в горизонтальное положение
     * @param roi - изображение с областью с авт. номером
     */
    private static void rotateRectROI(Mat roi) {
        MatOfPoint2f cntr = new MatOfPoint2f(getMaxAreaContour(roi, 0).toArray());
        RotatedRect rect = Imgproc.minAreaRect(cntr);
        Mat rotatedROI = new Mat();

        Double angle = rect.angle;
        Size size = rect.size;
        if (rect.angle < -45) {
            angle += 90;
            Double help = rect.size.height;
            rect.size.height = rect.size.width;
            rect.size.width = help;
        }
        Mat rotation = Imgproc.getRotationMatrix2D(rect.center, angle, 1.0);
        Imgproc.warpAffine(roi, rotatedROI, rotation, roi.size(), Imgproc.INTER_CUBIC);

        Imgproc.getRectSubPix(rotatedROI, size, rect.center, roi);
    }

    /**
     * функция для нахождения наибольшего контура на roi (границы номера)
     * @param roi - изображение с областью с авт. номером
     * @param mode - надо потестить, какой вариант лучше всего справляется, и оставить его (case был чисто для тестирования)
     * @return - (по-хорошему) границу номера
     */
    private static MatOfPoint getMaxAreaContour(Mat roi, int mode) {
        Mat blur = new Mat();
        Mat thresh = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        ArrayList<Double> areas = new ArrayList<>();

        Imgproc.bilateralFilter(roi, blur, 1, 55, 75);
        Imgproc.Canny(blur, thresh, 80, 200, 3, false);
        Imgproc.findContours(thresh, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint c : contours) {
            switch (mode) {
                case 0:
                    // наибольший контур по длине контура
                    areas.add(Imgproc.arcLength(new MatOfPoint2f(c.toArray()), false));
                    break;
                case 1:
                    // наибольший контур по площади контура
                    areas.add(Imgproc.contourArea(c));
                    break;
                case 2:
                    // наибольший контур по площади minAreaRect (моя функция)
                    RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(c.toArray()));
                    areas.add(myArea(rotatedRect));
                    break;
            }
        }
        Double max = 0.0;
        int index = 0;
        for (int i = 0; i < areas.size(); i++) {
            if (areas.get(i) > max) {
                max = areas.get(i);
                index = i;
            }
        }
        return contours.get(index);
    }

    /**
     * функция для нахождения площади rect
     * @param rect - повернутый прямоугольник
     * @return - значение площади :)))
     */
    private static Double myArea(RotatedRect rect) {
        Double angleDegr = rect.angle;
        if (rect.angle < -45) {
            angleDegr += 90;
        }
        double angleRad = angleDegr * Math.PI / 180.0;
        Point[] pointsPoint = new Point[4];
        rect.points(pointsPoint); // + sort
        Sort.sortPointsByY(pointsPoint);
        for (int i = 1; i < pointsPoint.length; i++) {
            pointsPoint[i].x -= pointsPoint[0].x;
            pointsPoint[i].y -= pointsPoint[1].y;
        }
        pointsPoint[0].x = 0;
        pointsPoint[0].y = 0;

        INDArray rotationMatrix = Nd4j.create(new double[]{Math.cos(angleRad), -Math.sin(angleRad),
                Math.sin(angleRad), Math.cos(angleRad)}, new int[]{2, 2});

        INDArray[] pointsINDArr =  new INDArray[4];
        for (int i = 0; i < 4; i++) {
            INDArray pt = Nd4j.create(new double[]{pointsPoint[i].x, pointsPoint[i].y}, new int[]{1, 2});
            pointsINDArr[i] = pt.mmul(rotationMatrix);
        }

        double height = 0;
        double width = 0;
        for (INDArray pt : pointsINDArr) {
            if (Math.abs(pt.getDouble(0, 0)) > width) {
                width = pt.getDouble(0, 0);
            }
            if (Math.abs(pt.getDouble(0, 1)) > height) {
                height = pt.getDouble(0, 1);
            }
        }


        return width * height;
    }

    /**
     * функция для выделения на номере отдельных символов (букв и цифр)
     * @param plate - изображение с номером
     * @return - массив изображений с символами одного номера
     */
    public static Mat[] getSymbols(Mat plate) {
        Mat thresh = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        ArrayList<Rect> rectSymbols = new ArrayList<>();
        ArrayList<Mat> matSymbols = new ArrayList<>();
        int e = 1; // небольшой отступ от границ символа

        Imgproc.cvtColor(plate, plate, Imgproc.COLOR_BGR2GRAY);
        // Imgproc.adaptiveThreshold(plate, thresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 49, 3);
        Imgproc.adaptiveThreshold(plate, thresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 13, 2);   // or 11, 2
        Imgproc.findContours(thresh, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint c: contours) {
            Rect myRect = Imgproc.boundingRect(c);
            if (myRect.width > 15 && myRect.width < 35 && myRect.height > 15 && myRect.height < 45) {
                myRect.x -= e;
                myRect.y -= e;
                myRect.height += 2 * e;
                myRect.width += 2 * e;

                rectSymbols.add(myRect);
            }
        }

        Rect[] rects = new Rect[rectSymbols.size()];  // для сортировки
        rects = rectSymbols.toArray(rects);
        Sort.sortRectsByX(rects);
        for (Rect rect : rects) {
            matSymbols.add(thresh.submat(rect));
        }

        Mat dilateMatrix = Mat.ones(3, 3, CvType.CV_8U);

        for (Mat symbol : matSymbols) {
            Imgproc.resize(symbol, symbol, new Size(28, 28),0, 0, Imgproc.INTER_LINEAR);
            Imgproc.dilate(symbol, symbol, dilateMatrix);
        }

        Mat[] mats = new Mat[matSymbols.size()];
        mats = matSymbols.toArray(mats);

        return mats;

    }

    /**
     * функция для получения ответа нейронных сетей
     * @param symbols - массив изображений символов (буква или цифра) одного номера
     * @param networkNum - нейронная сеть для распознавания цифр
     * @param networkLett - нейронная сеть для распознавания букв
     * @return - массив распознанных букв и цифр одного номера
     */
    public static ArrayList<String> getResults(Mat[] symbols, MultiLayerNetwork networkNum, MultiLayerNetwork networkLett) {
        ArrayList<String> lettersArrLst = new ArrayList<>();
        INDArray allSymbols = Nd4j.zeros(28, 28).reshape(1, 1, 28, 28);

        for (Mat symbolMat : symbols) {
            INDArray symbolINDArr = Nd4j.zeros(28, 28);
            for (int j = 0; j < 28; j++) {
                for (int k = 0; k < 28; k++) {
                    symbolINDArr.putScalar(new int[]{j, k}, symbolMat.get(j, k)[0]);
                }
            }
            symbolINDArr = symbolINDArr.reshape(1, 1, 28, 28);

            allSymbols = Nd4j.concat(0, allSymbols, symbolINDArr);
        }
        if (allSymbols.shape()[0] < 9) {
            return lettersArrLst;
        }

        INDArray numbersINDArr = Nd4j.concat(0, allSymbols.get(NDArrayIndex.interval(2, 5)), allSymbols.get(NDArrayIndex.interval(7, allSymbols.shape()[0])));
        INDArray lettersINDArr = Nd4j.concat(0, allSymbols.get(NDArrayIndex.interval(1, 2)), allSymbols.get(NDArrayIndex.interval(5, 7)));


        INDArray outputNum = networkNum.output(numbersINDArr).argMax(1);
        INDArray outputLett = networkLett.output(lettersINDArr).argMax(1);

        ArrayList<Integer> numbersArrLst = new ArrayList<>();
        for (int i = 0; i < outputNum.shape()[0]; i++) {
            for (int j = 0; j < outputNum.shape()[1]; j++) {
                numbersArrLst.add((int)outputNum.getDouble(i, j));
            }
        }

        for (int i = 0; i < outputLett.shape()[0]; i++) {
            for (int j = 0; j < outputLett.shape()[1]; j++) {
                lettersArrLst.add(alphabet[(int)outputLett.getDouble(i, j)]);
            }
        }

        for (int i = 0; i < numbersArrLst.size(); i++) {
            if (i < 3) {
                lettersArrLst.add(i + 1, String.valueOf(numbersArrLst.get(i)));
            } else {
                lettersArrLst.add(String.valueOf(numbersArrLst.get(i)));
            }
        }

        return lettersArrLst;
    }

}

