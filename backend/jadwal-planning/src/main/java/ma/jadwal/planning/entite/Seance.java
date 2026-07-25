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
import jakarta.persistence.Table;
import ma.jadwal.enseignant.entite.Enseignant;
import ma.jadwal.referentiel.entite.Creneau;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.entite.Salle;

@Entity
@Table(name = "seance")
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private PlanningVersion version;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "groupe_id", nullable = false)
    private Groupe groupe;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salle_id")
    private Salle salle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creneau_id")
    private Creneau creneau;

    @Column(name = "duree_unites", nullable = false)
    private int dureeUnites;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private SemaineSeance semaine = SemaineSeance.TOUTES;

    @Column(name = "bloc_alignement", length = 50)
    private String blocAlignement;

    @Column(name = "barrette_id")
    private Long barretteId;

    @Column(nullable = false)
    private boolean verrouillee = false;

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

    public Groupe getGroupe() {
        return groupe;
    }

    public void setGroupe(Groupe groupe) {
        this.groupe = groupe;
    }

    public Matiere getMatiere() {
        return matiere;
    }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
    }

    public Enseignant getEnseignant() {
        return enseignant;
    }

    public void setEnseignant(Enseignant enseignant) {
        this.enseignant = enseignant;
    }

    public Salle getSalle() {
        return salle;
    }

    public void setSalle(Salle salle) {
        this.salle = salle;
    }

    public Creneau getCreneau() {
        return creneau;
    }

    public void setCreneau(Creneau creneau) {
        this.creneau = creneau;
    }

    public int getDureeUnites() {
        return dureeUnites;
    }

    public void setDureeUnites(int dureeUnites) {
        this.dureeUnites = dureeUnites;
    }

    public SemaineSeance getSemaine() {
        return semaine;
    }

    public void setSemaine(SemaineSeance semaine) {
        this.semaine = semaine;
    }

    public String getBlocAlignement() {
        return blocAlignement;
    }

    public void setBlocAlignement(String blocAlignement) {
        this.blocAlignement = blocAlignement;
    }

    public Long getBarretteId() {
        return barretteId;
    }

    public void setBarretteId(Long barretteId) {
        this.barretteId = barretteId;
    }

    public boolean isVerrouillee() {
        return verrouillee;
    }

    public void setVerrouillee(boolean verrouillee) {
        this.verrouillee = verrouillee;
    }
}
