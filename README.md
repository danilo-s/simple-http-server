# OVERVIEW
This is a simple application to store and retrieve values.

In order to start the application, run the main class MyWebApplication.java. Currently the server listens on port 8080 and supports HTTP protocol only.

The application can be tested by opening a browser window at the address http://localhost:8080/.
The application can also be tested by sending HTTP requests, for example with Postman. There are 2 operations currently supported:

* saveKeyAndValue, which can be tested by sending a HTTP POST request at the following address http://127.0.0.1:8080/api/data?key=key&value=value
* retrieveValue, which can be tested by sending a HTTP GET request at the following address http://127.0.0.1:8080/api/data?key=key
