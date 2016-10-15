package no.difi.vefa.peppol.common.model;

import java.io.Serializable;
import java.util.UUID;

public class InstanceIdentifier implements Serializable {

    private static final long serialVersionUID = 3616828001672136897L;

    private String value;

    public static InstanceIdentifier generateUUID() {
        return of(UUID.randomUUID().toString());
    }

    public static InstanceIdentifier of(String value) {
        return new InstanceIdentifier(value);
    }

    @Deprecated
    public InstanceIdentifier(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceIdentifier that = (InstanceIdentifier) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
