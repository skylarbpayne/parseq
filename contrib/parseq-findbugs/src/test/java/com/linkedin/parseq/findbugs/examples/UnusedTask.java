package com.linkedin.parseq.findbugs.examples;

import com.linkedin.parseq.Task;


public class UnusedTask {
  public boolean methodWithUnusedTask() {
    Task<Integer> myTask = Task.value(3);
    return true;
  }
}
