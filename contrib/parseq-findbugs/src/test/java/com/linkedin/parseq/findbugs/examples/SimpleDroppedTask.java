package com.linkedin.parseq.findbugs.examples;

import com.linkedin.parseq.Task;


public class SimpleDroppedTask {
  public Task<Integer> dropTask() {
    return Task.value(3);
  }

  public int blah() {
    dropTask();
    int i = 3;
    i += 7;
    return i;
  }
}
