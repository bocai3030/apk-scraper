package com.bocai.ac.image;

public class ImageServiceResultObject {
	
	private final String imageIdStr;
	private final int width;
	private final int height;
	
	public ImageServiceResultObject ( String imageIdStr, int width, int height ) {
		this.imageIdStr = imageIdStr;
		this.width = width;
		this.height = height;
	}

	/**
	 * @return the imageIdStr
	 */
	public String getImageIdStr() {
		return imageIdStr;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

}
