package com.gs;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class XmlParser {
    private boolean includeAttributes = true;
    private boolean includeTextContent = true;

    public XmlParser() {
    }

    public XmlParser(boolean includeAttributes, boolean includeTextContent) {
        this.includeAttributes = includeAttributes;
        this.includeTextContent = includeTextContent;
    }

    public void setIncludeAttributes(boolean include) {
        this.includeAttributes = include;
    }

    public boolean isIncludeAttributes() {
        return includeAttributes;
    }

    public void setIncludeTextContent(boolean include) {
        this.includeTextContent = include;
    }

    public boolean isIncludeTextContent() {
        return includeTextContent;
    }

    public Map<String, Object> parseXmlToProperties(String xmlString) throws IOException {
        if (xmlString == null || xmlString.isEmpty()) {
            return new HashMap<>();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));

            Element rootElement = doc.getDocumentElement();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(rootElement.getNodeName(), elementToProperties(rootElement));
            return result;
        } catch (Exception e) {
            throw new IOException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    public Object elementToProperties(Element element) {
        Map<String, Object> properties = new LinkedHashMap<>();

        // Add attributes if enabled
        if (includeAttributes && element.hasAttributes()) {
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                org.w3c.dom.Attr attr = (org.w3c.dom.Attr) element.getAttributes().item(i);
                properties.put("@" + attr.getName(), attr.getValue());
            }
        }

        // Process child elements and text content
        Map<String, List<Object>> childrenMap = new LinkedHashMap<>();
        StringBuilder textContent = new StringBuilder();
        boolean hasElementChildren = false;

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                hasElementChildren = true;
                Element childElement = (Element) node;
                String childName = childElement.getNodeName();
                Object childValue = elementToProperties(childElement);

                childrenMap.computeIfAbsent(childName, k -> new ArrayList<>()).add(childValue);
            } else if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getNodeValue();
                if (includeTextContent && !text.trim().isEmpty()) {
                    textContent.append(text.trim());
                }
            }
        }

        // Build result
        if (!hasElementChildren && textContent.length() > 0) {
            // Element with only text content
            if (properties.isEmpty()) {
                return textContent.toString();
            } else {
                properties.put("#text", textContent.toString());
                return properties;
            }
        } else if (hasElementChildren) {
            // Element with child elements
            for (Map.Entry<String, List<Object>> entry : childrenMap.entrySet()) {
                List<Object> values = entry.getValue();
                if (values.size() == 1) {
                    properties.put(entry.getKey(), values.get(0));
                } else {
                    properties.put(entry.getKey(), values);
                }
            }

            if (textContent.length() > 0) {
                properties.put("#text", textContent.toString());
            }

            return properties;
        } else {
            // Empty element
            return properties.isEmpty() ? "" : properties;
        }
    }
}
