[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520catalogue%2520%28master%29%2520pipeline%2F)](https://jenkins.iudx.io/job/iudx%20catalogue%20(master)%20pipeline/lastBuild/)
[![Jenkins Coverage](https://img.shields.io/jenkins/coverage/jacoco?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520catalogue%2520%28master%29%2520pipeline%2F)](https://jenkins.iudx.io/job/iudx%20catalogue%20(master)%20pipeline/lastBuild/jacoco/)
[![Unit Tests](https://img.shields.io/jenkins/tests?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520catalogue%2520%28master%29%2520pipeline%2F&label=unit%20tests)](https://jenkins.iudx.io/job/iudx%20catalogue%20(master)%20pipeline/lastBuild/testReport/)
[![Performance Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520catalogue%2520%28master%29%2520pipeline%2F&label=performance%20tests)](https://jenkins.iudx.io/job/iudx%20catalogue%20(master)%20pipeline/lastBuild/performance/)
[![Security Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520catalogue%2520%28master%29%2520pipeline%2F&label=security%20tests)](https://jenkins.iudx.io/job/iudx%20catalogue%20(master)%20pipeline/lastBuild/zap/)
[![Integration Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520catalogue%2520%28master%29%2520pipeline%2F&label=integration%20tests)](https://jenkins.iudx.io/job/iudx%20catalogue%20(master)%20pipeline/lastBuild/Integration_20Test_20Report/)

![IUDX](./docs/iudx.png)
# iudx-catalogue-server
The catalogue is [IUDXs](https://iudx.org.in) data discovery and dataset metadata publishing portal.
It allows data providers to publish their data *resources* by making an IUDX vocabulary annotated meta-data document describing their datasource and affiliated terminologies.
The datasources publish their data to the IUDX *Resource Server*.
It allows consumers of such data to easily discover such *resources* by means of powerful
queries and consume the data from *Resource Servers* in an automated and machine interpretable way.

<p align="center">
<img src="./docs/cat_overview.png">
</p>


## Features
- Search and discovery of data resources hosted on IUDX platform
- Support for text, geo-spatial, relationship, list and attributes searches
- Upload, delete and modify operations on catalogue objects (meta-information corresponding to resources)
- Stores meta-information as JSON-LD documents using published vocabulary and attributes
- Scalable, service mesh architecture based implementation using open source components: Vert.X API framework, Elasticsearch for data-base
- Hazelcast and Zookeeper based cluster management and service discovery


## Live 
The live running instance of the IUDX catalogue can be found [here](https://catalogue.iudx.org.in).

## API Docs 
The api docs can be found [here](https://catalogue.iudx.org.in/apis).



## Get Started

### Prerequisite - Make configuration
Make a config file based on the template in `./configs/config-example.json` 
- Generate a certificate using Lets Encrypt or other methods
- Make a Java Keystore File and mention its path and password in the appropriate sections
- Modify the database url and associated credentials in the appropriate sections

### Docker based
1. Install docker and docker-compose
2. Clone this repo
3. Build the images 
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file you just created
5. Start the server in production (prod) or development (dev) mode using docker-compose 
   ` docker-compose up prod `


### Maven based
1. Install java 13 and maven
2. Use the maven exec plugin based starter to start the server 
   `mvn clean compile exec:java@catalogue-server`

### Redeployer
A hot-swappable redeployer is provided for quick development 
`./redeploy.sh`


### Keystore
The server requires certificates to be stored in Java keystore format.
1. Obtain certs for your domain using Letsencrypt. Note: Self signed certificates using openssl will also work.
2. Concat all pems into one file 
`sudo cat /etc/letsencrypt/live/demo.example.com/*.pem > fullcert.pem`
3. Convert to pkcs format 
` openssl pkcs12 -export -out fullcert.pkcs12 -in fullcert.pem`
4. Create new temporary keystore using JDK keytool, will prompt for password 
`keytool -genkey -keyalg RSA -alias mykeystore -keystore mykeystore.ks`  
`keytool -delete -alias mykeystore -keystore mykeystore.ks` 
5. Make JKS, will prompt for password 
`keytool -v -importkeystore -srckeystore fullcert.pkcs12 -destkeystore mykeystore.ks -deststoretype JKS`
6. Store JKS in config directory and edit the keyfile name and password entered in previous step
7. Mention keystore mount path (w.r.t docker-compose) in config.json



### Testing

### Unit tests
1. Run the server through either docker, maven or redeployer
2. Run the unit tests and generate a surefire report 
   `mvn clean test-compile surefire:test surefire-report:report`
3. Reports are stored in `./target/`


### Integration tests
Integration tests are through Postman/Newman whose script can be found from [here](./src/test/resources/iudx-catalogue-server-v4.0.postman_collection.json).
1. Install prerequisites 
   - [postman](https://www.postman.com/) + [newman](https://www.npmjs.com/package/newman)
   - [newman reporter-htmlextra](https://www.npmjs.com/package/newman-reporter-htmlextra)
2. Example Postman environment can be found [here](./configs/postman-env.json)
3. Run the server through either docker, maven or redeployer
4. Run the integration tests and generate the newman report 
   `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export .`
5. Reports are stored in `./target/`



## Contributing
We follow Git Merge based workflow 
1. Fork this repository
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name, or refer to a milestone name as mentioned in Github -> Projects  
4. Commit to your fork and raise a Pull Request with upstream


## License
[MIT](./LICENSE.txt)
