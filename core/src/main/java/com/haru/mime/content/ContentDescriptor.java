package com.haru.mime.content;

public abstract interface ContentDescriptor
{
  public abstract String getMimeType();

  public abstract String getMediaType();

  public abstract String getSubType();

  public abstract String getCharset();

  public abstract String getTransferEncoding();

  public abstract long getContentLength();
}