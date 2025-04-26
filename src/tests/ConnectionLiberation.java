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
import primitives.DisconnectPrimitive;
import enums.ConnectionStateEnum;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;

/**
 * Tests spécifiques pour la libération de connexion
 */
public class ConnectionLiberation {
    
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
        
        String testData = "Test data for connection liberation";
        FileManager.writeToFile(S_LEC_PATH, testData);
        
        er = new ER();
        et = new ET(er);
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
     * Test que le code de libération (00010011) est bien envoyé par ER
     * quand ET demande une libération de connexion
     */
    @Test
    public void testConnectionReleasePacket() throws Exception {
        resetStaticState();
        cleanupFiles();
        GlobalContext.initializeFiles();
        
        int transportId = 42;
        int sourceAddr = 60;
        int destAddr = 56;
        
        er = new ER();
        er.start();
        Thread.sleep(500);
        
        ConnectPrimitive connectPrim = new ConnectPrimitive(transportId, sourceAddr, destAddr);
        er.receivePrimitive(connectPrim);
        Thread.sleep(1000);
        
        FileManager.writeToFile(L_LEC_PATH, "00001111|0|" + destAddr + "|" + sourceAddr);
        Thread.sleep(1000);
        
        DisconnectPrimitive disconnectPrim = new DisconnectPrimitive(transportId, 0);
        er.receivePrimitive(disconnectPrim);
        Thread.sleep(1000);
        
        String l_ecrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        System.out.println("=== Contenu de L_ecr ===");
        System.out.println(l_ecrContent);
        
        assertTrue("ER devrait envoyer un paquet de libération (code 00010011)",
                l_ecrContent.contains("00010011"));
    }
    
    /**
     * Test que la connexion est bien supprimée des listes après libération
     */
    @Test
    public void testConnectionRemoval() throws Exception {
        er.start();
        et.init();
        
        et.start();
        
        Field connectionsField = ET.class.getDeclaredField("connections");
        connectionsField.setAccessible(true);
        List<?> connections = (List<?>) connectionsField.get(null);
        
        int initialSize = connections.size();
        assertTrue("Au moins une connexion devrait être créée", initialSize > 0);
        
        Thread.sleep(6000);
        
        int finalSize = connections.size();
        
        boolean connectionsRemoved = finalSize == 0;
        boolean connectionsTerminated = true;
        
        for (Object connObj : connections) {
            Connection conn = (Connection) connObj;
            if (conn != null && 
                conn.getEtatConnexion() == ConnectionStateEnum.ESTABLISHED && 
                !conn.isReleaseConfirmed()) {
                connectionsTerminated = false;
                break;
            }
        }
        
        assertTrue("Les connexions devraient être supprimées ou terminées",
                connectionsRemoved || connectionsTerminated);
    }
    
    /**
     * Test que le cycle complet établissement -> transfert -> libération fonctionne
     */
    @Test
    public void testFullConnectionLifecycle() throws Exception {
        resetStaticState();
        cleanupFiles();
        GlobalContext.initializeFiles();
        
        Field stationIterField = Connection.class.getDeclaredField("stationIter");
        stationIterField.setAccessible(true);
        stationIterField.set(null, 0);
        
        String testData = "Test data for full connection lifecycle";
        FileManager.writeToFile(S_LEC_PATH, testData);
        
        ER er = new ER();
        ET et = new ET(er);
        
        er.start();
        et.init();
        et.start();
        
        Thread.sleep(7000);
        
        String s_ercContent = new String(Files.readAllBytes(Paths.get(S_ERC_PATH)));
        String l_ecrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        
        System.out.println("=== Contenu de S_erc ===");
        System.out.println(s_ercContent);
        System.out.println("=== Contenu de L_ecr ===");
        System.out.println(l_ecrContent);
        
        assertTrue("La connexion devrait être établie", 
                s_ercContent.contains("établie"));
        
        assertTrue("Un paquet d'appel devrait être envoyé",
                l_ecrContent.contains("00001011"));
        
        assertTrue("Un paquet de données devrait être envoyé",
                l_ecrContent.contains(testData) || l_ecrContent.contains("00000000"));
        
        assertTrue("Un paquet de libération devrait être envoyé",
                l_ecrContent.contains("00010011"));
    }
    
    /**
     * Test que la libération fonctionne même si initiée par le distant
     */
    @Test
    public void testReleaseByRemote() throws Exception {
        er.start();
        et.init();
        
        et.start();
        
        Field connectionsField = ET.class.getDeclaredField("connections");
        connectionsField.setAccessible(true);
        List<?> connections = (List<?>) connectionsField.get(null);
        
        Thread.sleep(2000);
        
        if (!connections.isEmpty()) {
            Connection conn = (Connection) connections.get(0);
            int connectionId = conn.getIdentifier();
            
            String releasePacket = "00010011|" + connectionId + "|56|60";
            FileManager.writeToFile(L_LEC_PATH, releasePacket);
            
            Thread.sleep(2000);
            
            boolean connectionReleased = connections.isEmpty();
            
            if (!connectionReleased && !connections.get(0).equals(null)) {
                conn = (Connection) connections.get(0);
                connectionReleased = conn.isReleaseConfirmed();
            }
            
            assertTrue("La connexion devrait être libérée ou supprimée", connectionReleased);
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
            
            GlobalContext.resetConnectionIdCounter();
        } catch (Exception e) {
            System.err.println("Erreur lors de la réinitialisation de l'état: " + e.getMessage());
        }
    }
}