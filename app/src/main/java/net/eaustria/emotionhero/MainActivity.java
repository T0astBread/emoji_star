package net.eaustria.emotionhero;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements Detector.ImageListener, CameraDetector.CameraEventListener {

    private static final int MY_PERMISSIONS_REQUESTS = 1;
    final String LOG_TAG = MainActivity.class.getSimpleName();
    Button mStartSDKButton;
    Button surfaceViewVisibilityButton;
    TextView smileTextView;
    TextView ageTextView;
    TextView ethnicityTextView;
    ToggleButton toggleButton;

    SurfaceView mCameraPreview;

    boolean isCameraBack = false;
    boolean isSDKStarted = false;

    RelativeLayout mainLayout;

    CameraDetector mDetector;

    int previewWidth = 0;
    int previewHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        smileTextView = findViewById(R.id.smile_textview);
        ageTextView = findViewById(R.id.age_textview);
        ethnicityTextView = findViewById(R.id.ethnicity_textview);

        toggleButton = findViewById(R.id.front_back_toggle_button);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCameraBack = isChecked;
                switchCamera(isCameraBack? CameraDetector.CameraType.CAMERA_BACK : CameraDetector.CameraType.CAMERA_FRONT);
            }
        });

        mStartSDKButton = (Button) findViewById(R.id.sdk_start_button);
        mStartSDKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSDKStarted) {
                    isSDKStarted = false;
                    stopDetector();
                    mStartSDKButton.setText("Start Camera");
                } else {
                    isSDKStarted = true;
                    startDetector();
                    mStartSDKButton.setText("Stop Camera");
                }
            }
        });
        mStartSDKButton.setText("Start Camera");

        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        mCameraPreview = new SurfaceView(this) {
            @Override
            public void onMeasure(int widthSpec, int heightSpec) {
                int measureWidth = MeasureSpec.getSize(widthSpec);
                int measureHeight = MeasureSpec.getSize(heightSpec);
                int width;
                int height;
                if (previewHeight == 0 || previewWidth == 0) {
                    width = measureWidth;
                    height = measureHeight;
                } else {
                    float viewAspectRatio = (float)measureWidth/measureHeight;
                    float cameraPreviewAspectRatio = (float) previewWidth/previewHeight;

                    if (cameraPreviewAspectRatio > viewAspectRatio) {
                        width = measureWidth;
                        height =(int) (measureWidth / cameraPreviewAspectRatio);
                    } else {
                        width = (int) (measureHeight * cameraPreviewAspectRatio);
                        height = measureHeight;
                    }
                }
                setMeasuredDimension(width,height);
            }
        };

        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
        mCameraPreview.setLayoutParams(params);
        mainLayout.addView(mCameraPreview,0);

        surfaceViewVisibilityButton = (Button) findViewById(R.id.surfaceview_visibility_button);
        surfaceViewVisibilityButton.setText("HIDE SURFACE VIEW");
        surfaceViewVisibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraPreview.getVisibility() == View.VISIBLE) {
                    mCameraPreview.setVisibility(View.INVISIBLE);
                    surfaceViewVisibilityButton.setText("SHOW SURFACE VIEW");
                } else {
                    mCameraPreview.setVisibility(View.VISIBLE);
                    surfaceViewVisibilityButton.setText("HIDE SURFACE VIEW");
                }
            }
        });

        mDetector = new CameraDetector(this,
                                        CameraDetector.CameraType.CAMERA_FRONT,
                                        mCameraPreview);
        mDetector.setDetectSmile(true);
        mDetector.setDetectAge(true);
        mDetector.setDetectEthnicity(true);
        mDetector.setImageListener(this);
        mDetector.setOnCameraEventListener(this);
        mDetector.setDetectAllEmojis(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSDKStarted) {
            startDetector();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDetector();
    }

    void startDetector() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
            {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle("Permission necessary");
                alertBuilder.setMessage("CAMERA is necessary");
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(getParent(),
                                new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUESTS);
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUESTS);
            }
            //startDetector();
        } else {
            mDetector.start();
        }
    }

    void stopDetector() {
        if (mDetector.isRunning()) {
            mDetector.stop();
        }
    }

    void switchCamera(CameraDetector.CameraType type) {
        mDetector.setCameraType(type);
    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null)
            return;
        if (list.size() == 0) {
            smileTextView.setText("NO FACE");
            ageTextView.setText("");
            ethnicityTextView.setText("");
        } else {
            Face face = list.get(0);
            smileTextView.setText(String.format("SMILE\n%.2f",face.expressions.getSmile()));
            switch (face.appearance.getAge()) {
                case AGE_UNKNOWN:
                    ageTextView.setText("");
                    break;
                case AGE_UNDER_18:
                    ageTextView.setText(R.string.age_under_18);
                    break;
                case AGE_18_24:
                    ageTextView.setText(R.string.age_18_24);
                    break;
                case AGE_25_34:
                    ageTextView.setText(R.string.age_25_34);
                    break;
                case AGE_35_44:
                    ageTextView.setText(R.string.age_35_44);
                    break;
                case AGE_45_54:
                    ageTextView.setText(R.string.age_45_54);
                    break;
                case AGE_55_64:
                    ageTextView.setText(R.string.age_55_64);
                    break;
                case AGE_65_PLUS:
                    ageTextView.setText(R.string.age_over_64);
                    break;
            }

            switch (face.appearance.getEthnicity()) {
                case UNKNOWN:
                    ethnicityTextView.setText("");
                    break;
                case CAUCASIAN:
                    ethnicityTextView.setText(R.string.ethnicity_caucasian);
                    break;
                case BLACK_AFRICAN:
                    ethnicityTextView.setText(R.string.ethnicity_black_african);
                    break;
                case EAST_ASIAN:
                    ethnicityTextView.setText(R.string.ethnicity_east_asian);
                    break;
                case SOUTH_ASIAN:
                    ethnicityTextView.setText(R.string.ethnicity_south_asian);
                    break;
                case HISPANIC:
                    ethnicityTextView.setText(R.string.ethnicity_hispanic);
                    break;
            }

        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int width, int height, Frame.ROTATE rotate) {
        if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
            previewWidth = height;
            previewHeight = width;
        } else {
            previewHeight = height;
            previewWidth = width;
        }
        mCameraPreview.requestLayout();
    }
}
