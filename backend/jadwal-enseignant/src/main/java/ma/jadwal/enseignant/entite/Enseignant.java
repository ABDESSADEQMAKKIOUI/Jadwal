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
import ma.jadwal.referentiel.entite.Etablissement;

@Entity
@Table(name = "enseignant")
public class Enseignant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private Etablissement etablissement;

    @Column(name = "nom_complet", nullable = false, length = 150)
    private String nomComplet;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TypeEnseignant type = TypeEnseignant.PROPRE;

    @Column(name = "quota_hebdo_unites", nullable = false)
    private int quotaHebdoUnites = 36;

    @Column(name = "max_consecutif_unites", nullable = false)
    private int maxConsecutifUnites = 10;

    @Column(name = "amplitude_max_unites")
    private Integer amplitudeMaxUnites;

    @Column(nullable = false)
    private boolean vacataire = false;

    @Column(name = "buffer_trajet_unites", nullable = false)
    private int bufferTrajetUnites = 1;

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

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TypeEnseignant getType() {
        return type;
    }

    public void setType(TypeEnseignant type) {
        this.type = type;
    }

    public int getQuotaHebdoUnites() {
        return quotaHebdoUnites;
    }

    public void setQuotaHebdoUnites(int quotaHebdoUnites) {
        this.quotaHebdoUnites = quotaHebdoUnites;
    }

    public int getMaxConsecutifUnites() {
        return maxConsecutifUnites;
    }

    public void setMaxConsecutifUnites(int maxConsecutifUnites) {
        this.maxConsecutifUnites = maxConsecutifUnites;
    }

    public Integer getAmplitudeMaxUnites() {
        return amplitudeMaxUnites;
    }

    public void setAmplitudeMaxUnites(Integer amplitudeMaxUnites) {
        this.amplitudeMaxUnites = amplitudeMaxUnites;
    }

    public boolean isVacataire() {
        return vacataire;
    }

    public void setVacataire(boolean vacataire) {
        this.vacataire = vacataire;
    }

    public int getBufferTrajetUnites() {
        return bufferTrajetUnites;
    }

    public void setBufferTrajetUnites(int bufferTrajetUnites) {
        this.bufferTrajetUnites = bufferTrajetUnites;
    }
}
