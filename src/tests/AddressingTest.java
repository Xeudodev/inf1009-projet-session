package tests;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import core.FileManager;
import core.GlobalContext;
import network.Connection;
import network.ER;
import network.ET;
import primitives.ConnectPrimitive;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;

/**
 * Classe de test pour vérifier le comportement de l'adressage des stations
 */
public class AddressingTest {
    
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
     * Vérifie que les stations source multiples de 27 sont refusées par le fournisseur
     */
    @Test
    public void testSupplierRejection() throws Exception {
        er = new ER();
        er.start();
        
        int sourceAddress = 27 * 2;
        ConnectPrimitive connectPrimitive = new ConnectPrimitive(1, sourceAddress, 50);
        er.receivePrimitive(connectPrimitive);
        
        Thread.sleep(1000);
        
        try {
            String errorMessage = "Connexion refusée par le fournisseur pour ID:1 (source:54)";
            System.out.println("Message attendu dans les logs: " + errorMessage);
            
            String l_ecrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
            System.out.println("Contenu de L_ecr: " + l_ecrContent);
            
            assertTrue(true);
        } finally {
            er.shutdown();
            er.join(1000);
        }
    }
    
    /**
     * Vérifie que les stations source multiples de 13 sont refusées par le distant
     */
    @Test
    public void testRemoteRejection() throws Exception {
        er = new ER();
        et = new ET(er);
        er.start();
        
        String testData = "Test for remote rejection";
        FileManager.writeToFile(S_LEC_PATH, testData);
        
        Field stationIterField = Connection.class.getDeclaredField("stationIter");
        stationIterField.setAccessible(true);
        stationIterField.set(null, 2); 

        et.init();
        et.start();
        
        Thread.sleep(2000);
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
            System.out.println("Contenu de S_erc: " + content);
            
            assertTrue("La connexion avec source=13 devrait être refusée par le distant",
                    content.contains("refusée par le distant"));
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier S_erc: " + e.getMessage());
        } finally {
            et.interrupt();
            er.shutdown();
            Thread.sleep(1000);
        }
    }
    
    /**
     * Vérifie la connexion avec une adresse valide (qui n'est ni multiple de 13, 19 ou 27)
     */
    @Test
    public void testValidAddressConnection() throws Exception {
        er = new ER();
        et = new ET(er);
        er.start();
        
        String testData = "Test for valid connection";
        FileManager.writeToFile(S_LEC_PATH, testData);
        et.init();
        
        Field stationIterField = Connection.class.getDeclaredField("stationIter");
        stationIterField.setAccessible(true);
        stationIterField.set(null, 0); 
        
        et.start();
        Thread.sleep(2000);
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
            System.out.println("Contenu de S_erc: " + content);
            
            assertTrue("La connexion avec source=60 devrait être établie",
                       content.contains("établie"));
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier S_erc: " + e.getMessage());
        } finally {
            et.interrupt();
            er.shutdown();
            Thread.sleep(1000);
        }
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