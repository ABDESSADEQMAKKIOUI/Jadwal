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
@Table(name = "groupe")
public class Groupe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "niveau_id", nullable = false)
    private Niveau niveau;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(nullable = false)
    private int effectif = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private Groupe parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeGroupe type = TypeGroupe.CLASSE;

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

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public int getEffectif() {
        return effectif;
    }

    public void setEffectif(int effectif) {
        this.effectif = effectif;
    }

    public Groupe getParent() {
        return parent;
    }

    public void setParent(Groupe parent) {
        this.parent = parent;
    }

    public TypeGroupe getType() {
        return type;
    }

    public void setType(TypeGroupe type) {
        this.type = type;
    }
}
