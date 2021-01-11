# cd src
# javac *.java
# java InitServerGUI $1
mvn package &&

gnome-terminal -e 'mvn exec:java -Dexec.mainClass="InitServerGUI" -Dexec.args="127.0.0.1"'