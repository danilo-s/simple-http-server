package com.invidi.simplewebserver.main;

import com.invidi.simplewebserver.context.WebServerContext;
import com.invidi.simplewebserver.context.WebServerContextImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SimpleWebServer implements WebServer  {

   private ServerSocket serverSocket;
   private WebServerContext webServerContext;

   public SimpleWebServer(){
      this.webServerContext = new WebServerContextImpl();
   }

   @Override
   public void start(int port) {
      //throw new UnsupportedOperationException("Please implement me :)");
      try {
         serverSocket = new ServerSocket(port);
         while(serverSocket.isBound() && !serverSocket.isClosed()) {
            SimpleWebServerThread simpleWebServerThread = new SimpleWebServerThread(serverSocket.accept(), webServerContext);
            new Thread(simpleWebServerThread).start();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void stop() {
      //throw new UnsupportedOperationException("Please implement me :)");
      try {
         serverSocket.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public WebServerContext getWebContext() {
      return this.webServerContext;
   }
}
