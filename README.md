> I guess people are just cropping out all the sadness

This is yet another Android library project providing a simple image cropping `Activity`. It's based on the same AOSP code as the others.

## Goals

* Gradle build
* AAR & ApkLib artifacts 
* Backwards compatible to Gingerbread
* Example project
* Add tests and strip out unused complexity
* Better programming interface
* Modern user interface
* Move error handling to result code
* Fix bugs!
* Keep improvements from SoundCloud fork of android-cropimage

## Usage

#### Crop

`new Crop(inputUri).output(outputUri).asSquare().start(activity)`

Listen for the result of the crop:

    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK && requestCode == Crop.REQUEST_CROP) {
            if (Crop.isError(result)) {
            	// Do some error handling with Crop.getError(result)
        	} else {
            	// Do something with your cropped image!
        	}
        }
    }

#### Pick

The library provides a utility method to start an image picker:

`Crop.pickImage(activity)`

## How does it look?

![android-crop screenshot](screenshot.png)

## License & Credits

Apache License, version 2. Most of the cropping code is from AOSP. Thanks to android-cropimage contributors.
