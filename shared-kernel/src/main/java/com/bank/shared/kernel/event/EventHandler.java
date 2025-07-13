package com.bank.shared.kernel.event;

import java.lang.annotation.*;

/**
 * Annotation to mark methods as domain event handlers
 * 
 * Used with Spring's @EventListener for Event-Driven Architecture
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventHandler {
    
    /**
     * Optional name for the event handler
     */
    String name() default "";
    
    /**
     * Whether this handler should run asynchronously
     */
    boolean async() default false;
    
    /**
     * Order of execution when multiple handlers exist for the same event
     */
    int order() default 0;
}