# cd src
# javac *.java
# java InitClientGUI
mvn package &&

gnome-terminal -e 'mvn exec:java -Dexec.mainClass="InitClientGUI"'