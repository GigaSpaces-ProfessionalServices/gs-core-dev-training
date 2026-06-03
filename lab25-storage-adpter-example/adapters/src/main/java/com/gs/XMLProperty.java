package com.gs;

import java.io.IOException;
import java.util.*;

public class XMLProperty {
    String xmlContent;
    Map<String,Object> properties;
    private XmlParser xmlParser = new XmlParser();

    public XMLProperty() {
    }

    public XMLProperty(String xmlContent) {
        try {
            setXmlContent(xmlContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public XMLProperty(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) throws IOException {
        this.xmlContent = xmlContent;
        setProperties(parseXmlToProperties(xmlContent));
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XMLProperty that = (XMLProperty) o;
        if (xmlContent != null && ((XMLProperty) o).getXmlContent() !=null)
            return xmlContent.equals(that.xmlContent);
        else {
            if (properties != null && that.properties !=null)
                return properties.equals(that.properties);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xmlContent);
    }

    private Map<String, Object> parseXmlToProperties(String xmlString) throws IOException {
        return xmlParser.parseXmlToProperties(xmlString);
    }

    @Override
    public String toString() {
        if (xmlContent != null) return xmlContent;
        if (properties!=null) return properties.toString();
        return "";
    }


}
