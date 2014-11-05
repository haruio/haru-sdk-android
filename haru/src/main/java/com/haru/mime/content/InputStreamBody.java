package com.haru.mime.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamBody extends AbstractContentBody
{
  private final InputStream in;
  private final String filename;

  public InputStreamBody(InputStream in, String mimeType, String filename)
  {
    super(mimeType);
    if (in == null) {
      throw new IllegalArgumentException("Input stream may not be null");
    }
    this.in = in;
    this.filename = filename;
  }

  public InputStreamBody(InputStream in, String filename) {
    this(in, "application/octet-stream", filename);
  }

  public InputStream getInputStream() {
    return this.in;
  }

  @Deprecated
  public void writeTo(OutputStream out, int mode)
    throws IOException
  {
    writeTo(out);
  }

  public void writeTo(OutputStream out) throws IOException {
    if (out == null)
      throw new IllegalArgumentException("Output stream may not be null");
    try
    {
      byte[] tmp = new byte[4096];
      int l;
      while ((l = this.in.read(tmp)) != -1) {
        out.write(tmp, 0, l);
      }
      out.flush();
    } finally {
      this.in.close();
    }
  }

  public String getTransferEncoding() {
    return "binary";
  }

  public String getCharset() {
    return null;
  }

  public long getContentLength() {
    return -1L;
  }

  public String getFilename() {
    return this.filename;
  }
}