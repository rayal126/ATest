package com.tilt.view;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.tilt.R;

public class SearchPane extends RelativeLayout {
	private final int ID_OF_SEARCH_METHOD_BUTTON = 0x7777;
	private final int ID_OF_EDIT_TEXT = 0x7778;
	
	private static String keyword = "";
	
	private static final int SEARCH_BY_NAME = 0x1111;
	private static final int SEARCH_BY_BARCODE = 0x2222;
	private int searchMethod = SEARCH_BY_NAME;
	private final Context context;
	private Button searchMethodBtn;
	private EditText inputArea;
	private Button searchBtn;

	private static class SpinnerAdapter extends ArrayAdapter<String> {
		private Context context;
		private String[] objects;

		public SpinnerAdapter(final Context context,
				final int textViewResourceId, final String[] objects) {
			super(context, textViewResourceId, objects);
			this.context = context;
			this.objects = objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imgView = new ImageView(context);

			if (position == 0) {
				imgView.setBackgroundDrawable(context.getResources()
						.getDrawable(R.drawable.spinner_name));
			} else if (position == 1) {
				imgView.setBackgroundDrawable(context.getResources()
						.getDrawable(R.drawable.spinner_barcode));
			}

			return imgView;
		}
	}

	public SearchPane(Context context) {
		this(context, null);
	}

	/**
	 * This constructor is used when the class is built from an XML resource
	 * 
	 * @param context
	 * @param attrs
	 */
	public SearchPane(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		// initializes the search pane
		this.setLayoutParams(new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT));

		WindowManager wm = (WindowManager) context
				.getSystemService(Service.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		double scaleOfWidth = (double) display.getWidth()
				/ (double) getResources().getInteger(R.integer.ref_width);
		double scaleOfHeight = (double) display.getHeight()
				/ (double) getResources().getInteger(R.integer.ref_height);

		// initializes the searchMethodBtn
		final int REF_SEARCH_METHOD_BTN_WIDTH = 225;
		final int REF_SEARCH_METHOD_BTN_HEIGHT = 110;
		int widthOfBtn = (int) (REF_SEARCH_METHOD_BTN_WIDTH * scaleOfWidth);
		int heightOfBtn = (int) (REF_SEARCH_METHOD_BTN_HEIGHT * scaleOfHeight);
		RelativeLayout.LayoutParams paramsOfBtn = new RelativeLayout.LayoutParams(
				widthOfBtn, heightOfBtn);
		paramsOfBtn.addRule(RelativeLayout.ALIGN_LEFT, RelativeLayout.TRUE);
		searchMethodBtn = new Button(context);
		searchMethodBtn.setBackgroundDrawable(context.getResources()
				.getDrawable(R.drawable.spinner_name));
		searchMethodBtn.setLayoutParams(paramsOfBtn);
		searchMethodBtn.setId(ID_OF_SEARCH_METHOD_BUTTON);

		searchMethodBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				switch (searchMethod) {
				case SEARCH_BY_NAME:
					searchMethod = SEARCH_BY_BARCODE;
					searchMethodBtn
							.setBackgroundDrawable(SearchPane.this.context
									.getResources().getDrawable(
											R.drawable.spinner_barcode));
					inputArea.setHint("请输入商品条形码");
					break;
				case SEARCH_BY_BARCODE:
					searchMethod = SEARCH_BY_NAME;
					searchMethodBtn
							.setBackgroundDrawable(SearchPane.this.context
									.getResources().getDrawable(
											R.drawable.spinner_name));
					inputArea.setHint("请输入商品名称");
					break;
				default:
					break;
				}
			}
		});

		// initializes the input area
		final int REF_EDIT_TEXT_WIDTH = 580;
		final int REF_EDIT_TEXT_HEIGHT = 110;
		int widthOfEditText = (int) (REF_EDIT_TEXT_WIDTH * scaleOfWidth);
		int heightOfEditText = (int) (REF_EDIT_TEXT_HEIGHT * scaleOfHeight);
		RelativeLayout.LayoutParams paramsOfInputArea = new RelativeLayout.LayoutParams(
				widthOfEditText, heightOfEditText);
		paramsOfInputArea.addRule(RelativeLayout.RIGHT_OF,
				searchMethodBtn.getId());
		inputArea = new EditText(context);
		inputArea.setLayoutParams(paramsOfInputArea);
		inputArea.setId(ID_OF_EDIT_TEXT);
		inputArea.setBackgroundDrawable(context.getResources().getDrawable(
				R.drawable.edit_text));
		inputArea.setHint("请输入商品名称");
		
		// initializes the search button
		final int REF_SEARCH_BTN_WIDTH = 102;
		final int REF_SEARCH_BTN_HEIGHT = 110;
		int widthOfSearchBtn = (int) (REF_SEARCH_BTN_WIDTH * scaleOfWidth);
		int heightOfSearchBtn = (int) (REF_SEARCH_BTN_HEIGHT * scaleOfHeight);
		RelativeLayout.LayoutParams paramsOfSearchBtn = new RelativeLayout.LayoutParams(
				widthOfSearchBtn, heightOfSearchBtn);
		paramsOfSearchBtn.addRule(RelativeLayout.ALIGN_RIGHT, inputArea.getId());
		searchBtn = new Button(context);
		searchBtn.setLayoutParams(paramsOfSearchBtn);
		searchBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.search_btn));
		searchBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				String key = inputArea.getText().toString();

				if (key.equals("")) {
					Toast.makeText(SearchPane.this.context, "请输入关键字",
							Toast.LENGTH_SHORT).show();
				} else {
					Intent intent = new Intent(SearchPane.this.context,
							ProductListActivity.class);
					intent.putExtra(getResources().getText(R.string.search_key)
							.toString(), key);

					if (searchMethod == SEARCH_BY_NAME) {
						intent.putExtra(
								getResources().getText(R.string.search_method)
										.toString(),
								getResources().getText(R.string.search_by_name)
										.toString());
					} else if (searchMethod == SEARCH_BY_BARCODE) {
						intent.putExtra(
								getResources().getText(R.string.search_method)
										.toString(),
								getResources().getText(
										R.string.search_by_barcode).toString());
					}

					SearchPane.this.context.startActivity(intent);
				}
			}
		});
		
		this.addView(searchMethodBtn);
		this.addView(inputArea);
		this.addView(searchBtn);
	}
	
	public void setKeyWord(String key) {
		keyword = key;
		updateView();
	}
	
	public void updateView() {
		searchMethod = SEARCH_BY_NAME;
		searchMethodBtn.setBackgroundDrawable(SearchPane.this.context.getResources().getDrawable(R.drawable.spinner_name));
		this.inputArea.setText(keyword);
	}
}
