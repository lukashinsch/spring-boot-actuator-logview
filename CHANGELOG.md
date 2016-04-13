# spring-boot-actuator-logview changelog

## 0.2.10
- Set content type in log response (fixes [#21](https://github.com/lukashinsch/spring-boot-actuator-logview/issues/21))

## 0.2.9
- Add configuration option to override stylesheet urls (fixes [#20](https://github.com/lukashinsch/spring-boot-actuator-logview/issues/20))

## 0.2.8
- Remove tail option for files inside archives in UI (lead to 500 error before) (fixes [#17](https://github.com/lukashinsch/spring-boot-actuator-logview/issues/17))

## 0.2.7
- Correctly encode file/dir names in urls (fixes - [#19](https://github.com/lukashinsch/spring-boot-actuator-logview/issues/19))

## 0.2.6
- Fixed display issue with illegal whitespace
- Fix cdn url

## 0.2.5
- allow downloading only the last 50 lines of each file (closes [#13](https://github.com/lukashinsch/spring-boot-actuator-logview/pull/13))

## 0.2.4
- show file content when calling endpoint url without trailing slash (fixes [#11](https://github.com/lukashinsch/spring-boot-actuator-logview/issues/11))
- allow to specify logging path via "endpoints.logview.path" property (fixes [#3](https://github.com/lukashinsch/spring-boot-actuator-logview/issues/3)

## 0.2.3
- don't require trailing slash in endpoint url ("/log" instead of "/log/")

## 0.2.2
- allow viewing logfiles in subfolders inside archives

## 0.2.1
- switch to use dedicated freemarker config to allow loading template from jar
- use spring boot autoconfig

## 0.2
- basic support for searching logfiles (no archives, no subfolders)

## 0.1
- initial release
