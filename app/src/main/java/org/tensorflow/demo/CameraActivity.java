/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Image.Plane;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.speechsynthesizer.SpeechSynthesizer;
import com.baidu.speechsynthesizer.SpeechSynthesizerListener;
import com.baidu.speechsynthesizer.publicutility.SpeechError;

import org.tensorflow.demo.Classifier.Recognition;
import org.tensorflow.demo.env.Logger;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public abstract class CameraActivity extends Activity implements OnImageAvailableListener,SpeechSynthesizerListener {
  private static final Logger LOGGER = new Logger();
  private static final int PERMISSIONS_REQUEST = 1;
  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
  private boolean debug = false;

  private Handler handler;
  private HandlerThread handlerThread;
  private  SpeechSynthesizer speechSynthesizer;
  private List<Recognition> results;
  String dtr="";
  RecognitionScoreView A;

  TimerTask task = new TimerTask() {
    @Override
    public void run() {

      if (A.results!= null) {
        for (final Recognition recog : A.results) {
          dtr=recog.getTitle();
          Log.e("rusult:",dtr);
          if(!dtr.equals("")) {
            speak_string(dtr);
            dtr="";  break;
          }
        }
      }


    }
  };




  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_camera);

    Timer timer = new Timer();
    long delay = 0;
    long intevalPeriod = 2 * 1000;                 //ms
    timer.scheduleAtFixedRate(task, delay, intevalPeriod);
    speechSynthesizer = new SpeechSynthesizer(getApplicationContext(), "holder", this);
    // 此处需要将setApiKey方法的两个参数替换为你在百度开发者中心注册应用所得到的apiKey和secretKey
    speechSynthesizer.setApiKey("B6vXyqxq61WyT0HYHGfhoUi5", "ZAGmty5TMlhRB7dpy4YLxH4PnNHO29nV");
    speechSynthesizer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    speak_string("开始物品识别");

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }
  }

  public  void speak_string(final String str_in){
    new Thread(new Runnable() {

      @Override
      public void run() {
        setParams();
        int ret = speechSynthesizer.speak(str_in);
      }
    }).start();
  }
  public  void setParams() {
    speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
    speechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "5");
    speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
    speechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
    speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_ENCODE, SpeechSynthesizer.AUDIO_ENCODE_AMR);
    speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_RATE, SpeechSynthesizer.AUDIO_BITRATE_AMR_15K85);
  }

  @Override
  public  void onStartWorking(SpeechSynthesizer synthesizer) {

  }

  @Override
  public void onSpeechStart(SpeechSynthesizer synthesizer) {

  }

  @Override
  public void onSpeechResume(SpeechSynthesizer synthesizer) {

  }

  @Override
  public void onSpeechProgressChanged(SpeechSynthesizer synthesizer,
                                      int progress) {
    // TODO Auto-generated method stub

  }
  @Override
  public void onSpeechPause(SpeechSynthesizer synthesizer) {

  }
  @Override
  public void onSynthesizeFinish(SpeechSynthesizer arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public void onSpeechFinish(SpeechSynthesizer synthesizer) {

  }

  @Override
  public void onNewDataArrive(SpeechSynthesizer synthesizer,
                              byte[] audioData, boolean isLastData) {

  }

  @Override
  public void onError(SpeechSynthesizer synthesizer, SpeechError error) {

  }

  @Override
  public void onCancel(SpeechSynthesizer synthesizer) {

  }

  @Override
  public void onBufferProgressChanged(SpeechSynthesizer synthesizer,
                                      int progress) {
    // TODO Auto-generated method stub

  }


  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    if (!isFinishing()) {
      LOGGER.d("Requesting finish");
      finish();
    }

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    moveTaskToBack(true);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    switch (requestCode) {
      case PERMISSIONS_REQUEST: {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
          setFragment();
        } else {
          requestPermission();
        }
      }
    }
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) || shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
        Toast.makeText(CameraActivity.this, "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
    }
  }

  protected void setFragment() {
    final Fragment fragment = CameraConnectionFragment.newInstance(
        new CameraConnectionFragment.ConnectionCallback(){
          @Override
          public void onPreviewSizeChosen(final Size size, final int rotation) {
            CameraActivity.this.onPreviewSizeChosen(size, rotation);
          }
        },
        this, getLayoutId(), getDesiredPreviewFrameSize());

    getFragmentManager()
        .beginTransaction()
        .replace(R.id.container, fragment)
        .commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  public void requestRender() {
    final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
    if (overlay != null) {
      overlay.postInvalidate();
    }
  }

  public void addCallback(final OverlayView.DrawCallback callback) {
    final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
    if (overlay != null) {
      overlay.addCallback(callback);
    }
  }

  public void onSetDebug(final boolean debug) {}

  @Override
  public boolean onKeyDown(final int keyCode, final KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      debug = !debug;
      requestRender();
      onSetDebug(debug);
      return true;
    }
//    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
////      Intent intentOBJ = new Intent(Intent.ACTION_MAIN);
////      intentOBJ.addCategory(Intent.CATEGORY_LAUNCHER);
////      ComponentName cn = new ComponentName("com.kaku.weac", "com.kaku.weac.MainActivity");
////      intentOBJ.setComponent(cn);
////      startActivity(intentOBJ);
//      this.finish();
//      return true;
//    }
    return super.onKeyDown(keyCode, event);
  }


  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
  protected abstract int getLayoutId();
  protected abstract int getDesiredPreviewFrameSize();

}
