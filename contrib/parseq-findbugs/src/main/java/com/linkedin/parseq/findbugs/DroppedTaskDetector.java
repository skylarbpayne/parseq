package com.linkedin.parseq.findbugs;

import com.linkedin.parseq.Task;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import java.util.BitSet;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.Type;


/**
 * N.B. This is mostly a copy / paste of MethodReturnCheck. The logic we desire is extremely similar.
 * The main difference is that annotation checking is removed in favor of type checking for ParSeq Tasks.
 */
public class DroppedTaskDetector extends OpcodeStackDetector {
  private static final boolean DEBUG = SystemProperties.getBoolean("mrc.debug");
  private static final String BUG_PATTERN = "PARSEQ_DROPPED_TASK";
  private static final int HIGH_PRIORITY = 1;

  private static final int SCAN = 0;

  private static final int SAW_INVOKE = 1;

  private static final BitSet INVOKE_OPCODE_SET = new BitSet();
  static {
    INVOKE_OPCODE_SET.set(Constants.INVOKEINTERFACE);
    INVOKE_OPCODE_SET.set(Constants.INVOKESPECIAL);
    INVOKE_OPCODE_SET.set(Constants.INVOKESTATIC);
    INVOKE_OPCODE_SET.set(Constants.INVOKEVIRTUAL);
  }

  private boolean previousOpcodeWasNEW;

  private final BugAccumulator bugAccumulator;

  private XMethod callSeen;

  private int state;

  private int callPC;

  public DroppedTaskDetector(BugReporter bugReporter) {
    this.bugAccumulator = new BugAccumulator(bugReporter);
  }

  @Override
  public void visitClassContext(ClassContext classContext) {
    super.visitClassContext(classContext);
  }

  @Override
  public void visitAfter(Code code) {
    bugAccumulator.reportAccumulatedBugs();
  }

  @Override
  public void sawOpcode(int seen) {
    if (DEBUG) {
      System.out.printf("%3d %10s %3s %s%n", getPC(), OPCODE_NAMES[seen], state, stack);
    }

    checkForInitWithoutCopyOnStack: if (seen == INVOKESPECIAL && "<init>".equals(getNameConstantOperand())) {
      int arguments = PreorderVisitor.getNumberArguments(getSigConstantOperand());
      OpcodeStack.Item invokedOn = stack.getStackItem(arguments);
      if (invokedOn.isNewlyAllocated() && (!"<init>".equals(getMethodName()) || invokedOn.getRegisterNumber() != 0)) {

        for (int i = arguments + 1; i < stack.getStackDepth(); i++) {
          OpcodeStack.Item item = stack.getStackItem(i);
          if (item.isNewlyAllocated() && item.getSignature().equals(invokedOn.getSignature())) {
            break checkForInitWithoutCopyOnStack;
          }
        }
        callSeen = XFactory.createReferencedXMethod(this);
        callPC = getPC();
        sawMethodCallWithIgnoredReturnValue();
        state = SCAN;
        previousOpcodeWasNEW = false;
        return;
      }
    }

    if (state == SAW_INVOKE && isPop(seen)) {
      sawMethodCallWithIgnoredReturnValue();
    } else if (INVOKE_OPCODE_SET.get(seen)) {
      callPC = getPC();
      callSeen = XFactory.createReferencedXMethod(this);
      state = SAW_INVOKE;
      if (DEBUG) {
        System.out.println("  invoking " + callSeen);
      }
    } else {
      state = SCAN;
    }

    if (seen == NEW) {
      previousOpcodeWasNEW = true;
    } else {
      if (seen == INVOKESPECIAL && previousOpcodeWasNEW) {
        Type callReturnType = Type.getReturnType(callSeen.getMethodDescriptor().getSignature());
        if (doesImplementTask(callReturnType)) {
          bugAccumulator.accumulateBug(new BugInstance(this, BUG_PATTERN, HIGH_PRIORITY).addClassAndMethod(this)
              .addCalledMethod(this), this);
        }
      }
      previousOpcodeWasNEW = false;
    }
  }

  /**
   *
   */
  private void sawMethodCallWithIgnoredReturnValue() {
    {
      Type callReturnType = Type.getReturnType(callSeen.getMethodDescriptor().getSignature());
      if (doesImplementTask(callReturnType)) {
        int popPC = getPC();
        if (DEBUG) {
          System.out.println("Saw POP @" + popPC);
        }

        BugInstance warning = new BugInstance(this, BUG_PATTERN, HIGH_PRIORITY).addClassAndMethod(this).addMethod(callSeen)
            .describe(MethodAnnotation.METHOD_CALLED);
        bugAccumulator.accumulateBug(warning, SourceLineAnnotation.fromVisitedInstruction(this, callPC));
      }
      state = SCAN;
    }
  }

  private boolean isPop(int seen) {
    return seen == Constants.POP || seen == Constants.POP2;
  }

  private boolean doesImplementTask(Type type) {
    Class<?> clazz;
    try {
      clazz = Class.forName(type.toString());
    } catch (ClassNotFoundException e) {
      return false;
    }
    return Task.class.isAssignableFrom(clazz);
  }
}
