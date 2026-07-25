package ma.jadwal.enseignant.entite;

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
import ma.jadwal.referentiel.entite.Jour;

@Entity
@Table(name = "preference_horaire")
public class PreferenceHoraire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Jour jour;

    @Column(name = "index_debut", nullable = false)
    private int indexDebut;

    @Column(name = "duree_unites", nullable = false)
    private int dureeUnites = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TypePreference type = TypePreference.EVITER;

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

    public Jour getJour() {
        return jour;
    }

    public void setJour(Jour jour) {
        this.jour = jour;
    }

    public int getIndexDebut() {
        return indexDebut;
    }

    public void setIndexDebut(int indexDebut) {
        this.indexDebut = indexDebut;
    }

    public int getDureeUnites() {
        return dureeUnites;
    }

    public void setDureeUnites(int dureeUnites) {
        this.dureeUnites = dureeUnites;
    }

    public TypePreference getType() {
        return type;
    }

    public void setType(TypePreference type) {
        this.type = type;
    }
}
