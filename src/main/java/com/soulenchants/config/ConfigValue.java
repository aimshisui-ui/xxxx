package com.soulenchants.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a field to a YAML config key. The ConfigBinder uses reflection to
 * read the value at the given path; if the key is missing, the field keeps
 * its initialized (default) value so new config entries auto-upgrade without
 * wiping customisation.
 *
 * Supported field types: int, long, double, boolean, String,
 * java.util.List&lt;String&gt;. Add more types in ConfigBinder.coerce().
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigValue {
    /** Dot-path into the YAML file (e.g. "bleed.proc-chance-per-level"). */
    String value();

    /** Optional human-readable comment. Not currently emitted but captured for tooling. */
    String comment() default "";
}
