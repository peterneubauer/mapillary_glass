package com.mapillary.glass;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.google.android.glass.media.Sounds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.view.View.INVISIBLE;


public class MainActiviy extends Activity implements SurfaceHolder.Callback {


    private static final String TAG = MainActiviy.class.getName();
    private static final int SPEECH_REQUEST = 0;

    private View spinner;
    private static final String startSequence = "start";
    private AudioManager mAudioManager;

    private TextView mTextView;
    private UUID sequenceUUID;
    private Thread sequenceThread;
    private boolean shotInProgress;
    private boolean autoSequence;
    private int nrPhotos = 0;
    private Camera mCamera;
    private SurfaceView cameraSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, startSequence);
//        startActivityForResult(intent, SPEECH_REQUEST);
        setContentView(R.layout.main_activiy);
        spinner = findViewById(R.id.spinner);
        spinner.setVisibility(INVISIBLE);
        cameraSurface = (SurfaceView)findViewById(R.id.surfaceView);
        mTextView = (TextView) findViewById(R.id.textView);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mCamera = Camera.open();
        mCamera.lock();
        try {
            mCamera.setPreviewDisplay(cameraSurface.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        Log.d(TAG, String.format("camera %s", mCamera));

    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, String.format("Key pressed %s", event));
        if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // Stop the preview and release the camera.
            // Execute your logic as quickly as possible
            // so the capture happens quickly.
            startSequence();
            mCamera.startPreview();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }


    private void takePicture() {
        Log.d(TAG, String.format("takePicture2"));
        mCamera.takePicture(null, null
                , new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Log.d(TAG, String.format("onPictureTaken"));
                        Date now = new Date();
                        //aproximate to half between callback and requested
                        String pictureCaptureDate = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS").format(now.getTime());
                        String photoFile = pictureCaptureDate + ".jpg";

                        File pictureFile = new File(getFilesDir().getAbsolutePath(), photoFile);
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.flush();
                            fos.close();
                            Log.d(TAG, "Saved to: " + pictureFile.getAbsolutePath());
                        } catch (Exception error) {
                            error.printStackTrace();
                            Log.d(TAG, "File" + pictureFile.getAbsolutePath() + " not saved: "
                                    + error.getMessage());
                            Toast.makeText(getBaseContext(), "Image could not be saved.",
                                    Toast.LENGTH_LONG).show();
                        }


                        nrPhotos++;
                        mTextView.setText(nrPhotos + " pictures");
                        shotInProgress = false;
                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        mAudioManager.playSoundEffect(Sounds.SUCCESS);
        Log.d(TAG, String.format("onActivityResult %s", data));

        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            //Get results of speech to text
            String memoResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            if (memoResult.equals(startSequence)) {
                startSequence();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private void startSequence() {
        Log.d(TAG, "startSequence");
//        stopAutoSequence();
        nrPhotos = 0;
        autoSequence = true;
        sequenceUUID = UUID.randomUUID();
        autoTakeShot();
    }

    public void stopAutoSequence() {

        if (sequenceThread != null) {
            sequenceThread.interrupt();
            sequenceThread = null;
            shotInProgress = false;
        }

    }

    public void autoTakeShot() {

        Runnable sequenceRunnable = new Runnable() {

            @Override
            public void run() {
                Looper.prepare();
                flash("starting sequence");
//                while (autoSequence) {
                    Log.d(TAG, "Auto-fired shot" + nrPhotos);
//                    flash("photos: " + nrPhotos);
                    if(!shotInProgress) {
                        takePicture();
                    } else {
                        Log.d(TAG, "shot in progress ...");
                    }
                    try {
                        //wait 2s for the next shot
                        Thread.sleep(2 * 1000);
                    } catch (InterruptedException e) {
                        autoSequence = false;
                    }
//                }

            }


        };
        sequenceThread = new Thread(sequenceRunnable, "SequenceShots");
        sequenceThread.start();
    }

    private void flash(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(message);
            }
        });
    }

    private void showToast(final String msg) {
        Toast.makeText(getBaseContext(), msg,
                Toast.LENGTH_SHORT).show();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            mCamera.setPreviewDisplay(cameraSurface.getHolder());
            mCamera.startPreview();
            Log.d(TAG, "startPreview");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
