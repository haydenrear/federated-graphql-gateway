package com.hayden.gateway.federated;

import org.yaml.snakeyaml.serializer.Serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cdc {

    Class<? extends Serializer> serializer = Serializer.class;

}
