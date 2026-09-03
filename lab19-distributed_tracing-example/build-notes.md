**Note**: as of `exec-maven-plugin` 3.6.x, this command as written fails with
`NoClassDefFoundError: zipkin2/reporter/okhttp3/OkHttpSender` - `xap-reporter`'s Zipkin sender is a
`runtime`-scope transitive dependency, not `compile`-scope, so `-Dexec.classpathScope=compile` excludes it.
Switching to `-Dexec.classpathScope=runtime` resolves the classpath but can hang indefinitely on JVM exit
(a known `exec-maven-plugin` 3.6.x issue with its isolated-classloader shutdown hook). The reliable way to
run this class is a plain `java` invocation with the runtime classpath built by Maven:
```
mvn dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile=cp.txt
java -cp "target/classes:$(cat cp.txt)" -Dcom.gs.jini_lus.locators=localhost -Dcom.gs.jini_lus.groups=xap-17.3.0 com.gs.TraceHelper
```

