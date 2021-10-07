package it.geosolutions.jaiext.jiffle.parser.node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.geosolutions.jaiext.jiffle.parser.JiffleType;

public class ImageProperty extends Expression {

    public enum Property {
        BANDS("bands", "getBands", JiffleType.D);

        private final JiffleType type;
        private final String name;
        private final String runtimeMethod;

        Property(String name, String runtimeMethod, JiffleType type) {
            this.name = name;
            this.runtimeMethod = runtimeMethod;
            this.type = type;
        }

        public JiffleType getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getRuntimeMethod() {
            return runtimeMethod;
        }
    }

    private final Property property;
    private final String varName;

    public ImageProperty(String varName, String property) {
        this(varName, lookupProperty(property));
    }

    public static Property lookupProperty(String property) {
        Map<String, Property> properties =
                Arrays.stream(Property.values())
                        .collect(Collectors.toMap(p -> p.getName(), p -> p));
        Property result = properties.get(property);
        if (result == null) {
            List<String> keys = properties.keySet().stream().sorted().collect(Collectors.toList());
            throw new IllegalArgumentException(
                    "Could not find a property named '"
                            + property
                            + "', available values are: "
                            + keys);
        }
        return result;
    }

    public ImageProperty(String varName, Property property) {
        super(property.getType());
        this.varName = varName;
        this.property = property;
    }

    @Override
    public void write(SourceWriter w) {
        w.append(toString());
    }

    @Override
    public String toString() {
        return property.getRuntimeMethod() + "(\"" + varName + "\")";
    }
}
