# Variables
MVN = mvn
MAIN_AGGREGATION_SERVER = com.mycompany.app.AggregationServer
MAIN_GET_CLIENT = com.mycompany.app.GETClient
MAIN_CONTENT_SERVER = com.mycompany.app.ContentServer

# Default target
all: compile

# Compile the project using Maven
compile:
	$(MVN) clean compile

# Run Aggregation Server
run-aggregation-server:
	$(MVN) exec:java -Dexec.mainClass="$(MAIN_AGGREGATION_SERVER)"

# Run GET Client
run-get-client:
	$(MVN) exec:java -Dexec.mainClass="$(MAIN_GET_CLIENT)" -Dexec.args="localhost:4567"

# Run Content Server
run-content-server:
	$(MVN) exec:java -Dexec.mainClass="$(MAIN_CONTENT_SERVER)" -Dexec.args="localhost:4567 /home/tonypham/distributedSystem2024S2/my-app/src/main/java/com/mycompany/app/dataFile1.txt"

# Run tests
test:
	$(MVN) test

# Clean up generated files
clean:
	$(MVN) clean

.PHONY: all compile run-aggregation-server run-get-client run-content-server test clean
