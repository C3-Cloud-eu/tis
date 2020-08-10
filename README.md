# C3-Cloud Technical Interoperability Suite

TIS is a full stack application comprised of Java backend, database and web frontend. 

C3-Cloud TIS enables patient data synchronization from local EHR to C3-Cloud. More specifically, TIS imports patient data from local EHR into C3-Cloud FHIR Repository either on demand or at scheduled intervals. 

NOTE: TIS requires integration with a Semantic and Structural Mapper to convert responses from EHR systems to HL7 FHIR format. IT also requires a FHIR repository to push data into. Connections to both can be configured in the `application.yml`.

The TIS application is still in development so functions (esp. tests) may not run as expected, if you find a bug, please report it to @omarisgreat

- Programming language
  - Backend: [Kotlin](https://kotlinlang.org), [Java](https://java.com/en/download/)
  - Frontend: [JavaScript](https://www.w3schools.com/js/default.asp)
- Framework
  - Backend: [Spring Boot 2.1](https://spring.io/projects/spring-boot)
  - Frontend: [Reactjs](https://reactjs.org), [Ant-design](https://ant.design)
- Database
  - [MongoDB 4.0](https://www.mongodb.com)

Detailed technical information on TIS requirements and design are included in the [C3-Cloud](https://c3-cloud.eu) deliverables D3.1, D3.3, D6.1, D7.4 and D8.3

## Installation

- The server folder contains the java backend project.
- The webapp folder contains the web frontend project.

The backend project is built with [Gradle](https://gradle.org). [IntelliJ IDE](https://www.jetbrains.com/idea/) is recommended to build and run the backend application during development, although the project can be built from command line too.   

- Install Java 8+
- Build the source (install gradle and gradlew). In the project folder `tis/server`, run:

```gradlew build```

- Run the backend application:

```./gradlew bootRun```

Note IDEs like Intellij have built-in support for Gradle and can run the above tasks from GUI.

The frontend project is built with [npm](https://www.npmjs.com). VS Code is recommended as the editor during development, but generally it is recommended to build and run the project from command line. 

- Install Node.js
- Build the source
  - In the project folder `tis/webapp`, from command line run: 

  ```npm run build```

  - This will download all dependency libs into local folder node_modules and generate all files in local folder build. 
- Run the frontend application in local development mode

  ```npm start```

  - This will start a local web server to run the web app. Open <http://localhost:3000> in the browser to see the app. 

Package a self-contained application including both backend and frontend. 
- Copy `tis/webapp/build` to `tis/server/src/main/resources`
- Rename the folder `resources/build` to `resources/static`
- In `tis/server`, run:
  ```gradlew bootJar```
  - This will generate a complete self-contained executable jar file `c3cloud-tis.jar` in `tis/server/build/libs`.

## Initialise the MongoDB Database

- Install MongoDB
- Create database tis
- Create collection Task
- Insert data integration task json documents into collection Task (located in `tis/mongodb`)
  - `mongodb/provider1-task.json` – data integration task for site using CDA/SOAP API
  - `mongodb/provider2-task.json` – data integration task for site using JSON API
  - `mongodb/provider3-task.json` – data integration task for site using CSV imports

## Run the application

- Copy `tis/server/build/libs/c3cloud-tis.jar` to any folder where want to run the application
- Copy `tis/server/application.yml` to the folder where c3cloud-tis.jar resides
- Run: 
```java –jar c3cloud-tis.jar```

This will start the application. Open <http://localhost> in browser to access the web UI.

## Configuration

To configure the application to connect to a new site, you will need to edit the `application.yml` file, replacing placeholders with API endpoints and secrets as well as credentials for accessing the TIS front end and configuring email notifications.

More detailed information regarding functionality and usage can be found in the C3-Cloud public deliverables or by contacting @omarisgreat.
