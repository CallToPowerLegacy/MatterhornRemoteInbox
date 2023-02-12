rm -rf ./felix-cache/*
java -Dfelix.config.properties=file:./conf/config.properties -Dbundles.configuration.location=./conf -Dlog4j.configuration=file:./conf/services/org.ops4j.pax.logging.properties -Dorg.ops4j.pax.logging.DefaultServiceLog.level=INFO -jar bin/felix.jar ./felix-cache
