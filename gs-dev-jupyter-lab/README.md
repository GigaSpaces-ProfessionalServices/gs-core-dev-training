# Jupyter notebook setup

## Installation requirements

| Component | Comment |
| --- | --- |
| Java | Java 17 is needed to try the code examples |
| python3 | v 3.8.10 or higher |
| pip | Python package manager |
| jupyter | pip3 install jupyter |
| Ganymede Java kernel | https://github.com/allen-ball/ganymede |

### Note
The Jupyter install copies executable files to a local directory, such as `/home/dixson/.local/bin`. The Jupyter installation __WARNed__ this was not in the `PATH`.

Update the path in the `~/.profile`. For example, you will need to add this PATH location.
```
export PATH=$PATH:/home/dixson/.local/bin
```

## Ganymede kernel installation

1. Download the Ganymede.zip

2. Install the Ganymede kernel. Example script is below.

```
# start ganymedeinstall.sh script
# install the kernel

JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

GANYMEDE_JAR="ganymede-2.1.2.20230910.jar"

$JAVA_HOME/bin/java \
      -jar $GANYMEDE_JAR \
      -i \
      --id-suffix=gs-dev-lab-1.0 --display-name-suffix="with gs-dev-lab 1.0"

# end ganymedeinstall.sh script
```

3. Validate the kernel just installed

```
jupyter kernelspec list
Available kernels:
  ganymede-2.1.2-java-17-gs-dev-lab-1.0    /home/dixson/.local/share/jupyter/kernels/ganymede-2.1.2-java-17-gs-dev-lab-1.0
  python3                                  /home/dixson/.local/share/jupyter/kernels/python3
```

## Update the kernel.json to include GigaSpace lookup locators

Update the kernel.json to include the System Property `-Dcom.gs.jini_lus.locators="localhost"`. This will be needed to remote connect to your GigaSpaces installation.

See example file `/home/dixson/.local/share/jupyter/kernels/ganymede-2.1.2-java-17-gs-dev-lab-1.0/kernel.json` below:

```
{
  "argv" : [ "/usr/lib/jvm/java-17-openjdk-amd64/bin/java", "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED", "--illegal-access=permit", "-Djava.awt.headless=true", "-Djdk.disableLastUsageTracking=true", "-Dcom.gs.jini_lus.locators=localhost", "-jar", "/home/dixson/.local/share/jupyter/kernels/ganymede-2.1.2-java-17-gs-dev-lab-1.0/kernel.jar", "-f", "{connection_file}" ],
  "display_name" : "Ganymede 2.1.2 (Java 17) with gs-dev-lab 1.0",
  "env" : {
    "JUPYTER_CONFIG_DIR" : "/home/dixson/.jupyter",
    "JUPYTER_CONFIG_PATH" : "/home/dixson/.jupyter:/home/dixson/.local/etc/jupyter:/usr/etc/jupyter:/usr/local/etc/jupyter:/etc/jupyter",
    "JUPYTER_DATA_DIR" : "/home/dixson/.local/share/jupyter",
    "JUPYTER_RUNTIME_DIR" : "/home/dixson/.local/share/jupyter/runtime"
  },
  "interrupt_mode" : "message",
  "language" : "java"
}
```
## Start the jupyter notebook

Go to the `gs-dev-jupyter-lab` directory.
```
jupyter notebook
```



