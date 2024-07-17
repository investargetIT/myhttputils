package com.cyanrocks.myhttputils;

import java.util.Map;

public interface Interceptor {
   Map<String, String> headers();
}
