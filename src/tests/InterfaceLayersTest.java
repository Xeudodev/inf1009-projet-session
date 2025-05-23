package tests;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import core.FileManager;
import core.GlobalContext;
import network.ER;
import network.ET;
import packets.DataPacket;
import primitives.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;

public class InterfaceLayersTest {
    
    private ER er;
    private ET et;
    private static final String S_LEC_PATH = "S_lec";
    private static final String S_ERC_PATH = "S_erc";
    private static final String L_LEC_PATH = "L_lec";
    private static final String L_ECR_PATH = "L_ecr";
    
    @Before
    public void setUp() {
        resetStaticState();
        cleanupFiles();
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        
        GlobalContext.initializeFiles();
        
        String testData = "Test data for connection";
        FileManager.writeToFile(S_LEC_PATH, testData);
        
        er = new ER();
        et = new ET(er);
    }
    
    @After
    public void tearDown() {
        if (et != null && et.isAlive()) {
            et.interrupt();
            try {
                et.join(2000);
                
                if (et.isAlive()) {
                    System.err.println("ATTENTION: ET ne s'est pas arrêté proprement, forçage...");
                    et.stop();
                }
            } catch (InterruptedException e) {
                System.err.println("Erreur lors de l'arrêt de ET: " + e.getMessage());
            }
        }
        
        if (er != null) {
            er.shutdown();
            try {
                er.join(2000);
                
                if (er.isAlive()) {
                    System.err.println("ATTENTION: ER ne s'est pas arrêté proprement, forçage...");
                    er.interrupt();
                    
                    er.join(1000);
                    
                    if (er.isAlive()) {
                        er.stop();
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Erreur lors de l'arrêt de ER: " + e.getMessage());
            }
        }
        
        cleanupFiles();
        
        resetStaticState();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }
    
    /**
     * Vérifie que ET peut envoyer une primitive ConnectPrimitive à ER
     */
    @Test
    public void testETtoERConnectPrimitive() throws InterruptedException {
        int uniqueTransportId = 99;
        
        er.start();
        
        ConnectPrimitive connectPrimitive = new ConnectPrimitive(uniqueTransportId, 60, 50);
        er.receivePrimitive(connectPrimitive);
        Thread.sleep(1000);
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
            System.out.println("Contenu de L_ecr: " + content);
            
            assertTrue("ER devrait écrire un paquet d'appel dans L_ecr", 
                    content.contains("00001011"));
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier L_ecr: " + e.getMessage());
        } finally {
            er.shutdown();
            er.join(2000);
            
            if (er.isAlive()) {
                System.err.println("ER n'a pas pu être arrêté proprement, interruption forcée");
                er.interrupt();
            }
        }
        
        Thread.sleep(500);
    }
    
    /**
     * Vérifie que ER peut envoyer une primitive à ET
     */
    @Test
    public void testERtoETConnectConfirmation() throws InterruptedException {
        Thread.sleep(1000);
        
        er = new ER();
        et = new ET(er);
        
        er.start();
        
        et.init();
        et.start();
        
        Thread.sleep(3000);
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
            System.out.println("Contenu de S_erc: " + content);
            
            assertTrue("ET devrait logger la confirmation ou le refus de connexion dans S_erc", 
                    content.contains("établie") || 
                    content.contains("confirmée") || 
                    content.contains("refusée")); 
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier S_erc: " + e.getMessage());
        } finally {
            et.interrupt();
            if (et.isAlive()) {
                try {
                    et.join(2000);
                } catch (InterruptedException e) {}
            }
            
            er.shutdown();
            try {
                er.join(2000);
            } catch (InterruptedException e) {}
            
            if (er.isAlive() || (et != null && et.isAlive())) {
                System.err.println("Certains threads n'ont pas pu être arrêtés proprement");
                if (er.isAlive()) er.interrupt();
                if (et.isAlive()) et.interrupt();
            }
        }
        
        Thread.sleep(500);
    }
    
    /**
     * Vérifie le cycle complet de communication entre ET et ER pour une connexion réussie
     */
    @Test
    public void testFullCommunicationCycle() throws Exception {
        cleanupFiles();
        GlobalContext.initializeFiles();
        resetStaticState();
        
        Thread.sleep(1000);
        
        er = new ER();
        et = new ET(er);

        String uniqueMarker = "Connection_" + System.currentTimeMillis();
        FileManager.writeToFile(S_LEC_PATH, uniqueMarker);
        
        try {
            er.start();
            et.init();
            et.start();
            
            Thread.sleep(3000);
            
            String l_ecrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
            System.out.println("Contenu de L_ecr: " + l_ecrContent);
            
            assertTrue("ER devrait écrire un paquet d'appel dans L_ecr", 
                    l_ecrContent.contains("00001011"));
            
            String s_ercContent = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
            System.out.println("Contenu de S_erc: " + s_ercContent);
            
            boolean connectionProcessed = s_ercContent.contains("Connexion") && 
                    (s_ercContent.contains("établie") || 
                     s_ercContent.contains("refusée"));
            
            assertTrue("ET et ER devraient avoir communiqué sur l'état de la connexion", 
                    connectionProcessed);
        } finally {
            if (et != null && et.isAlive()) {
                et.interrupt();
                et.join(1000);
            }
            
            if (er != null) {
                er.shutdown();
                er.join(1000);
            }
            
            cleanupFiles();
            resetStaticState();
        }
        
        Thread.sleep(500);
    }
    
    /**
     * Vérifie que ET peut envoyer une primitive DataPrimitive à ER et que ER réagit correctement
     */
    @Test
    public void testDataTransfer() throws Exception {
        er.start();

        ConnectPrimitive connectPrimitive = new ConnectPrimitive(1, 60, 50);
        er.receivePrimitive(connectPrimitive);
        
        Thread.sleep(1500);
        
        ConnectPrimitive confirmPrimitive = new ConnectPrimitive(1, 0);
        ET.receivePrimitive(confirmPrimitive);
        
        et.init();
        et.update();
        
        Thread.sleep(500);
        
        DataPrimitive dataPrimitive = new DataPrimitive(1, "Test data for network layer");
        er.receivePrimitive(dataPrimitive);
        
        Thread.sleep(2000);
        
        String l_ecrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        
        int lineCount = l_ecrContent.split("\n").length;
        assertTrue("ER devrait avoir écrit des paquets dans L_ecr (trouvé " + lineCount + " lignes)", 
                lineCount > 1);
        
        assertTrue("ER devrait avoir écrit un paquet d'appel dans L_ecr", 
                l_ecrContent.contains("00001011"));
        assertTrue("ER devrait avoir écrit des paquets de données dans L_ecr", 
                l_ecrContent.contains(DataPacket.class.getSimpleName()) || 
                !l_ecrContent.lines().filter(line -> !line.contains("00001011")).findAny().isEmpty());
                
        er.shutdown();
        er.join(1000);
    }
    
    /**
     * Nettoie les fichiers de test
     */
    private void cleanupFiles() {
        deleteFile(S_LEC_PATH);
        deleteFile(S_ERC_PATH);
        deleteFile(L_LEC_PATH);
        deleteFile(L_ECR_PATH);
    }
    
    /**
     * Supprime un fichier s'il existe
     */
    private void deleteFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression du fichier " + path + ": " + e.getMessage());
        }
    }
    
    /**
     * Réinitialise l'état statique qui pourrait affecter d'autres tests
     */
    private void resetStaticState() {
        try {
            Field queueField = ET.class.getDeclaredField("primitivesQueue");
            queueField.setAccessible(true);
            ((Queue<?>) queueField.get(null)).clear();
            
            Field connectionsField = ET.class.getDeclaredField("connections");
            connectionsField.setAccessible(true);
            ((List<?>) connectionsField.get(null)).clear();
        } catch (Exception e) {
        }
        
        GlobalContext.resetConnectionIdCounter();
    }
}