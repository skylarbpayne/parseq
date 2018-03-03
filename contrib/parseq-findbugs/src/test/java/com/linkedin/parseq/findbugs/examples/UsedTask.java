package com.linkedin.parseq.findbugs.examples;

import com.linkedin.parseq.Task;


public class UsedTask {
  public Task<Integer> doSomeComputation() {
    return Task.callable(() -> true)
        .flatMap(shouldReturnFive -> shouldReturnFive ? Task.value(5) : defaultValueTask());
  }

  private Task<Integer> defaultValueTask() {
    return Task.value(2);
  }
}
