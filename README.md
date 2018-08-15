Schibsted Web Application
=========================

Build
-----

### Generate Jar
``` $ gradle jar``` 

Run
---

### Run
```$ java -jar build/libs/schibsted-${VERSION}.jar```

### Debug
```$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -jar build/libs/schibsted-${VERSION}.jar```


