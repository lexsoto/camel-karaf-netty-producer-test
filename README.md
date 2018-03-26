Demonstrates an issue where using Camel netty4-http producer template to invoke a rest service returns null response body.

The NettyProducerIT uses Pax-Exam to launch an instance of Karaf with a Camel Rest route with a simple Echo Rest Service.
The Echo rest service returns the same payload.
The integration test sends a POST request to this service and fails to access the body from the exchange. 

To reproduce:

*   mvn clean install 
 