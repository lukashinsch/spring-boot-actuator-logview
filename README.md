[![Coverage Status](https://coveralls.io/repos/lukashinsch/spring-boot-actuator-logview/badge.svg?branch=master)](https://coveralls.io/r/lukashinsch/spring-boot-actuator-logview?branch=master)
[![Build Status](https://travis-ci.org/lukashinsch/spring-boot-actuator-logview.svg?branch=master)](https://travis-ci.org/lukashinsch/spring-boot-actuator-logview)

# spring-boot-actuator-logview
(Very) simple logfile viewer as spring boot actuator endpoint

Note: this is currently work-in-progress:
* no library yet, includes sample app

Features
* allow quick access to spring-boot web application logfiles
* uses actuator framework to provide management endpoint

Planned features
* browser sub-directories
* look inside archived (compressed) log files
* search in files
* support other templating engines other than freemarker

Dependencies
* spring boot (web, actuator, freemarker) (tested with 1.2.1.RELESE)
* prettytime
* commons-io

Howto use
* include LogViewerEndpoint and logview.ftl in project
* configure logging.path in spring environment
* endpoint will be available under <management-base>/log
