# spring-boot-actuator-logview changelog

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
