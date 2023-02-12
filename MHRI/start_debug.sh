sudo rm -rf ./felix-cache/*
sudo rm -rf ./database/*
sudo rm -rf ./PathToInbox/*
sudo rm -rf ./logs/*
sudo rm -rf ./mhri.key
java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=n -Dfelix.config.properties=file:./conf/config.properties -Dbundles.configuration.location=./conf -Dlog4j.configuration=file:./conf/services/org.ops4j.pax.logging.properties -Dorg.ops4j.pax.logging.DefaultServiceLog.level=INFO -jar bin/felix.jar ./felix-cache
