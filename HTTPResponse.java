import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

enum StatusCode {
    OK(200, "OK"),
    NOT_FOUND(404, "Not Found"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_REQUEST(400, "Bad Request"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

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
    private final StatusCode statusCode;
    private HTTPRequest httpRequest;

    public HTTPResponse(StatusCode statusCode, HTTPRequest httpRequest) {
        this.statusCode = statusCode;
        this.httpRequest = httpRequest;
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
}
