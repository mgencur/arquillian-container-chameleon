package org.arquillian.container.chameleon.spi.model;

import org.jboss.arquillian.container.spi.ConfigurationException;

public class Target {

    public static enum Type {
        Remote, Managed, Embedded, Default;

        public static Type from(String name) {
            for(Type type : Type.values()) {
                if(type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    private String server;
    private String version;
    private Type type;

    public Type getType() {
        return type;
    }

    public String getServer() {
        return server;
    }

    public String getVersion() {
        return version;
    }

    public static Target from(String source) {
        Target target = new Target();

        String[] sections = source.split(":");
        if (sections.length < 2 || sections.length > 3) {
            throw new ConfigurationException("Wrong target format [" + source + "] server:version:type");
        }
        target.server = sections[0].toLowerCase();
        target.version = sections[1];
        if(sections.length > 2) {
            for (Type type : Type.values()) {
                if (sections[2].toLowerCase().contains(type.name().toLowerCase())) {
                    target.type = type;
                    break;
                }
            }
            if(target.type == null) {
                throw new ConfigurationException("Unknown target type " + sections[2] + ". Supported " + Target.Type.values());
            }
        } else {
            target.type = Type.Default;
        }
        return target;
    }

    @Override
    public String toString() {
        return server + ":" + version + ":" + type;
    }
}