all: compile
	@echo -e '[INFO] Done!'
clean:
	@echo -e '[INFO] Cleaning Up..'	
	@-rm -rf src/main/java/cs455/scaling/**/*.class

compile: 
	@echo -e '[INFO] Compiling the Source..'
	@javac src/main/java/cs455/scaling/**/*.java
