package com.bocai.ac.image;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to capture image metadata
 * @author cschertz
 *
 */
public class ImageMetadata {
    
    private final Map<String, String> data = new HashMap<String, String>();
    private boolean isScreenShot;
    private boolean foundMatch = false;

    /**
     * Construct an ImageMetadata class
     * @param results
     * @param screenShot
     * @throws SQLException
     */
    public ImageMetadata(ResultSet results, boolean screenShot) throws SQLException {
        // TODO Auto-generated constructor stub
        
        isScreenShot = screenShot;
        
        ResultSetMetaData resultsInfo = results.getMetaData();
        
        
        for (int colIndex = 1; colIndex <= resultsInfo.getColumnCount(); colIndex++) {
            data.put(resultsInfo.getColumnName(colIndex), results.getString(colIndex));
        }
        
        String imageType = data.get("imageType");
        
        if (screenShot == false) {
            if (imageType != null) {
                if (imageType.equalsIgnoreCase("screenshot")) {
                    isScreenShot = true;
                }
            }
        }
    }
    

    
    
    /**
     * @return  the key to use for this object
     */
    public String getKey() {
        return getSource();
    }
    
    
    /**
     * @return the image source URL 
     */
    public String getSource() {
        return data.get("source");
    }
    
    /**
     * @return the image ID
     */
    public String getImageId() {
        return data.get("imageId");
    }
    
    /**
     * @return the image type 
     */
    public String getImageType() {
        return data.get("imageType");
    }
    
    
    /**
     * 
     * @return the path to the image
     */
    public String getImagePath() {
        return data.get("imagePath");
    }
    
    /**
     * @return the display order for a screen shot 
     */
    public String getDisplayOrder() {
        return data.get("displayOrder");
    }

    /**
     *  
     * @return if we found an image that maches this one 
     *
     */
    public boolean getFoundMatch() {
        return foundMatch;
    }
    
    /**
     * Set that we found a match for the image 
     * @param value
     */
    public void setFoundMatch(boolean value) {
        foundMatch = value;
    }
    
    /**
     * @return true if this image is a screen shot
     */
    public boolean isScreenShot() {
        return isScreenShot;
    }
    
}
