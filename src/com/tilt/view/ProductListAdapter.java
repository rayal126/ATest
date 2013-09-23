package com.tilt.view;

import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tilt.R;
import com.tilt.util.AsyncImageLoader;
import com.tilt.util.InternetBitmapLoader;

public class ProductListAdapter extends BaseAdapter {
	private Context context;
	private List<JSONObject> productList;
	
	public ProductListAdapter(final Context context, final List<JSONObject> productList) {
		super();
		this.context = context;
		this.productList = productList;
	}
	
	@Override
	public int getCount() {
		return productList.size();
	}
	
	@Override
	public Object getItem(int position) {
		if ((position < 0) || (position >= productList.size())) {
			return null;
		} else {
			return productList.get(position);
		}
	}
	
	@Override
	public long getItemId(int position) {
		try {
			Object obj =  this.getItem(position);
			if (obj == null) {
				return 0;
			} else {
				JSONObject jsonObj = (JSONObject)obj;
				return jsonObj.getInt("id");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.product_list_item, null);
			holder = new ViewHolder();
			holder.imgView = (ImageView)convertView.findViewById(R.id.image);
			holder.nameView = (TextView)convertView.findViewById(R.id.name);
			holder.priceView = (TextView)convertView.findViewById(R.id.price);
			holder.priceBeforeRateView = (TextView)convertView.findViewById(R.id.priceBeforeRate);
			holder.soldView = (TextView)convertView.findViewById(R.id.sold);
			holder.locationView = (TextView)convertView.findViewById(R.id.location);
			holder.tmallIcon = (ImageView) convertView.findViewById(R.id.tmallIcon);
			holder.nickView = (TextView)convertView.findViewById(R.id.nick);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		JSONObject obj = (JSONObject)this.getItem(position);
		
		if (obj == null) {
			return null;
		}
		
		try {
			String tempImgURL = obj.getString("pic_path"); 
			String imgURL = tempImgURL.replace("60x60", "120x120");
			String name = obj.getString("title");
			String price = obj.getString("price");
			String isInLimitPromotion = obj.getString("isInLimitPromotion");
			String sold = obj.getString("sold");
			String location = obj.getString("location");
			String nick = obj.getString("nick");
			int index = location.indexOf(" ");
			String province;
			if (index >= 0) {
				province = location.substring(0, index);
			} else {
				province = location;
			}
			
			String isTmall = obj.getString("isB2c");
			
			holder.nameView.setText(name);
			holder.soldView.setText("ÊÛ³ö£º" + sold + "¼þ");
			holder.locationView.setText(province);
			holder.nickView.setText(nick);
			if (isInLimitPromotion.equals("false")) {
				holder.priceView.setText(price);
			} else {
				holder.priceBeforeRateView.setText(price);
				String priceAfterRate = obj.getString("priceWithRate");
				holder.priceView.setText(priceAfterRate);
			}
			
			if (isTmall.equals("1")) {
				holder.tmallIcon.setVisibility(View.VISIBLE);
			} else {
				holder.tmallIcon.setVisibility(View.GONE);
			}
			
			AsyncImageLoader.getInstance().loadImage(imgURL, holder.imgView, new InternetBitmapLoader());
			return convertView;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return convertView;
	}
	
	private static class ViewHolder {
		ImageView imgView;
		TextView nameView;
		TextView priceView;
		TextView priceBeforeRateView;
		TextView soldView;
		TextView locationView;
		ImageView tmallIcon;
		TextView nickView;
	}
}
