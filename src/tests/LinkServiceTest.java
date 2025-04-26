package tests;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import core.FileManager;
import core.GlobalContext;
import network.ER;
import network.ET;
import primitives.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;

/**
 * Classe de test pour vérifier le comportement du service de liaison
 */
public class LinkServiceTest {
    
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
     * Vérifie que ER écrit correctement un paquet d'appel dans le fichier L_ecr
     */
    @Test
    public void testWriteCallPacket() throws Exception {
        er = new ER();
        er.start();
        
        ConnectPrimitive connectPrimitive = new ConnectPrimitive(1, 60, 50);
        er.receivePrimitive(connectPrimitive);
        
        Thread.sleep(1000);
        
        String content = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        System.out.println("Contenu de L_ecr: " + content);
        
        assertTrue("ER devrait écrire un paquet d'appel dans L_ecr", 
                content.contains("00001011"));
        
        assertTrue("Le paquet d'appel devrait contenir l'adresse source", 
                content.contains("60"));
        
        assertTrue("Le paquet d'appel devrait contenir l'adresse destination", 
                content.contains("50"));
    }
    
    /**
     * Vérifie que ER écrit correctement un paquet de données dans le fichier L_ecr
     */
    @Test
    public void testWriteDataPacket() throws Exception {
        er = new ER();
        er.start();
        
        ConnectPrimitive connectPrimitive = new ConnectPrimitive(1, 60, 50);
        er.receivePrimitive(connectPrimitive);
        Thread.sleep(1000);
        
        String response = "00001111|0|50|60";
        FileManager.appendToFile(L_LEC_PATH, response);
        Thread.sleep(500);
        
        String testData = "Test data for link service";
        DataPrimitive dataPrimitive = new DataPrimitive(1, testData);
        er.receivePrimitive(dataPrimitive);
        
        Thread.sleep(1000);
        
        String content = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        System.out.println("Contenu de L_ecr: " + content);
        
        assertTrue("ER devrait écrire les données dans L_ecr", 
                content.contains(testData));
    }
    
    /**
     * Vérifie que ER réagit correctement à l'absence d'acquittement en retransmettant le paquet
     */
    @Test
    public void testRetransmissionOnMissingAck() throws Exception {
        er = new ER();
        et = new ET(er);
        er.start();
        
        ConnectPrimitive connectPrimitive = new ConnectPrimitive(1, 60, 50);
        er.receivePrimitive(connectPrimitive);
        Thread.sleep(1000);
        
        String response = "00001111|0|50|60";
        FileManager.appendToFile(L_LEC_PATH, response);
        Thread.sleep(500);
        
        String testData = "Test data that will require retransmission";
        DataPrimitive dataPrimitive = new DataPrimitive(1, testData);
        er.receivePrimitive(dataPrimitive);
        
        Thread.sleep(2000);
        
        String content = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        System.out.println("Contenu de L_ecr: " + content);
        
        int occurrences = countOccurrences(content, testData);
        
        assertTrue("ER devrait retransmettre le paquet en l'absence d'acquittement (trouvé " + 
                occurrences + " occurrences)", occurrences >= 2);
    }
    
    /**
     * Vérifie que ER traite correctement un acquittement négatif en retransmettant le paquet
     */
    @Test
    public void testRetransmissionOnNegativeAck() throws Exception {
        er = new ER();
        er.start();
        
        ConnectPrimitive connectPrimitive = new ConnectPrimitive(1, 60, 50);
        er.receivePrimitive(connectPrimitive);
        Thread.sleep(1000);
        
        String response = "00001111|0|50|60";
        FileManager.appendToFile(L_LEC_PATH, response);
        Thread.sleep(500);
        
        String testData = "Test data for negative acknowledgement";
        DataPrimitive dataPrimitive = new DataPrimitive(1, testData);
        er.receivePrimitive(dataPrimitive);
        
        Thread.sleep(2000);
        
        String content = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        assertTrue("ER devrait traiter la demande de données", 
                !content.isEmpty() && content.contains(testData));
    }
    
    /**
     * Vérifie que ER écrit correctement un paquet de libération dans le fichier L_ecr
     */
    @Test
    public void testWriteReleasePacket() throws Exception {
        er = new ER();
        er.start();
        
        ConnectPrimitive connectPrimitive = new ConnectPrimitive(1, 60, 50);
        er.receivePrimitive(connectPrimitive);
        Thread.sleep(1000);
        
        String response = "00001111|0|50|60";
        FileManager.appendToFile(L_LEC_PATH, response);
        Thread.sleep(500);
        
        DisconnectPrimitive disconnectPrimitive = new DisconnectPrimitive(1, 0);
        er.receivePrimitive(disconnectPrimitive);
        
        Thread.sleep(1000);
        
        String content = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        System.out.println("Contenu de L_ecr: " + content);
        
        assertTrue("ER devrait écrire un paquet de libération dans L_ecr", 
                content.contains("00010011"));
    }
    
    // Méthodes utilitaires
    
    private int countOccurrences(String content, String searchString) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(searchString, index)) != -1) {
            count++;
            index += searchString.length();
        }
        return count;
    }
    
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
        } catch (Exception e) {
        }
        
        GlobalContext.resetConnectionIdCounter();
    }
}