package com.cyanrocks.myhttputils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpDataInfo {
   private Map<String, List<String>> headerFields = new HashMap(2);
   private Integer responseCode;
   private Boolean chunked = false;
   private Long lenth;
   private String contentType;
   private String contentDisposition;
   private byte[] body;
   private int timeout;
   private int readtimeout;

   public Map<String, List<String>> getHeaderFields() {
      return this.headerFields;
   }

   public byte[] getBody() {
      return this.body;
   }

   public void setBody(byte[] body) {
      this.body = body;
   }

   public Boolean getChunked() {
      return this.chunked;
   }

   public void setChunked(Boolean chunked) {
      this.chunked = chunked;
   }

   public Long getLenth() {
      return this.lenth;
   }

   public String getContentType() {
      return this.contentType;
   }

   public void setContentType(String contentType) {
      this.contentType = contentType;
   }

   public void setLenth(Long lenth) {
      this.lenth = lenth;
   }

   public int getTimeout() {
      return this.timeout;
   }

   public Integer getResponseCode() {
      return this.responseCode;
   }

   public void setResponseCode(Integer responseCode) {
      this.responseCode = responseCode;
   }

   public void setTimeout(int timeout) {
      this.timeout = timeout;
   }

   public int getReadtimeout() {
      return this.readtimeout;
   }

   public void setReadtimeout(int readtimeout) {
      this.readtimeout = readtimeout;
   }

   public String getContentDisposition() {
      return this.contentDisposition;
   }

   public void setContentDisposition(String contentDisposition) {
      this.contentDisposition = contentDisposition;
   }

   public void setHeaderFields(Map<String, List<String>> headerFields) {
      this.headerFields = headerFields;
   }

   public HttpDataInfo(Map<String, List<String>> headerFields) {
      this.headerFields = headerFields;
   }

   public HttpDataInfo() {
   }
}
