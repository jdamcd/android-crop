> I guess people are just cropping out all the sadness

This is another Android library project providing a simple image cropping `Activity`, based on code from AOSP.

## Goals

I started this in my [hacker time](http://backstage.soundcloud.com/2011/12/stop-hacker-time/) with the intention of replacing our dated [android-cropimage fork](https://github.com/soundcloud/android-cropimage).

* Gradle build with AAR & ApkLib artifacts
* Modern UI
* Backwards compatible to Gingerbread
* Simple builder for configuration
* Example project
* More tests, less unused complexity

## Usage

First, declare `CropImageActivity` in your manifest file:

`<activity android:name="com.soundcloud.android.crop.CropImageActivity" />`

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

Apache License, version 2.  
Most of the cropping code originates from AOSP.  
Thanks to android-cropimage contributors.
