mvn package &&

gnome-terminal -e 'mvn exec:java -Dexec.mainClass="SenderGUI"'
gnome-terminal -e 'mvn exec:java -Dexec.mainClass="ReceiverGUI"'