from keras.preprocessing.image import ImageDataGenerator, load_img, img_to_array
import numpy
from keras.models import Sequential
from keras.layers import Dense
from keras.layers import Dropout
from keras.layers import Flatten
from keras.layers.convolutional import Conv2D
from keras.layers.convolutional import MaxPooling2D
from keras import backend as K
K.set_image_dim_ordering('th')

seed = 7
numpy.random.seed(seed)

datagen = ImageDataGenerator(rescale=1./255)
batch = 20

train_gen = datagen.flow_from_directory('letters_data_auto/train',
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')
valid_gen = datagen.flow_from_directory('letters_data_auto/validation',
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')
test_gen = datagen.flow_from_directory('letters_data_auto/test',
                                        target_size=(28, 28),
                                       color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')

model = Sequential()
model.add(Conv2D(30, (5, 5), input_shape=(1, 28, 28), activation='relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Conv2D(15, (3, 3), activation='relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Dropout(0.2))
model.add(Flatten())
model.add(Dense(128, activation='relu'))
model.add(Dense(50, activation='relu'))
model.add(Dense(12, activation='softmax'))

model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])

model.fit_generator(train_gen, steps_per_epoch=3500 // batch, validation_data=valid_gen, validation_steps=1200 // batch, epochs=30, verbose=2)

scores = model.evaluate_generator(test_gen, steps=2000 // 30)
print("Точность работы на тестовых данных: %.2f%%" % (scores[1]*100))
model.save('model1')