for (( i=1; i<=$1; i++ ))
do
	gnome-terminal -e 'mvn exec:java -Dexec.mainClass="initGUI"'
done

