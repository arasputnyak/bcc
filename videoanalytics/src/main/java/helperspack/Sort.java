package helperspack;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class Sort {

    public static void sortRectsByX(Rect[] rects) {
        for (int i = 0; i < rects.length; i++) {
            int f = i;
            while (f > 0 && rects[f].x < rects[f - 1].x) {
                Rect rect = rects[f];
                rects[f] = rects[f - 1];
                rects[f - 1] = rect;
                f -= 1;
            }
        }
    }

    public static void sortPointsByY(Point[] points) {
        for (int i = 0; i < points.length; i++) {
            int f = i;
            while (f > 0 && points[f].y < points[f - 1].y) {
                Point point = points[f];
                points[f] = points[f - 1];
                points[f - 1] = point;
                f -= 1;
            }
        }
    }
}