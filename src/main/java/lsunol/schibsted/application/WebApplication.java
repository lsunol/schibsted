package lsunol.schibsted.application;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import lsunol.schibsted.controllers.ApplicationController;
import lsunol.schibsted.controllers.IApplicationController;
import lsunol.schibsted.database.RepositoryManager;
import lsunol.schibsted.database.UserRepository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static lsunol.schibsted.application.ClassManagement.isSubclassOf;

public class WebApplication {

    private final static Logger log = Logger.getLogger(WebApplication.class.getName());

    private static HttpServer server = null;

    public static void main(String[] args) {
        try {
            // Create the Http Server
            server = HttpServer.create(new InetSocketAddress(ApplicationConstants.SERVER_PORT), 0);

            // Set single threaded executor service
            server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());

            // Initialization of starting users registry
            UserRepository userRepo = RepositoryManager.getUserRepository();
            userRepo.addNewUser("admin", "1234", Arrays.asList(ApplicationConstants.ADMIN_ROLENAME));
            userRepo.addNewUser("page1user", "1234", Arrays.asList("PAGE_1"));
            userRepo.addNewUser("page2user", "1234", Arrays.asList("PAGE_2"));
            userRepo.addNewUser("page3user", "1234", Arrays.asList("PAGE_3"));
            userRepo.addNewUser("page12user", "1234", Arrays.asList("PAGE_1", "PAGE_2"));

            // Retrieve the list of registrable controllers
            List<Class> webControllers = getWebControllersList();

            // Initializing web controllers
            webControllers.forEach(controller -> {
                try {
                    ApplicationController controllerInstance = (ApplicationController) controller.newInstance();
                    log.info("Registering controller: " + controllerInstance.getRequestMapping());
                    HttpContext context = server.createContext(controllerInstance.getRequestMapping(), controllerInstance);
                    Method authenticatorMethod = Arrays.stream(controller.getMethods()).filter(method -> method.getReturnType().equals(Authenticator.class)).findFirst().orElse(null);
                    if (authenticatorMethod != null) {
                        try {
                            context.setAuthenticator((Authenticator) authenticatorMethod.invoke(controllerInstance));
                        } catch (InvocationTargetException e) {
                            log.log(Level.SEVERE, "An error occurred while retrieving the authenticator from the controller: " + e.getMessage(), e);
                        }
                    }
                } catch (InstantiationException | IllegalAccessException iae) {
                    log.severe("Could not instantiate class '" + controller + "' and thus could not initialize its expected request path.");
                }
            });
            server.start();
            log.info("Web application started successfully.");
        } catch (Exception e) {
            log.log(Level.SEVERE, "An error occurred while starting the web application server.", e);
        }
    }

    /**
     * Returns a list containing all the instantiable Classes (web controllers) that inherit from {@link IApplicationController}.
     *
     * @return a list containing all the instantiable Classes (web controllers) that inherit from {@link IApplicationController}.
     * @throws IOException            if any error occurred while scanning package classes in search of web controllers.
     * @throws URISyntaxException     if any error occurred while scanning package classes in search of web controllers.
     * @throws ClassNotFoundException if any error occurred while scanning package classes in search of web controllers.
     */
    private static List<Class> getWebControllersList() throws IOException, URISyntaxException, ClassNotFoundException {
        List<Class> webControllers = new LinkedList<>();
        Package controllersPackage = IApplicationController.class.getPackage();
        ArrayList<String> myClasses = getClassNamesFromPackage(controllersPackage.getName());
        for (String classInPackage : myClasses) {
            Class<?> controller = ClassLoader.getSystemClassLoader().loadClass(controllersPackage.getName() + "." + classInPackage.replace("/", ""));
            if (isSubclassOf(controller, ApplicationController.class) && !Modifier.isAbstract(controller.getModifiers()) && !Modifier.isInterface(controller.getModifiers())) {
                webControllers.add(controller);
            }
        }
        return webControllers;
    }

    /**
     * Returns the list of class names that belong to the Package having the fully qualified name specified in <code>packageName</code>.
     *
     * @param packageName fully qualified package name.
     * @return the list of class names that belong to the Package having the fully qualified name specified in <code>packageName</code>.
     * @throws IOException
     * @throws URISyntaxException
     */
    private static ArrayList<String> getClassNamesFromPackage(String packageName) throws IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<String>();

        packageName = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageName);

        if (packageURL.getProtocol().equals("jar")) {
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            // build jar file name, then loop through zipped entries
            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
            log.info(">" + jarFileName);
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            while (jarEntries.hasMoreElements()) {
                entryName = jarEntries.nextElement().getName();
                if (entryName.startsWith(packageName) && !entryName.endsWith("/") && !entryName.contains("$") && entryName.length() > packageName.length() + 5) {
                    entryName = entryName.substring(packageName.length(), entryName.lastIndexOf('.'));
                    if (!entryName.substring(1).contains("/")) names.add(entryName);
                }
            }
            // loop through files in classpath
        } else {
            URI uri = new URI(packageURL.toString());
            File folder = new File(uri.getPath());
            // won't work with path which contains blank (%20)
            // File folder = new File(packageURL.getFile());
            File[] contenuti = folder.listFiles();
            String entryName;
            for (File actual : contenuti) {
                entryName = actual.getName();
                if (entryName.contains(".")) {
                    entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                    names.add(entryName);
                } // else -> is not a java file class, but a sub-package
            }
        }
        return names;
    }
}
