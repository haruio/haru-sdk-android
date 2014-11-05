package com.haru.mime;

import com.haru.mime.content.ContentBody;

import org.apache.http.util.ByteArrayBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class HttpMultipart {
    private static final ByteArrayBuffer FIELD_SEP = encode(MIME.DEFAULT_CHARSET, ": ");
    private static final ByteArrayBuffer CR_LF = encode(MIME.DEFAULT_CHARSET, "\r\n");
    private static final ByteArrayBuffer TWO_DASHES = encode(MIME.DEFAULT_CHARSET, "--");
    private final String subType;
    private final Charset charset;
    private final String boundary;
    private final List<FormBodyPart> parts;
    private final HttpMultipartMode mode;

    private static ByteArrayBuffer encode(Charset charset, String string) {
        ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
        ByteArrayBuffer bab = new ByteArrayBuffer(encoded.remaining());
        bab.append(encoded.array(), encoded.position(), encoded.remaining());
        return bab;
    }

    private static void writeBytes(ByteArrayBuffer b, OutputStream out) throws IOException {
        out.write(b.buffer(), 0, b.length());
    }

    private static void writeBytes(String s, Charset charset, OutputStream out) throws IOException {
        ByteArrayBuffer b = encode(charset, s);
        writeBytes(b, out);
    }

    private static void writeBytes(String s, OutputStream out) throws IOException {
        ByteArrayBuffer b = encode(MIME.DEFAULT_CHARSET, s);
        writeBytes(b, out);
    }

    private static void writeField(MinimalField field, OutputStream out) throws IOException {
        writeBytes(field.getName(), out);
        writeBytes(FIELD_SEP, out);
        writeBytes(field.getBody(), out);
        writeBytes(CR_LF, out);
    }

    private static void writeField(MinimalField field, Charset charset, OutputStream out) throws IOException {
        writeBytes(field.getName(), charset, out);
        writeBytes(FIELD_SEP, out);
        writeBytes(field.getBody(), charset, out);
        writeBytes(CR_LF, out);
    }

    public HttpMultipart(String subType, Charset charset, String boundary, HttpMultipartMode mode) {
        if (subType == null) {
            throw new IllegalArgumentException("Multipart subtype may not be null");
        }
        if (boundary == null) {
            throw new IllegalArgumentException("Multipart boundary may not be null");
        }
        this.subType = subType;
        this.charset = (charset != null ? charset : MIME.DEFAULT_CHARSET);
        this.boundary = boundary;
        this.parts = new ArrayList();
        this.mode = mode;
    }

    public HttpMultipart(String subType, Charset charset, String boundary) {
        this(subType, charset, boundary, HttpMultipartMode.STRICT);
    }

    public HttpMultipart(String subType, String boundary) {
        this(subType, null, boundary);
    }

    public String getSubType() {
        return this.subType;
    }

    public Charset getCharset() {
        return this.charset;
    }

    public HttpMultipartMode getMode() {
        return this.mode;
    }

    public List<FormBodyPart> getBodyParts() {
        return this.parts;
    }

    public void addBodyPart(FormBodyPart part) {
        if (part == null) {
            return;
        }
        this.parts.add(part);
    }

    public String getBoundary() {
        return this.boundary;
    }

    private void doWriteTo(HttpMultipartMode mode, OutputStream out, boolean writeContent)
            throws IOException {
        ByteArrayBuffer boundary = encode(this.charset, getBoundary());
        for (FormBodyPart part : this.parts) {
            writeBytes(TWO_DASHES, out);
            writeBytes(boundary, out);
            writeBytes(CR_LF, out);

            Header header = part.getHeader();

            switch (mode.ordinal()) {
                case 1:
                    for (MinimalField field : header) {
                        writeField(field, out);
                    }
                    break;
                case 2:
                    MinimalField cd = part.getHeader().getField("Content-Disposition");
                    writeField(cd, this.charset, out);
                    String filename = part.getBody().getFilename();
                    if (filename != null) {
                        MinimalField ct = part.getHeader().getField("Content-Type");
                        writeField(ct, this.charset, out);
                    }
                    break;
            }
            writeBytes(CR_LF, out);

            if (writeContent) {
                part.getBody().writeTo(out);
            }
            writeBytes(CR_LF, out);
        }
        writeBytes(TWO_DASHES, out);
        writeBytes(boundary, out);
        writeBytes(TWO_DASHES, out);
        writeBytes(CR_LF, out);
    }

    public void writeTo(OutputStream out)
            throws IOException {
        doWriteTo(this.mode, out, true);
    }

    public long getTotalLength() {
        long contentLen = 0L;
        for (FormBodyPart part : this.parts) {
            ContentBody body = part.getBody();
            long len = body.getContentLength();
            if (len >= 0L)
                contentLen += len;
            else {
                return -1L;
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            doWriteTo(this.mode, out, false);
            byte[] extra = out.toByteArray();
            return contentLen + extra.length;
        } catch (IOException ex) {
        }
        return -1L;
    }
}