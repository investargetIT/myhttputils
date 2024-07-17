package com.cyanrocks.myhttputils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class MyHttp {
   public static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
   public static final String MULTIPART_CONTENT_TYPE = "multipart/form-data;charset=UTF-8";
   public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";
   public static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";
   public static final String TEXTPLAN_TYPE = "text/plain; charset=UTF-8";
   private static final int D_TIMEOUT = 6000;
   private static final int READ_TIMEOUT = 72000;
   private static final String CRLF = "\r\n";

   private static URLConnection openConnection(String url, Proxy proxy) {
      try {
         return proxy != null ? (new URL(url)).openConnection(proxy) : (new URL(url)).openConnection();
      } catch (IOException var3) {
         IOException e = var3;
         throw new IllegalStateException(e);
      }
   }

   public static int getResponseCode(String url, String contentType, String cookie, Proxy proxy) {
      return getResponseCode(url, contentType, cookie, proxy, 6000);
   }

   public static long getContentLength(URLConnection conn) {
      String cl = conn.getHeaderField("Content-Length");
      return cl != null && cl.trim().length() > 0 ? Long.valueOf(cl) : 0L;
   }

   public static String getContentType(URLConnection conn) {
      return conn.getHeaderField("Content-Type");
   }

   public static String getContentDisposition(URLConnection conn) {
      return conn.getHeaderField("Content-Disposition");
   }

   public static String getSetCookie(URLConnection conn) {
      return conn.getHeaderField("Set-Cookie");
   }

   public static String getFileName(URLConnection conn) {
      String cd = conn.getHeaderField("Content-Disposition");
      return cd != null && cd.contains("attachment") && cd.contains("filename") ? cd.split(";")[1].split("=")[1].replaceAll("[\"']", "") : null;
   }

   public static int getResponseCode(String url, String contentType, String cookie, Proxy proxy, int timeout) {
      URLConnection conn = getUrlConnection(url, contentType, cookie, proxy, timeout, 72000);
      return getResponseCode(conn);
   }

   public static String getLocation(URLConnection conn) {
      return conn.getHeaderField("Location");
   }

   public static Map<String, List<String>> getHeaders(URLConnection conn) {
      Map<String, List<String>> hds = conn.getHeaderFields();
      return hds;
   }

   public static int getResponseCode(URLConnection conn) {
      String headerField = conn.getHeaderField(0);
      return headerField != null ? Integer.valueOf(headerField.split("\\s+")[1]) : -1;
   }

   public static URLConnection getURLConnection(String url, String contentType, String cookie, int timeout) {
      return getUrlConnection(url, contentType, cookie, (Proxy)null, timeout, 72000);
   }

   public static URLConnection getURLConnection(String url, String contentType, String cookie) {
      return getUrlConnection(url, contentType, cookie, (Proxy)null, 6000, 72000);
   }

   public static URLConnection getUrlConnection(String url, String contentType, String cookie, Proxy proxy) {
      return getUrlConnection(url, contentType, cookie, proxy, 6000, 72000);
   }

   public static URLConnection getUrlConnection(String url, String contentType, String cookie, Proxy proxy, int timeout, int readtimeout) {
      Map<String, String> heads = getHeads(contentType, cookie);
      return getUrlConnection(url, proxy, timeout, heads, readtimeout);
   }

   public static URLConnection getUrlConnection(String url, String contentType, String cookie, Proxy proxy, int timeout, int readtimeout, String remoteIp, String userAgent) {
      Map<String, String> heads = getHeads(contentType, cookie);
      if (remoteIp != null && remoteIp.trim().length() > 1) {
         heads.put("X-Real-IP", remoteIp);
      }

      if (userAgent != null && userAgent.trim().length() > 1) {
         heads.put("User-Agent", userAgent);
      }

      return getUrlConnection(url, proxy, timeout, heads, readtimeout);
   }

   public static URLConnection getUrlConnection(String url, Proxy proxy, int timeout, Map<String, String> heads, int readTimeout) {
      try {
         URLConnection conn = openConnection(url, proxy);
         if (timeout > 0) {
            conn.setConnectTimeout(timeout);
         } else {
            conn.setConnectTimeout(6000);
         }

         if (readTimeout > 0) {
            conn.setReadTimeout(readTimeout);
         } else {
            conn.setReadTimeout(72000);
         }

         if (heads != null && heads.size() > 0) {
            Set<Map.Entry<String, String>> ens = heads.entrySet();
            Iterator var7 = ens.iterator();

            while(var7.hasNext()) {
               Map.Entry<String, String> en = (Map.Entry)var7.next();
               if (en.getValue() != null) {
                  conn.setRequestProperty((String)en.getKey(), (String)en.getValue());
               }
            }

            if (ens.stream().noneMatch((k) -> {
               return ((String)k.getKey()).trim().equalsIgnoreCase("User-Agent");
            })) {
               conn.setRequestProperty("User-Agent", getUserAgent());
            }
         } else {
            conn.setRequestProperty("User-Agent", getUserAgent());
         }

         setHeader(conn);
         conn.setRequestProperty("Connection", "close");
         if (!conn.getDoOutput()) {
            conn.setDoOutput(true);
         }

         return conn;
      } catch (Exception var9) {
         Exception e = var9;
         throw new IllegalStateException(e);
      }
   }

   private static final void setHeader(URLConnection conn) {
      Map<String, String> hds = getGlobalHeads();
      Set<Map.Entry<String, String>> ens = hds.entrySet();
      Iterator var3 = ens.iterator();

      while(var3.hasNext()) {
         Map.Entry<String, String> en = (Map.Entry)var3.next();
         String key = (String)en.getKey();
         if (conn.getRequestProperty(key) == null) {
            String value = (String)en.getValue();
            conn.setRequestProperty(key, value);
         }
      }

   }

   private static final Map<String, String> getGlobalHeads() {
      ServiceLoader<Interceptor> loader = ServiceLoader.load(Interceptor.class);
      if (loader == null) {
         return new HashMap(0);
      } else {
         Map<String, String> hds = new HashMap(3);
         Iterator<Interceptor> it = loader.iterator();

         while(it.hasNext()) {
            Interceptor icpt = (Interceptor)it.next();
            Set<Map.Entry<String, String>> ens = icpt.headers().entrySet();
            ens.forEach((en) -> {
               String key = (String)en.getKey();
               String value = (String)en.getValue();
               if (value != null && value.trim().length() > 0) {
                  hds.put(key, value);
               }

            });
         }

         return hds;
      }
   }

   private static String getUserAgent() {
      return String.format("%s/%s/%s/%s/%s/%s/%s/%s", "Windows", System.nanoTime(), "Chrome", "Safari", "QQBrowser", "Mozilla", "Firefox", "IE");
   }

   public static byte[] getData(String url) {
      URLConnection conn = getURLConnection(url, (String)null, (String)null);
      return getResponseData(conn);
   }

   public static byte[] getData(Map<String, String> heads, String url) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, 6000, heads, 72000);
      return getResponseData(conn);
   }

   public static byte[] getData(String url, String cookie) {
      URLConnection conn = getURLConnection(url, (String)null, cookie);
      return getResponseData(conn);
   }

   public static byte[] getResponseData(URLConnection conn) {
      try {
         InputStream inputStream = getRemoteInStream(conn);
         return getBytes(inputStream);
      } catch (IOException var3) {
         IOException e = var3;
         int statusCode = getResponseCode(conn);
         if (statusCode == 502) {
            throw new StatusException(e, statusCode);
         } else {
            throw new IllegalStateException(e);
         }
      }
   }

   public static byte[] getBytes(InputStream in) {
      try {
         BufferedInputStream bis = new BufferedInputStream(in);
         Throwable var2 = null;

         try {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            byte[] b = new byte[20960];

            for(int len = bis.read(b); len != -1; len = bis.read(b)) {
               bao.write(b, 0, len);
            }

            byte[] var6 = bao.toByteArray();
            return var6;
         } catch (Throwable var16) {
            var2 = var16;
            throw var16;
         } finally {
            if (bis != null) {
               if (var2 != null) {
                  try {
                     bis.close();
                  } catch (Throwable var15) {
                     var2.addSuppressed(var15);
                  }
               } else {
                  bis.close();
               }
            }

         }
      } catch (Exception var18) {
         Exception e = var18;
         throw new IllegalStateException(e);
      }
   }

   public static byte[] getBytes(InputStream in, long length) {
      try {
         BufferedInputStream bis = new BufferedInputStream(in);
         Throwable var4 = null;

         try {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            byte[] b = new byte[20960];
            int len = bis.read(b);

            while(true) {
               if (len != -1) {
                  bao.write(b, 0, len);
                  if ((long)bao.size() < length) {
                     len = bis.read(b);
                     continue;
                  }
               }

               byte[] var8 = bao.toByteArray();
               return var8;
            }
         } catch (Throwable var18) {
            var4 = var18;
            throw var18;
         } finally {
            if (bis != null) {
               if (var4 != null) {
                  try {
                     bis.close();
                  } catch (Throwable var17) {
                     var4.addSuppressed(var17);
                  }
               } else {
                  bis.close();
               }
            }

         }
      } catch (Exception var20) {
         Exception e = var20;
         throw new IllegalStateException(e);
      }
   }

   public static String getContent(String url) {
      URLConnection conn = getURLConnection(url, (String)null, (String)null);
      return getResponseContent(conn, StandardCharsets.UTF_8);
   }

   public static String getContent(String url, int connectTimeout, int readTimeout) {
      URLConnection conn = getUrlConnection(url, (String)null, (String)null, (Proxy)null, connectTimeout, readTimeout);
      return getResponseContent(conn, StandardCharsets.UTF_8);
   }

   public static String getContent(String url, int connectTimeout, int readTimeout, String remoteIp, String userAgent) {
      URLConnection conn = getUrlConnection(url, (String)null, (String)null, (Proxy)null, connectTimeout, readTimeout, remoteIp, userAgent);
      return getResponseContent(conn, StandardCharsets.UTF_8);
   }

   public static String getContent(String url, String cookie, String contentType) {
      URLConnection conn = getURLConnection(url, contentType, cookie);
      return getResponseContent(conn, StandardCharsets.UTF_8);
   }

   public static String getContent(String url, String charsetName) {
      URLConnection conn = getURLConnection(url, (String)null, (String)null);
      return getResponseContent(conn, Charset.forName(charsetName));
   }

   public static String getContent(String url, Map<String, String> heads, String charsetName) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, 6000, heads, 72000);
      return getResponseContent(conn, Charset.forName(charsetName));
   }

   public static String getContent(String url, Map<String, String> heads, String charsetName, int connectTimeout, int readTimeout) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, connectTimeout, heads, readTimeout);
      return getResponseContent(conn, Charset.forName(charsetName));
   }

   public static HttpDataInfo getContent(String url, Map<String, String> heads, int connectTimeout, int readTimeout) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, connectTimeout, heads, readTimeout);
      return getResponseDataInfo(conn);
   }

   public static String getContent(String url, Map<String, String> heads) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, 6000, heads, 72000);
      return getResponseContent(conn, StandardCharsets.UTF_8);
   }

   public static String getContent(String url, int connectTimeout, int readTimeout, Map<String, String> heads, Proxy proxy) {
      URLConnection conn = getUrlConnection(url, proxy, 6000, heads, 72000);
      return getResponseContent(conn, StandardCharsets.UTF_8);
   }

   public static byte[] getDataByJson(String url, String json) {
      URLConnection conn = getURLConnection(url, "application/json;charset=UTF-8", (String)null);

      try {
         OutputStream out = getRemoteOutStream(conn);
         Throwable var4 = null;

         byte[] var5;
         try {
            if (json != null && json.trim().length() > 0) {
               out.write(json.getBytes(StandardCharsets.UTF_8));
            }

            var5 = getResponseData(conn);
         } catch (Throwable var15) {
            var4 = var15;
            throw var15;
         } finally {
            if (out != null) {
               if (var4 != null) {
                  try {
                     out.close();
                  } catch (Throwable var14) {
                     var4.addSuppressed(var14);
                  }
               } else {
                  out.close();
               }
            }

         }

         return var5;
      } catch (IOException var17) {
         IOException e = var17;
         throw new IllegalStateException(e);
      }
   }

   public static HttpDataInfo downloadByJson(String url, String json, Map<String, String> heads, int connectTimeout, int readTimeout) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, connectTimeout, heads, readTimeout);

      try {
         OutputStream out = getRemoteOutStream(conn);
         Throwable var7 = null;

         HttpDataInfo var8;
         try {
            if (json != null && json.trim().length() > 0) {
               out.write(json.getBytes(StandardCharsets.UTF_8));
            }

            var8 = getResponseDataInfo(conn);
         } catch (Throwable var18) {
            var7 = var18;
            throw var18;
         } finally {
            if (out != null) {
               if (var7 != null) {
                  try {
                     out.close();
                  } catch (Throwable var17) {
                     var7.addSuppressed(var17);
                  }
               } else {
                  out.close();
               }
            }

         }

         return var8;
      } catch (IOException var20) {
         IOException e = var20;
         throw new IllegalStateException(e);
      }
   }

   public static String getResponseContent(URLConnection conn, Charset cs) {
      return new String(getResponseDataInfo(conn).getBody(), cs);
   }

   private static HttpDataInfo getResponseDataInfo(URLConnection conn) {
      byte[] bt = getResponseData(conn);
      HttpDataInfo hd = new HttpDataInfo(getHeaders(conn));
      hd.setResponseCode(getResponseCode(conn));
      hd.setContentType(getContentType(conn));
      hd.setContentDisposition(getContentDisposition(conn));
      hd.setLenth(getContentLength(conn));
      hd.setBody(bt);
      return hd;
   }

   public static String postForm(String url, Map<String, String[]> inputs) {
      URLConnection conn = getURLConnection(url, "application/x-www-form-urlencoded;charset=UTF-8", (String)null);
      return postForm(inputs, conn);
   }

   public static String postForm(Map<String, String> inputs, String url) {
      URLConnection conn = getURLConnection(url, "application/x-www-form-urlencoded;charset=UTF-8", (String)null);
      return postForm(mapToFormVs(inputs), conn);
   }

   public static Map<String, String[]> mapToFormVs(Map<String, String> inputs) {
      Map<String, String[]> inputss = new HashMap();
      if (inputs != null) {
         Iterator var2 = inputs.entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry<String, String> en = (Map.Entry)var2.next();
            inputss.put(en.getKey(), new String[]{(String)en.getValue()});
         }
      }

      return inputss;
   }

   public static String postForm(String url, Map<String, String[]> inputs, int connectTimeout, int readTimeout, String remoteIp, String userAgent) {
      URLConnection conn = getUrlConnection(url, "application/x-www-form-urlencoded;charset=UTF-8", (String)null, (Proxy)null, connectTimeout, readTimeout, remoteIp, userAgent);
      return postForm(inputs, conn);
   }

   public static String postForm(String url, Map<String, String[]> inputs, int connectTimeout, int readTimeout, Map<String, String> heads) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, connectTimeout, heads, readTimeout);
      return postForm(inputs, conn);
   }

   public static String postForm(String url, Map<String, String[]> inputs, int connectTimeout, int readTimeout, Map<String, String> heads, Proxy proxy) {
      URLConnection conn = getUrlConnection(url, proxy, connectTimeout, heads, readTimeout);
      return postForm(inputs, conn);
   }

   public static String postForm(Map<String, String[]> inputs, URLConnection conn) {
      try {
         OutputStream out = getRemoteOutStream(conn);
         Throwable var3 = null;

         String var18;
         try {
            String encparam = urlencoded(inputs);
            if (encparam != null) {
               byte[] body = encparam.getBytes(StandardCharsets.UTF_8);
               out.write(body);
            }

            var18 = getResponseContent(conn, StandardCharsets.UTF_8);
         } catch (Throwable var15) {
            var3 = var15;
            throw var15;
         } finally {
            if (out != null) {
               if (var3 != null) {
                  try {
                     out.close();
                  } catch (Throwable var14) {
                     var3.addSuppressed(var14);
                  }
               } else {
                  out.close();
               }
            }

         }

         return var18;
      } catch (IOException var17) {
         IOException e = var17;
         throw new IllegalStateException(e);
      }
   }

   public static String urlencoded(Map<String, String[]> inputs) throws UnsupportedEncodingException {
      if (inputs != null && inputs.size() > 0) {
         Iterator<Map.Entry<String, String[]>> ite = inputs.entrySet().iterator();
         StringBuilder sb = new StringBuilder();

         while(ite.hasNext()) {
            Map.Entry<String, String[]> en = (Map.Entry)ite.next();

            for(int i = 0; i < ((String[])en.getValue()).length; ++i) {
               String v = ((String[])en.getValue())[i];
               sb.append((String)en.getKey()).append("=").append(URLEncoder.encode(v, StandardCharsets.UTF_8.toString()));
               if (i < ((String[])en.getValue()).length - 1) {
                  sb.append("&");
               }
            }

            if (ite.hasNext()) {
               sb.append("&");
            }
         }

         return sb.toString();
      } else {
         return null;
      }
   }

   public static String callByJson(String url, String json) throws IOException {
      HttpDataInfo reqjson = new HttpDataInfo();
      reqjson.setBody(json.getBytes(StandardCharsets.UTF_8));
      reqjson.setContentType("application/json;charset=UTF-8");
      HttpDataInfo res = call(url, reqjson);
      return getCallContent(res);
   }

   public static String callByForm(String url, Map<String, String[]> inputs) throws IOException {
      HttpDataInfo formdata = new HttpDataInfo();
      String enparam = urlencoded(inputs);
      if (enparam != null) {
         formdata.setBody(enparam.getBytes(StandardCharsets.UTF_8));
      }

      formdata.setContentType("application/x-www-form-urlencoded;charset=UTF-8");
      HttpDataInfo res = call(url, formdata);
      return getCallContent(res);
   }

   public static String call(String url, int connectTimeout, int readTimeout) throws IOException {
      HttpDataInfo req = new HttpDataInfo();
      req.setReadtimeout(readTimeout);
      req.setTimeout(connectTimeout);
      HttpDataInfo res = call(url, req);
      return getCallContent(res);
   }

   public static String getCallContent(HttpDataInfo res) {
      if (res.getContentType() != null) {
         String[] arr = res.getContentType().split(";");
         if (arr.length > 1) {
            String[] cs = arr[1].split("=");
            if (cs.length == 2) {
               try {
                  return new String(res.getBody(), cs[1]);
               } catch (UnsupportedEncodingException var4) {
                  UnsupportedEncodingException e = var4;
                  throw new IllegalArgumentException(e);
               }
            } else {
               return new String(res.getBody(), StandardCharsets.UTF_8);
            }
         } else {
            return new String(res.getBody(), StandardCharsets.UTF_8);
         }
      } else {
         return new String(res.getBody(), StandardCharsets.UTF_8);
      }
   }

   public static HttpDataInfo call(String url, HttpDataInfo req) throws IOException {
      URL u = new URL(url);
      if ("https".equalsIgnoreCase(u.getProtocol())) {
         return callByTls(url, req);
      } else {
         Socket socket = new Socket();
         Throwable var4 = null;

         HttpDataInfo var5;
         try {
            var5 = socketcall(u, req, socket);
         } catch (Throwable var14) {
            var4 = var14;
            throw var14;
         } finally {
            if (socket != null) {
               if (var4 != null) {
                  try {
                     socket.close();
                  } catch (Throwable var13) {
                     var4.addSuppressed(var13);
                  }
               } else {
                  socket.close();
               }
            }

         }

         return var5;
      }
   }

   public static HttpDataInfo callByTls(String url, HttpDataInfo req) throws IOException {
      SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
      SSLSocket socket = (SSLSocket)sslsf.createSocket();
      Throwable var4 = null;

      HttpDataInfo var5;
      try {
         socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
         var5 = socketcall(new URL(url), req, socket);
      } catch (Throwable var14) {
         var4 = var14;
         throw var14;
      } finally {
         if (socket != null) {
            if (var4 != null) {
               try {
                  socket.close();
               } catch (Throwable var13) {
                  var4.addSuppressed(var13);
               }
            } else {
               socket.close();
            }
         }

      }

      return var5;
   }

   private static HttpDataInfo socketcall(URL u, HttpDataInfo req, Socket socket) throws SocketException, MalformedURLException, IOException {
      socket.setReuseAddress(true);
      socket.setKeepAlive(true);
      socket.setTcpNoDelay(true);
      socket.setSoLinger(true, 0);
      if (req.getReadtimeout() > 0) {
         socket.setSoTimeout(req.getReadtimeout());
      } else {
         socket.setSoTimeout(72000);
      }

      HttpDataInfo res = new HttpDataInfo();
      String mpost = "POST";
      boolean post = req.getBody() != null && req.getBody().length > 0;
      String method = post ? mpost : "GET";
      int port = u.getPort() == -1 ? u.getDefaultPort() : u.getPort();
      String host = u.getHost();
      socket.connect(new InetSocketAddress(host, port), req.getTimeout() > 0 ? req.getTimeout() : 6000);
      OutputStream out = socket.getOutputStream();
      StringBuilder sb = new StringBuilder(String.format("%s %s HTTP/1.1\r\n", method, u.getFile()));
      sb.append(String.format("Host: %s\r\n", u.getHost()));
      if (req.getHeaderFields() != null) {
         Set<Map.Entry<String, List<String>>> ens = req.getHeaderFields().entrySet();
         if (!ens.stream().anyMatch((enx) -> {
            return ((String)enx.getKey()).equalsIgnoreCase("User-Agent");
         })) {
            req.getHeaderFields().put("User-Agent", Arrays.asList(getUserAgent()));
         }

         if (!ens.stream().anyMatch((enx) -> {
            return ((String)enx.getKey()).equalsIgnoreCase("Connection");
         })) {
            req.getHeaderFields().put("Connection", Arrays.asList("close"));
         }

         if (post && !ens.stream().anyMatch((enx) -> {
            return ((String)enx.getKey()).equalsIgnoreCase("Content-Length");
         })) {
            req.getHeaderFields().put("Content-Length", Arrays.asList(String.valueOf(req.getBody().length)));
         }

         if (!ens.stream().anyMatch((enx) -> {
            return ((String)enx.getKey()).equalsIgnoreCase("Content-Type");
         })) {
            if (req.getContentType() != null) {
               req.getHeaderFields().put("Content-Type", Arrays.asList(req.getContentType()));
            } else {
               req.getHeaderFields().put("Content-Type", Arrays.asList("text/html;charset=UTF-8"));
            }
         }

         Map<String, String> hds = getGlobalHeads();
         Set<Map.Entry<String, String>> hdens = hds.entrySet();
         Iterator var14 = hdens.iterator();

         Map.Entry en;
         String v;
         while(var14.hasNext()) {
            en = (Map.Entry)var14.next();
            String key = (String)en.getKey();
            if (ens.stream().noneMatch((enx) -> {
               return ((String)enx.getKey()).equalsIgnoreCase(key);
            })) {
               v = (String)en.getValue();
               req.getHeaderFields().put(key, Arrays.asList(v));
            }
         }

         var14 = ens.iterator();

         label133:
         while(true) {
            do {
               do {
                  if (!var14.hasNext()) {
                     break label133;
                  }

                  en = (Map.Entry)var14.next();
               } while(en.getKey() == null);
            } while(((String)en.getKey()).equalsIgnoreCase("Host"));

            Iterator var31 = ((List)en.getValue()).iterator();

            while(var31.hasNext()) {
               v = (String)var31.next();
               sb.append(String.format("%s:%s\r\n", en.getKey(), v));
            }
         }
      }

      sb.append("\r\n");
      String reqstr = sb.toString();
      out.write(reqstr.getBytes());
      if (post) {
         out.write(req.getBody());
      }

      out.flush();
      InputStream in = socket.getInputStream();
      int c = in.read();

      StringBuilder rsb;
      for(rsb = new StringBuilder(); c != -1; c = in.read()) {
         char cc = (char)c;
         rsb.append(cc);
         if (rsb.toString().endsWith("\r\n\r\n")) {
            break;
         }
      }

      String hdstr = rsb.toString();
      Map<String, List<String>> headerFields = new HashMap();
      String[] hds = hdstr.split("\r\n");
      String[] var18 = hds;
      int var19 = hds.length;

      for(int var20 = 0; var20 < var19; ++var20) {
         String hd = var18[var20];
         String[] kvs = hd.split(":", 2);
         if (kvs.length == 2) {
            String key = kvs[0];
            List<String> vs = (List)headerFields.get(key);
            if (vs == null) {
               vs = new ArrayList(1);
               headerFields.put(key, vs);
            }

            ((List)vs).add(kvs[1]);
            if (key.trim().equalsIgnoreCase("Transfer-Encoding")) {
               res.setChunked(Arrays.asList(((String)((List)vs).get(0)).trim().split(",")).stream().anyMatch((s) -> {
                  return s.trim().equals("chunked");
               }));
            } else if (key.trim().equalsIgnoreCase("Content-Length")) {
               res.setLenth(Long.valueOf(((String)((List)vs).get(0)).trim()));
            } else if (key.trim().equalsIgnoreCase("Content-Type")) {
               res.setContentType(((String)((List)vs).get(0)).trim());
            } else if (key.trim().equalsIgnoreCase("Content-Disposition")) {
               res.setContentDisposition(((String)((List)vs).get(0)).trim());
            }
         } else {
            String[] arhd = hd.split("\\s+");
            if (arhd.length < 2) {
               throw new SocketException("Connection reset !!!");
            }

            res.setResponseCode(Integer.valueOf(arhd[1]));
         }
      }

      res.setHeaderFields(headerFields);
      if (res.getChunked()) {
         res.setBody(getChunkedBytes(in));
      } else {
         res.setBody(getBytes(in, res.getLenth()));
      }

      return res;
   }

   public static byte[] getChunkedBytes(InputStream in) throws IOException {
      ByteArrayOutputStream bao = new ByteArrayOutputStream();
      StringBuilder clensb = new StringBuilder();

      for(int c = in.read(); c != -1; c = in.read()) {
         char cc = (char)c;
         if (cc != '\n') {
            clensb.append(cc);
         } else {
            clensb.deleteCharAt(clensb.length() - 1);
            String ll = clensb.toString();
            int rlen = Integer.parseInt(ll, 16);
            if (rlen == 0) {
               break;
            }

            while(rlen > 0) {
               byte[] cbt = new byte[rlen];
               int llen = in.read(cbt);
               rlen -= llen;
               bao.write(cbt, 0, llen);
            }

            clensb = new StringBuilder();
            in.skip(2L);
         }
      }

      return bao.toByteArray();
   }

   public static void sendbody(byte[] body, OutputStream out) throws IOException {
      if (body != null && body.length > 0) {
         try {
            out.write(body);
         } finally {
            if (out != null) {
               out.close();
            }

         }
      }

   }

   public static boolean byteChange(InputStream in, OutputStream out) {
      try {
         if (in == null || in.available() <= 0 || out == null) {
            boolean var21 = false;
            return var21;
         } else {
            byte[] b = new byte[10240];

            for(int len = in.read(b); len > 0; len = in.read(b)) {
               out.write(b, 0, len);
            }

            boolean var4 = true;
            return var4;
         }
      } catch (IOException var18) {
         IOException e = var18;
         throw new IllegalStateException(e);
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (IOException var17) {
            }
         }

         if (out != null) {
            try {
               out.close();
            } catch (IOException var16) {
            }
         }

      }
   }

   public static String postJson(String url, String json) {
      URLConnection conn = getURLConnection(url, "application/json;charset=UTF-8", (String)null);
      return sendBodyData(json, conn);
   }

   public static String postJson(String url, String json, String headName, String headValue) {
      HashMap<String, String> heads = new HashMap(2);
      heads.put("Content-Type", "application/json;charset=UTF-8");
      if (headName != null && headValue != null) {
         heads.put(headName, headValue);
      }

      URLConnection conn = getUrlConnection(url, (Proxy)null, 6000, heads, 72000);
      return sendBodyData(json, conn);
   }

   public static String postJson(String url, String json, Map<String, String> heads) {
      if (heads != null) {
         if (!((Map)heads).containsKey("Content-Type")) {
            ((Map)heads).put("Content-Type", "application/json;charset=UTF-8");
         }
      } else {
         heads = new HashMap(1);
         ((Map)heads).put("Content-Type", "application/json;charset=UTF-8");
      }

      URLConnection conn = getUrlConnection(url, (Proxy)null, 6000, (Map)heads, 72000);
      return sendBodyData(json, conn);
   }

   private static String sendBodyData(String json, URLConnection conn) {
      return new String(sendBodyData(conn, json).getBody(), StandardCharsets.UTF_8);
   }

   private static HttpDataInfo sendBodyData(URLConnection conn, String json) {
      try {
         OutputStream out = getRemoteOutStream(conn);
         Throwable var3 = null;

         HttpDataInfo var4;
         try {
            if (json != null && json.trim().length() > 0) {
               out.write(json.getBytes(StandardCharsets.UTF_8));
            }

            var4 = getResponseDataInfo(conn);
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (out != null) {
               if (var3 != null) {
                  try {
                     out.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  out.close();
               }
            }

         }

         return var4;
      } catch (IOException var16) {
         IOException e = var16;
         throw new IllegalStateException(e);
      }
   }

   private static OutputStream getRemoteOutStream(URLConnection conn) throws IOException {
      try {
         return conn.getOutputStream();
      } catch (IOException var2) {
         IOException e = var2;
         if (!"connect timed out".equals(e.getMessage()) && (e.getMessage() == null || !e.getMessage().contains("Connection refused") && !e.getMessage().contains("Connection reset"))) {
            throw e;
         } else {
            sleep(4L);
            return conn.getOutputStream();
         }
      }
   }

   private static InputStream getRemoteInStream(URLConnection conn) throws IOException {
      try {
         return conn.getInputStream();
      } catch (SocketTimeoutException var2) {
         SocketTimeoutException e = var2;
         if ("connect timed out".equals(e.getMessage())) {
            sleep(4L);
            return conn.getInputStream();
         } else {
            throw e;
         }
      }
   }

   private static void sleep(long timeout) {
      try {
         TimeUnit.SECONDS.sleep(timeout);
      } catch (InterruptedException var3) {
      }

   }

   public static String postBody(String url, String data, String contentType) {
      URLConnection conn = getURLConnection(url, contentType, (String)null);
      return sendBodyData(data, conn);
   }

   public static String postBody(String url, String data, String contentType, int connectTimeout, int readTimeout) {
      URLConnection conn = getUrlConnection(url, contentType, (String)null, (Proxy)null, connectTimeout, readTimeout);
      return sendBodyData(data, conn);
   }

   public static String postBody(String url, String data, String contentType, int connectTimeout, int readTimeout, String remoteIp, String userAgent) {
      URLConnection conn = getUrlConnection(url, contentType, (String)null, (Proxy)null, connectTimeout, readTimeout, remoteIp, userAgent);
      return sendBodyData(data, conn);
   }

   public static String callBody(String url, String data, String contentType, int connectTimeout, int readTimeout) throws IOException {
      HttpDataInfo req = new HttpDataInfo();
      req.setBody(data.getBytes(StandardCharsets.UTF_8));
      req.setContentType(contentType);
      req.setTimeout(connectTimeout);
      req.setReadtimeout(readTimeout);
      HttpDataInfo res = call(url, req);
      return getCallContent(res);
   }

   public static String postBody(String url, String data, Map<String, String> heads) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, 6000, heads, 72000);
      return sendBodyData(data, conn);
   }

   public static String postBody(String url, String data, Map<String, String> heads, int connectTimeout, int readTimeout) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, connectTimeout, heads, readTimeout);
      return sendBodyData(data, conn);
   }

   public static HttpDataInfo postBody(String url, Map<String, String> heads, String data, int connectTimeout, int readTimeout) {
      URLConnection conn = getUrlConnection(url, (Proxy)null, connectTimeout, heads, readTimeout);
      return sendBodyData(conn, data);
   }

   public static HttpDataInfo postFormData(String url, List<FileDataInfo> files, Map<String, String[]> inputs, Map<String, String> heads) {
      URLConnection uc = getUrlConnection(url, (Proxy)null, 6000, heads, 72000);
      String boundary = String.format("----WebKitFormBoundary%s", System.nanoTime());
      uc.setRequestProperty("Content-Type", String.format("%s; boundary=%s", "multipart/form-data;charset=UTF-8", boundary));
      uc.setUseCaches(false);

      try {
         OutputStream os = getRemoteOutStream(uc);
         Throwable var7 = null;

         HttpDataInfo var28;
         try {
            if (inputs != null && inputs.size() > 0) {
               Set<Map.Entry<String, String[]>> ines = inputs.entrySet();
               Iterator var9 = ines.iterator();

               while(var9.hasNext()) {
                  Map.Entry<String, String[]> en = (Map.Entry)var9.next();
                  String[] var11 = (String[])en.getValue();
                  int var12 = var11.length;

                  for(int var13 = 0; var13 < var12; ++var13) {
                     String v = var11[var13];
                     os.write(String.format("--%s", boundary).getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write(String.format("Content-Disposition: form-data; name=\"%s\"", en.getKey()).getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write(String.format("Content-Type: %s", "text/plain; charset=UTF-8").getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write(v.getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                  }
               }
            }

            if (files != null) {
               Iterator var27 = files.iterator();

               while(var27.hasNext()) {
                  FileDataInfo file = (FileDataInfo)var27.next();
                  if (file.getFiledata() != null) {
                     os.write(String.format("--%s", boundary).getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", file.getInputName(), file.getFilename()).getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write(String.format("Content-Type: %s", file.getContentType()).getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write(String.format("Content-Transfer-Encoding: %s", "binary").getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                     os.write(file.getFiledata());
                     os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                  }
               }
            }

            os.write(String.format("--%s--", boundary).getBytes(StandardCharsets.UTF_8));
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            var28 = getResponseDataInfo(uc);
         } catch (Throwable var23) {
            var7 = var23;
            throw var23;
         } finally {
            if (os != null) {
               if (var7 != null) {
                  try {
                     os.close();
                  } catch (Throwable var22) {
                     var7.addSuppressed(var22);
                  }
               } else {
                  os.close();
               }
            }

         }

         return var28;
      } catch (IOException var25) {
         IOException e = var25;
         throw new IllegalStateException(e);
      }
   }

   public static String postMultipartFormData(String url, List<FileDataInfo> files, Map<String, String[]> inputs, Map<String, String> heads) {
      HttpDataInfo data = postFormData(url, files, inputs, heads);
      return new String(data.getBody(), StandardCharsets.UTF_8);
   }

   public static String postMultipartFormData(byte[] filedata, String url, String inputName, String filename, String contentType, Map<String, String[]> inputs, Map<String, String> heads) {
      return postMultipartFormData(url, Arrays.asList(new FileDataInfo(filedata, filename, inputName, contentType)), inputs, heads);
   }

   public static Map<String, String> getMap(String... kvs) {
      if (kvs != null && kvs.length > 0 && kvs.length % 2 == 0) {
         Map<String, String> rq = new HashMap(kvs.length % 2);

         for(int i = 0; i < kvs.length; ++i) {
            String var10001 = kvs[i];
            ++i;
            rq.put(var10001, kvs[i]);
         }

         return rq;
      } else {
         return null;
      }
   }

   public static Map<String, String[]> getMapVs(String... kvs) {
      if (kvs != null && kvs.length > 0 && kvs.length % 2 == 0) {
         Map<String, String[]> rq = new HashMap(kvs.length % 2);

         for(int i = 0; i < kvs.length; ++i) {
            String var10001 = kvs[i];
            String[] var10002 = new String[1];
            ++i;
            var10002[0] = kvs[i];
            rq.put(var10001, var10002);
         }

         return rq;
      } else {
         return null;
      }
   }

   public static String uploadImage(byte[] img, String url) {
      return uploadFile(img, url, "img1", String.format("%s.%s", System.nanoTime(), "jpg"), "image/jpeg");
   }

   public static String uploadFile(byte[] img, String url, String inputName, String filename, String contentType) {
      return uploadFile(img, url, inputName, filename, contentType, 6000, 72000);
   }

   public static String uploadFile(byte[] img, String url, String inputName, String filename, String contentType, int timeout, int readtimeout) {
      URLConnection uc = getUrlConnection(url, (String)null, (String)null, (Proxy)null, timeout, readtimeout);
      return sendFile(img, inputName, filename, contentType, uc);
   }

   public static String uploadFile(byte[] filedata, String url, String inputName, String filename, String contentType, int timeout, int readtimeout, Map<String, String> heads) {
      return sendFile(filedata, inputName, filename, contentType, getUrlConnection(url, (Proxy)null, timeout, heads, readtimeout));
   }

   private static String sendFile(byte[] img, String inputName, String filename, String contentType, URLConnection uc) {
      String boundary = String.format("----WebKitFormBoundary%s%s", System.nanoTime(), Thread.currentThread().getId());
      uc.setRequestProperty("Content-Type", String.format("%s; boundary=%s", "multipart/form-data;charset=UTF-8", boundary));
      uc.setUseCaches(false);

      try {
         OutputStream os = getRemoteOutStream(uc);
         Throwable var7 = null;

         String var8;
         try {
            os.write(String.format("--%s", boundary).getBytes(StandardCharsets.UTF_8));
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", inputName, filename).getBytes(StandardCharsets.UTF_8));
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(String.format("Content-Type: %s", contentType).getBytes(StandardCharsets.UTF_8));
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(img);
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(String.format("--%s--", boundary).getBytes(StandardCharsets.UTF_8));
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            var8 = getResponseContent(uc, StandardCharsets.UTF_8);
         } catch (Throwable var18) {
            var7 = var18;
            throw var18;
         } finally {
            if (os != null) {
               if (var7 != null) {
                  try {
                     os.close();
                  } catch (Throwable var17) {
                     var7.addSuppressed(var17);
                  }
               } else {
                  os.close();
               }
            }

         }

         return var8;
      } catch (IOException var20) {
         IOException e = var20;
         throw new IllegalStateException(e);
      }
   }

   private static Map<String, String> getHeads(String contentType, String cookie) {
      Map<String, String> heads = new HashMap(7);
      if (contentType != null && contentType.trim().length() > 1) {
         heads.put("Content-Type", contentType);
      } else {
         heads.put("Content-Type", "text/html;charset=UTF-8");
      }

      if (cookie != null && cookie.trim().length() > 1) {
         heads.put("Cookie", cookie);
      }

      return heads;
   }
}
