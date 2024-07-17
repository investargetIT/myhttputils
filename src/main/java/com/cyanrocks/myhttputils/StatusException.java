package com.cyanrocks.myhttputils;

public class StatusException extends RuntimeException {
   private Integer statusCode;
   private static final long serialVersionUID = -150948622665534019L;

   public Integer getStatusCode() {
      return this.statusCode;
   }

   public void setStatusCode(Integer statusCode) {
      this.statusCode = statusCode;
   }

   public StatusException(Throwable cause, Integer statusCode) {
      super(cause);
      this.statusCode = statusCode;
   }
}
