package com.tathva.tathva_live.activity;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.tathva.tathva_live.camera.CameraSurface;
import com.tathva.tathva_live.data.ARData;
import com.tathva.tathva_live.ui.Marker;
import com.tathva.tathva_live.widget.VerticalSeekBar;
import com.tathva.tathva_live.widget.VerticalTextView;

/**
 * This class extends the SensorsActivity and is designed tie the AugmentedView
 * and zoom bar together.
 * 
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class AugmentedReality extends SensorsActivity implements OnTouchListener {

    private static final String TAG = "AugmentedReality";
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    private static final int ZOOMBAR_BACKGROUND_COLOR = Color.argb(125, 55, 55, 55);
    private static final String END_TEXT = FORMAT.format(AugmentedReality.MAX_ZOOM) + " km";
    private static final int END_TEXT_COLOR = Color.WHITE;

    protected static WakeLock wakeLock = null;
    protected static CameraSurface camScreen = null;
    protected static VerticalSeekBar myZoomBar = null;
    protected static VerticalTextView endLabel = null;
    protected static LinearLayout zoomLayout = null;
    protected static AugmentedView augmentedView = null;

    public static final float MAX_ZOOM = 100; // in KM
    public static final float ONE_PERCENT = MAX_ZOOM / 100f;
    public static final float TEN_PERCENT = 10f * ONE_PERCENT;
    public static final float TWENTY_PERCENT = 2f * TEN_PERCENT;
    public static final float EIGHTY_PERCENTY = 4f * TWENTY_PERCENT;

    public static boolean portrait = false;
    public static boolean useCollisionDetection = false;
    public static boolean showRadar = false;
    public static boolean showZoomBar = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        camScreen = new CameraSurface(this);
        setContentView(camScreen);

        augmentedView = new AugmentedView(this);
        augmentedView.setOnTouchListener(this);
        LayoutParams augLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addContentView(augmentedView, augLayout);

        zoomLayout = new LinearLayout(this);
        zoomLayout.setVisibility((showZoomBar) ? LinearLayout.VISIBLE : LinearLayout.GONE);
        zoomLayout.setOrientation(LinearLayout.VERTICAL);
        zoomLayout.setPadding(5, 5, 5, 5);
        zoomLayout.setBackgroundColor(ZOOMBAR_BACKGROUND_COLOR);

        endLabel = new VerticalTextView(this);
        endLabel.setText(END_TEXT);
        endLabel.setTextColor(END_TEXT_COLOR);
        LinearLayout.LayoutParams zoomTextParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        zoomTextParams.gravity = Gravity.CENTER;
        zoomLayout.addView(endLabel, zoomTextParams);

        myZoomBar = new VerticalSeekBar(this);
        myZoomBar.setMax(100);
        myZoomBar.setProgress(5);
        myZoomBar.setOnSeekBarChangeListener(myZoomBarOnSeekBarChangeListener);
        LinearLayout.LayoutParams zoomBarParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
        zoomBarParams.gravity = Gravity.CENTER_HORIZONTAL;
        zoomLayout.addView(myZoomBar, zoomBarParams);

        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, Gravity.RIGHT);
        addContentView(zoomLayout, frameLayoutParams);

        updateDataOnZoom();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();

        wakeLock.acquire();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();

        wakeLock.release();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSensorChanged(SensorEvent evt) {
        super.onSensorChanged(evt);

        if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER || evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            augmentedView.postInvalidate();
        }
    }

    private OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener = new OnSeekBarChangeListener() {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateDataOnZoom();
            camScreen.invalidate();
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            // Ignore
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            updateDataOnZoom();
            camScreen.invalidate();
        }
    };

    private static float calcZoomLevel() {
        int myZoomLevel = myZoomBar.getProgress();
        float myout = 2;

        /*float percent = 0;
        if (myZoomLevel <= 25) {
            percent = myZoomLevel / 25f;
            myout = ONE_PERCENT * percent;
        } else if (myZoomLevel > 25 && myZoomLevel <= 50) {
            percent = (myZoomLevel - 25f) / 25f;
            myout = ONE_PERCENT + (TEN_PERCENT * percent);
        } else if (myZoomLevel > 50 && myZoomLevel <= 75) {
            percent = (myZoomLevel - 50f) / 25f;
            myout = TEN_PERCENT + (TWENTY_PERCENT * percent);
        } else {
            percent = (myZoomLevel - 75f) / 25f;
            myout = TWENTY_PERCENT + (EIGHTY_PERCENTY * percent);
        }*/

        return myout;
    }

    /**
     * Called when the zoom bar has changed.
     */
    protected void updateDataOnZoom() {
        float zoomLevel = calcZoomLevel();
        ARData.setRadius(zoomLevel);
        ARData.setZoomLevel(FORMAT.format(zoomLevel));
        ARData.setZoomProgress(myZoomBar.getProgress());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouch(View view, MotionEvent me) {
        // See if the motion event is on a Marker
        for (Marker marker : ARData.getMarkers()) {
            if (marker.handleClick(me.getX(), me.getY())) {
                if (me.getAction() == MotionEvent.ACTION_UP) markerTouched(marker);
                return true;
            }
        }

        return super.onTouchEvent(me);
    };

    protected void markerTouched(Marker marker) {
        Log.w(TAG, "markerTouched() not implemented.");
    }
}
