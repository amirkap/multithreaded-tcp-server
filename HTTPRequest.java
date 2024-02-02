import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

enum RequestType {
    GET, POST, HEAD, TRACE
}

public class HTTPRequest {
    private RequestType type;
    private String requestedResource;
    private boolean isImage;
    private boolean isValid = true;
    private int contentLength;
    private String referer;
    private String userAgent;
    private Map<String, String> parameters;

    public HTTPRequest(String httpRequest) {
        parameters = new HashMap<>();
        parseRequest(httpRequest);
    }

    private void parseRequest(String httpRequest) {
        String[] lines = httpRequest.split("\r\n");
        boolean validRequestTypeFound = false;

        for (String line : lines) {
            if (Arrays.stream(RequestType.values())
                    .anyMatch(requestType -> line.startsWith(requestType.name()))) {
                validRequestTypeFound = true;
                parseRequestLine(line);
            } else if (line.startsWith("Content-Length:")) {
                this.contentLength = Integer.parseInt(line.substring(15).trim());
            } else if (line.startsWith("Referer:")) {
                this.referer = line.substring(8).trim();
            } else if (line.startsWith("User-Agent:")) {
                this.userAgent = line.substring(11).trim();
            }
        }

        if (!validRequestTypeFound) {
            isValid = false;
        }
    }

    private void parseRequestLine(String line) {
        String[] parts = line.split(" ");
        if (parts.length > 1) {
            this.type = RequestType.valueOf(parts[0]);
            String url = parts[1];

            url = url.replaceAll("/\\.\\./", "/");

            if (url.contains("?")) {
                String[] urlParts = url.split("\\?");
                this.requestedResource = urlParts[0];
                parseParameters(urlParts[1]);
            } else {
                this.requestedResource = url;
            }
            this.isImage = requestedResource.matches(".*\\.(jpg|bmp|gif)$");
        }
    }

    private void parseParameters(String paramString) {
        String[] pairs = paramString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
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

    public String getRequestedResource() {
        return requestedResource;
    }

    public boolean isImage() {
        return isImage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public boolean isValid() {
        return isValid;
    }
}
