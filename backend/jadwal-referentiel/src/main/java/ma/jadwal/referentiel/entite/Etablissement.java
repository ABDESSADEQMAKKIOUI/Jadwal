package ma.jadwal.referentiel.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "etablissement")
public class Etablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String ville;

    @Column(length = 30)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEtablissement statut = StatutEtablissement.ACTIF;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "grille_json", columnDefinition = "text")
    private String grilleJson;

    @Column(name = "amplitude_max_unites", nullable = false)
    private int amplitudeMaxUnites = 16;

    @PrePersist
    void avantInsertion() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
        if (statut == null) {
            statut = StatutEtablissement.ACTIF;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public StatutEtablissement getStatut() {
        return statut;
    }

    public void setStatut(StatutEtablissement statut) {
        this.statut = statut;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getGrilleJson() {
        return grilleJson;
    }

    public void setGrilleJson(String grilleJson) {
        this.grilleJson = grilleJson;
    }

    public int getAmplitudeMaxUnites() {
        return amplitudeMaxUnites;
    }

    public void setAmplitudeMaxUnites(int amplitudeMaxUnites) {
        this.amplitudeMaxUnites = amplitudeMaxUnites;
    }
}
