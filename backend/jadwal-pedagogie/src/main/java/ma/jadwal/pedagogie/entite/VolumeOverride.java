package ma.jadwal.pedagogie.entite;

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
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Matiere;

@Entity
@Table(name = "volume_override", uniqueConstraints = @UniqueConstraint(columnNames = {"groupe_id", "matiere_id"}))
public class VolumeOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "groupe_id", nullable = false)
    private Groupe groupe;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @Column(name = "volume_unites", nullable = false)
    private int volumeUnites;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getVolumeUnites() {
        return volumeUnites;
    }

    public void setVolumeUnites(int volumeUnites) {
        this.volumeUnites = volumeUnites;
    }
}
