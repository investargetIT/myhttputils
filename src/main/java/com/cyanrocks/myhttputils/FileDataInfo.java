package com.cyanrocks.myhttputils;

public class FileDataInfo {
   private byte[] filedata;
   private String filename;
   private String inputName;
   private String contentType;

   public byte[] getFiledata() {
      return this.filedata;
   }

   public void setFiledata(byte[] filedata) {
      this.filedata = filedata;
   }

   public String getFilename() {
      return this.filename;
   }

   public void setFilename(String filename) {
      this.filename = filename;
   }

   public String getInputName() {
      return this.inputName;
   }

   public void setInputName(String inputName) {
      this.inputName = inputName;
   }

   public FileDataInfo(byte[] filedata, String filename, String inputName, String contentType) {
      this.filedata = filedata;
      this.filename = filename;
      this.inputName = inputName;
      this.contentType = contentType;
   }

   public FileDataInfo() {
   }

   public String getContentType() {
      return this.contentType;
   }

   public void setContentType(String contentType) {
      this.contentType = contentType;
   }
}
