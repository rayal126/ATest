package com.tilt.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tilt.R;
import com.tilt.util.Searcher;

public class ProductListActivity extends Activity {
	private final String TAG = ProductListActivity.class.getSimpleName();
	
	private final int NUM_DISPLAY_PER_PAGE = 10;
	private final int MSG_GET_PRODUCT_LIST = 0x7000;
	private final int MSG_SORT_PRODUCT_LIST = 0x7001;
	private final int MSG_CANNOT_FIND = 0x7002;
	
	private final int THREADPOOL_SIZE = 5;
	private ExecutorService executorService;

	private SearchPane searchPane;
	private ListView productListView;
	private LinearLayout listFooter;
	private ProgressDialog progressDialog;
	private TextView moreItemsTextView;
	private LinearLayout loadingPane;
	private Button sortBytPriceBtn;
	private Button sortBySaleBtn;
	private Button sortByCoefpBtn;
	private Button sortByRatesumBtn;
	private ProductListAdapter adapter;

	private String nameOfProduct;
	private int currentPage = 0;
	private int sortMethod = Searcher.WITHOUT_SORT;
	private List<JSONObject> productList;
	private JSONArray jsonArray;
	
	/**
	 * handles the update of the ListView
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_GET_PRODUCT_LIST:  {
				progressDialog.dismiss();
				int size = jsonArray.length();
				for (int ix = 0; ix < size; ++ix) {
					JSONObject item = jsonArray.optJSONObject(ix);
					productList.add(item);
				}
				
				adapter.notifyDataSetChanged();
				loadingPane.setVisibility(View.GONE);
				moreItemsTextView.setVisibility(View.VISIBLE);
				break;
			}
			case MSG_SORT_PRODUCT_LIST:	 {
				progressDialog.dismiss();
				int size = jsonArray.length();
				productList.clear();
				for (int ix = 0; ix < size; ++ix) {
					JSONObject item = jsonArray.optJSONObject(ix);
					productList.add(item);
				}
				adapter.notifyDataSetChanged();
				loadingPane.setVisibility(View.GONE);
				moreItemsTextView.setVisibility(View.VISIBLE);
				break;
			}
			case MSG_CANNOT_FIND:
				progressDialog.dismiss();
				Toast.makeText(ProductListActivity.this, "无法找到该产品的信息", Toast.LENGTH_LONG).show();
				loadingPane.setVisibility(View.GONE);
				moreItemsTextView.setVisibility(View.VISIBLE);
				break;
			default:
				Log.e(TAG, "Undefined Message Type!");
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// requires full screen
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = this.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// initializes
		this.setContentView(R.layout.activity_product_list);
		this.initViews();
		this.initListener();
		executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
		productList = new ArrayList<JSONObject>();
		adapter = new ProductListAdapter(ProductListActivity.this, productList);
		productListView.setAdapter(adapter);

		// gets the name of the product
		Intent intent = ProductListActivity.this.getIntent();
		String key = intent.getStringExtra(getResources().getText(
				R.string.search_key).toString());
		String searchMethod = intent.getStringExtra(getResources().getText(
				R.string.search_method).toString());
		
		if (searchMethod.equals(getResources().getText(
				R.string.search_by_barcode).toString())) {
			nameOfProduct = Searcher.getNameOfBarcode(key);
		} else {
			nameOfProduct = key;
		}

		this.searchPane.setKeyWord(nameOfProduct);
		this.showProgressDialog();
		executorService.submit(new GetProductListThread());
	}

	private void initViews() {
		searchPane = (SearchPane) this.findViewById(R.id.searchPane);
		productListView = (ListView) this.findViewById(R.id.productList);
		listFooter = (LinearLayout) LayoutInflater.from(this).inflate(
				R.layout.product_list_footer, null);
		moreItemsTextView = (TextView) listFooter
				.findViewById(R.id.moreItemsTextView);
		loadingPane = (LinearLayout) listFooter.findViewById(R.id.loadingPane);
		productListView.addFooterView(listFooter);
		sortBytPriceBtn = (Button) this.findViewById(R.id.sortByPriceBtn);
		sortBySaleBtn = (Button) this.findViewById(R.id.sortBySaleBtn);
		sortByCoefpBtn = (Button) this.findViewById(R.id.sortByCoefpBtn);
		sortByRatesumBtn = (Button) this.findViewById(R.id.sortByRatesumBtn);
	}

	private void initListener() {
		moreItemsTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				moreItemsTextView.setVisibility(View.GONE);
				loadingPane.setVisibility(View.VISIBLE);
				executorService.submit(new GetProductListThread());
			}
		});
		
		productListView.setOnItemClickListener(new OnItemClickListener() {
			@Override 
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				JSONObject obj = productList.get(position);
				String url = obj.optString("auctionURL");
				Intent intent = new Intent(ProductListActivity.this, ProductDetailActivity.class);
				intent.putExtra("URL", url);
				startActivity(intent);
			}
		});
	
		// sorts the products by their price
		sortBytPriceBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				showProgressDialog();
				sortMethod = Searcher.SORT_BY_PRICE;
				executorService.submit(new SortProductListThread());
			}
		});
		
		// sorts the products by their sale
		sortBySaleBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				showProgressDialog();
				sortMethod = Searcher.SORT_BY_SALE;
				executorService.submit(new SortProductListThread());
			}
		});
		
		// sorts the products by their coefp
		sortByCoefpBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				showProgressDialog();
				sortMethod = Searcher.SORT_BY_COEFP;
				executorService.submit(new SortProductListThread());
			}
		});
		
		// sorts the products by their ratesum
		sortByRatesumBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				showProgressDialog();
				sortMethod = Searcher.SORT_BY_RATESUM;
				executorService.submit(new SortProductListThread());
			}
		});
	}

	private void showProgressDialog() {
		progressDialog = new ProgressDialog(this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setTitle("请稍等");
		progressDialog.setMessage("获取数据中...");
		progressDialog.show();
	}
	
	/**
	 * gets the product list 
	 * @author Rayal
	 *
	 */
	private class GetProductListThread extends Thread {
		@Override
		public void run() {
			try {
				JSONObject obj = Searcher.search(nameOfProduct, ++currentPage, NUM_DISPLAY_PER_PAGE, sortMethod);
				String totalResults = obj.getString("totalResults");
				
				if (totalResults.equals("0")) {
					Message msg = new Message();
					msg.what = MSG_CANNOT_FIND;
					handler.sendMessage(msg);
				} else {
					jsonArray = obj.getJSONArray("itemsArray");
					Message msg = new Message();
					msg.what = MSG_GET_PRODUCT_LIST;
					handler.sendMessage(msg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * sorts the product list by price, sale, coefp of ratesum
	 * @author Rayal
	 *
	 */
	private class SortProductListThread extends Thread {
		@Override
		public void run() {
			try {
				JSONObject obj = Searcher.search(nameOfProduct, 1, NUM_DISPLAY_PER_PAGE*currentPage, sortMethod);
				String totalResults = obj.getString("totalResults");
				
				if (totalResults.equals("0")) {
					Message msg = new Message();
					msg.what = MSG_CANNOT_FIND;
					handler.sendMessage(msg);
				} else {
					jsonArray = obj.getJSONArray("itemsArray");
					Message msg = new Message();
					msg.what = MSG_SORT_PRODUCT_LIST;
					handler.sendMessage(msg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
