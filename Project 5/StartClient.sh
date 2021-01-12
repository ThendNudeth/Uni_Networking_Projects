# cd src
# rm *.class
# javac *.java
# java ClientInitGUI $1
mvn package &&

gnome-terminal -e 'mvn exec:java -Dexec.mainClass="ClientInitGUI" -Dexec.args="127.0.0.1"'
