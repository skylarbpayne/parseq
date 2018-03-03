package com.linkedin.parseq.findbugs.examples;

import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tuple2Task;


public class DroppedTupleTask {
  private Tuple2Task<Integer, Integer> dropTupleTask() {
    return Task.par(Task.value(3), Task.value(4));
  }

  public void blah() {
    dropTupleTask();
  }
}
