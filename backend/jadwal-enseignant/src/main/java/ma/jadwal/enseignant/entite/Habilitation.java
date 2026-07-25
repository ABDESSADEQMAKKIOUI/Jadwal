package ma.jadwal.enseignant.entite;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.entite.Niveau;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "habilitation", uniqueConstraints = @UniqueConstraint(columnNames = {"enseignant_id", "matiere_id"}))
public class Habilitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "habilitation_niveau",
            joinColumns = @JoinColumn(name = "habilitation_id"),
            inverseJoinColumns = @JoinColumn(name = "niveau_id"))
    private Set<Niveau> niveaux = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Enseignant getEnseignant() {
        return enseignant;
    }

    public void setEnseignant(Enseignant enseignant) {
        this.enseignant = enseignant;
    }

    public Matiere getMatiere() {
        return matiere;
    }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
    }

    public Set<Niveau> getNiveaux() {
        return niveaux;
    }

    public void setNiveaux(Set<Niveau> niveaux) {
        this.niveaux = niveaux;
    }
}
