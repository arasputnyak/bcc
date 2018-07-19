import cv2
import numpy as np
from keras.models import load_model
import PIL
from PIL import Image
import pytesseract
import time


def get_ind(image):
    blur = cv2.bilateralFilter(image, 1, 55, 75)
    thresh_0 = cv2.Canny(blur, 80, 200, apertureSize=3)
    # thresh_0 = cv2.adaptiveThreshold(image, 255, cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY, 23, 1)
    im_0, contours_0, hierarchy_0 = cv2.findContours(thresh_0, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    c = 0
    rect_0 = cv2.minAreaRect(contours_0[c])
    a = area(rect_0)
    for j in range(1, len(contours_0)):
        rect_1 = cv2.minAreaRect(contours_0[j])
        if a < area(rect_1):
            a = area(rect_1)
            c = j
    contour = contours_0[c]
    return contour


def get_ind_fixed(image):
    # blur = cv2.bilateralFilter(image, 1, 55, 75)
    thresh_0 = cv2.Canny(image, 80, 200, apertureSize=3)
    im_0, contours_0, hierarchy_0 = cv2.findContours(thresh_0, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    areas = []
    for c in contours_0:
        # areas.append(cv2.contourArea(c))
        areas.append(cv2.arcLength(c, False))
    ind = areas.index(max(areas))
    cnt = contours_0[ind]
    return cnt


def _2d_array_sort(array, ind):
    for j in range(len(array)):
        f = j
        while f > 0 and array[f][ind] < array[f - 1][ind]:
            array[f], array[f - 1] = array[f - 1], array[f].copy()
            f -= 1


def area(rect):
    ang = rect[2]
    if rect[2] < -45:
        ang = 90 + ang
    rotation_matrix = np.array([[np.cos(np.deg2rad(ang)), -np.sin(np.deg2rad(ang))],
                                [np.sin(np.deg2rad(ang)), np.cos(np.deg2rad(ang))]])
    box_points = cv2.boxPoints(rect)
    # находим самую нижнюю точку, вокруг которой будем
    # совершать поворот точек прямоугольника
    # (первый элемент отсортированного по y массива)
    box_points = box_points.tolist()
    _2d_array_sort(box_points, 1)
    np.asarray(box_points)
    # переносим начало координат в нижний угол прямогольника
    for j in range(1, len(box_points)):
        box_points[j][0] -= box_points[0][0]
        box_points[j][1] -= box_points[0][1]
    box_points[0] = np.asarray([0, 0])
    #  сорешаем поворот (умножаем на матрицу поворота)
    for j in range(1, len(box_points)):
        box_points[j] = np.dot(box_points[j], rotation_matrix)
    width_0 = 0
    height_0 = 0
    for j in range(1, len(box_points)):
        if abs(box_points[j][0]) > 1:
            width_0 = abs(box_points[j][0])
        if abs(box_points[j][1]) > 1:
            height_0 = abs(box_points[j][1])
    return width_0 * height_0


def del_excess_cntr(image):
    im_0, contours_0, hierarchy_0 = cv2.findContours(image, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    cnt = get_ind(image)
    for c in contours_0:
        if not cmpr_cntr(cnt, c):
            x_0, y_0, w_0, h_0 = cv2.boundingRect(c)
            for j in reversed(range(y_0, y_0 + h_0)):
                for jj in range(x_0, x_0 + w_0):
                    image[j][jj] = 0


def cmpr_cntr(cnt1, cnt2):
    x_0, y_0, w_0, h_0 = cv2.boundingRect(cnt1)
    x_1, y_1, w_1, h_1 = cv2.boundingRect(cnt2)
    if w_0 * h_0 - w_1 * h_1 < 50:
        return True
    else:
        return False


def internal_cntr(array):
    if type(array) == list:
        end = len(array)
    else:
        end = array.shape[0]
    indexes = []  # массив индексов внутренних контуров
    for j in range(end):
        q = array[j]
        for l in range(len(array)):
            if array[l][0] > q[0] and array[l][0] + array[l][2] < q[0] + q[2]:
                indexes.append(l)
    return indexes


# индексы маленьких контуров
def small_cntr(array):
    indexes = []
    end = array.shape[0]
    for j in range(end):
        if (array[j][2] < 120 or array[j][2] > 140) and (array[j][3] < 35 or array[j][3] > 55):
            indexes.append(j)
    return indexes


# main
# model_numbers = load_model('model1')   # нейронная сеть на цифры
model_numbers = load_model('model2')   # нейронная сеть на цифры (5)
model_letters = load_model('model1')   # нейронная сеть на буквы (9)
kernel = np.ones((3, 3), np.uint8)   # ядро дилатации/эрозии
plate_cascade = cv2.CascadeClassifier('haarcascade_russian_plate_number.xml')   # каскада Хаара на автономера
plate_numbers = []   # список авт. номеров (номер = изображение)
plate_numbers2 = []   # список авт. номеров (номер = массив символов) = lst of lst_of_symbols
alphabet = ['A', 'B', 'C', 'E', 'H', 'K', 'M', 'O', 'P', 'T', 'X', 'Y']


image_bgr = cv2.imread('test_pics/test9_unrec.jpg')   # загружаем исходное изображение
image_bw = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)   # ч/б

# находим авт. номера
numbers = plate_cascade.detectMultiScale(image_bw, 1.3, 10)
numbers = np.asarray(numbers)

_2d_array_sort(numbers, 0)

numbers = np.delete(numbers, internal_cntr(numbers), 0)
print(numbers[0][2])
numbers = np.delete(numbers, small_cntr(numbers), 0)

# заполняем plate_numbers (изображениями с номерами)
for (x, y, w, h) in numbers:
    plate_numbers.append(image_bw[y:y + h, x:x + w])
    # выделение номера на исходном изображении
    # cv2.rectangle(image_bgr, (x, y), (x + w, y + h), (255, 0, 255), 2)

# идем по всем найденным областям с номерами
for k in range(len(plate_numbers)):
    coords_of_symbols = []  # координаты контуров
    lst_of_symbols = []  # список ROI (т.е. символов)
    lst_of_numbers = []  # цифры
    lst_of_letters = []  # буквы
    answer_numb = []   # integer
    answer_lett = []   # string

    # обрезаем ненужную часть изображения
    rectangle = cv2.minAreaRect(get_ind(plate_numbers[k]))
    angle = rectangle[2]
    height, width = plate_numbers[k].shape
    if rectangle[2] < -45:
        angle += 90
    matrix = cv2.getRotationMatrix2D(rectangle[0], angle, 1.0)
    plate_numbers[k] = cv2.warpAffine(plate_numbers[k], matrix, (width, height))
    x, y, w, h = cv2.boundingRect(get_ind(plate_numbers[k]))
    plate_numbers[k] = plate_numbers[k][y:y + h, x:x + w]  # только номер

    plate_numbers[k] = cv2.resize(plate_numbers[k], (250, 50), interpolation=cv2.INTER_LINEAR)  # номер 250*50

    # находим все контуры
    thresh = cv2.adaptiveThreshold(plate_numbers[k], 255, cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY, 49, 1)
    im, contours, hierarchy = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    thresh2 = cv2.cvtColor(thresh, cv2.COLOR_GRAY2BGR)
    cv2.drawContours(thresh2, contours, -1, (0, 255, 0), 1)

    # оставляем только буквы и цифры (их КООРДИНАТЫ)
    sin = 2  # поГРЕШность )))
    for i in range(len(contours)):
        x, y, w, h = cv2.boundingRect(contours[i])
        # 10-30, 15-40
        if 10 < w < 35 and 15 < h < 40:
            coords_of_symbols.append([x - sin, y - sin, w + 2 * sin, h + 2 * sin])

    # сортируем массив координат контуров по х-овой (т.е. в порядке слева направо)
    _2d_array_sort(coords_of_symbols, 0)

    # удаляем лишние контуры (внутри символов)
    for i in reversed(internal_cntr(coords_of_symbols)):
        del coords_of_symbols[i]

    # инвертируем цвета для нейронной сети на цифры (для старой модели на мнист)
    # cv2.bitwise_not(thresh, thresh)

    # получаем список изображений с символами
    for i in range(len(coords_of_symbols)):
        lst_of_symbols.append(thresh[coords_of_symbols[i][1]:coords_of_symbols[i][1] + coords_of_symbols[i][3],
                              coords_of_symbols[i][0]:coords_of_symbols[i][0] + coords_of_symbols[i][2]])

    # приводим к общему виду для подачи нейросетям
    for i in range(len(lst_of_symbols)):
        lst_of_symbols[i] = cv2.resize(lst_of_symbols[i], (28, 28), interpolation=cv2.INTER_LINEAR)

    pillow_img = PIL.Image.fromarray(thresh)
    text = pytesseract.image_to_string(pillow_img)
    print('tesseract = ' + text)

    if len(lst_of_symbols) < 8:
        txt = 'unrecognized'
        print(txt)
        cv2.putText(image_bgr, txt, (numbers[k][0], numbers[k][1] - 10),
                    cv2.FONT_HERSHEY_DUPLEX, 0.7, (255, 0, 255), 1)
    else:
        for i in range(len(lst_of_symbols)):
            if 0 < i < 4 or i > 5:
                lst_of_numbers.append(lst_of_symbols[i])
            else:
                lst_of_letters.append(lst_of_symbols[i])

        for i in range(len(lst_of_numbers)):
            lst_of_numbers[i] = cv2.erode(lst_of_numbers[i], kernel, iterations=1)

        # for s in lst_of_letters: cv2.bitwise_not(s, s)

        lst_of_numbers = np.asarray(lst_of_numbers)
        lst_of_numbers = lst_of_numbers.reshape(lst_of_numbers.shape[0], 1, 28, 28).astype('float32')

        lst_of_letters = np.asarray(lst_of_letters)
        lst_of_letters = lst_of_letters.reshape(lst_of_letters.shape[0], 1, 28, 28).astype('float32')

        start_time = time.time()
        probability_numb = model_numbers.predict_proba(lst_of_numbers)
        probability_lett = model_letters.predict_proba(lst_of_letters)
        print("--- %s seconds ---" % (time.time() - start_time))

        for i in range(len(probability_numb)):
            answer_numb.append(np.argmax(probability_numb[i], axis=0))

        for i in range(len(probability_lett)):
            answer_lett.append(np.argmax(probability_lett[i], axis=0))

        for i in range(len(answer_lett)):
            answer_lett[i] = alphabet[answer_lett[i]]

        for i in range(len(answer_numb)):
            if i < 3:
                answer_lett.insert(i + 1, answer_numb[i])
            else:
                answer_lett.append(answer_numb[i])

        print('neural net = ')
        # print(answer_lett)
        txt = ''.join(str(e) for e in answer_lett)
        print(txt)
        cv2.putText(image_bgr, txt, (numbers[k][0], numbers[k][1] - 10),
                    cv2.FONT_HERSHEY_DUPLEX, 0.7, (255, 0, 255), 1)
        cv2.rectangle(image_bgr, (numbers[k][0], numbers[k][1]),
                      (numbers[k][0] + numbers[k][2], numbers[k][1] + numbers[k][3]),
                      (255, 0, 255), 2)

        del coords_of_symbols
        del lst_of_symbols
        del lst_of_numbers
        del lst_of_letters
        del answer_numb
        del answer_lett

# cv2.imshow('2', thresh2)
cv2.imshow('result', image_bgr)
# print("--- %s seconds ---" % (time.time() - start_time))
# cv2.imshow('1', thresh2)
cv2.waitKey(0)
cv2.destroyAllWindows()
