package com.bocai.ac.image;

import java.io.File;
import java.sql.Connection;

/**
 * @author cschertz
 *
 */
public abstract class BaseImageData {
	protected final File imageFile;
	protected final String mimeType;
	protected final String tableName;
	protected final String packageName;
	protected final String versionName;
	protected final String sourceUrl;
	protected Connection dbConnection;
	protected String imageId;
	
	/**
	 * Constructor
	 * @param imageFile
	 * @param mimeType
	 * @param dbConnection 
	 * @param tableName 
	 * @param packageName
	 * @param versionName
	 * @param sourceUrl
	 */
	public BaseImageData (File imageFile, String mimeType, Connection dbConnection, String tableName, String packageName, String versionName, String sourceUrl) {
		this.imageFile = imageFile;
		this.mimeType = mimeType;
		this.dbConnection = dbConnection;
		this.tableName = tableName;
		this.packageName = packageName;
		this.versionName = versionName;
		this.sourceUrl = sourceUrl;
		imageId = null;
	}
	
	/**
	 * update image
	 */
	public abstract void updateDb();
	
	/**
	 * @return the image id after the update has happened 
	 */
	public String getImageId() {
	    return imageId;
	}

}
