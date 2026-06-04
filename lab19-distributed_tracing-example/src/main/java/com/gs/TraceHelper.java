package com.gs;
import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.tracing.ZipkinTracerBean;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.concurrent.Callable;

    public class TraceHelper {
    private static ZipkinTracerBean tracingBean;
    private static TraceHelper helper;
    public static String zipkinurl = "http://localhost:9411/";


    protected TraceHelper() {
        tracingBean = new ZipkinTracerBean("test").setStartActive(true).setZipkinUrl(zipkinurl);
        tracingBean.start();   // registers the tracer with GlobalOpenTelemetry
    }

    static public synchronized TraceHelper getInstance() {
        if (helper == null)
            helper = new TraceHelper();
        return helper;
    }

    public static <T> T wrap(String name, Callable<T> c) throws Exception {
        Tracer tracer = GlobalOpenTelemetry.getTracer("com.gs.test");
        Span span = tracer.spanBuilder(name).startSpan();
        try (Scope scope = span.makeCurrent()) {
            return c.call();
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.toString());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    static GigaSpace gs;
    public static void main(String[] args) throws Exception {
        TraceHelper.getInstance();
        gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).create();
        wrap("write", () -> {
            write();
            return 0;
        });

        wrap("read", () -> {
            read();
            return 0;
        });

        close();
    }

    public static void read() {
        gs.read(new MyTest());
    }

    public static void write() {
        gs.write(new MyTest("3","myTest"));
    }

    public static void close(){
        if (tracingBean != null){
            try {
                tracingBean.destroy();
            }
            catch (Exception e){}
        }
    }

    @SpaceClass
    public static class MyTest implements java.io.Serializable{
        String id;
        String txt;

        public MyTest() {
        }

        public MyTest(String id, String txt) {
            this.id = id;
            this.txt = txt;
        }

        @SpaceId(autoGenerate = false)
        @SpaceRouting
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }


        public String getTxt() {
            return txt;
        }

        public void setTxt(String txt) {
            this.txt = txt;
        }
    }



}
