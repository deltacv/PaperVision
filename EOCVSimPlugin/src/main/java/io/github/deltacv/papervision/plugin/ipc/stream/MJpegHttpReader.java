package io.github.deltacv.papervision.plugin.ipc.stream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation of a connection to motion jpeg (multipart/x-mixed-replace) stream, and using it as an Itarable like this:
 *   <pre>
 *   public static void main(String... strings) {
 *       VideoSource src = new VideoSource("http://91.85.203.9/axis-cgi/mjpg/video.cgi");
 *       try {
 *           src.start();
 *           for (byte[] img : src) {
 *               Files.write(Paths.get("c:/tmp/mjpeg/" + UUID.randomUUID().toString() + ".jpg"), img);
 *           }
 *       } catch (IOException e) {
 *           e.printStackTrace();
 *       }
 *   }
 *   </pre>
 *
 *
 * @author Arseny Kovalchuk<br/><a href="http://www.linkedin.com/in/arsenykovalchuk/">LinkedIn&reg; Profile</a>
 *
 */
public class MJpegHttpReader implements Iterable<byte[]> {

    private final static String MULTIPART_MIXED_REPLACE = "multipart/x-mixed-replace";
    private final static String BOUNDARY_PART = "boundary=";
    private final static String CONTENT_TYPE_HEADER = "content-length";

    private String boundaryPart;

    private final URL url;

    private HttpURLConnection connection;
    private BufferedInputStream inputStream;

    private boolean connected = false;

    public MJpegHttpReader(URL url) {
        this.url = url;
    }

    public MJpegHttpReader(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public URL getUrl() {
        return url;
    }

    private void start() {
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            inputStream = new BufferedInputStream(connection.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        connected = true;
    }

    public void stop() {
        try {
            if (inputStream != null) {
                inputStream.close();
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        connected = false;
    }

    public void run() {
        start();
        if (!connected)
            throw new IllegalStateException(
                    "connection lost immediately after connect");

        String contentType = connection.getContentType();
        if (contentType != null && !contentType.startsWith(MULTIPART_MIXED_REPLACE))
            throw new IllegalArgumentException("Unsupported Content-Type: " + contentType);


        boundaryPart = contentType.substring(contentType.indexOf(BOUNDARY_PART)
                + BOUNDARY_PART.length());
    }


    @Override
    public Iterator<byte[]> iterator() {
        try {
            return new ImagesIterator(boundaryPart, connection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class ImagesIterator implements Iterator<byte[]> {

        private final String boundary;

        private final InputStream stream;
        private boolean hasNext;

        private byte[] buffer = new byte[0];

        ImagesIterator(String boundaryPart, HttpURLConnection conn) throws IOException {
            // Some cameras provide Content-Type header with ; boundary=--myboundary,
            // then they use it as is without prefixing it with --
            this.boundary = boundaryPart.startsWith("--") ? boundaryPart : "--" + boundaryPart;
            this.stream = new BufferedInputStream(conn.getInputStream(), 8192);
            this.hasNext = true;
        }

        private String readLine() throws IOException {
            int capacity = 512;
            byte[] buffer = new byte[capacity];
            StringBuilder stringBuffer = new StringBuilder(512);

            for (; ; ) {
                stream.mark(capacity);
                int i = 0;

                for (; i < capacity; i++) {
                    byte LF = 0x0A;

                    if (buffer[i] == LF) {
                        stream.reset();
                        stream.read(buffer, 0, i + 1);
                        stringBuffer.append(new String(buffer, 0, i));
                        return stringBuffer.toString().trim();
                    }
                }
                stringBuffer.append(new String(buffer, 0, capacity));
            }

        }

        private void readUntilBoundary() throws IOException, InterruptedException {
            for(;;) {
                String s = readLine();
                if (boundary.equals(s) || !hasNext) {
                    break;
                } else if (s.equals(boundary + "--")) /* end of stream */{
                    hasNext = false;
                    break;
                }
            }
        }

        /**
         * Reads headers from the stream
         * @return
         * @throws IOException
         * @throws InterruptedException
         */
        private Map<String, String> readHeaders() throws IOException, InterruptedException {
            String line = null;
            Map<String, String> headers = new HashMap<>();
            for(;;) {
                line = readLine();

                if (line.trim().isEmpty()) {
                    return headers;
                } else {
                    String[] parts = line.split(": ");
                    headers.put(parts[0].toLowerCase(), parts[1]);
                }
            }
        }

        @Override
        public boolean hasNext() {
            synchronized (this)  {
                return this.hasNext;
            }
        }

        /**
         * Note! Throws RuntimeException(IOException | InterruptedException).
         *
         * It's usable especially in case of InterruptedException, when this source
         * is being to use in the thread like StreamThread
         */
        @Override
        public byte[] next() {
            synchronized (this) {
                try {
                    readUntilBoundary();
                    Map<String, String> headers = readHeaders();
                    String contentLength = headers.get(CONTENT_TYPE_HEADER);

                    int length = 0;
                    try {
                        length = Integer.parseInt(contentLength);
                    } catch (NumberFormatException e) {
                        return buffer;
                    }

                    if(buffer.length < length) {
                        buffer = new byte[length];
                    }

                    int bytes = 0;
                    while (bytes < length) {
                        bytes += stream.read(buffer, bytes, length - bytes);
                    }

                    return buffer;
                } catch (IOException | InterruptedException e) {
                    // e.printStackTrace();
                    // see StreamThread how it's to be used.
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void remove() {
        }

    }

}
