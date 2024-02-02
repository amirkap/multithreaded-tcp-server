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

    private StringBuilder response = new StringBuilder();

    private String contentType;

    Map<String, String> headers = new HashMap<>();


    public HTTPResponse(HTTPRequest httpRequest) {
        this.httpRequest = httpRequest;
        generateResponse();
    }

    private byte[] readFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bFile = new byte[(int) file.length()];

            while (fileInputStream.available() != 0) {
                fileInputStream.read(bFile, 0, bFile.length);
            }

            return bFile;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void generateResponse() {
        if (httpRequest == null) {
            this.statusCode = StatusCode.INTERNAL_SERVER_ERROR;
            response.append(getStatusCodeResponseLine());
            return;
        } else if (!httpRequest.isValid()) {
            this.statusCode = StatusCode.BAD_REQUEST;
            response.append("HTTP/1.1 ").append(statusCode.getCode()).append(" ").append(statusCode.getDescription()).append("\r\n");
            return;
        }

        switch (httpRequest.getType()) {
            case GET:
                this.statusCode = StatusCode.OK;
                String userHome = System.getProperty("user.home");
                String fullPath = TCPServerMultithreaded.ROOT + httpRequest.getRequestedResource();
                fullPath = fullPath.replaceFirst("^~", userHome);
                File requestedFile = new File(fullPath);

                if (requestedFile.exists() && requestedFile.isFile()) {
                    String fileName = requestedFile.getName().toLowerCase();
                    setContentTypeHeader(fileName);

                    response.append(getStatusCodeResponseLine());

                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
                    }
                    response.append("\r\n");

                    try {
                        byte[] fileContent = readFile(requestedFile);
                        response.append(new String(fileContent));
                    } catch (Exception e) {
                        this.statusCode = StatusCode.INTERNAL_SERVER_ERROR;
                        response.append(getStatusCodeResponseLine());
                    }

                } else {
                    this.statusCode = StatusCode.NOT_FOUND;
                    response.append(getStatusCodeResponseLine());
                }
                break;
            default:
                this.statusCode = StatusCode.NOT_IMPLEMENTED;
                response.append(getStatusCodeResponseLine());
        }

    }

    private void setContentTypeHeader(String fileName) {
        if (fileName.endsWith(".html")) {
            contentType = "text/html";
        } else if (fileName.endsWith(".bmp") || fileName.endsWith(".gif") || fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
            contentType = "image";
        } else if (fileName.endsWith(".ico")) {
            contentType = "icon";
        } else {
            contentType = "application/octet-stream";
        }

        headers.put("Content-Type", contentType);
    }

    public StringBuilder getStatusCodeResponseLine() {
        StringBuilder statusCodeResponseLine = new StringBuilder("HTTP/1.1 ");
        statusCodeResponseLine.append(statusCode.getCode()).append(" ").append(statusCode.getDescription()).append("\r\n");

        return statusCodeResponseLine;
    }

    public StringBuilder getResponse() {
        return response;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }
}
