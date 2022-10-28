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
            String method = tokenizer.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get file requested
            String fileRequested = tokenizer.nextToken().toLowerCase();

            if (fileRequested.endsWith("/"))
                fileRequested += DEFAULT_FILE;

            if (fileRequested.startsWith("/api"))
                handleController(fileRequested, method, out, dataOut);
            else {
                //resource handling
                File file = new File(ws.getStaticPath(), fileRequested);
                int fileLength = (int) file.length();
                String content = Utility.getContentType(fileRequested);
                byte[] fileData = Utility.readFileData(file, fileLength);

                // send HTTP Headers
                out.println("HTTP/1.1 " + HttpStatus.OK);
                out.println("Date: " + new Date());
                out.println("Content-type: " + content);
                out.println("Content-length: " + fileLength);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer

                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleController(String fileRequested, String httpMethod, PrintWriter out, BufferedOutputStream dataOut) throws Exception {
        URI uri = new URI(fileRequested);
        // /api/data
        String fullPath = uri.getPath();
        String path = fullPath.substring(fullPath.indexOf("/"), fullPath.lastIndexOf("/"));
        String methodPath = fullPath.substring(fullPath.lastIndexOf("/"), fullPath.length());
        // key=a&value=a
        String query = uri.getQuery();
        Map<String, String> key2value = Utility.parseQueryString(query);

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
                    //method for path + http method
                    if (pathValue.equals(methodPath) && (method.toString().equals(httpMethod))) {
                        Annotation[][] parameterAnnotations = m.getParameterAnnotations();
                        String[] params = new String[parameterAnnotations.length];
                        for (int i = 0; i < parameterAnnotations.length; i++)
                            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                                QueryParam qp = (QueryParam) parameterAnnotations[i][j];
                                String value = qp.value();
                                params[i] = key2value.get(value);
                            }
                        m.setAccessible(true);
                        Object result = m.invoke(controller, params);
                        if(result!=null) {
                            ObjectMapper mapper = new ObjectMapper();
                            String object = mapper.writeValueAsString(result);
                            out.println("HTTP/1.1 " + HttpStatus.OK);
                            out.println("Date: " + new Date());
                            out.println("Content-type: application/json");
                            //out.println("Content-length: " + object.length());
                            out.println(); // blank line between headers and content, very important !
                            out.flush(); // flush character output stream buffer
                            dataOut.write(object.getBytes(StandardCharsets.UTF_8), 0, object.length());
                            dataOut.flush();
                        }else {
                            out.println("HTTP/1.1 " + HttpStatus.OK);
                            out.println("Date: " + new Date());
                            out.println("Content-type: application/json");
                            //out.println("Content-length: " + object.length());
                            //out.println(object);
                            out.println(); // blank line between headers and content, very important !
                            out.flush(); // flush character output stream buffer

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
}