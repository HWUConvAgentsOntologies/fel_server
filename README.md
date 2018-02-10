# Fast Entity Linking RESTful service

**fel_server** is a Spring application that exposes a RESTful API for a custom version of the Fast Entity Linking (FEL) service from Yahoo.
At the moment the service returns a list of candidate annotations for each span of text in the source text specified in the request.
The server listens to the port 8080 by default and it can answer to POST requests with JSON-formatted body. An example of request could be the following:

```json
{
  "text": "I really love Star Wars.",
  "types": ["http://www.wikidata.org/entity/Q5", "http://www.wikidata.org/entity/Q11424", "http://www.wikidata.org/entity/Q7889"]
}
```

In the example request the `text` field (compulsory) specifies the source text and the `types` field represents a list of Wikidata identifiers that can be used so as to filter out 
not desired annotations types.

## Installation

In order to corerctly install the service the FEL library should be installed first. In order to do that, we recommend
cloning the [FEL repository](https://github.com/aleSuglia/FEL/tree/master) and then execute `mvn install` (be sure that the Maven package is installed).

If there are no errors in the installation procedure you can proceed with the installation of the Spring service.
Again it is a Maven application that you can easily install or compile and execute. Before executing the service 
be sure to have the FEL model on your computer. After that, clone this repository and copy the file
`src/main/resources/application-alessandro.properties` in the same directory giving to it an identifier name that you prefer 
(I will use $PROFILE_ID$ to denote it here). In addition, you need to edit the `fel_server.hash_filename` field by specifying 
the path to the FEL model file. If required, feel free to change the other fields.

At this point, you can execute the service by using the following command:
```
mvn spring-boot:run -Dspring.profiles.active=$PROFILE_ID$
``` 
where $PROFILE_ID$ should be replace with the identifier that you have used before as a name for the properties file.
It is important to specify it correctly because the system will automatically load all the parameters from that file.