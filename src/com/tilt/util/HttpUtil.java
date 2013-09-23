package com.tilt.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public final class HttpUtil {
	private static final HttpClient httpClient = new DefaultHttpClient();
	public static final String TAOBAO_BASE_URL = "http://s.m.taobao.com/";
	public static final String BARCODE_BASE_URL = "http://setup.3533.com/ean/";
	
	public static String getRequest(String url) {
		HttpGet get = null;
		try {
			get = new HttpGet(url);
			HttpResponse response = httpClient.execute(get);
			final int statusCode = response.getStatusLine().getStatusCode();
			
			if (statusCode == HttpStatus.SC_OK) {
				String result = EntityUtils.toString(response.getEntity());
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (get != null) {
				get.abort();
			}
		}

		return null;
	}
}
