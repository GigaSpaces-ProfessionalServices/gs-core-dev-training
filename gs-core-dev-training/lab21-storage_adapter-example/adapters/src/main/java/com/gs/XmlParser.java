package com.gs;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class XmlParser {
    private boolean includeAttributes = true;
    private boolean includeTextContent = true;

    public Map<String, Object> parseXmlToProperties(String xmlString) throws IOException {
        if (xmlString == null || xmlString.isEmpty()) {
            return new HashMap<>();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            doc.getDocumentElement().normalize();
            Map<String, Object> result = new LinkedHashMap<>();
            parseElement(doc.getDocumentElement(), result);
            return result;
        } catch (Exception e) {
            throw new IOException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    private void parseElement(Element element, Map<String, Object> parent) {
        String tagName = element.getTagName();
        NodeList children = element.getChildNodes();

        boolean hasChildElements = false;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                hasChildElements = true;
                break;
            }
        }

        if (!hasChildElements) {
            if (includeTextContent) {
                parent.put(tagName, element.getTextContent().trim());
            }
        } else {
            Map<String, Object> childMap = new LinkedHashMap<>();
            if (includeAttributes) {
                NamedNodeMap attrs = element.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    childMap.put("@" + attr.getNodeName(), attr.getNodeValue());
                }
            }
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    parseElement((Element) child, childMap);
                }
            }
            parent.put(tagName, childMap);
        }
    }

    public void setIncludeAttributes(boolean includeAttributes) {
        this.includeAttributes = includeAttributes;
    }

    public boolean isIncludeAttributes() {
        return includeAttributes;
    }

    public void setIncludeTextContent(boolean includeTextContent) {
        this.includeTextContent = includeTextContent;
    }

    public boolean isIncludeTextContent() {
        return includeTextContent;
    }
}
