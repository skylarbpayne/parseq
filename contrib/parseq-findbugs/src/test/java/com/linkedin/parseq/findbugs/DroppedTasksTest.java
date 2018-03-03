package com.linkedin.parseq.findbugs;

import com.linkedin.parseq.findbugs.examples.DroppedCallableTask;
import com.linkedin.parseq.findbugs.examples.DroppedTupleTask;
import com.linkedin.parseq.findbugs.examples.EmbeddedDroppedTask;
import com.linkedin.parseq.findbugs.examples.SimpleDroppedTask;
import com.linkedin.parseq.findbugs.examples.UnusedTask;
import com.linkedin.parseq.findbugs.examples.UsedTask;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.DetectorToDetector2Adapter;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.NoOpFindBugsProgress;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.classfile.impl.FilesystemCodeBaseLocator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;


public class DroppedTasksTest {

  private static final String BUG_TYPE = "PARSEQ_DROPPED_TASK";

  @Mock
  private BugReporter _bugReporter;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(_bugReporter.getProjectStats()).thenReturn(new ProjectStats());

    IClassFactory classFactory = ClassFactory.instance();
    IClassPath classPath = classFactory.createClassPath();
    IAnalysisCache analysisCache = classFactory
        .createAnalysisCache(classPath, _bugReporter);

    Global.setAnalysisCacheForCurrentThread(analysisCache);
    FindBugs2.registerBuiltInAnalysisEngines(analysisCache);

    ICodeBaseLocator codeBaseLocator = new FilesystemCodeBaseLocator(".");
    IClassPathBuilder builder = classFactory.createClassPathBuilder(_bugReporter);
    builder.addCodeBase(codeBaseLocator, true);
    builder.scanNestedArchives(true);
    IClassPathBuilderProgress progress = new NoOpFindBugsProgress();
    builder.build(classPath, progress);

    AnalysisContext analysisContext = new AnalysisContext(new Project());
    AnalysisContext.setCurrentAnalysisContext(analysisContext);
  }

  @Test(description = "Dropped Task is detected")
  public void detectSimpleDroppedTask() throws Exception {
    runDroppedTaskDetector(SimpleDroppedTask.class);
    ArgumentCaptor<BugInstance> bugInstanceArgumentCaptor = ArgumentCaptor.forClass(BugInstance.class);
    verify(_bugReporter).reportBug(bugInstanceArgumentCaptor.capture());
    assertThat(bugInstanceArgumentCaptor.getValue().getType()).isEqualTo(BUG_TYPE);
  }

  @Test(description = "Dropped Tuple2Task is detected")
  public void detectDroppedTupleTask() throws Exception {
    runDroppedTaskDetector(DroppedTupleTask.class);
    ArgumentCaptor<BugInstance> bugInstanceArgumentCaptor = ArgumentCaptor.forClass(BugInstance.class);
    verify(_bugReporter).reportBug(bugInstanceArgumentCaptor.capture());
    assertThat(bugInstanceArgumentCaptor.getValue().getType()).isEqualTo(BUG_TYPE);
  }

  @Test(description = "Dropped Callable is detected")
  public void detectDroppedCallable() throws Exception {
    runDroppedTaskDetector(DroppedCallableTask.class);
    ArgumentCaptor<BugInstance> bugInstanceArgumentCaptor = ArgumentCaptor.forClass(BugInstance.class);
    verify(_bugReporter).reportBug(bugInstanceArgumentCaptor.capture());
    assertThat(bugInstanceArgumentCaptor.getValue().getType()).isEqualTo(BUG_TYPE);
  }

  @Test(description = "Embedded Dropped Task is detected")
  public void detectEmbeddedDroppedTask() throws Exception {
    runDroppedTaskDetector(EmbeddedDroppedTask.class);
    ArgumentCaptor<BugInstance> bugInstanceArgumentCaptor = ArgumentCaptor.forClass(BugInstance.class);
    verify(_bugReporter).reportBug(bugInstanceArgumentCaptor.capture());
    assertThat(bugInstanceArgumentCaptor.getValue().getType()).isEqualTo(BUG_TYPE);
  }

  @Test(description = "Unused Task variable is not a bug")
  public void detectUnusedTask() throws Exception {
    runDroppedTaskDetector(UnusedTask.class);
    verify(_bugReporter, times(0)).reportBug(any(BugInstance.class));
  }

  @Test(description = "Used tasks are not bugs")
  public void detectUsedTask() throws Exception {
    runDroppedTaskDetector(UsedTask.class);
    verify(_bugReporter, times(0)).reportBug(any(BugInstance.class));
  }

  private <T> void runDroppedTaskDetector(Class<T> clazz) throws CheckedAnalysisException {
    ClassDescriptor descriptor = DescriptorFactory.createClassDescriptor(clazz);
    // Interning the class is a quick way to make sure all class fields / methods can be resolved
    AnalysisContext.currentXFactory().intern(descriptor.getXClass());
    // Detect the dropped Tasks
    Detector2 detector = new DetectorToDetector2Adapter(new DroppedTaskDetector(_bugReporter));
    detector.visitClass(descriptor);
  }
}
