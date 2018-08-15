package lsunol.schibsted.application;

import java.lang.reflect.Method;
import java.util.Arrays;

public class ClassManagement {

    /**
     * Returns true if the <code>queriedClass</code> is a subclass of <code>subclassOf</code>. False otherwise.
     * @param queriedClass {@link Class} expected to be (or not) subclass of <code>subclassOf</code>.
     * @param subclassOf {@link Class} expected to be (or not) superclass of <code>queriedClass</code>.
     * @return true if the <code>queriedClass</code> is a subclass of <code>subclassOf</code>. False otherwise.
     */
    public static boolean isSubclassOf(Class<?> queriedClass, Class<?> subclassOf) {
        Class<?> superClass = queriedClass.getSuperclass();
        if (superClass == null) return false;
        else if (superClass.getName().equals(subclassOf.getName())) return true;
        else return isSubclassOf(superClass, subclassOf);
    }

    /**
     * Returns true if the <code>queriedClass</code> is an implementation of <code>implementsTo</code>. False otherwise.
     * @param queriedClass {@link Class} expected to be (or not) an implementation of <code>implementsTo</code>.
     * @param implementsTo {@link Class} expected to be (or not) an interface of <code>queriedClass</code>.
     * @return true if the <code>queriedClass</code> is an implementation of <code>implementsTo</code>. False otherwise.
     */
    public static boolean isImplementation(Class<?> queriedClass, Class<?> implementsTo) {
        if (queriedClass == null) return false;
        else {
            Class[] interfaces = queriedClass.getInterfaces();
            Class clz = Arrays.stream(interfaces).filter(interf4ce -> interf4ce.getName().equals(implementsTo.getName())).findFirst().orElse(null);
            if (clz != null) return true;
            else return isImplementation(queriedClass.getSuperclass(), implementsTo);
        }
    }

    /**
     * Returns true if the <code>queriedClass</code> is a subclass or an implementation of <code>implementsToOrSubclassOf</code>. False otherwise.
     * @param queriedClass {@link Class} expected to be (or not) an implementation or a subclass of <code>implementsToOrSubclassOf</code>.
     * @param implementsToOrSubclassOf {@link Class} expected to be (or not) an interface or superclass of <code>queriedClass</code>.
     * @return true if the <code>queriedClass</code> is a subclass or an implementation of <code>implementsToOrSubclassOf</code>. False otherwise.
     */
    public static boolean isSubclassOrImplements(Class<?> queriedClass, Class<?> implementsToOrSubclassOf) {
        return isSubclassOf(queriedClass, implementsToOrSubclassOf) || isImplementation(queriedClass, implementsToOrSubclassOf);
    }

    /**
     * Returns a new array containing the parameters the <code>method</code> expects in its invocation.
     * @param method {@link Method} the parameters from which are expected to be returned.
     * @param availableParameters possible candidates to be used as parameters for <code>method</code>. Only one type of
     *                            each primitive or raw type can be used, as it is the way to determine which parameters
     *                            fit with the <code>method</code> definition.
     * @return a new array containing the parameters the <code>method</code> expects in its invocation.
     */
    public static Object[] getMethodParameters(Method method, Object... availableParameters) {
        Class[] parametersSpec = method.getParameterTypes();
        Object[] parameters = new Object[parametersSpec.length];
        int i = 0;
        for (Class expectedParam : parametersSpec) {
            for (Object availableParam : availableParameters) {
                if (availableParam != null &&
                        (isSamePrimitiveTypeAs(availableParam, expectedParam) ||
                                availableParam.getClass().equals(expectedParam) ||
                                        isSubclassOrImplements(availableParam.getClass(), expectedParam)
                        )
                ) parameters[i] = availableParam;
            }
            i++;
        }
        return parameters;
    }

    /**
      * Returns true whether the type of the <code>object</code> param is the same java primitive of <code>clz</code>.
     * @param object {@link Object} to be compared with <code>clz</code>.
     * @param clz expected {@link Class} type.
     * @return true whether the type of the <code>object</code> param is the same java primitive of <code>clz</code>.
     * False otherwise.
     */
    private static boolean isSamePrimitiveTypeAs(Object object, Class clz) {
        if (object.getClass().equals(Byte.class) && clz.getName().toLowerCase().contains("byte")) return true;
        if (object.getClass().equals(Short.class) && clz.getName().toLowerCase().contains("short")) return true;
        if (object.getClass().equals(Integer.class) && clz.getName().toLowerCase().contains("int")) return true;
        if (object.getClass().equals(Long.class) && clz.getName().toLowerCase().contains("long")) return true;
        if (object.getClass().equals(Float.class) && clz.getName().toLowerCase().contains("float")) return true;
        if (object.getClass().equals(Double.class) && clz.getName().toLowerCase().contains("double")) return true;
        if (object.getClass().equals(Character.class) && clz.getName().toLowerCase().contains("char")) return true;
        if (object.getClass().equals(Boolean.class) && clz.getName().toLowerCase().contains("boolean")) return true;
        return false;
    }
}
