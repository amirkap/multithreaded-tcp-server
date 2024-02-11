import java.io.*;
import java.util.HashMap;
import java.util.Map;


enum StatusCode {
    OK(200, "OK"), NOT_FOUND(404, "Not Found"), NOT_IMPLEMENTED(501, "Not Implemented"), BAD_REQUEST(400, "Bad Request"), INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    REQUEST_TIMEOUT(408, "Request Timeout");

    private final int code;
    private final String description;

    StatusCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

public class HTTPResponse {
    private StatusCode statusCode;
    private final HTTPRequest httpRequest;
    private final StringBuilder response = new StringBuilder();
    private final StringBuilder responseLine = new StringBuilder();
    Map<String, String> headers = new HashMap<>();
    private final StringBuilder headersString = new StringBuilder();
    private StringBuilder body = new StringBuilder();
    private static final String[] HTML_SUFFIXES = {".html"};
    private static final String[] IMAGE_SUFFIXES = {".bmp", ".gif", ".png", ".jpg"};
    private static final String[] ICON_SUFFIXES = {".ico"};
    private static final int CHUNK_SIZE = 1000;

    private static final String PARAMS_INFO_HTML = "params_info.html";

    public HTTPResponse(HTTPRequest httpRequest) {
        this.httpRequest = httpRequest;
        generateResponse();
    }

    public void generateResponse() {
        try {
            handleRequest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRequest() {
        if (httpRequest == null) {
            handleInternalServerError();
        }
        else if (!httpRequest.isTimedOut()) {
            handleRequestTimeout();
        }
         else if (!httpRequest.isValid()) {
            handleBadRequest();
        } else if (!httpRequest.isImplemented()) {
            handleNotImplemented();
        } else {
            switch (httpRequest.getType()) {
                case GET:
                    handleGetRequest();
                    break;
                case POST:
                    handlePostRequest();
                    break;
                case HEAD:
                    handleHeadRequest();
                    break;
                case TRACE:
                    handleTraceRequest();
                    break;
                default:
                    handleInternalServerError();
                    break;
            }
        }

        setResponse();
    }

    private void handleInternalServerError() {
        this.statusCode = StatusCode.INTERNAL_SERVER_ERROR;
        setResponseLine();
    }

    private void handleBadRequest() {
        this.statusCode = StatusCode.BAD_REQUEST;
        setResponseLine();
    }

    private void handleRequestTimeout() {
        this.statusCode = StatusCode.REQUEST_TIMEOUT;
        setResponseLine();
    }

    private void handlePostRequest() {
        if (httpRequest.getRequestedResource().equals(PARAMS_INFO_HTML)) {
            this.statusCode = StatusCode.OK;
            setResponseLine();
            String fileName = httpRequest.getRequestedResource().toLowerCase();
            setContentTypeHeaderBasedOnFileName(fileName);
            body.append(embedParamsInHtml(getFullPathOfRequestedResource(), httpRequest.getParameters()));
        } else {
            File requestedFile = getFile();

            if (requestedFile.exists() && requestedFile.isFile()) {
                handleFileExists(requestedFile, true);
            }
        }
    }

    private void handleGetRequest() {
        File requestedFile = getFile();

        if (requestedFile.exists() && requestedFile.isFile()) {
            handleFileExists(requestedFile, true);
        } else {
            handleFileNotFound();
        }
    }

    private void handleHeadRequest() {
        File requestedFile = getFile();

        if (requestedFile.exists() && requestedFile.isFile()) {
            handleFileExists(requestedFile, false);
        } else {
            handleFileNotFound();
        }
    }

    private void handleTraceRequest() {
        this.statusCode = StatusCode.OK;
        setResponseLine();
        setContentLengthHeader(httpRequest.getRequestString());
        headers.put("Content-Type", "message/http");
        body.append(httpRequest.getRequestString());
    }

    private File getFile() {
        String fullPath = getFullPathOfRequestedResource();

        return new File(fullPath);
    }

    private String getFullPathOfRequestedResource() {
        String userHome = System.getProperty("user.home");
        String fullPath = TCPServerMultithreaded.ROOT + httpRequest.getRequestedResource();
        fullPath = fullPath.replaceFirst("^~", userHome);

        return fullPath;
    }

    private void handleFileExists(File requestedFile, boolean shouldSendContent) {
        String fileName = requestedFile.getName().toLowerCase();

        this.statusCode = StatusCode.OK;
        setResponseLine();

        setContentTypeHeaderBasedOnFileName(fileName);

        if (shouldSendContent) {
            try {
                byte[] fileContent = readFile(requestedFile);
                body.append(new String(fileContent, httpRequest.isImage() ? "ISO-8859-1" : "UTF-8"));
                if (!headers.get("Content-Type").equals("icon")) {
                    setContentLengthHeader(body.toString());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setHeadersString() {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headersString.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
    }

    private void handleFileNotFound() {
        this.statusCode = StatusCode.NOT_FOUND;
        setResponseLine();
    }

    private void handleNotImplemented() {
        this.statusCode = StatusCode.NOT_IMPLEMENTED;
        setResponseLine();
    }

    private byte[] readFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bFile = new byte[(int) file.length()];

            while (fileInputStream.available() != 0) {
                fileInputStream.read(bFile, 0, bFile.length);
            }

            fileInputStream.close();
            return bFile;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setContentTypeHeaderBasedOnFileName(String fileName) {
        String contentType;

        if (endsWithAny(fileName, HTML_SUFFIXES)) {
            contentType = "text/html";
        } else if (endsWithAny(fileName, IMAGE_SUFFIXES)) {
            contentType = "image";
        } else if (endsWithAny(fileName, ICON_SUFFIXES)) {
            contentType = "icon";
        } else {
            contentType = "application/octet-stream";
        }

        headers.put("Content-Type", contentType);
    }

    private void setContentLengthHeader(String responseBody) {
        headers.put("Content-Length", String.valueOf(responseBody.length()));
    }

    public void setResponseLine() {
        this.responseLine.append("HTTP/1.1 ").append(statusCode.getCode()).append(" ").append(statusCode.getDescription());
    }

    public void setResponse() {
        setHeadersString();
        manipulateBodyBasedOnChunkHeader();

        response.append(responseLine).append("\r\n").append(headersString).append("\r\n").append(body);
    }

    private void manipulateBodyBasedOnChunkHeader() {
        if (shouldUseChunkedEncoding()) {
            headers.put("Transfer-Encoding", "chunked");
            chunkBody();
        }
    }

    private boolean shouldUseChunkedEncoding() {
        if (httpRequest != null) {
            String chunkedHeader = httpRequest.getHeaders().get("chunked");
            return "yes".equalsIgnoreCase(chunkedHeader);
        }

        return false;
    }

    private void chunkBody() {
        int index = 0;
        StringBuilder chunkedBody = new StringBuilder();

        while (index < body.length()) {
            int endIndex = Math.min(index + CHUNK_SIZE, body.length());
            String chunk = body.substring(index, endIndex);

            chunkedBody.append(Integer.toHexString(chunk.length())).append("\r\n");
            chunkedBody.append(chunk).append("\r\n");

            index += CHUNK_SIZE;
        }

        chunkedBody.append("0\r\n\r\n");

        body = new StringBuilder(chunkedBody.toString());
    }

    private String embedParamsInHtml(String filePath, Map<String, String> params) {
        StringBuilder htmlContentBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean bodyTagFound = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains("</body>")) {
                    bodyTagFound = isBodyTagFoundLogic(params, htmlContentBuilder);
                }
                if (!bodyTagFound) {
                    htmlContentBuilder.append(line).append("\n");
                } else {
                    htmlContentBuilder.append(line).append("\n");
                    bodyTagFound = false;
                }
            }

            return htmlContentBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isBodyTagFoundLogic(Map<String, String> params, StringBuilder htmlContentBuilder) {
        boolean bodyTagFound;
        bodyTagFound = true;
        StringBuilder paramsBuilder = new StringBuilder();
        paramsBuilder.append("<table border=\"1\">\n<tr>\n<th>Parameter Name</th>\n<th>Parameter Value</th>\n</tr>\n");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramsBuilder.append("<tr><td>")
                    .append(entry.getKey())
                    .append("</td><td>")
                    .append(entry.getValue())
                    .append("</td></tr>\n");
        }
        paramsBuilder.append("</table>\n");
        htmlContentBuilder.append(paramsBuilder.toString());
        return bodyTagFound;
    }

    public StringBuilder getResponse() {
        return response;
    }

    private static boolean endsWithAny(String value, String[] suffixes) {
        for (String suffix : suffixes) {
            if (value.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
