package tests;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Suite de tests qui exécute les tests d'interface couche par couche
 * en garantissant une isolation complète entre chaque test
 */
public class InterfaceLayersTestSuite {
    
    private InterfaceLayersTest testInstance;
    
    @Before
    public void setUp() {
        // Ne rien faire ici, chaque test crée sa propre instance
    }
    
    @After
    public void tearDown() throws Exception {
        if (testInstance != null) {
            // Nettoyer les fichiers système après chaque test
            Files.deleteIfExists(Paths.get("S_lec"));
            Files.deleteIfExists(Paths.get("S_erc"));
            Files.deleteIfExists(Paths.get("L_lec"));
            Files.deleteIfExists(Paths.get("L_ecr"));
            
            // Laisser du temps au système
            Thread.sleep(1000);
        }
    }
    
    @Test
    public void test1_ETtoERConnectPrimitive() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 1: testETtoERConnectPrimitive ====");
        testInstance = new InterfaceLayersTest();
        testInstance.setUp();
        testInstance.testETtoERConnectPrimitive();
        testInstance.tearDown();
        System.gc(); // Demande de garbage collection
        Thread.sleep(2000); // Pause longue entre tests
    }
    
    @Test
    public void test2_ERtoETConnectConfirmation() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 2: testERtoETConnectConfirmation ====");
        testInstance = new InterfaceLayersTest();
        testInstance.setUp();
        testInstance.testERtoETConnectConfirmation();
        testInstance.tearDown();
        System.gc();
        Thread.sleep(2000);
    }
    
    @Test
    public void test3_FullCommunicationCycle() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 3: testFullCommunicationCycle ====");
        testInstance = new InterfaceLayersTest();
        testInstance.setUp();
        testInstance.testFullCommunicationCycle();
        testInstance.tearDown();
        System.gc();
        Thread.sleep(2000);
    }
    
    @Test
    public void test4_DataTransfer() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 4: testDataTransfer ====");
        testInstance = new InterfaceLayersTest();
        testInstance.setUp();
        testInstance.testDataTransfer();
        testInstance.tearDown();
    }
}