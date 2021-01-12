# cd src
# rm *.class
# javac *.java
# java Server
mvn package &&

gnome-terminal -e 'mvn exec:java -Dexec.mainClass="Server"'