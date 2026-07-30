package ma.jadwal.scolarite.entite;

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
import ma.jadwal.referentiel.entite.Groupe;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Élève inscrit dans un établissement. Donnée personnelle de mineur :
 * ne jamais journaliser le contenu de cette entité, seuls les identifiants
 * techniques peuvent apparaître dans les traces.
 */
@Entity
@Table(name = "eleve")
public class Eleve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "groupe_id")
    private Groupe groupe;

    @Column(name = "code_massar", nullable = false, length = 30)
    private String codeMassar;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = "nom_ar", length = 100)
    private String nomAr;

    @Column(name = "prenom_ar", length = 100)
    private String prenomAr;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance", length = 120)
    private String lieuNaissance;

    /**
     * {@code columnDefinition} explicite : Hibernate mappe par défaut une chaîne
     * de longueur 1 sur {@code char(1)}, alors que le DDL déclare {@code varchar(1)}.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 1, columnDefinition = "varchar(1)")
    private Sexe sexe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEleve statut = StatutEleve.INSCRIT;

    @Column(name = "tuteur_nom", length = 150)
    private String tuteurNom;

    @Column(name = "tuteur_telephone", length = 30)
    private String tuteurTelephone;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @PrePersist
    void avantInsertion() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
        if (statut == null) {
            statut = StatutEleve.INSCRIT;
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

    public Groupe getGroupe() {
        return groupe;
    }

    public void setGroupe(Groupe groupe) {
        this.groupe = groupe;
    }

    public String getCodeMassar() {
        return codeMassar;
    }

    public void setCodeMassar(String codeMassar) {
        this.codeMassar = codeMassar;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNomAr() {
        return nomAr;
    }

    public void setNomAr(String nomAr) {
        this.nomAr = nomAr;
    }

    public String getPrenomAr() {
        return prenomAr;
    }

    public void setPrenomAr(String prenomAr) {
        this.prenomAr = prenomAr;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getLieuNaissance() {
        return lieuNaissance;
    }

    public void setLieuNaissance(String lieuNaissance) {
        this.lieuNaissance = lieuNaissance;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public void setSexe(Sexe sexe) {
        this.sexe = sexe;
    }

    public StatutEleve getStatut() {
        return statut;
    }

    public void setStatut(StatutEleve statut) {
        this.statut = statut;
    }

    public String getTuteurNom() {
        return tuteurNom;
    }

    public void setTuteurNom(String tuteurNom) {
        this.tuteurNom = tuteurNom;
    }

    public String getTuteurTelephone() {
        return tuteurTelephone;
    }

    public void setTuteurTelephone(String tuteurTelephone) {
        this.tuteurTelephone = tuteurTelephone;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }
}
