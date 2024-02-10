Multi-Threaded TCP Server Project
=================================

Authors: Tom Shlaim and Amir Kaplan

This project implements a multi-threaded TCP server capable of handling HTTP requests. 
The server is designed to efficiently process multiple client connections concurrently, using Java's concurrency features and socket programming.

Core Components
---------------

TCPServerMultithreaded.java:
----------------------------
This is the main server class responsible for initializing the server, listening for incoming connections, and handling these connections in separate threads.
It utilizes an ExecutorService to manage a pool of threads, improving scalability and resource utilization.

HTTPRequest.java:
-----------------
This class parses incoming HTTP requests, extracting vital information such as request method, URI, headers, and body content.
It supports various HTTP methods including GET, POST, HEAD, and TRACE, facilitating versatile request handling.

HTTPResponse.java:
------------------
Responsible for generating HTTP responses based on the processed requests.
It sets appropriate status codes, headers, and body content, handling file serving and error reporting.
 Supports content type determination for text and binary files, and implements chunked transfer encoding.

Design Overview
---------------

The server's design emphasizes concurrency and modularity. 
By processing each client connection in a separate thread, the server can handle multiple connections simultaneously without blocking.
This design is particularly effective for I/O-bound operations such as network communication.
Configuration flexibility is provided through an external config.ini file, allowing easy adjustments to server settings such as port number and root directory without modifying the source code.

Server Flow:
------------
1. A client connects to the server, and a new thread is allocated for handling the client's request.
2. The 'HTTPRequest' class parses the incoming request, identifying the method, URI, and any headers or content.
3. Based on the parsed request, the server processes it, potentially accessing files or resources as needed.
4. The 'HTTPResponse' class constructs an appropriate response, which includes setting the status code, headers, and any response body content.
5. The response is sent back to the client, and the connection is closed or kept alive for further requests, depending on the HTTP headers.

External shell scripts (compile.sh and run.sh) streamline the compilation and execution process, ensuring that the server can be easily built and run from various environments.