package com.tilt.view;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tilt.R;

public class ProductDetailActivity extends Activity {
	private WebView productDetailView;
	private String url;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// requires full screen
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = this.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		this.setContentView(R.layout.activity_product_detail);
		productDetailView = (WebView) this.findViewById(R.id.productDetailView);
		url = this.getIntent().getStringExtra("URL");
		productDetailView.getSettings().setJavaScriptEnabled(true);
		productDetailView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
		productDetailView.loadUrl(url);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && (productDetailView.canGoBack())) {
			productDetailView.goBack();
		}
		
		return super.onKeyDown(keyCode, event);
	}
}
