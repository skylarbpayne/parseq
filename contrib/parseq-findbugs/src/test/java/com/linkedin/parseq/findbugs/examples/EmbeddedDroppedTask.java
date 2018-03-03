package com.linkedin.parseq.findbugs.examples;

import com.linkedin.parseq.Task;


public class EmbeddedDroppedTask {
  private Task<Integer> addOneTask(int i) {
    return Task.value(i + 1);
  }

  public Task<Integer> doComputation() {
    return Task.callable(() -> 1)
        .flatMap(i -> {
          addOneTask(i);
          return Task.value(i);
        });
  }
}
