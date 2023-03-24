package org.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.drools.core.common.DefaultFactHandle;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.event.DebugAgendaEventListener;
import org.junit.After;
import org.junit.Before;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

/**
* Unit test for simple App.
*/
public class CorrelationRaceConditionTest extends TestCase {
  private KieServices ks;
  private KieContainer kc;
  private KieSession kieSession;
  private KieRuntimeLogger logger;
  private Thread droolsThread;

  @Before
  public void setUp() {
    ks = KieServices.Factory.get();
    kc = ks.getKieClasspathContainer();
    kieSession = kc.newKieSession("CorrelationRaceConditionKS");
    kieSession.addEventListener(new DebugAgendaEventListener());
    kieSession.addEventListener(new DebugRuleRuntimeEventListener());
    logger = KieServices.get().getLoggers().newThreadedFileLogger(kieSession, "./target/CorrelationRaceCondition", 1000);

    droolsThread = new Thread() {
      public void run(){
        kieSession.fireUntilHalt();
      }
    };
    droolsThread.start();
  }

  @After
  public void tearDown() throws InterruptedException {
    kieSession.halt();
    logger.close();
    kieSession.dispose();
    droolsThread.join();
  }

  /**
  * Create the test case
  *
  * @param testName name of the test case
  */
  public CorrelationRaceConditionTest(String testName) {
    super(testName);
  }


  /**
  * @return the suite of tests being tested
  */
  public static Test suite() {
    return new TestSuite(CorrelationRaceConditionTest.class);
  }

  /**
  * Rigourous Test :-)
  */
  public void testCloneEvent() {
    final Event event = new Event(new Date(), "DeviceOffline", "loc001", "device01");
    Event testClone = event.clone();
    assertEquals(event, testClone);
  }

  public void testSingleOfflineEvent() throws InterruptedException {
    final Event event = new Event(new Date(), "DeviceOffline", "loc001", "device01");
    kieSession.insert(event);
    /*
    * If we don't sleep for at least 150ms here, the Drools rules will not have finished firing
    * when we attempt to verify the expected end state.
    */
    Thread.sleep(150);
    listRulesInSession();
    listFactHandleClasses();
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    assertEquals("The number of offline devices is wrong \n" + aff.getOfflineDevices(), 1,
    aff.getOfflineDevices().size());
    List<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Incorrect remaining offline device", "device01", offlineDevices.get(0));
  }

  /**
  * Insert 14 DeviceOffline events, followed by 14 DeviceAdopted events.
  * The DeviceNames in each set differ slightly, with the set of DeviceOffline
  * events omitting device014 and the set of DeviceAdopted events omitting
  * device08.
  * At the end, there should be a DeviceOfflineAffliction with exactly one
  * device name in it, and that name should be "device08".
  * @throws InterruptedException
  */
  public void testMultipleOfflineAndAdoptedEvents() throws InterruptedException {
    final Event templateOfflineEvent = new Event(new Date(), "DeviceOffline", "loc001", "device01");
    final Event templateAdoptedEvent = new Event(new Date(), "DeviceAdopted", "loc001", "device01");

    // insert a set of offline events
    for (int i = 1; i < 15; i++) {
      if (i == 14) {
        continue;
      }
      Event offlineEvent = templateOfflineEvent.clone().setTime(new Date())
          .setDeviceName(templateOfflineEvent.getDeviceName().replace("1",String.valueOf(i)));
      kieSession.insert(offlineEvent);
    }
    // insert a set of adopted events
    for (int i = 1; i < 15; i++) {
      if (i == 8) {
        continue;
      }
      Event adoptedEvent = templateAdoptedEvent.clone().setTime(new Date())
          .setDeviceName(templateAdoptedEvent.getDeviceName().replace("1",String.valueOf(i)));
      kieSession.insert(adoptedEvent);
    }
    /*
    * If we don't sleep for at least 150ms here, the Drools rules will not have finished firing
    * when we attempt to verify the expected end state.
    */
    Thread.sleep(150);
    listRulesInSession();
    listFactHandleClasses();
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    assertEquals("The number of offline devices is wrong \n" + aff.getOfflineDevices(), 1,
        aff.getOfflineDevices().size());
    List<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Incorrect number of remaining offline devices", 1, offlineDevices.size());
    assertEquals("Incorrect remaining offline device", "device08", offlineDevices.get(0));
  }

  private void listRulesInSession() {
    Collection<KiePackage> packages = kieSession.getKieBase().getKiePackages();
    System.err.println("printing inventory of rules");
    packages.forEach(p -> p.getRules().forEach(r -> System.err.println("\t" + r.getName())));
    System.err.println("end of rules inventory");
  }

  private void listFactHandleClasses() {
    System.err.println("printing inventory of fact class names");
    kieSession.getFactHandles().forEach(fh -> {
      InternalFactHandle ifh = (InternalFactHandle) fh;
      System.err.println("\t" + ifh.getObject().getClass().getSimpleName());
    });
    System.err.println("end of fact class name inventory");
  }
  
  private DeviceOfflineAffliction getDeviceOfflineAffliction() {
    return kieSession.getFactHandles().stream()
          .filter(fh -> fh.getClass().getSimpleName().equals(DefaultFactHandle.class.getSimpleName()))
          .map(InternalFactHandle.class::cast)
          .map(InternalFactHandle::getObject)
          .map(DeviceOfflineAffliction.class::cast)
          .collect(Collectors.toList()).get(0);    
  }

}