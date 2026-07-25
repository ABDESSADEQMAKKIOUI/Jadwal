package ma.jadwal.api.generation;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * État en mémoire d'une génération en cours : meilleur score connu, statut,
 * demande d'arrêt (I-05) et émetteurs SSE abonnés à la progression.
 */
public class ProgressionGeneration {

    private final Long generationId;
    private final Instant lancement = Instant.now();
    private final CopyOnWriteArrayList<SseEmitter> emetteurs = new CopyOnWriteArrayList<>();
    private volatile String score;
    private volatile String statut = "EN_COURS";
    private volatile boolean arretDemande = false;

    public ProgressionGeneration(Long generationId) {
        this.generationId = generationId;
    }

    public Long getGenerationId() {
        return generationId;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public boolean isArretDemande() {
        return arretDemande;
    }

    public void demanderArret() {
        this.arretDemande = true;
    }

    public long tempsEcouleSecondes() {
        return Duration.between(lancement, Instant.now()).toSeconds();
    }

    public Map<String, Object> donneesCourantes() {
        Map<String, Object> donnees = new LinkedHashMap<>();
        donnees.put("score", score);
        donnees.put("tempsEcouleSecondes", tempsEcouleSecondes());
        donnees.put("statut", statut);
        return donnees;
    }

    public void ajouterEmetteur(SseEmitter emetteur) {
        emetteurs.add(emetteur);
        emetteur.onCompletion(() -> emetteurs.remove(emetteur));
        emetteur.onTimeout(() -> emetteurs.remove(emetteur));
        emetteur.onError(e -> emetteurs.remove(emetteur));
    }

    public List<SseEmitter> emetteurs() {
        return emetteurs;
    }
}
