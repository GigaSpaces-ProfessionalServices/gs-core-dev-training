package com.gs.grpc.common;

import com.gigaspaces.rest.grpc.proto.SpaceDocument;
import com.gigaspaces.rest.grpc.proto.Value;

import java.util.Map;
import java.util.TreeMap;

/**
 * Converting to and from the gRPC {@code Value}. It has four kinds only - string, int64, double, bool -
 * and "not set" for null. Every other Java type has to be carried as one of these.
 */
public final class Values {

    private Values() {
    }

    public static Value of(Object value) {
        Value.Builder v = Value.newBuilder();
        switch (value) {
            case null -> { }
            case String s -> v.setStringValue(s);
            case Integer i -> v.setInt64Value(i);
            case Long l -> v.setInt64Value(l);
            case Double d -> v.setDoubleValue(d);
            case Boolean b -> v.setBoolValue(b);
            default -> throw new IllegalArgumentException("The gRPC Value has no kind for " + value.getClass().getName());
        }
        return v.build();
    }

    /** A Value back to Java. An int64 always comes back as a Long, whatever the space property type is. */
    public static Object toJava(Value v) {
        return switch (v.getKindCase()) {
            case STRING_VALUE -> v.getStringValue();
            case INT64_VALUE -> v.getInt64Value();
            case DOUBLE_VALUE -> v.getDoubleValue();
            case BOOL_VALUE -> v.getBoolValue();
            case KIND_NOT_SET -> null;
        };
    }

    /** Builds a document from name/value pairs: {@code doc("Product", "id", "p1", "price", 9.5)}. */
    public static SpaceDocument doc(String type, Object... nameValuePairs) {
        SpaceDocument.Builder doc = SpaceDocument.newBuilder().setTypeName(type);
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            doc.putProperties((String) nameValuePairs[i], of(nameValuePairs[i + 1]));
        }
        return doc.build();
    }

    /** A document on one line, properties sorted, with the kind of each value: {@code price=9.5(double)}. */
    public static String format(SpaceDocument doc) {
        StringBuilder s = new StringBuilder(doc.getTypeName()).append(" {");
        String sep = "";
        for (Map.Entry<String, Value> p : new TreeMap<>(doc.getPropertiesMap()).entrySet()) {
            s.append(sep).append(p.getKey()).append('=').append(toJava(p.getValue()))
                    .append('(').append(kind(p.getValue())).append(')');
            sep = ", ";
        }
        return s.append('}').toString();
    }

    public static String kind(Value v) {
        return switch (v.getKindCase()) {
            case STRING_VALUE -> "string";
            case INT64_VALUE -> "int64";
            case DOUBLE_VALUE -> "double";
            case BOOL_VALUE -> "bool";
            case KIND_NOT_SET -> "not set";
        };
    }
}
