package com.tilt.barcode;

public class Barcode {
	private String content;
	private boolean decoded;
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return this.content;
	}
	
	public void setDecoded(boolean decoded) {
		this.decoded = decoded;
	}
	
	public boolean isDecoded() {
		return this.decoded;
	}
}
