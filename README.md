## Intro

This is a Java web application used for calculate top percentile response time from logs
Use an array to store response time as index, count as value
Also provides bucket/sampling approximation to deal with big response time/large amount of logs
 
Assume total lines of logs is N, max response time is M (in ms)
Time-complexity: O(N)
Space-complexity: O(M)

## Dependency

- Java8
- Maven3

## Build

mvn clean install

java -jar *.jar(default using 8080 port)

curl -X GET http://localhost:8080/stats/tp?percentiles=90,95,99

## Config

Default config could be modified by external configuration properties.

Create config/application.properties

The detailed config is in ConfigProperties.java
