package ma.jadwal.pedagogie.entite;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.entite.Niveau;

@Entity
@Table(name = "maquette", uniqueConstraints = @UniqueConstraint(columnNames = {"niveau_id", "matiere_id"}))
public class Maquette {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "niveau_id", nullable = false)
    private Niveau niveau;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @Column(name = "volume_unites", nullable = false)
    private int volumeUnites;

    @Column(name = "volume_unites_b")
    private Integer volumeUnitesB;

    @Column(name = "max_par_jour_unites", nullable = false)
    private int maxParJourUnites = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Dedoublement dedoublement = Dedoublement.AUCUN;

    @Column(name = "nb_sous_groupes", nullable = false)
    private int nbSousGroupes = 2;

    @Column(name = "co_enseignants", nullable = false)
    private int coEnseignants = 1;

    @Column(name = "patterns_json", columnDefinition = "text")
    private String patternsJson;

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

    public Niveau getNiveau() {
        return niveau;
    }

    public void setNiveau(Niveau niveau) {
        this.niveau = niveau;
    }

    public Matiere getMatiere() {
        return matiere;
    }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
    }

    public int getVolumeUnites() {
        return volumeUnites;
    }

    public void setVolumeUnites(int volumeUnites) {
        this.volumeUnites = volumeUnites;
    }

    public Integer getVolumeUnitesB() {
        return volumeUnitesB;
    }

    public void setVolumeUnitesB(Integer volumeUnitesB) {
        this.volumeUnitesB = volumeUnitesB;
    }

    public int getMaxParJourUnites() {
        return maxParJourUnites;
    }

    public void setMaxParJourUnites(int maxParJourUnites) {
        this.maxParJourUnites = maxParJourUnites;
    }

    public Dedoublement getDedoublement() {
        return dedoublement;
    }

    public void setDedoublement(Dedoublement dedoublement) {
        this.dedoublement = dedoublement;
    }

    public int getNbSousGroupes() {
        return nbSousGroupes;
    }

    public void setNbSousGroupes(int nbSousGroupes) {
        this.nbSousGroupes = nbSousGroupes;
    }

    public int getCoEnseignants() {
        return coEnseignants;
    }

    public void setCoEnseignants(int coEnseignants) {
        this.coEnseignants = coEnseignants;
    }

    public String getPatternsJson() {
        return patternsJson;
    }

    public void setPatternsJson(String patternsJson) {
        this.patternsJson = patternsJson;
    }
}
