package com.bocai.ac.content;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

public class ContentRegExMultiImage extends ContentRegExMulti {

    protected ContentRegExMultiImage(final String name, final String type, final String op, final Node xml) {
        super(name, type, op, xml);
    }

    /**
     * @param transactionId
     * @param sourceUrl
     * @param filePath
     * @return The content type for createImage() to avoid another connection
     * @throws IOException
     * @throws TransformerException
     */
    public String saveImageToFile(final String sourceUrl, final String filePath) throws IOException, TransformerException {

        final URL url = new URL(sourceUrl);
        final URLConnection connection = url.openConnection();
        final String contentType = connection.getContentType();
        final int contentLength = connection.getContentLength();
        if ((contentType.startsWith("text/"))) {
            throw (new IOException(String.format("The image URL '%s' does not resolve to binary data", url.toString())));
        }

        InputStream raw = null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            if (contentLength > 0) {
                raw = connection.getInputStream();
                in = new BufferedInputStream(raw);
                final byte[] data = new byte[contentLength];
                int bytesRead = 0;
                int offset = 0;
                while (offset < contentLength) {
                    bytesRead = in.read(data, offset, data.length - offset);
                    if (bytesRead == -1) {
                        break;
                    }
                    offset += bytesRead;
                }
                in.close();

                if (offset != contentLength) {
                    throw (new IOException(String.format("While resolving image URL '%s' %d bytes where received when %d bytes where expected", url.toString(), offset, contentLength)));
                }

                final File outFile = new File(filePath);
                outFile.delete();
                out = new FileOutputStream(filePath);
                out.write(data);
            } else {
                raw = connection.getInputStream();
                in = new BufferedInputStream(raw);
                final byte[] data = new byte[2048];

                final File outFile = new File(filePath);
                outFile.delete();
                out = new FileOutputStream(filePath);

                int bytesRead = 0;
                while ((bytesRead = in.read(data)) != -1) {
                    out.write(data, 0, bytesRead);
                }
                in.close();
            }
        } finally {
            if (raw != null) {
                raw.close();
                raw = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
        return contentType;
    }

    public int getOrientation(final String transactionId, final File imageFile, final String imageType) {
        return 0; // portrait by default
    }
}
