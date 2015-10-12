package com.bocai.ac.image;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.MalformedJsonException;

/**
 * This is a temporary solution. We will have a client for image service soon.
 *
 * @author Weizhi
 */
public class ImageServiceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageServiceConnector.class);

    private static String serviceURL;
    private static ConcurrentLinkedQueue<BaseImageData> queue;
    private static int maxQueueItems;
    private static ExecutorService executorService;
    private static boolean alive = true;

    public static boolean isAlive() {
        return ImageServiceConnector.alive;
    }

    public static ConcurrentLinkedQueue<BaseImageData> getQueue() {
        return ImageServiceConnector.queue;
    }

    public static ExecutorService getExecutor() {
        return ImageServiceConnector.executorService;
    }

    public static void initialize() {
        ImageServiceConnector.serviceURL = null; // TODO =ConfigCache.getConfigValue("image.service.endpoint");
        ImageServiceConnector.queue = new ConcurrentLinkedQueue<BaseImageData>();
        ImageServiceConnector.maxQueueItems = 1000; // TODO
    }

    /**
     * Add item to queue
     *
     * @param imageData
     */
    public static void enqueueImageData(final BaseImageData imageData) {

        if (ImageServiceConnector.queue.size() >= ImageServiceConnector.maxQueueItems) {
            ImageServiceConnector.LOGGER.warn(String.format("Image processiong queue full. Wait untill available..."));
        }
        while (ImageServiceConnector.queue.size() >= ImageServiceConnector.maxQueueItems) {
            try {
                Thread.sleep(1000);

            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        ImageServiceConnector.queue.add(imageData);
    }

    /**
     * Start process image
     *
     * @param executor
     */
    public static void start(final ExecutorService executor) {
        ImageServiceConnector.initialize();
        ImageServiceConnector.executorService = executor;
        ImageServiceConnector.executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        while (ImageServiceConnector.queue.isEmpty()) {
                            Thread.sleep(1000);
                        }
                        final BaseImageData imageData = ImageServiceConnector.queue.poll();
                        if (imageData != null) {
                            imageData.updateDb();
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                    if (ImageServiceConnector.queue.isEmpty()) {
                        System.out.println("ss");
                        ImageServiceConnector.alive = false;
                    }
                }
            }
        });
    }

    /**
     * @param imageFile
     * @return the imageId string
     */
    public static ImageServiceResultObject createImage(final File imageFile, final String mimeType) {
        final DefaultHttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        final String uri = String.format("%s/%s", ImageServiceConnector.serviceURL, "image/images/create?version=20120612");
        final HttpPost post = new HttpPost(uri);
        final MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

        // For File parameters
        entity.addPart(imageFile.getName(), new FileBody((imageFile), mimeType));
        post.setEntity(entity);
        HttpResponse response;
        JsonObject jsonObject = null;
        JsonElement returnElement = null;
        JsonElement idElement = null;
        JsonElement widthElement = null;
        JsonElement heightElement = null;

        // Retry four times when connection failure occurs
        for (int i = 0; i < 5; i++) {
            try {
                response = client.execute(post);
                jsonObject = ImageServiceConnector.parseJson(response.getEntity().getContent());
            } catch (final ClientProtocolException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
            if (entity != null) {
                // Do not need the rest
                post.abort();
            }
            if (jsonObject != null) {
                returnElement = jsonObject.get("return");
                if (returnElement != null) {
                    idElement = returnElement.getAsJsonObject().get("id");
                    widthElement = returnElement.getAsJsonObject().get("width");
                    heightElement = returnElement.getAsJsonObject().get("height");
                }
                if (idElement != null) {
                    return new ImageServiceResultObject(idElement.getAsString(), widthElement.getAsInt(), heightElement.getAsInt());
                } else {
                    ImageServiceConnector.LOGGER.warn(String.format("Image Service error response: %s", jsonObject.toString()));
                }
            }
            ImageServiceConnector.LOGGER.warn(String.format("Connection retry: %d", i));
        }
        return null;
    }

    private static JsonObject parseJson(final InputStream response) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        JsonObject jsonObject = null;
        try {
            final StringBuilder jsonStr = new StringBuilder();
            String jsonLine = null;
            while ((jsonLine = reader.readLine()) != null) {
                jsonStr.append(jsonLine);
            }

            final JsonParser jsonParser = new JsonParser();
            final JsonElement jsonElement = jsonParser.parse(jsonStr.toString());
            jsonObject = jsonElement.getAsJsonObject();
        } catch (final MalformedJsonException e) {
            ImageServiceConnector.LOGGER.warn("Image Service connection interrupted. Retrying");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
