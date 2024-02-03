import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

enum RequestType {
    GET, POST, HEAD, TRACE
}

public class HTTPRequest {
    private RequestType type;
    private String requestString;
    private String requestedResource;
    private boolean isImage;
    private boolean isValid = true;
    // private int contentLength;
    // private String referer;
    // private String userAgent;
    private Map<String, String> parameters = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private String body = "";

    public HTTPRequest(String httpRequest) {
        this.requestString = httpRequest;
        parseRequest(httpRequest);
    }

    private void parseRequest(String httpRequest) {
        String[] parts = httpRequest.split("\r\n\r\n", 2); // Split headers and body
        String[] headerLines = parts[0].split("\r\n");
        boolean validRequestTypeFound = false;

        // Process headers
        for (String line : headerLines) {
            if (Arrays.stream(RequestType.values()) // request can be valid even if request line is not the first line (?)
                    .anyMatch(requestType -> line.startsWith(requestType.name()))) {
                validRequestTypeFound = true;
                parseRequestLine(line);
            } else {
                String[] headerParts = line.split(": ", 2);
                if (headerParts.length == 2) {
                    this.headers.put(headerParts[0], headerParts[1]);
                }
            }
        }

        if (!validRequestTypeFound) {
            isValid = false;
            return;
        }

        // Parse body if present and Content-Length is set
        if (parts.length > 1 && headers.containsKey("Content-Length")) {
             // this.contentLength = Integer.parseInt(headers.get("Content-Length"));
            this.body = parts[1];
        }
            // Check Content-Type and parse accordingly
            String contentType = headers.get("Content-Type");
            if (contentType != null) {
                if (contentType.contains("application/x-www-form-urlencoded")) {
                    parseParameters(this.body);
                }
            }
    }

    private void parseRequestLine(String line) {
        String[] parts = line.split(" ");
        if (parts.length > 1) {
            this.type = RequestType.valueOf(parts[0]);
            String url = parts[1];

            url = url.replaceAll("/\\.\\./", "/");

            this.requestedResource = (url.contains("?"))
                    ? (Objects.equals(url.split("\\?")[0], "/") ? TCPServerMultithreaded.DEFAULT_PAGE : url.split("\\?")[0].substring(1))
                    : (Objects.equals(url, "/") ? TCPServerMultithreaded.DEFAULT_PAGE : url.substring(1));
            if (url.contains("?")) {
                parseParameters(url.split("\\?")[1]);
            }

            this.isImage = requestedResource.matches(".*\\.(jpg|bmp|gif)$");
        }
    }

    private void parseParameters(String paramString) {
        String[] pairs = paramString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length > 1) {
                this.parameters.put(keyValue[0], keyValue[1]);
            } else {
                this.parameters.put(keyValue[0], "");
            }
        }
    }

    public RequestType getType() {
        return type;
    }

    public String getRequestString() {
        return requestString;
    }
    public String getRequestedResource() {
        return requestedResource;
    }

    public boolean isImage() {
        return isImage;
    }

    // public int getContentLength() {
    //     return contentLength;
    // }

    // public String getReferer() {
    //     return referer;
    // }

    // public String getUserAgent() {
    //     return userAgent;
    // }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public boolean isValid() {
        return isValid;
    }
}
