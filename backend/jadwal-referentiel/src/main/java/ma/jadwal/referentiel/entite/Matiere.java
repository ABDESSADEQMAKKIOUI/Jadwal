package ma.jadwal.referentiel.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "matiere", uniqueConstraints = @UniqueConstraint(columnNames = {"etablissement_id", "code"}))
public class Matiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private Etablissement etablissement;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false)
    private int coefficient = 1;

    @Column(name = "poids_cognitif", nullable = false)
    private int poidsCognitif = 3;

    @Column(nullable = false, length = 7)
    private String couleur = "#6366f1";

    @Column(name = "type_salle_requis", length = 50)
    private String typeSalleRequis;

    @Column(name = "equipements_requis", columnDefinition = "text")
    private String equipementsRequis;

    @Column(name = "duree_min_unites", nullable = false)
    private int dureeMinUnites = 1;

    @Column(name = "duree_max_unites", nullable = false)
    private int dureeMaxUnites = 4;

    @Column(name = "eviter_avant_dejeuner", nullable = false)
    private boolean eviterAvantDejeuner = false;

    @Column(name = "eviter_fin_journee", nullable = false)
    private boolean eviterFinJournee = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Etablissement getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(Etablissement etablissement) {
        this.etablissement = etablissement;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(int coefficient) {
        this.coefficient = coefficient;
    }

    public int getPoidsCognitif() {
        return poidsCognitif;
    }

    public void setPoidsCognitif(int poidsCognitif) {
        this.poidsCognitif = poidsCognitif;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getTypeSalleRequis() {
        return typeSalleRequis;
    }

    public void setTypeSalleRequis(String typeSalleRequis) {
        this.typeSalleRequis = typeSalleRequis;
    }

    public String getEquipementsRequis() {
        return equipementsRequis;
    }

    public void setEquipementsRequis(String equipementsRequis) {
        this.equipementsRequis = equipementsRequis;
    }

    public int getDureeMinUnites() {
        return dureeMinUnites;
    }

    public void setDureeMinUnites(int dureeMinUnites) {
        this.dureeMinUnites = dureeMinUnites;
    }

    public int getDureeMaxUnites() {
        return dureeMaxUnites;
    }

    public void setDureeMaxUnites(int dureeMaxUnites) {
        this.dureeMaxUnites = dureeMaxUnites;
    }

    public boolean isEviterAvantDejeuner() {
        return eviterAvantDejeuner;
    }

    public void setEviterAvantDejeuner(boolean eviterAvantDejeuner) {
        this.eviterAvantDejeuner = eviterAvantDejeuner;
    }

    public boolean isEviterFinJournee() {
        return eviterFinJournee;
    }

    public void setEviterFinJournee(boolean eviterFinJournee) {
        this.eviterFinJournee = eviterFinJournee;
    }
}
