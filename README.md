Demonstrates an issue where using Camel netty4-http producer template to invoke a rest service returns null response body.

The NettyProducerIT integration test class uses Pax-Exam to launch an instance of Karaf with a Blueprint Camel route with a simple netty4-http consumer which returns the same payload it receives. 

The test sends a POST request to this endpoint and fails to access the body of the exchange's response. 

To reproduce:

*   mvn clean install 
 
