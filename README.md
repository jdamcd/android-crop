> I guess people are just cropping out all the sadness

An Android library project to provide a simple image cropping `Activity`, based on code from AOSP.

[![Build Status](https://travis-ci.org/jdamcd/android-crop.png)](https://travis-ci.org/jdamcd/android-crop)

## Goals

* Gradle build with AAR
* Modern UI
* Backwards compatible to SDK 14
* Simple builder for configuration
* Example project
* More tests, less unused complexity

## Usage

First, declare `CropImageActivity` in your manifest file:

`<activity android:name="com.soundcloud.android.crop.CropImageActivity" />`

#### Crop

`Crop.of(inputUri, outputUri).asSquare().start(activity)`

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

#### Dependency

The AAR is published on Maven Central:

`compile 'com.soundcloud.android:android-crop:0.9.10@aar'`

#### Apps

Apps that use this library include: [SoundCloud](https://play.google.com/store/apps/details?id=com.soundcloud.android), [Depop](https://play.google.com/store/apps/details?id=com.depop)

## How does it look?

![android-crop screenshot](screenshot.png)

## License

This project is based on the [AOSP](https://source.android.com) camera image cropper via [android-cropimage](https://github.com/lvillani/android-cropimage).

Copyright 2014 [SoundCloud](https://soundcloud.com)  
Apache License, Version 2.0 
