package tests;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.util.Queue;
import java.util.List;

import core.GlobalContext;
import network.Connection;
import network.ET;

/**
 * Suite de tests qui exécute les tests d'interface couche par couche
 * en garantissant une isolation complète entre chaque test
 */
public class InterfaceLayersTestSuite {
    
    private InterfaceLayersTest testInstance;
    
    @Before
    public void setUp() {
        cleanupFiles();
        resetGlobalState();
    }
    
    @After
    public void tearDown() throws Exception {
        if (testInstance != null) {
            testInstance.tearDown();
            cleanupFiles();
            resetGlobalState();
            
            System.gc();
            
            Thread.sleep(2000);
        }
    }
    
    @Test
    public void test1_ETtoERConnectPrimitive() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 1: testETtoERConnectPrimitive ====");
        testInstance = new InterfaceLayersTest();
        
        cleanupFiles();
        GlobalContext.initializeFiles();
        
        testInstance.setUp();
        testInstance.testETtoERConnectPrimitive();
        testInstance.tearDown();
        
        cleanupFiles();
        resetGlobalState();
        
        Thread.sleep(3000);
    }
    
    @Test
    public void test2_ERtoETConnectConfirmation() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 2: testERtoETConnectConfirmation ====");
        testInstance = new InterfaceLayersTest();
        
        cleanupFiles();
        GlobalContext.initializeFiles();
        
        testInstance.setUp();
        testInstance.testERtoETConnectConfirmation();
        testInstance.tearDown();
        
        cleanupFiles();
        resetGlobalState();
        
        Thread.sleep(3000);
    }
    
    @Test
    public void test3_FullCommunicationCycle() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 3: testFullCommunicationCycle ====");
        testInstance = new InterfaceLayersTest();
        
        cleanupFiles();
        GlobalContext.initializeFiles();
        
        testInstance.setUp();
        try {
            testInstance.testFullCommunicationCycle();
        } catch (AssertionError e) {
            String errorMsg = e.getMessage();
            System.err.println("ÉCHEC DU TEST: " + errorMsg);
            
            try {
                if (Files.exists(Paths.get("L_ecr"))) {
                    String content = new String(Files.readAllBytes(Paths.get("L_ecr")));
                    System.err.println("Contenu de L_ecr:\n" + content);
                } else {
                    System.err.println("Le fichier L_ecr n'existe pas!");
                }
            } catch (Exception ex) {
                System.err.println("Erreur lors de la vérification des fichiers: " + ex.getMessage());
            }
            
            throw e;
        } finally {
            testInstance.tearDown();
            
            cleanupFiles();
            resetGlobalState();
            
            Thread.sleep(3000);
        }
    }
    
    @Test
    public void test4_DataTransfer() throws Exception {
        System.out.println("\n\n==== EXÉCUTION DU TEST 4: testDataTransfer ====");
        testInstance = new InterfaceLayersTest();
        
        cleanupFiles();
        GlobalContext.initializeFiles();
        
        testInstance.setUp();
        testInstance.testDataTransfer();
        testInstance.tearDown();
        
        cleanupFiles();
        resetGlobalState();
    }
    
    private void cleanupFiles() {
        try {
            Files.deleteIfExists(Paths.get("S_lec"));
            Files.deleteIfExists(Paths.get("S_erc"));
            Files.deleteIfExists(Paths.get("L_lec"));
            Files.deleteIfExists(Paths.get("L_ecr"));
            
            Thread.sleep(500);
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage des fichiers: " + e.getMessage());
        }
    }
    
    private void resetGlobalState() {
        try {
            GlobalContext.resetConnectionIdCounter();
            
            Field queueField = ET.class.getDeclaredField("primitivesQueue");
            queueField.setAccessible(true);
            ((Queue<?>) queueField.get(null)).clear();
            
            Field connectionsField = ET.class.getDeclaredField("connections");
            connectionsField.setAccessible(true);
            ((List<?>) connectionsField.get(null)).clear();
            
            Field stationIterField = Connection.class.getDeclaredField("stationIter");
            stationIterField.setAccessible(true);
            stationIterField.set(null, 0);
        } catch (Exception e) {
            System.err.println("Erreur lors de la réinitialisation de l'état global: " + e.getMessage());
        }
    }
}