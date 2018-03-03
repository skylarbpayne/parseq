package com.linkedin.parseq.findbugs.examples;

import com.linkedin.parseq.Task;


public class DroppedCallableTask {
  private Task<Boolean> isLessThanFiveTask(int i) {
    return Task.callable(() -> i < 5);
  }

  private void someMethod() {
    isLessThanFiveTask(4);
  }
}
