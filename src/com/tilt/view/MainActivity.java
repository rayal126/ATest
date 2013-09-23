package com.tilt.view;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import com.tilt.R;
import com.tilt.algorithm.ImageUtil;
import com.tilt.util.BitmapManager;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private int ANIMATION_DELAY_TIME = 500;
	private final int REQUEST_PICK = 0x1000;
	private final int MSG_START_ACTIVITY_WITHOUT_RESULT= 0x3330;
	private final int MSG_START_ACTIVITY_WITH_RESULT= 0x3331;
	private final int MSG_APPEAR_UI = 0x3332;
	private final int MSG_DISAPPEAR_UI = 0x3333;
	
	private SearchPane searchPane;
	private Button scanerBtn;
	private Button cameraBtn;
	private Button loadPicBtn;
	private Button settingBtn;
	private boolean initializedOpencv = false;

	/**
	 * loads the openCV
	 */
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				break;
			}
			default: {
				super.onManagerConnected(status);
				break;
			}
			}
		}
	};

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_START_ACTIVITY_WITHOUT_RESULT: {
				Intent intent = (Intent) msg.obj;
				startActivity(intent);
				break;
			}
			case MSG_START_ACTIVITY_WITH_RESULT: {
				Intent intent = (Intent) msg.obj;
				startActivityForResult(intent, msg.arg1);
				break;
			}
			case MSG_APPEAR_UI: {
				scanerBtn.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,
						R.anim.appear));
				cameraBtn.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,
						R.anim.appear));
				loadPicBtn.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,
						R.anim.appear));
				settingBtn.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,
						R.anim.appear));
				scanerBtn.setVisibility(View.VISIBLE);
				cameraBtn.setVisibility(View.VISIBLE);
				loadPicBtn.setVisibility(View.VISIBLE);
				settingBtn.setVisibility(View.VISIBLE);
				break;
			}
			case MSG_DISAPPEAR_UI: {
				scanerBtn.startAnimation(AnimationUtils.loadAnimation(
						MainActivity.this, R.anim.disappear));
				cameraBtn.startAnimation(AnimationUtils.loadAnimation(
						MainActivity.this, R.anim.disappear));
				loadPicBtn.startAnimation(AnimationUtils.loadAnimation(
						MainActivity.this, R.anim.disappear));
				settingBtn.startAnimation(AnimationUtils.loadAnimation(
						MainActivity.this, R.anim.disappear));
				scanerBtn.setVisibility(View.GONE);
				cameraBtn.setVisibility(View.GONE);
				loadPicBtn.setVisibility(View.GONE);
				settingBtn.setVisibility(View.GONE);
				break;
			}
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// initializes the window and the layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = this.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);

		// gets the widgets
		searchPane = (SearchPane) this.findViewById(R.id.searchPane);
		scanerBtn = (Button) this.findViewById(R.id.scanerBtn);
		cameraBtn = (Button) this.findViewById(R.id.cameraBtn);
		loadPicBtn = (Button) this.findViewById(R.id.loadPicBtn);
		settingBtn = (Button) this.findViewById(R.id.settingBtn);
		this.setButtonListener();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!initializedOpencv) {
			if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3,
					this, mLoaderCallback)) {
				Log.e(TAG, "Cannot connect to the OpenCV Manager");
			} else {
				initializedOpencv = true;
				Log.i(TAG, "Connected to the OpenCV Manager successfully!");
			}
		}
		
		// calls the UI elements to appear
		Message msg = new Message();
		msg.what = MSG_APPEAR_UI;
		handler.sendMessage(msg);
		searchPane.updateView();
	}

	/**
	 * sets the buttons listeners
	 */
	private void setButtonListener() {
		scanerBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// updates the UI elements 
				Message msg = new Message();
				msg.what = MSG_DISAPPEAR_UI;
				handler.sendMessage(msg);
				
				// starts the activity after ANIMATION_DELAY_TIME
				Intent intent = new Intent(MainActivity.this,
						ScannerActivity.class);
				Message startActivityMsg = new Message();
				startActivityMsg.what = MSG_START_ACTIVITY_WITHOUT_RESULT;
				startActivityMsg.obj = intent;
				handler.sendMessageDelayed(startActivityMsg, ANIMATION_DELAY_TIME);
			}
		});

		cameraBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// updates the UI elements 
				Message msg = new Message();
				msg.what = MSG_DISAPPEAR_UI;
				handler.sendMessage(msg);
				
				// starts the activity after ANIMATION_DELAY_TIME
				Intent intent = new Intent(MainActivity.this,
						CameraActivity.class);
				Message startActivityMsg = new Message();
				startActivityMsg.what = MSG_START_ACTIVITY_WITHOUT_RESULT;
				startActivityMsg.obj = intent;
				handler.sendMessageDelayed(startActivityMsg, ANIMATION_DELAY_TIME);
			}
		});

		loadPicBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// updates the UI elements 
				Message msg = new Message();
				msg.what = MSG_DISAPPEAR_UI;
				handler.sendMessage(msg);
				
				// starts the activity after ANIMATION_DELAY_TIME
				Intent intent = new Intent(Intent.ACTION_PICK, null);
				intent.setDataAndType(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
				Message startActivityMsg = new Message();
				startActivityMsg.what = MSG_START_ACTIVITY_WITH_RESULT;
				startActivityMsg.obj = intent;
				startActivityMsg.arg1 = REQUEST_PICK;
				handler.sendMessageDelayed(startActivityMsg, ANIMATION_DELAY_TIME);
			}
		});
		
		settingBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(MainActivity.this, "此功能还没开放", Toast.LENGTH_LONG);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_PICK) {
			try {
				Uri originalUri = data.getData();
				ContentResolver cr = this.getContentResolver();
				Bitmap picture = MediaStore.Images.Media.getBitmap(cr,
						originalUri);

				if (picture == null) {
					Log.w(TAG, "the picture is null");
				} else {
					// reduces the picture if necessray
					final int MAX_SIZE_OF_PICTURE = 2048;
					float scale = ImageUtil.getScaleFactor(picture,
							MAX_SIZE_OF_PICTURE);
					picture = ImageUtil.scalePicture(picture, scale, scale,
							true);

					// starts the image processing activity
					String PIC_INDEX = "PICTURE_FROM_SDCARD";
					BitmapManager.getInstance().putBitmap(PIC_INDEX, picture);
					Intent intent = new Intent(MainActivity.this,
							ProcessActivity.class);
					intent.putExtra(ProcessActivity.DISPLAY_PICTURE_KEY,
							PIC_INDEX);
					this.startActivity(intent);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
