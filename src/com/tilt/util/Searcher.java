package com.tilt.util;

import org.json.JSONObject;

import com.tilt.algorithm.Util;

public class Searcher {
	private static final String[] SORT_METHODS = { "", "&sort=bid",
			"&sort=_sale", "&sort=_coefp", "&sort=_ratesum" };
	public static final int WITHOUT_SORT = 0; // 不排序
	public static final int SORT_BY_PRICE = 1; // 根据价格排序
	public static final int SORT_BY_SALE = 2; // 根据销量排序
	public static final int SORT_BY_COEFP = 3; // 根据人气排序
	public static final int SORT_BY_RATESUM = 4; // 根据信用排序

	/**
	 * searches the information of product in www.taobao.com
	 * 
	 * @param name
	 *            the name of the product
	 * @param page
	 *            the page of the product list
	 * @param num
	 *            number of products displayed per page
	 * @param sortMethod
	 *            WITHOUT_SORT : without sorting(default order) SORT_BY_PRICE :
	 *            sort by the price SORT_BY_SALE : sort by the sale (产品销量)
	 *            SORT_BY_COEFP : sort by the coefp (产品人气) SORT_BY_RATESUM :
	 *            sort by the ratesum (卖家信用)
	 * @return
	 */
	public static JSONObject search(final String name, final int page,
			final int num, final int sortMethod) {
		if ((name == null) || (sortMethod < 0)
				|| (sortMethod >= SORT_METHODS.length)) {
			throw new IllegalArgumentException();
		}

		try {
			String url = HttpUtil.TAOBAO_BASE_URL
					+ "search.htm?vm=nw&search_wap_mall=false&v=*&page=" + page
					+ "&q=" + name + "&n=" + num + SORT_METHODS[sortMethod];
			JSONObject object = new JSONObject(HttpUtil.getRequest(url));
			return object;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * gets the name of the product by the barcode number
	 * 
	 * @param barcode
	 *            the barcode number of the product
	 * @return the name of the product, return null if it cannot find the name
	 *         of the barcode
	 */
	public static String getNameOfBarcode(String barcode) {
		Util.checkArgumentsAreNotNull(barcode);

		try {
			String url = HttpUtil.BARCODE_BASE_URL + "index?keyword=" + barcode;
			JSONObject obj = new JSONObject(HttpUtil.getRequest(url));
			String name = obj.getString("name");
			return name;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
