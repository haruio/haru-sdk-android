package com.haru.mime;

import com.haru.mime.content.ContentBody;

public class FormBodyPart
{
  private final String name;
  private final Header header;
  private final ContentBody body;

  public FormBodyPart(String name, ContentBody body)
  {
    if (name == null) {
      throw new IllegalArgumentException("Name may not be null");
    }
    if (body == null) {
      throw new IllegalArgumentException("Body may not be null");
    }
    this.name = name;
    this.body = body;
    this.header = new Header();

    generateContentDisp(body);
    generateContentType(body);
    generateTransferEncoding(body);
  }

  public String getName() {
    return this.name;
  }

  public ContentBody getBody() {
    return this.body;
  }

  public Header getHeader() {
    return this.header;
  }

  public void addField(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Field name may not be null");
    }
    this.header.addField(new MinimalField(name, value));
  }

  protected void generateContentDisp(ContentBody body) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("form-data; name=\"");
    buffer.append(getName());
    buffer.append("\"");
    if (body.getFilename() != null) {
      buffer.append("; filename=\"");
      buffer.append(body.getFilename());
      buffer.append("\"");
    }
    addField("Content-Disposition", buffer.toString());
  }

  protected void generateContentType(ContentBody body) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(body.getMimeType());
    if (body.getCharset() != null) {
      buffer.append("; charset=");
      buffer.append(body.getCharset());
    }
    addField("Content-Type", buffer.toString());
  }

  protected void generateTransferEncoding(ContentBody body) {
    addField("Content-Transfer-Encoding", body.getTransferEncoding());
  }
}