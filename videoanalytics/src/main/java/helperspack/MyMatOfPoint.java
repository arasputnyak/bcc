package helperspack;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Moments;

public class MyMatOfPoint {
    private MatOfPoint matOfPoint;
    private double contourArea = 0;
    private Point center; // отвечает за положение в кадре
    private int time = 0;
    private boolean f2 = false; // чтобы обновить фон только один раз (в то время как предмет обводиться будет каждый кадр в теч. 3-х секунд)

    public MyMatOfPoint(MatOfPoint matOfPoint) {
        this.matOfPoint = matOfPoint;
    }

    public MatOfPoint getMatOfPoint() {
        return matOfPoint;
    }

    public void setMatOfPoint(MatOfPoint matOfPoint) {
        this.matOfPoint = matOfPoint;
    }

    public double getContourArea() {
        return contourArea;
    }

    public void setContourArea(double contourArea) {
        this.contourArea = contourArea;
    }

    public void setCenter(Moments m) {
        double x = m.get_m10() / m.get_m00();
        double y = m.get_m01() / m.get_m00();
        this.center = new Point(x, y);
    }

    public Point getCenter() {
        return center;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public boolean isF2() {
        return f2;
    }

    public void setF2(boolean f2) {
        this.f2 = f2;
    }
}
