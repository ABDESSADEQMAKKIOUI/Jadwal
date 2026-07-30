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
import ma.jadwal.planning.entite.Seance;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Évènement de vie scolaire saisi sur la feuille d'appel : absence, retard
 * ou exclusion, rattaché soit à une demi-journée, soit à une séance précise.
 * Donnée personnelle de mineur : jamais dans les journaux.
 */
@Entity
@Table(name = "absence")
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @Column(name = "date_absence", nullable = false)
    private LocalDate dateAbsence;

    @Enumerated(EnumType.STRING)
    @Column(name = "demi_journee", nullable = false, length = 12)
    private DemiJournee demiJournee = DemiJournee.MATIN;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seance_id")
    private Seance seance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TypeAbsence type = TypeAbsence.ABSENCE;

    @Column(nullable = false)
    private boolean justifiee = false;

    @Column(length = 200)
    private String motif;

    @Column(name = "saisie_par")
    private Long saisiePar;

    @Column(name = "date_saisie", nullable = false, updatable = false)
    private Instant dateSaisie;

    @PrePersist
    void avantInsertion() {
        if (dateSaisie == null) {
            dateSaisie = Instant.now();
        }
        if (demiJournee == null) {
            demiJournee = DemiJournee.MATIN;
        }
        if (type == null) {
            type = TypeAbsence.ABSENCE;
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

    public Eleve getEleve() {
        return eleve;
    }

    public void setEleve(Eleve eleve) {
        this.eleve = eleve;
    }

    public LocalDate getDateAbsence() {
        return dateAbsence;
    }

    public void setDateAbsence(LocalDate dateAbsence) {
        this.dateAbsence = dateAbsence;
    }

    public DemiJournee getDemiJournee() {
        return demiJournee;
    }

    public void setDemiJournee(DemiJournee demiJournee) {
        this.demiJournee = demiJournee;
    }

    public Seance getSeance() {
        return seance;
    }

    public void setSeance(Seance seance) {
        this.seance = seance;
    }

    public TypeAbsence getType() {
        return type;
    }

    public void setType(TypeAbsence type) {
        this.type = type;
    }

    public boolean isJustifiee() {
        return justifiee;
    }

    public void setJustifiee(boolean justifiee) {
        this.justifiee = justifiee;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Long getSaisiePar() {
        return saisiePar;
    }

    public void setSaisiePar(Long saisiePar) {
        this.saisiePar = saisiePar;
    }

    public Instant getDateSaisie() {
        return dateSaisie;
    }

    public void setDateSaisie(Instant dateSaisie) {
        this.dateSaisie = dateSaisie;
    }
}
