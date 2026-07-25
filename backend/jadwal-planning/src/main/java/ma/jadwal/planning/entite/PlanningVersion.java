package ma.jadwal.planning.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import ma.jadwal.referentiel.entite.Etablissement;

import java.time.Instant;

@Entity
@Table(name = "planning_version")
public class PlanningVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private Etablissement etablissement;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "creee_le", nullable = false, updatable = false)
    private Instant creeeLe;

    @PrePersist
    void avantInsertion() {
        if (creeeLe == null) {
            creeeLe = Instant.now();
        }
    }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreeeLe() {
        return creeeLe;
    }

    public void setCreeeLe(Instant creeeLe) {
        this.creeeLe = creeeLe;
    }
}
