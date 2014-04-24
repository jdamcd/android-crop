> I guess people are just cropping out all the sadness

This is another Android library project providing a simple image cropping `Activity`, based on code from AOSP.

[![Build Status](https://travis-ci.org/jdamcd/android-crop.png)](https://travis-ci.org/jdamcd/android-crop)

## Goals

I put this together in my [hacker time](http://backstage.soundcloud.com/2011/12/stop-hacker-time/) as a replacement for our old [android-cropimage fork](https://github.com/soundcloud/android-cropimage).

* Gradle build with AAR
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

Listen for the result of the crop (see example project if you want to do some error handling):

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            doSomethingWithCroppedImage(outputUri);
        }
    }

#### Pick

The library provides a utility method to start an image picker:

`Crop.pickImage(activity)`

## How does it look?

![android-crop screenshot](screenshot.png)

## License

This project is based on the [AOSP](https://source.android.com) camera image cropper via [android-cropimage](https://github.com/lvillani/android-cropimage).

Copyright 2014 [SoundCloud](https://soundcloud.com)  
Apache License, Version 2.0 
