package com.bocai.ac;

import java.io.File;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public abstract class Utility {
    private static final Pattern _PACKAGE_NAME_PATTERN = Pattern.compile("details\\?id=(.*?)(&|\"|$)", Pattern.DOTALL | Pattern.MULTILINE);

    public static String xmlToString(final Node node) throws TransformerException {
        final Source source = new DOMSource(node);
        final StringWriter stringWriter = new StringWriter();
        final Result result = new StreamResult(stringWriter);
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(source, result);
        return (stringWriter.getBuffer().toString());
    }

    public static String getAndroidPackageNameFromURL(final String url) {
        final Matcher matcher = Utility._PACKAGE_NAME_PATTERN.matcher(url);
        try {
            if (!matcher.find()) {
                throw (new IllegalStateException());
            }
            return (matcher.group(1));
        } catch (final Exception err) {
            throw (new RuntimeException("Failed to parse a package name from the source URL '" + url + "'", err));
        }
    }

    public static String pickVersionCode(final String versionName, final Map<String, String> versionInfo) {
        String versionCode = null;
        if (versionInfo != null) {
            for (final String code : versionInfo.keySet()) {
                final String name = versionInfo.get(code);
                if (name.equals(versionName)) {
                    versionCode = code;
                    break;
                }
            }
        }
        return (versionCode);
    }

    public static String escapeTextForDB(String sourceText) {
        if (sourceText == null) {
            return (null);
        }

        // sourceText = sourceText.replaceAll("[^\\u0000-\\uFFFF]", "\uFFFD"); // advised by http://stackoverflow.com/questions/4035562/java-regex-match-characters-outside-basic-multilingual-plane
        sourceText = sourceText.replaceAll("[^\\u0000-\\uFFFF]", "\u0020"); // replace it by blank

        return ((sourceText.replace("\\", "\\\\")).replace("'", "\\'"));
    }

    public static String md5FilePath(final File imagesFolder, final String packageName) {
        try {
            // Calculate md5( package name )
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(packageName.getBytes("UTF-8"));
            final BigInteger bi = new BigInteger(1, md.digest());
            final String hash = bi.toString(16);

            // Create destination folders and return full path
            final File imagesHashed = new File(new File(new File(imagesFolder, hash.substring(0, 1)), hash.substring(1, 2)), hash);
            if (imagesHashed.mkdirs() || imagesHashed.isDirectory()) {
                return new File(imagesHashed, UUID.randomUUID().toString()).toString();
            } else {
                return null;
            }

        } catch (final Throwable e) {
            throw (new RuntimeException(e));
        }

    }

}
