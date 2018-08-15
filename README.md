Schibsted Web Application
=========================

Build
-----

To generate the runnable jar file execute the following:

### Generate Jar
``` $ gradle jar``` 

Run
---

To run the application execute the following:

### Run
```$ java -jar build/libs/schibsted-${VERSION}.jar```

### Debug
```$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -jar build/libs/schibsted-${VERSION}.jar```

When the application starts, it performs some initializations which end up in the following message:

```Web application started successfully.```

After this message appears in console, the following URLs will be available on the system:

* http://localhost:9090/login
* http://localhost:9090/page1 (only accessible when accessing as a user with the role "PAGE_1")
* http://localhost:9090/page2 (only accessible when accessing as a user with the role "PAGE_2")
* http://localhost:9090/page3 (only accessible when accessing as a user with the role "PAGE_3")
* http://localhost:9090/logout
* http://localhost:9090/api/users (only accessible when accessing with the admin credentials)

The system has a built-in security system. Login credentials follow:

| User       | Password | Roles          |
|------------|----------|----------------|
| page1user  | 1234     | PAGE_1         |
| page2user  | 1234     | PAGE_2         |
| page3user  | 1234     | PAGE_3         |
| page12user | 1234     | PAGE_1, PAGE_2 |
| admin      | 1234     | ADMIN          |

REST API
--------

The REST API enables user querying, creation, modification and deletion by using the standard http methods GET, POST, PUT and DELETE.

