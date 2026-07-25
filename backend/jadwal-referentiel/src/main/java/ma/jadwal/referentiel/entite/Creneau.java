package ma.jadwal.referentiel.entite;

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

@Entity
@Table(name = "creneau")
public class Creneau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private Etablissement etablissement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Jour jour;

    @Column(name = "index_debut", nullable = false)
    private int indexDebut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TypeCreneau type = TypeCreneau.COURS;

    @Column(name = "unites_disponibles", nullable = false)
    private int unitesDisponibles = 1;

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

    public TypeCreneau getType() {
        return type;
    }

    public void setType(TypeCreneau type) {
        this.type = type;
    }

    public int getUnitesDisponibles() {
        return unitesDisponibles;
    }

    public void setUnitesDisponibles(int unitesDisponibles) {
        this.unitesDisponibles = unitesDisponibles;
    }
}
