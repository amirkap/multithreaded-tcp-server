import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

enum RequestType {
    GET, POST, HEAD, TRACE
}

public class HTTPRequest {
    private RequestType type;
    private final String requestString;
    private String requestedResource;
    private boolean isImage;
    private boolean isValid = true;
    private boolean isImplemented = true;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private String body = "";

    public HTTPRequest(String httpRequest) {
        this.requestString = httpRequest;
        parseRequest(httpRequest);
    }

    private void parseRequest(String httpRequest) {
        String[] parts = httpRequest.split("\r\n\r\n", 2);
        String[] requestLines = parts[0].split("\r\n");
        String requestLine = requestLines[0];
        String[] headerLines = Arrays.copyOfRange(requestLines, 1, requestLines.length);

        validateAndParseRequestLine(requestLine);
        parseHeaders(headerLines);

        if (parts.length > 1 && headers.containsKey("Content-Length")) {
            this.body = parts[1];
        }
        String contentType = headers.get("Content-Type");
        if (contentType != null) {
            if (contentType.contains("application/x-www-form-urlencoded")) {
                parseParameters(this.body);
            }
        }
    }

    private void validateAndParseRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");

        if (parts.length != 3) {
            this.isValid = false;
        }

        try {
            this.type = RequestType.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            this.isImplemented = false;
        }

        String url = parts[1];
        url = url.replaceAll("/\\.\\./", "/");
        this.requestedResource = (url.contains("?"))
                ? (Objects.equals(url.split("\\?")[0], "/") ? TCPServerMultithreaded.DEFAULT_PAGE : url.split("\\?")[0].substring(1))
                : (Objects.equals(url, "/") ? TCPServerMultithreaded.DEFAULT_PAGE : url.substring(1));
        if (url.contains("?")) {
            parseParameters(url.split("\\?")[1]);
        }

        this.isImage = requestedResource.matches(".*\\.(jpg|bmp|gif)$");

        if (!"HTTP/1.0".equals(parts[2]) && !"HTTP/1.1".equals(parts[2])) {
            this.isValid = false;
        }
    }

    private void parseHeaders(String[] lines) {
        for (String line : lines) {
            String[] headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                this.headers.put(headerParts[0], headerParts[1]);
            }
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

    public Map<String, String> getParameters() {
        return parameters;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean isImplemented() {
        return isImplemented;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
