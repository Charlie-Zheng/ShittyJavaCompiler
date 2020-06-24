Please run CUP with the -locations flag. The below compiles the CUP and jFlex files into Java, then compiles the Java files

java -jar "java-cup-11b.jar" -locations NewParser.cup && jflex test.flex && javac -d ./ -cp ./java-cup-11b-runtime.jar:. *.java



Execute using:

java -cp "java-cup-11b-runtime.jar:." Scan ./ms3/Pass/*.t1