package com.invidi.simplewebserver.context;

import com.invidi.simplewebserver.annotations.Path;
import com.invidi.simplewebserver.annotations.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class WebServerContextImpl implements WebServerContext{

    private String staticPath;
    private Object controller;

    @Override
    public String getStaticPath(){
        return this.staticPath;
    }

    @Override
    public void setStaticPath(String staticPath) {
        this.staticPath = staticPath;
    }

    @Override
    public Object getController(){
        return this.controller;
    }

    @Override
    public void addController(Object controller){
        this.controller = controller;
    }
}
