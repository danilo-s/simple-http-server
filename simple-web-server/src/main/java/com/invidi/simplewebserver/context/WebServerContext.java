package com.invidi.simplewebserver.context;

import com.invidi.simplewebserver.annotations.RestController;

public interface WebServerContext {

   // TODO: Add methods for supporting static resources and controller mapping
    public void setStaticPath(String path);
    public String getStaticPath();
    public void addController(Object controller);
    public Object getController();

}
