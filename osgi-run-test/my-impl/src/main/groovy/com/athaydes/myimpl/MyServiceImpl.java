package com.athaydes.myimpl;

import com.athaydes.myapi.MyService;

/**
 *
 */
public class MyServiceImpl implements MyService {

    @Override
    public String message() {
        return "This is a MyService implementation of class " + getClass().getName();
    }

}
