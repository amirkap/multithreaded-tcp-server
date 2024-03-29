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
    private boolean isTimedOut = false;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private String body = "";

    public HTTPRequest(String httpRequest) {
        this.requestString = httpRequest;
        parseRequest(httpRequest);
    }

    public HTTPRequest(boolean isTimedOut) {
        this.isTimedOut = isTimedOut;
        this.requestString = "";
    }

    private void parseRequest(String httpRequest) {
        if (httpRequest.trim().isEmpty()) {
            this.isValid = false;
            return;
        }
        String[] parts = httpRequest.split("\r\n\r\n", 2);
        String[] requestLines = parts[0].split("\r\n");
        String requestLine = requestLines[0];
        String[] headerLines = Arrays.copyOfRange(requestLines, 1, requestLines.length);

        validateAndParseRequestLine(requestLine);
        parseHeaders(headerLines);

        if (parts.length > 1 && headers.containsKey("Content-Length")) {
            this.body = parts[1];
        }
        if (!this.body.isEmpty() && (this.type == RequestType.POST || this.type == RequestType.GET)) {
                parseParameters(this.body);
            }
        }

    private void validateAndParseRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ", -1);

        if (parts.length != 3) {
            this.isValid = false;
            return;
        }
        if (!"HTTP/1.0".equals(parts[2]) && !"HTTP/1.1".equals(parts[2])) {
            this.isValid = false;
            return;
        }
        try {
            this.type = RequestType.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            this.isImplemented = false;
            return;
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

    public boolean isTimedOut() {
        return isTimedOut;
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
