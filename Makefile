all: testserver.class

clean:
	$(RM) *.class *~

testserver.class: testserver.java
	javac testserver.java


