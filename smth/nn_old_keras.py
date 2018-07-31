from keras.preprocessing.image import ImageDataGenerator, load_img, img_to_array
import numpy
# from keras.models import Sequential
# from keras.layers import Dense
# from keras.layers import Dropout
# from keras.layers import Flatten
# from keras.layers.convolutional import Convolution2D
# from keras.layers.convolutional import MaxPooling2D
from keras import backend as K
K.set_image_dim_ordering('th')

from keras.layers import Convolution2D, MaxPooling2D,Dropout
from keras.layers.core import Dense, Activation, Flatten
from keras.models import Sequential
from keras.optimizers import SGD

seed = 7
numpy.random.seed(seed)
batch = 25

datagen = ImageDataGenerator(rescale=1./255)

train_gen = datagen.flow_from_directory("num_dat\\train\\",
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')
valid_gen = datagen.flow_from_directory("num_dat\\validation\\",
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')
test_gen = datagen.flow_from_directory("num_dat\\test\\",
                                        target_size=(28, 28),
                                        color_mode='grayscale',
                                        batch_size=batch,
                                        class_mode='categorical')

model = Sequential()
# model.add(Conv2D(30, (5, 5), input_shape=(1, 28, 28), activation='relu'))
model.add(Convolution2D(30, 5, 5, input_shape=(1, 28, 28)))
model.add(Activation("relu"))
model.add(MaxPooling2D(pool_size=(2, 2)))
# model.add(Conv2D(15, (3, 3), activation='relu'))
model.add(Convolution2D(15, 3, 3))
model.add(Activation("relu"))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Dropout(0.3))
model.add(Flatten())
# model.add(Dense(128, activation='relu'))
# model.add(Dense(50, activation='relu'))
# model.add(Dense(10, activation='softmax'))
model.add(Dense(output_dim=128))
model.add(Activation("relu"))
model.add(Dense(output_dim=50))
model.add(Activation("relu"))
model.add(Dense(output_dim=10))
model.add(Activation("softmax"))

# model.compile(loss='categorical_crossentropy', optimizer=SGD(lr=0.01, momentum=0.9, nesterov=True),metrics=["accuracy"])
model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])

# model.fit_generator(train_gen, steps_per_epoch=4500 // batch, validation_data=valid_gen, validation_steps=1900 // batch, epochs=30, verbose=2)
model.fit_generator(train_gen, samples_per_epoch=6500 // batch, nb_epoch=30, verbose=2, validation_data=valid_gen, nb_val_samples=2900 // batch)

# scores = model.evaluate_generator(test_gen, steps=2000 // 30)
scores = model.evaluate_generator(test_gen, val_samples=2300 // 30)

print("Acc on test data: %.2f%%" % (scores[1]*100))
model.save('model_num2')