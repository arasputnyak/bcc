from keras.preprocessing.image import ImageDataGenerator, array_to_img, img_to_array, load_img

datagen = ImageDataGenerator(
        samplewise_center=True,
        # featurewise_center=True,
        samplewise_std_normalization=True,
        rotation_range=15,
        width_shift_range=0.1,
        height_shift_range=0.05,
        shear_range=0.8,
        zoom_range=0.1,
        horizontal_flip=False,
        vertical_flip=False,
        fill_mode='nearest')

img = load_img('/media/anastasia/08147719147708C8/Data/Prog/Python/mnist_project/numbers_dataset/train/5/5.jpg')  # this is a PIL image
x = img_to_array(img)  # array (3, 150, 150)
x = x.reshape((1,) + x.shape)  # array (1, 3, 150, 150)

# generates batches of randomly transformed images and saves the results to 'save_to_dir' directory
i = 0
for batch in datagen.flow(x, batch_size=1,
                          save_to_dir='numbers_dataset/train/5', save_prefix='5', save_format='jpg'):
    i += 1
    if i > 50:
        break