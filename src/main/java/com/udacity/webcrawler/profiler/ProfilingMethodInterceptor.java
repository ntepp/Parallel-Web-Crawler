package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;

  ProfilingState profilingState;
  Object targetClass;
  ZonedDateTime startTime;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, Object targetClass, ProfilingState profilingState, ZonedDateTime startTime) {
    this.clock = Objects.requireNonNull(clock);
    this.targetClass = Objects.requireNonNull(targetClass);
    this.profilingState = Objects.requireNonNull(profilingState);
    this.startTime = Objects.requireNonNull(startTime);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    Object profiledObject;
    Instant start = null;
    boolean isProfiled = Objects.nonNull(method.getAnnotation(Profiled.class));
    if(isProfiled) {
      start = clock.instant();
    }
    try {
      profiledObject =  method.invoke(targetClass,args);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    } finally {
      if(isProfiled){
        Duration duration = Duration.between(start, clock.instant());
        profilingState.record(targetClass.getClass(),method,duration);
      }
    }

    return profiledObject;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProfilingMethodInterceptor that = (ProfilingMethodInterceptor) o;
    return Objects.equals(clock, that.clock) && Objects.equals(profilingState, that.profilingState) && Objects.equals(targetClass, that.targetClass) && Objects.equals(startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clock, profilingState, targetClass, startTime);
  }
}
