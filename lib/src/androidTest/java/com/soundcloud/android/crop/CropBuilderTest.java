package com.soundcloud.android.crop;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.fest.assertions.api.ANDROID;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

public class CropBuilderTest extends BaseTestCase {

    private Activity activity;
    private Crop builder;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        activity = mock(Activity.class);
        when(activity.getPackageName()).thenReturn("com.example");

        builder = Crop.of(Uri.parse("image:input"), Uri.parse("image:output"));
    }

    public void testInputUriSetAsData() {
        ANDROID.assertThat(builder.getIntent(activity)).hasData("image:input");
    }

    public void testOutputUriSetAsExtra() {
        Intent intent = builder.getIntent(activity);
        Uri output = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);

        assertThat(output.toString()).isEqualTo("image:output");
    }

    public void testAspectRatioSetAsExtras() {
        builder.withAspect(16, 10);

        Intent intent = builder.getIntent(activity);

        assertThat(intent.getIntExtra("aspect_x", 0)).isEqualTo(16);
        assertThat(intent.getIntExtra("aspect_y", 0)).isEqualTo(10);
    }

    public void testFixedAspectRatioSetAsExtras() {
        builder.asSquare();

        Intent intent = builder.getIntent(activity);

        assertThat(intent.getIntExtra("aspect_x", 0)).isEqualTo(1);
        assertThat(intent.getIntExtra("aspect_y", 0)).isEqualTo(1);
    }

    public void testMaxSizeSetAsExtras() {
        builder.withMaxSize(400, 300);

        Intent intent = builder.getIntent(activity);

        assertThat(intent.getIntExtra("max_x", 0)).isEqualTo(400);
        assertThat(intent.getIntExtra("max_y", 0)).isEqualTo(300);
    }

    public void testBuildsIntentWithMultipleOptions() {
        builder.asSquare().withMaxSize(200, 200);

        Intent intent = builder.getIntent(activity);

        assertThat(intent.getIntExtra("aspect_x", 0)).isEqualTo(1);
        assertThat(intent.getIntExtra("aspect_y", 0)).isEqualTo(1);
        assertThat(intent.getIntExtra("max_x", 0)).isEqualTo(200);
        assertThat(intent.getIntExtra("max_y", 0)).isEqualTo(200);
    }

    public void testAsPngSetAsExtras() {
        builder.asPng(true);

        Intent intent = builder.getIntent(activity);

        assertThat(intent.getBooleanExtra("as_png", false)).isEqualTo(true);
    }
}
