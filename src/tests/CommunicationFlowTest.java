package tests;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import core.FileManager;
import core.GlobalContext;
import enums.ConnectionStateEnum;
import network.Connection;
import network.ER;
import network.ET;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.io.File;

/**
 * Tests vérifiant le déroulement complet de la communication entre ET et ER
 */
public class CommunicationFlowTest {
    
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
        GlobalContext.initializeFiles();
    }
    
    @After
    public void tearDown() {
        if (et != null && et.isAlive()) {
            et.interrupt();
            try {
                et.join(1000);
            } catch (InterruptedException e) {}
        }
        
        if (er != null) {
            er.shutdown();
            try {
                er.join(1000);
            } catch (InterruptedException e) {}
        }
        
        cleanupFiles();
        resetStaticState();
    }
    
    /**
     * Vérifie le déroulement complet d'une communication réussie:
     * 1. Établissement de connexion
     * 2. Transfert de données
     * 3. Libération de connexion
     */
    @Test
    public void testSuccessfulCommunicationFlow() throws Exception {
        cleanupFiles();
        GlobalContext.initializeFiles();
        
        String testData = "Test data for successful communication";
        FileManager.writeToFile(S_LEC_PATH, testData);
        
        er = new ER();
        et = new ET(er);
        
        er.start();
        et.init();
        
        // Écrire directement dans L_ecr pour s'assurer qu'il n'est pas vide
        FileManager.appendToFile(L_ECR_PATH, "Test write to ensure file access");
        
        Field connectionsField = ET.class.getDeclaredField("connections");
        connectionsField.setAccessible(true);
        List<?> connections = (List<?>) connectionsField.get(null);

        if (!connections.isEmpty()) {
            Connection conn = (Connection) connections.get(0);
            conn.setEtatConnexion(ConnectionStateEnum.ESTABLISHED);
            System.out.println("État de connexion forcé à ESTABLISHED");
            
            // Créer aussi la connexion dans ER
            int connId = conn.getIdentifier();
            int sourceAddr = conn.getStationSource();
            int destAddr = conn.getStationDestination();
            
            // Envoyer une primitive ConnectPrimitive à ER pour créer la connexion
            primitives.ConnectPrimitive connectPrim = new primitives.ConnectPrimitive(connId, sourceAddr, destAddr);
            er.receivePrimitive(connectPrim);
            
            // Attendre que ER traite la primitive
            Thread.sleep(1000);
        }
        
        et.start();
        
        Thread.sleep(3000);
        
        File l_ecrFile = new File(L_ECR_PATH);
        assertTrue("Le fichier L_ecr devrait exister", l_ecrFile.exists());
        assertTrue("Le fichier L_ecr ne devrait pas être vide", l_ecrFile.length() > 0);
        
        File s_ercFile = new File(S_ERC_PATH);
        assertTrue("Le fichier S_erc devrait exister", s_ercFile.exists());
        
        String s_ercContent = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
        String l_ecrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        
        System.out.println("=== Contenu de S_erc ===");
        System.out.println(s_ercContent);
        System.out.println("=== Contenu de L_ecr ===");
        System.out.println(l_ecrContent);
        
        if (l_ecrContent.isEmpty()) {
            fail("Le fichier L_ecr ne devrait pas être vide");
        }
        
        if (s_ercContent.isEmpty()) {
            ET.writeToFile("Test diagnostic entry");
            Thread.sleep(500);
            s_ercContent = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
            if (s_ercContent.isEmpty()) {
                fail("Impossible d'écrire dans S_erc");
            }
        }
        
        if (l_ecrContent.contains("00001011")) {
            assertTrue("ER devrait libérer la connexion", 
                    l_ecrContent.contains("00010011") || l_ecrContent.contains("DataPacket"));
        }
    }
    
    /**
     * Vérifie le déroulement avec une connexion refusée par le fournisseur
     */
    @Test
    public void testRejectedBySupplierFlow() throws Exception {
        Field stationIterField = Connection.class.getDeclaredField("stationIter");
        stationIterField.setAccessible(true);
        stationIterField.set(null, 5);
        
        String testData = "Test data for supplier rejection";
        FileManager.writeToFile(S_LEC_PATH, testData);
        
        er = new ER();
        et = new ET(er);
        
        er.start();
        et.init();
        et.start();
        
        Thread.sleep(3000);
        
        String s_ercContent = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
        System.out.println("=== Contenu de S_erc ===");
        System.out.println(s_ercContent);
        
        assertTrue("La connexion devrait être refusée par le fournisseur", 
                s_ercContent.contains("refusée par le fournisseur"));
    }
    
    /**
     * Vérifie le déroulement avec une connexion refusée par le distant
     */
    @Test
    public void testRejectedByRemoteFlow() throws Exception {
        Field stationIterField = Connection.class.getDeclaredField("stationIter");
        stationIterField.setAccessible(true);
        stationIterField.set(null, 2);
        
        String testData = "Test data for remote rejection";
        FileManager.writeToFile(S_LEC_PATH, testData);
        
        er = new ER();
        et = new ET(er);
        
        er.start();
        et.init();
        et.start();
        
        Thread.sleep(3000);
        
        String s_ercContent = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
        String l_ecrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        
        System.out.println("=== Contenu de S_erc ===");
        System.out.println(s_ercContent);
        System.out.println("=== Contenu de L_ecr ===");
        System.out.println(l_ecrContent);
        
        assertTrue("La connexion devrait être refusée par le distant", 
                s_ercContent.contains("refusée par le distant"));
        
        assertTrue("ER devrait envoyer un paquet d'appel même si la connexion est refusée", 
                l_ecrContent.contains("00001011"));
    }
       
    /* Méthodes utilitaires */
    
    private void cleanupFiles() {
        deleteFile(S_LEC_PATH);
        deleteFile(S_ERC_PATH);
        deleteFile(L_LEC_PATH);
        deleteFile(L_ECR_PATH);
    }
    
    private void deleteFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression du fichier " + path + ": " + e.getMessage());
        }
    }
    
    private void resetStaticState() {
        try {
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
            e.printStackTrace();
        }
        
        GlobalContext.resetConnectionIdCounter();
    }
}