package network;
import enums.ConnectionStateEnum;
import java.util.Random;

import core.GlobalContext;

public class Connection extends Thread { 
    /**
     * L'état de la connexion.
     */
    private ConnectionStateEnum etatConnexion;

    /**
     * L'identifiant de la connexion.
     */
    private int indentifier;

    /**
     * Le data qui sera envoyé.
     */
    private String data;
    
    /**
     * La station source de la connexion.
     */
    private int stationSource;
    
    /**
     * La station destination de la connexion.
     */
    private int stationDestination;
    
    /**
     * Compteur pour les stations
     */
    private static int stationIter = 0;
    
    /**
     * Indique si la demande de connexion a été traitée
     */
    private boolean processed = false;
    
    /**
     * Indique si les données ont été envoyées
     */
    private boolean dataSent = false;
    
    /**
     * Indique si la libération a été demandée
     */
    private boolean releaseRequested = false;
    
    /**
     * Indique si la libération a été confirmée
     */
    private boolean releaseConfirmed = false;
    
    /**
     * Constructeur de la classe Connection.
     * @param data Le data à envoyer.
     */
    public Connection(String data) {
        setIdentifier();
        this.etatConnexion = ConnectionStateEnum.WAITING_CONFIRMATION;
        this.data = data;
        assignStations(); // Assigne automatiquement les stations lors de la création
    }
    
    /**
     * Assigne les stations source et destination selon une logique prédéfinie.
     */
    private void assignStations() {
        switch (stationIter) {
            case 0:
                stationSource = 60;
                stationDestination = 56;
                break;
            case 1:
                stationSource = 27;
                stationDestination = 231;
                break;
            case 2:
                stationSource = 13;
                stationDestination = 237;
                break;
            case 3:
                stationSource = 150;
                stationDestination = 250;
                break;
            case 4:
                stationSource = 19;
                stationDestination = 109;
                break;
            case 5:
                stationSource = 54;
                stationDestination = 34;
                break;
            case 6:
                stationSource = 139;
                stationDestination = 6;
                break;
            default:
                Random stationNumberGenerator = new Random();
                stationSource = stationNumberGenerator.nextInt(255);
                while ((stationDestination = stationNumberGenerator.nextInt(255)) == stationSource) {
                    stationDestination = stationNumberGenerator.nextInt(255);
                }
                break;
        }
        stationIter++;
    }

    @Override
    public void run() {
        try {
            // Simuler le traitement de la connexion
            Thread.sleep(2000); // Simule un délai de 2 secondes
            // Nous ne changeons plus l'état ici, c'est ET qui le fait en fonction des réponses d'ER
        } catch (InterruptedException e) {
            System.err.println("Erreur lors du traitement de la connexion " + indentifier + ": " + e.getMessage());
        }
    }

    /**
     * Retourne l'état de la connexion.
     */
    public ConnectionStateEnum getEtatConnexion() {
        return etatConnexion;
    }

    /**
     * Définit l'état de la connexion.
     */
    public void setEtatConnexion(ConnectionStateEnum etatConnexion) {
        this.etatConnexion = etatConnexion;
    }

    /**
     * Retourne l'identifiant de la connexion.
     */
    public int getIdentifier() {
        return indentifier;
    }

    /**
     * Définit l'identifiant de la connexion.
     */
    public void setIdentifier() {
        this.indentifier = GlobalContext.getNextConnectionId();
    }
    
    /**
     * Retourne la station source de la connexion.
     */
    public int getStationSource() {
        return stationSource;
    }
    
    /**
     * Définit la station source de la connexion.
     * @param stationSource La station source à définir
     */
    public void setStationSource(int stationSource) {
        this.stationSource = stationSource;
    }
    
    /**
     * Retourne la station destination de la connexion.
     */
    public int getStationDestination() {
        return stationDestination;
    }
    
    /**
     * Définit la station destination de la connexion.
     * @param stationDestination La station destination à définir
     */
    public void setStationDestination(int stationDestination) {
        this.stationDestination = stationDestination;
    }
    
    /**
     * Retourne les données à envoyer.
     */
    public String getData() {
        return data;
    }
    
    /**
     * Indique si la connexion a été traitée
     */
    public boolean isProcessed() {
        return processed;
    }
    
    /**
     * Définit si la connexion a été traitée
     */
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
    /**
     * Indique si les données ont été envoyées
     */
    public boolean isDataSent() {
        return dataSent;
    }
    
    /**
     * Définit si les données ont été envoyées
     */
    public void setDataSent(boolean dataSent) {
        this.dataSent = dataSent;
    }
    
    /**
     * Indique si la libération a été demandée
     */
    public boolean isReleaseRequested() {
        return releaseRequested;
    }
    
    /**
     * Définit si la libération a été demandée
     */
    public void setReleaseRequested(boolean releaseRequested) {
        this.releaseRequested = releaseRequested;
    }
    
    /**
     * Indique si la libération a été confirmée
     */
    public boolean isReleaseConfirmed() {
        return releaseConfirmed;
    }
    
    /**
     * Définit si la libération a été confirmée
     */
    public void setReleaseConfirmed(boolean releaseConfirmed) {
        this.releaseConfirmed = releaseConfirmed;
    }

    @Override
    public String toString() {
        return "Connection Details:\n" +
               "-------------------\n" +
               "State          : " + etatConnexion + "\n" +
               "Identifier     : " + indentifier + "\n" +
               "Data           : " + data + "\n" +
               "Source Station : " + stationSource + "\n" +
               "Dest Station   : " + stationDestination + "\n" +
               "Processed      : " + processed + "\n" +
               "Data Sent      : " + dataSent + "\n" +
               "Release Req    : " + releaseRequested + "\n" +
               "Release Conf   : " + releaseConfirmed + "\n" +
               "-------------------";
    }
}