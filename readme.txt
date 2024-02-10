Multi-Threaded TCP Server Project README
========================================

This project implements a multi-threaded TCP server capable of handling HTTP requests. The server is designed to efficiently process multiple client connections concurrently, using Java's concurrency features and socket programming.

Core Components
---------------

TCPServerMultithreaded.java:
----------------------------
This is the main server class responsible for initializing the server, listening for incoming connections, and handling these connections in separate threads. It utilizes an ExecutorService to manage a pool of threads, improving scalability and resource utilization.

HTTPRequest.java:
-----------------
This class parses incoming HTTP requests, extracting vital information such as request method, URI, headers, and body content. It supports various HTTP methods including GET, POST, HEAD, and TRACE, facilitating versatile request handling.

HTTPResponse.java:
------------------
Responsible for generating HTTP responses based on the processed requests. It sets appropriate status codes, headers, and body content, handling file serving and error reporting. Supports content type determination for text and binary files, and implements chunked transfer encoding for large files.

Design Overview
---------------

The server's design emphasizes concurrency and modularity. By processing each client connection in a separate thread, the server can handle multiple connections simultaneously without blocking. This design is particularly effective for I/O-bound operations such as network communication. Configuration flexibility is provided through an external config.ini file, allowing easy adjustments to server settings such as port number and root directory without modifying the source code.

External shell scripts (compile.sh and run.sh) streamline the compilation and execution process, ensuring that the server can be easily built and run from various environments. The config.ini file is used to configure server parameters like port number and document root, enhancing the server's flexibility and ease of use.

This README provides an overview of the project's implementation and design. For detailed information on the server's functionality and usage, refer to the source code and comments within each file.