package tests;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import core.FileManager;
import core.GlobalContext;
import network.ET;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class FileInitializationTest {
    
    private static final String S_LEC_PATH = "S_lec";
    private static final String S_ECR_PATH = "S_ecr";
    private static final String L_LEC_PATH = "L_lec";
    private static final String L_ECR_PATH = "L_ecr";
    
    @Before
    public void setUp() {
        cleanupFiles();
        GlobalContext.initializeFiles();
    }
    
    @After
    public void tearDown() {
        cleanupFiles();
    }
    
    @Test
    public void testFilesExist() {
        assertTrue("Le fichier S_lec n'existe pas", new File(S_LEC_PATH).exists());
        assertTrue("Le fichier S_ecr n'existe pas", new File(S_ECR_PATH).exists());
        assertTrue("Le fichier L_lec n'existe pas", new File(L_LEC_PATH).exists());
        assertTrue("Le fichier L_ecr n'existe pas", new File(L_ECR_PATH).exists());
    }
    
    @Test
    public void testS_lecContent() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(S_LEC_PATH)));
        assertFalse("S_lec devrait contenir du contenu initial", content.isEmpty());
    }
    
    @Test
    public void testEmptyOutputFiles() throws IOException {
        String sEcrContent = new String(Files.readAllBytes(Paths.get(S_ECR_PATH)));
        String lEcrContent = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        String lLecContent = new String(Files.readAllBytes(Paths.get(L_LEC_PATH)));
        
        assertTrue("S_ecr devrait être vide initialement", sEcrContent.isEmpty());
        assertTrue("L_ecr devrait être vide initialement", lEcrContent.isEmpty());
        assertTrue("L_lec devrait être vide initialement", lLecContent.isEmpty());
    }
    
    @Test
    public void testWriteToS_ecr() throws IOException {
        String testMessage = "Test message";
        ET.writeToFile(testMessage);
        
        String content = new String(Files.readAllBytes(Paths.get(S_ECR_PATH)));
        assertTrue("Le message devrait être écrit dans S_ecr", content.contains(testMessage));
    }
    
    @Test
    public void testWriteToL_ecr() throws IOException {
        String testMessage = "Test packet";
        FileManager.appendToFile(L_ECR_PATH, testMessage);
        
        String content = new String(Files.readAllBytes(Paths.get(L_ECR_PATH)));
        assertTrue("Le message devrait être écrit dans L_ecr", content.contains(testMessage));
    }
    
    private void cleanupFiles() {
        deleteFile(S_LEC_PATH);
        deleteFile(S_ECR_PATH);
        deleteFile(L_LEC_PATH);
        deleteFile(L_ECR_PATH);
    }
    
    private void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }
}