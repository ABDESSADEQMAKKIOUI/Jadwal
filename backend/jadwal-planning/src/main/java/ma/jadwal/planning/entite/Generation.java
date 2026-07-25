package ma.jadwal.planning.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "generation")
public class Generation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id")
    private PlanningVersion version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatutGeneration statut = StatutGeneration.EN_COURS;

    @Column(length = 120)
    private String score;

    @Column(name = "duree_max_secondes", nullable = false)
    private int dureeMaxSecondes = 120;

    @Column(name = "rapport_faisabilite", columnDefinition = "text")
    private String rapportFaisabilite;

    // "analyse" est un mot réservé PostgreSQL (ANALYSE/ANALYZE) : colonne nommée analyse_json.
    @Column(name = "analyse_json", columnDefinition = "text")
    private String analyse;

    @Column(name = "lancee_le", nullable = false, updatable = false)
    private Instant lanceeLe;

    @Column(name = "terminee_le")
    private Instant termineeLe;

    @PrePersist
    void avantInsertion() {
        if (lanceeLe == null) {
            lanceeLe = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEtablissementId() {
        return etablissementId;
    }

    public void setEtablissementId(Long etablissementId) {
        this.etablissementId = etablissementId;
    }

    public PlanningVersion getVersion() {
        return version;
    }

    public void setVersion(PlanningVersion version) {
        this.version = version;
    }

    public StatutGeneration getStatut() {
        return statut;
    }

    public void setStatut(StatutGeneration statut) {
        this.statut = statut;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public int getDureeMaxSecondes() {
        return dureeMaxSecondes;
    }

    public void setDureeMaxSecondes(int dureeMaxSecondes) {
        this.dureeMaxSecondes = dureeMaxSecondes;
    }

    public String getRapportFaisabilite() {
        return rapportFaisabilite;
    }

    public void setRapportFaisabilite(String rapportFaisabilite) {
        this.rapportFaisabilite = rapportFaisabilite;
    }

    public String getAnalyse() {
        return analyse;
    }

    public void setAnalyse(String analyse) {
        this.analyse = analyse;
    }

    public Instant getLanceeLe() {
        return lanceeLe;
    }

    public void setLanceeLe(Instant lanceeLe) {
        this.lanceeLe = lanceeLe;
    }

    public Instant getTermineeLe() {
        return termineeLe;
    }

    public void setTermineeLe(Instant termineeLe) {
        this.termineeLe = termineeLe;
    }
}
