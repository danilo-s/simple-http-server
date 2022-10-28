package com.invidi.simplewebserver.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invidi.simplewebserver.annotations.Path;
import com.invidi.simplewebserver.annotations.QueryParam;
import com.invidi.simplewebserver.annotations.RestController;
import com.invidi.simplewebserver.context.WebServerContext;
import com.invidi.simplewebserver.model.HttpStatus;
import com.invidi.simplewebserver.model.RequestMethod;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;

public class SimpleWebServerThread implements Runnable {

    private Socket socket;
    private WebServerContext ws;
    static final String DEFAULT_FILE = "index.html";

    public SimpleWebServerThread(Socket socket, WebServerContext ws) {
        this.socket = socket;
        this.ws = ws;
    }

    @Override
    public void run() {
        try (BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {

            String input = reader.readLine();

            // we parse the request with a string tokenizer
            StringTokenizer tokenizer = new StringTokenizer(input);
            // we get file method
            String httpMethod = tokenizer.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get file requested
            String resourceRequested = tokenizer.nextToken().toLowerCase();

            if (resourceRequested.endsWith("/"))
                resourceRequested += DEFAULT_FILE;

            if (resourceRequested.startsWith("/api"))
                handleController(resourceRequested, httpMethod, out, dataOut);
            else {
                //static resource handling
                File file = new File(ws.getStaticPath(), resourceRequested);
                int fileLength = (int) file.length();
                String content = Utility.getContentType(resourceRequested);
                byte[] fileData = Utility.readFileData(file, fileLength);
                writeResponse(out, dataOut, HttpStatus.OK.toString(), content, fileLength, fileData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleController(String resourceRequested, String httpMethod, PrintWriter out, BufferedOutputStream dataOut) throws Exception {
        URI uri = new URI(resourceRequested);
        // /api/data
        String fullPath = uri.getPath();
        String path = fullPath.substring(fullPath.indexOf("/"), fullPath.lastIndexOf("/"));
        String methodPath = fullPath.substring(fullPath.lastIndexOf("/"), fullPath.length());

        Object controller = ws.getController();
        Class<?> cls = controller.getClass();
        Annotation controllerAnnotation = cls.getDeclaredAnnotations()[0];
        if (controllerAnnotation instanceof RestController) {
            RestController rc = (RestController) controllerAnnotation;
            String rcPath = rc.value();
            //controller for this path
            if (path.equals(rcPath)) {
                Method[] declaredMethods = cls.getDeclaredMethods();
                for (Method m : declaredMethods) {
                    Annotation methodAnnotation = m.getDeclaredAnnotations()[0];
                    Path p = (Path) methodAnnotation;
                    RequestMethod method = p.method();
                    String pathValue = p.value();
                    //method identified by path + http method
                    if (pathValue.equals(methodPath) && (method.toString().equals(httpMethod))) {
                        // key=a&value=a
                        String query = uri.getQuery();
                        Map<String, String> key2value = Utility.parseQueryString(query);

                        Annotation[][] parameterAnnotations = m.getParameterAnnotations();
                        String[] params = new String[parameterAnnotations.length];
                        for (int i = 0; i < parameterAnnotations.length; i++)
                            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                                QueryParam qp = (QueryParam) parameterAnnotations[i][j];
                                String queryKey = qp.value();
                                params[i] = key2value.get(queryKey);
                            }
                        m.setAccessible(true);
                        Object result = m.invoke(controller, params);
                        if(result!=null) {
                            ObjectMapper mapper = new ObjectMapper();
                            String resultAsString = mapper.writeValueAsString(result);
                            writeResponse(out, dataOut, HttpStatus.OK.toString(), "application/json", resultAsString.length(), resultAsString.getBytes(StandardCharsets.UTF_8));
                        }else {
                            writeResponse(out, dataOut, HttpStatus.OK.toString(), "application/json", 0, null);
               }
                        break;
                    }
                }
            }
        } else {
            //not implemented
            throw new UnsupportedOperationException("not implemented");
        }
    }

    private void writeResponse(PrintWriter out, BufferedOutputStream dataOut , String httpStatus, String content, int fileLength, byte[] data) throws IOException {
        // send HTTP Headers
        out.println("HTTP/1.1 " + HttpStatus.OK);
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer

        if(data!=null) {
            dataOut.write(data, 0, fileLength);
            dataOut.flush();
        }
    }
}