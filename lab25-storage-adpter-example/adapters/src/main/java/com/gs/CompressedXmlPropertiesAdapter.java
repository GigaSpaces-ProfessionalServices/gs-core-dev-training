package com.gs;/*
 * Copyright (c) 2008-2019, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.gigaspaces.client.storage_adapters.BinaryWrapper;
import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;
import com.gigaspaces.internal.io.CompressedMarshObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Adapter for storing compressed XML in space within XMLProperty objects.
 *
 * Stores XML content as compressed bytes in the space and provides both the
 * decompressed XML content and parsed properties (Map<String, Object>) representing
 * the XML structure, allowing direct property access and indexing of nested values.
 *
 * Input: XMLProperty with xmlContent set to an XML string
 * Storage: Compressed bytes of the XML content
 * Output: XMLProperty with both xmlContent (decompressed) and properties (parsed Map)
 *
  */
public class CompressedXmlPropertiesAdapter extends PropertyStorageAdapter {

    private int compressionLevel = Deflater.DEFAULT_COMPRESSION;
    private XmlParser xmlParser = new XmlParser();

    public CompressedXmlPropertiesAdapter() {
    }

    public CompressedXmlPropertiesAdapter(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }



    @Override
    public String getName() {
        return "CompressedXmlProperties" + (useBase64Wrapper() ? "-base64" : "");
    }

    @Override
    public Class<?> getStorageClass() {
        return useBase64Wrapper() ? String.class : CompressedMarshObject.class;
    }

    @Override
    public boolean supportsEqualsMatching() {
        return true;  // Maps don't support direct equals matching
    }

    @Override
    public boolean supportsOrderedMatching() {
        return true;
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof XMLProperty)) {
            throw new IOException("CompressedXmlPropertiesAdapter only accepts XMLProperty values, received: " + value.getClass().getName());
        }
        XMLProperty xmlProperty = (XMLProperty) value;
        String xmlString = xmlProperty.getXmlContent();
        if (xmlString == null || xmlString.isEmpty()) {
            return null;
        }
        byte[] compressed = compressXml(xmlString);
        return wrapBinary(compressed);
    }

    @Override
    public Object fromSpace(Object value) throws IOException, ClassNotFoundException {
        if (value == null) {
            return null;
        }
        byte[] compressed = unwrapBinary(value);
        String xmlString = decompressXml(compressed);
        XMLProperty xmlProperty = new XMLProperty();
        xmlProperty.setXmlContent(xmlString);
        return xmlProperty;
    }

    @Override
    public BinaryWrapper toBinaryWrapper(byte[] data) {
        return new CompressedMarshObject(data);
    }

    /**
     * Compresses an XML string using DEFLATE compression.
     */
    private byte[] compressXml(String xmlString) throws IOException {
        if (xmlString.isEmpty()) {
            return new byte[0];
        }

        byte[] input = xmlString.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[input.length];

        Deflater deflater = new Deflater(compressionLevel, true);
        try {
            deflater.setInput(input);
            deflater.finish();
            int compressedSize = deflater.deflate(output);

            if (compressedSize == output.length) {
                return output;
            }

            byte[] result = new byte[compressedSize];
            System.arraycopy(output, 0, result, 0, compressedSize);
            return result;
        } finally {
            deflater.end();
        }
    }

    /**
     * Decompresses a compressed XML byte array back to an XML string.
     */
    private String decompressXml(byte[] compressed) throws IOException {
        if (compressed.length == 0) {
            return "";
        }

        byte[] output = new byte[compressed.length * 2];
        int decompressedSize = 0;

        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(compressed);

            while (!inflater.finished()) {
                int count = inflater.inflate(output, decompressedSize, output.length - decompressedSize);
                decompressedSize += count;

                if (decompressedSize == output.length && !inflater.finished()) {
                    byte[] newOutput = new byte[output.length * 2];
                    System.arraycopy(output, 0, newOutput, 0, output.length);
                    output = newOutput;
                }
            }

            return new String(output, 0, decompressedSize, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IOException("Failed to decompress XML data", e);
        } finally {
            inflater.end();
        }
    }

    private Map<String, Object> parseXmlToProperties(String xmlString) throws IOException {
        return xmlParser.parseXmlToProperties(xmlString);
    }

    public void setCompressionLevel(int level) {
        if (level < Deflater.NO_COMPRESSION || level > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Compression level must be between " +
                Deflater.NO_COMPRESSION + " and " + Deflater.BEST_COMPRESSION);
        }
        this.compressionLevel = level;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setIncludeAttributes(boolean include) {
        this.xmlParser.setIncludeAttributes(include);
    }

    public boolean isIncludeAttributes() {
        return xmlParser.isIncludeAttributes();
    }

    public void setIncludeTextContent(boolean include) {
        this.xmlParser.setIncludeTextContent(include);
    }

    public boolean isIncludeTextContent() {
        return xmlParser.isIncludeTextContent();
    }
}
