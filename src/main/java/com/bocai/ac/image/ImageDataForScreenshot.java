package com.bocai.ac.image;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bocai.ac.Utility;

/**
 * @author cschertz
 */
public class ImageDataForScreenshot extends BaseImageData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageDataForScreenshot.class);
    private static final Logger DATA_LOGGER = LoggerFactory.getLogger("DATA_LOGGER");

    private final int displayOrder;

    /**
     * Constructor
     *
     * @param imageFile
     * @param mimeType
     * @param dbConnection
     * @param tableName
     * @param packageName
     * @param versionName
     * @param sourceUrl
     * @param displayOrder
     */
    public ImageDataForScreenshot(final File imageFile, final String mimeType, final Connection dbConnection, final String tableName, final String packageName, final String versionName,
            final String sourceUrl, final int displayOrder) {
        super(imageFile, mimeType, dbConnection, tableName, packageName, versionName, sourceUrl);
        this.displayOrder = displayOrder;
    }

    @Override
    public void updateDb() {
        final ImageServiceResultObject resultObject = ImageServiceConnector.createImage(this.imageFile, this.mimeType);
        this.imageId = resultObject.getImageIdStr();
        if (this.imageId == null) {
            ImageDataForScreenshot.LOGGER.warn(String.format("Image Service create image failure. Image URL: {%s}", this.sourceUrl));
        } else {
            Statement dbStatement = null;
            try {
                dbStatement = this.dbConnection.createStatement();
                final String sql = String
                        .format("UPDATE %1$s SET imageId='%2$s', width = %3$d, height = %4$d WHERE packageName='%5$s' AND versionName='%6$s' AND source='%7$s' AND displayOrder ='%8$d';", this.tableName, this.imageId, resultObject
                                .getWidth(), resultObject.getHeight(), Utility.escapeTextForDB(this.packageName), Utility.escapeTextForDB(this.versionName), Utility.escapeTextForDB(this.sourceUrl), this.displayOrder);
                ImageDataForScreenshot.DATA_LOGGER.info(String.format("UpdateDB: %s", sql));
                if (dbStatement.executeUpdate(sql) != 1) {
                    throw (new RuntimeException("Database UPDATE failed: '" + sql + "'"));
                }

            } catch (final SQLException e) {
                e.printStackTrace();
            } finally {
                if (dbStatement != null) {
                    try {
                        dbStatement.close();
                        dbStatement = null;
                    } catch (final SQLException e) {
                    }
                }
            }
        }
    }
}
