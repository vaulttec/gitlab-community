GitLab Community  [![Build Status](https://travis-ci.org/vaulttec/gitlab-community.svg?branch=master)](https://travis-ci.org/vaulttec/gitlab-community) [![Docker Image](https://img.shields.io/docker/pulls/tjuerge/gitlab-community.svg)](https://hub.docker.com/r/tjuerge/gitlab-community)
===============

Spring Boot application which uses [GitLab Subgroups](https://docs.gitlab.com/ee/user/group/subgroups/) together with  [Mattermost Private Channels](https://docs.mattermost.com/help/getting-started/organizing-conversations.html#private-channels) to provide a community website.
 

## Install Maven Wrapper
```
cd /path/to/project
mvn -N io.takari:maven:wrapper
```

## Run the project with

```
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=test
```

Open browser to http://localhost:8080/


## To package the project run

```
./mvnw clean package
```
