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
batch = 20

datagen = ImageDataGenerator(rescale=1./255)

train_gen = datagen.flow_from_directory('numbers_dataset/train',
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')
valid_gen = datagen.flow_from_directory('numbers_dataset/validation',
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')
test_gen = datagen.flow_from_directory('numbers_dataset/test',
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')

model = Sequential()
model.add(Conv2D(30, (5, 5), input_shape=(1, 28, 28), activation='relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Conv2D(15, (3, 3), activation='relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Dropout(0.3))
model.add(Flatten())
model.add(Dense(128, activation='relu'))
model.add(Dense(50, activation='relu'))
model.add(Dense(10, activation='softmax'))

model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])

model.fit_generator(train_gen, steps_per_epoch=4500 // batch, validation_data=valid_gen, validation_steps=1900 // batch, epochs=30, verbose=2)

scores = model.evaluate_generator(test_gen, steps=2000 // 30)
print("Acc on test data: %.2f%%" % (scores[1]*100))
model.save('model2')
