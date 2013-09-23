package com.tilt.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tilt.R;
import com.tilt.algorithm.Arguments;
import com.tilt.algorithm.ImageUtil;
import com.tilt.algorithm.Tilt;
import com.tilt.barcode.Barcode;
import com.tilt.barcode.BarcodeDecodeThread;
import com.tilt.util.BitmapManager;
import com.tilt.util.Searcher;

public class ProcessActivity extends Activity {
	public static final String DISPLAY_PICTURE_KEY = "DISPLAY_PICTURE";
	private static final String TAG = ProcessActivity.class.getSimpleName();

	private final int DIALOG_DECODE = 0x221;
	private final int DIALOG_TILT = 0x222;
	
	private final int UI_DISPLAY = 0;
	private final int UI_TILT = 1;
	private final int UI_CUT = 2;
	private int currentUI = UI_DISPLAY;
	
	private final String DIR_PATH = Environment.getExternalStorageDirectory()
			.getPath() + "/TILT_Pictures/";
	private final String TEMP_FILE_PATH = DIR_PATH + "_temp.jpg";

	private PictureView pictureView;
	private LinearLayout btnPane;
	private Button queryBtn;
	private Button tiltBtn;
	private Button saveBtn;
	private Button cutBtn;
	private Button clockWiseBtn;
	private Button antiClockWiseBtn;
	private Button confirmBtn;
	private Bitmap currentPicture;
	private Map<Integer, Dialog> dialogs;

	/**
	 * update the UI
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
//			case UPDATE_CAMERA_TO_DISPLAY:
//				picture = cameraManager.getLastPhoto();
//				pictureView.setVisibility(View.VISIBLE);
//				pictureView.setImageBitmap(picture);
//				btnPane.startAnimation(AnimationUtils.loadAnimation(
//						CameraActivity.this, R.anim.right_to_left_appear));
//				btnPane.setVisibility(View.VISIBLE);
//				currentUI = UI_DISPLAY;
//				break;
			case R.id.display_to_tilt:
				buttonPaneDisappear();
				currentUI = UI_TILT;
				break;
			case R.id.display_to_cut:
				buttonPaneDisappear();
				currentUI = UI_CUT;
				break;
			case R.id.tilt_to_display:
				buttonPaneAppear();
				dialogs.get(DIALOG_TILT).dismiss();
				currentUI = UI_DISPLAY;
				break;
			case R.id.cut_to_display:
				buttonPaneAppear();
				currentUI = UI_DISPLAY;
				break;
//			case UPDATE_DISPLAY_TO_CAMERA:
//				pictureView.setVisibility(View.GONE);
//				btnPane.startAnimation(AnimationUtils.loadAnimation(
//						CameraActivity.this, R.anim.left_to_right_disappear));
//				btnPane.setVisibility(View.GONE);
//				takePhotoBtn.startAnimation(AnimationUtils.loadAnimation(
//						CameraActivity.this, R.anim.right_to_left_appear));
//				takePhotoBtn.setVisibility(View.VISIBLE);
//				cameraManager.startPreview();
//				currentUI = UI_CAMERA;
//				break;
			case R.id.barcode_decoded:
				Object obj = msg.obj;

				if (!(obj instanceof Barcode)) {
					Toast.makeText(ProcessActivity.this,
							"Error in decoding the barcode!", Toast.LENGTH_LONG)
							.show();
				} else {
					Barcode barcode = (Barcode) msg.obj;
					
					if (!barcode.isDecoded()) {
						Toast.makeText(ProcessActivity.this,
								"扫描条形码失败", Toast.LENGTH_LONG).show();
					} else {
						Intent intent = new Intent(ProcessActivity.this, ProductListActivity.class);
						intent.putExtra(getResources().getText(R.string.search_key).toString(), barcode.getContent());
						intent.putExtra(getResources().getText(R.string.search_method).toString(), getResources().getText(R.string.search_by_barcode).toString());
						startActivity(intent);
					}
				}

				dialogs.get(DIALOG_DECODE).dismiss();
				currentUI = UI_DISPLAY;
				break;
			default:
				Log.i(TAG, "Not defined message type");
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// initializes the window and the layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = this.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		this.setContentView(R.layout.activity_process);

		// gets the widgets
		this.pictureView = (PictureView) this.findViewById(R.id.pictureView);
		this.btnPane = (LinearLayout) this.findViewById(R.id.btnPane);
		btnPane = (LinearLayout) this.findViewById(R.id.btnPane);
		queryBtn = (Button) this.findViewById(R.id.queryBtn);
		tiltBtn = (Button) this.findViewById(R.id.tiltBtn);
		saveBtn = (Button) this.findViewById(R.id.saveBtn);
		confirmBtn = (Button) this.findViewById(R.id.confirmBtn);
		cutBtn = (Button) this.findViewById(R.id.cutBtn);
		clockWiseBtn = (Button) this.findViewById(R.id.clockWiseBtn);
		antiClockWiseBtn = (Button) this.findViewById(R.id.antiClockWiseBtn);
		
		// initializes the widgets
		initDialog();
		setButtonListener();

//		pictures = new ArrayList<Bitmap>();
		String pictureIndex = this.getIntent().getStringExtra(DISPLAY_PICTURE_KEY);
		currentPicture = BitmapManager.getInstance().getBitmap(pictureIndex);
		
		if (currentPicture != null) {
			pictureView.setImageBitmap(currentPicture);
//			this.addPicture(currentPicture);
		}
	}

	private void setButtonListener() {
		queryBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// decode the barcode
				pictureView.setState(PictureView.STATE_NOT_DRAWABLE);
				Thread barcodeDecoder = new BarcodeDecodeThread(handler, currentPicture);
				barcodeDecoder.start();
				showDialog(DIALOG_DECODE);
			}
		});

		tiltBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// use a sub-thread to save the temp picture
				pictureView.setState(PictureView.STATE_TILT);
				new Thread() {
					@Override
					public void run() {
						saveImage(TEMP_FILE_PATH, currentPicture);
					}
				}.start();

				// tells the handler to update the UI
				Message msg = new Message();
				msg.what = R.id.display_to_tilt;
				handler.sendMessage(msg);
			}
		});

		// save the picture to the sd card
		saveBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				pictureView.setState(PictureView.STATE_NOT_DRAWABLE);
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd_HH-mm-ss");
				String currentDateandTime = sdf.format(new Date());
				String path = DIR_PATH + currentDateandTime + ".jpg";
				if (saveImage(path, currentPicture)) {
					Toast.makeText(ProcessActivity.this, "成功保存照片",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(ProcessActivity.this, "无法保存照片",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		cutBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				pictureView.setState(PictureView.STATE_CUT);
				// tells the handler to update the UI
				Message msg = new Message();
				msg.what = R.id.display_to_cut;
				handler.sendMessage(msg);
			}
		});
		
		clockWiseBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				currentPicture = ImageUtil.rotatePicture(currentPicture, 90);
				pictureView.invalidate();
			}
		});
		
		confirmBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (currentUI == UI_TILT) {
					// read the temp image
					final Mat image = Highgui.imread(TEMP_FILE_PATH);

					if ((image == null) || (image.empty())) {
						Log.e(TAG, "Cannot read the image(" + TEMP_FILE_PATH
								+ ")");
					}

					// uses a new thread to implement the tilt function
					showDialog(DIALOG_TILT);

					new Thread() {
						@Override
						public void run() {
							Arguments args = new Arguments(image, pictureView
									.getSelectedPoints(), pictureView
									.getImageBoundary());
							Mat resImage = Tilt.Tilt(args);

							// write the image into the sd card
							String path = DIR_PATH + "tilt_result.jpg";
							Highgui.imwrite(path, resImage);
							
							if ((currentPicture != null) && (!currentPicture.isRecycled())) {
								currentPicture.recycle();
							}
							
							currentPicture = null;
							currentPicture = BitmapFactory.decodeFile(path);
//							addPicture(currentPicture);
							
							// tells the handler to update the UI
							Message msg = new Message();
							msg.what = R.id.tilt_to_display;
							handler.sendMessage(msg);
						}
					}.start();
				} else if (currentUI == UI_CUT) {
					currentPicture = pictureView.getSelectedImage();
//					addPicture(currentPicture);
					
					// tells the handler to update the UI
					Message msg = new Message();
					msg.what = R.id.cut_to_display;
					handler.sendMessage(msg);
				}
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (currentUI == this.UI_DISPLAY) {
				finish();
			} else {
				Message msg = new Message();
				
				if (currentUI == this.UI_TILT) {
					msg.what = R.id.tilt_to_display;
				} else if (currentUI == this.UI_CUT) { 
					msg.what = R.id.cut_to_display;
				}
				
				handler.sendMessage(msg);
			}
			break;
		default:
			break;
		}
		
		return true;
	}
	
	/**
	 * initializes the dialogs
	 */
	private void initDialog() {
		dialogs = new HashMap<Integer, Dialog>();

		// initializes the barcode decoding progress dialog
		ProgressDialog decodeDialog = new ProgressDialog(this);
		decodeDialog.setMessage("正在识别条形");
		decodeDialog.setCancelable(false);
		decodeDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		decodeDialog.setIndeterminate(false);
		dialogs.put(DIALOG_DECODE, decodeDialog);

		// initializes the tilt progress dialog
		ProgressDialog tiltDialog = new ProgressDialog(this);
		tiltDialog.setMessage("正在纠正图片");
		tiltDialog.setCancelable(false);
		tiltDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		tiltDialog.setIndeterminate(false);
		dialogs.put(DIALOG_TILT, tiltDialog);
	}

	@Override
	public Dialog onCreateDialog(int id, Bundle status) {
		return dialogs.get(id);
	}
	
	/**
	 * saves the image into the sd card
	 * @param path
	 * @param image
	 * @return
	 */
	private boolean saveImage(String path, Bitmap image) {
		// create the parent files if necessary 
		int index = path.lastIndexOf("/");	
		if (index > 0) {
			String parentDirPath = path.substring(0, index);
			File parentDir = new File(parentDirPath);
			
			if (!parentDir.exists()) {
				parentDir.mkdirs();
			}
		}
		
		// saves the image into the sd card 
		OutputStream os = null;

		try {
			os = new FileOutputStream(path);
			image.compress(CompressFormat.JPEG, 100, os);
		} catch (Exception e) {
			return false;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}
	
	/**
	 * displays the button pane
	 */
	private void buttonPaneAppear() {
		pictureView.setState(PictureView.STATE_NOT_DRAWABLE);
		confirmBtn.startAnimation(AnimationUtils.loadAnimation(
				ProcessActivity.this, R.anim.left_to_right_disappear));
		confirmBtn.setVisibility(View.GONE);
		pictureView.setImageBitmap(currentPicture);
		btnPane.setVisibility(View.VISIBLE);
	}
	
	/**
	 * hides the button pane
	 */
	private void buttonPaneDisappear() {
		btnPane.startAnimation(AnimationUtils.loadAnimation(
				ProcessActivity.this, R.anim.left_to_right_disappear));
		btnPane.setVisibility(View.GONE);
		confirmBtn.startAnimation(AnimationUtils.loadAnimation(
				ProcessActivity.this, R.anim.right_to_left_appear));
		confirmBtn.setVisibility(View.VISIBLE);
	}
	
//	private void addPicture(Bitmap picture) {
//		++currentIndex;
//		pictures.add(picture);
//	}
}
