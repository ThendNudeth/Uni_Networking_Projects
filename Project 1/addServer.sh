mvn package &&

gnome-terminal -e 'mvn exec:java -Dexec.mainClass="ChatServer"'
