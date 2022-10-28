package com.invidi.mywebproject;

import com.invidi.mywebproject.controllers.MyController;
import com.invidi.simplewebserver.annotations.RestController;
import com.invidi.simplewebserver.main.SimpleWebServer;
import com.invidi.simplewebserver.main.WebServer;

import java.io.File;

public class MyWebApplication {

   private final static String myPath = "my-web-project\\src\\main\\resources\\static";

   public static void main(String[] args)  throws Exception {
      final WebServer ws = new SimpleWebServer();

      // TODO: Set path for static files
      // TODO: Register controller MyController

      /*
       * Example:
       *
       *  ws.getWebContext().setStaticPath("/static");
       *  ws.getWebContext().addController(new MyController());
       */
      ws.getWebContext().setStaticPath(myPath);
      ws.getWebContext().addController(new MyController());
      ws.start(8080);
   }
}
