package pharmatrack.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeReference<T> {
    protected final Type type;

    protected TypeReference() {
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType pt) {
            this.type = pt.getActualTypeArguments()[0];
        } else {
            this.type = Object.class;
        }
    }

    public Type getType() {
        return type;
    }
}