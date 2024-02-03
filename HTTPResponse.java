import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


enum StatusCode {
    OK(200, "OK"), NOT_FOUND(404, "Not Found"), NOT_IMPLEMENTED(501, "Not Implemented"), BAD_REQUEST(400, "Bad Request"), INTERNAL_SERVER_ERROR(500, "Internal Server Error");

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

    Map<String, String> headers = new HashMap<>();

    private static final String[] HTML_SUFFIXES = {".html"};
    private static final String[] IMAGE_SUFFIXES = {".bmp", ".gif", ".png", ".jpg"};
    private static final String[] ICON_SUFFIXES = {".ico"};


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
        } else if (!httpRequest.isValid()) {
            handleBadRequest();
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
                    handleNotImplemented();    
                    break;
            }
        } 

        if (this.statusCode != StatusCode.OK) {
            response.append(getStatusCodeResponseLine());
        }
    }

    private void handleInternalServerError() {
        this.statusCode = StatusCode.INTERNAL_SERVER_ERROR;
    }

    private void handleBadRequest() {
        this.statusCode = StatusCode.BAD_REQUEST;
    }

    private void handlePostRequest() {
        this.statusCode = StatusCode.OK;
        
        // debug line
        System.out.println("POST request received:\n" + httpRequest.getParameters());
        
    }

    private void handleGetRequest() {
        String userHome = System.getProperty("user.home");
        String fullPath = TCPServerMultithreaded.ROOT + httpRequest.getRequestedResource();
        fullPath = fullPath.replaceFirst("^~", userHome);
        File requestedFile = new File(fullPath);

        if (requestedFile.exists() && requestedFile.isFile()) {
            handleFileExists(requestedFile);
        } else {
            handleFileNotFound();
        }
    }

    private void handleFileExists(File requestedFile) {
        String fileName = requestedFile.getName().toLowerCase();
        setContentTypeHeader(fileName);

        this.statusCode = StatusCode.OK;
        response.append(getStatusCodeResponseLine());

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        response.append("\r\n");

        try {
            byte[] fileContent = readFile(requestedFile);
            response.append(new String(fileContent, httpRequest.isImage() ? "ISO-8859-1" : "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleHeadRequest() {
        this.statusCode = StatusCode.OK;
        // todo: implement
    }

    private void handleTraceRequest() {
        this.statusCode = StatusCode.OK;
        // todo: implement
    }

    private void handleFileNotFound() {
        this.statusCode = StatusCode.NOT_FOUND;
    }

    private void handleNotImplemented() {
        this.statusCode = StatusCode.NOT_IMPLEMENTED;
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

    private void setContentTypeHeader(String fileName) {
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

    public StringBuilder getStatusCodeResponseLine() {
        return new StringBuilder("HTTP/1.1 ").append(statusCode.getCode()).append(" ").append(statusCode.getDescription()).append("\r\n");
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
