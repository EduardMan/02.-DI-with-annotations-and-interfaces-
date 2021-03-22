package tech.itpark.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class Container {
    private final Map<String, Object> values = new HashMap<>();
    private final Map<Class<?>, Object> objects = new HashMap<>();
    private final Set<Class<?>> definitions = new HashSet<>();

    public void register(Class<?>... definitions) {
        String badDefinitions = Arrays.stream(definitions)
                .filter(o -> o.getDeclaredConstructors().length != 1)
                .map(o -> o.getName())
                .collect(Collectors.joining(", "));
        if (!badDefinitions.isEmpty()) {
            throw new AmbiguousConstructorException(badDefinitions);
        }

        this.definitions.addAll(Arrays.asList(definitions));
    }

    public void register(String name, Object value) {
        if (values.containsKey(name)) {
            throw new AmbiguousValueNameException(String.format("%s with value %s", name, value.toString()));
        }

        values.put(name, value);
    }

    public void wire() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        HashSet<Class<?>> todo = new HashSet<>(definitions);
        if (todo.isEmpty()) {
            return;
        }

        while (!todo.isEmpty()) {
            final int todoBeginStageSize = todo.size();

            final Iterator<Class<?>> clzIterator = todo.iterator();
            while (clzIterator.hasNext()) {
                final Class<?> clz = clzIterator.next();
                final Constructor<?> declaredConstructor = clz.getDeclaredConstructors()[0];
                boolean allArgumentsAlreadyInitializedOr0 = allArgumentsAlreadyInitializedOr0(declaredConstructor);

                if (allArgumentsAlreadyInitializedOr0) {
                    Set<Object> filledConstructorParameters = fillConstructorParametersWithArguments(declaredConstructor);
                    fillObjectsMapWithClassAndInterfaces(clz, declaredConstructor.newInstance(filledConstructorParameters.toArray()));
                    clzIterator.remove();
                }
            }

            if (todoBeginStageSize == todo.size()) {
                throw new UnmetDependenciesException(todo.toString());
            }
        }
    }

    private boolean allArgumentsAlreadyInitializedOr0(Constructor<?> constructor) {
        final Parameter[] parameters = constructor.getParameters();

        if (parameters.length == 0) {
            return true;
        } else {
            for (Parameter parameter :
                    parameters) {

                if (parameter.isAnnotationPresent(Inject.class)) {
                    if (!values.containsKey(parameter.getAnnotation(Inject.class).value())) {
                        return false;
                    }
                } else if (!objects.containsKey(parameter.getType())) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<Object> fillConstructorParametersWithArguments(Constructor<?> constructor) {
        Set<Object> filledConstructorParameters = new HashSet<>();

        for (Parameter constructorParameter :
                constructor.getParameters()) {
            if (constructorParameter.isAnnotationPresent(Inject.class)) {
                filledConstructorParameters.add(values.get(constructorParameter.getAnnotation(Inject.class).value()));
            } else {
                filledConstructorParameters.add(objects.get(constructorParameter.getType()));
            }
        }

        return filledConstructorParameters;
    }

    private void fillObjectsMapWithClassAndInterfaces(Class<?> clz, Object instance) {
        objects.put(clz, instance);
        for (Class<?> iface :
                clz.getInterfaces()) {
            objects.put(iface, instance);
        }
    }
}
