package ma.jadwal.scolarite.depot;

import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.StatutEleve;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Toutes les méthodes filtrent par établissement : aucune lecture d'élève
 * ne doit pouvoir franchir la frontière d'un locataire.
 */
public interface EleveRepository extends JpaRepository<Eleve, Long> {

    Optional<Eleve> findByIdAndEtablissementId(Long id, Long etablissementId);

    Optional<Eleve> findByEtablissementIdAndCodeMassar(Long etablissementId, String codeMassar);

    boolean existsByEtablissementIdAndCodeMassar(Long etablissementId, String codeMassar);

    boolean existsByEtablissementIdAndCodeMassarAndIdNot(Long etablissementId, String codeMassar, Long id);

    List<Eleve> findByEtablissementIdAndGroupeIdOrderByNomAscPrenomAsc(Long etablissementId, Long groupeId);

    List<Eleve> findByEtablissementIdAndIdInOrderByNomAscPrenomAsc(Long etablissementId, Collection<Long> ids);

    long countByEtablissementId(Long etablissementId);

    long countByEtablissementIdAndStatut(Long etablissementId, StatutEleve statut);

    long countByEtablissementIdAndGroupeId(Long etablissementId, Long groupeId);

    long countByEtablissementIdAndGroupeIsNull(Long etablissementId);

    /**
     * Effectifs par groupe pour un statut donné, dénominateur du taux
     * d'absentéisme par classe.
     *
     * <p>La requête part du groupe et non de l'élève : une classe sans aucun
     * élève du statut demandé remonte quand même, avec un effectif à zéro, ce
     * qui évite de la faire disparaître des statistiques. Elle vit dans ce dépôt
     * parce que seul le module scolarité connaît à la fois {@code Groupe} et
     * {@code Eleve}, le référentiel ignorant la scolarité.
     *
     * <p>Le filtre d'établissement s'applique aux deux côtés de la jointure :
     * ni le groupe ni l'élève d'un autre locataire ne peuvent être comptés.
     */
    @Query("""
            select new ma.jadwal.scolarite.depot.EffectifGroupe(g.id, g.libelle, count(e.id))
            from Groupe g
              left join Eleve e
                     on e.groupe = g
                    and e.etablissementId = :etablissementId
                    and e.statut = :statut
            where g.etablissement.id = :etablissementId
              and (:groupeId is null or g.id = :groupeId)
              and (:niveauId is null or g.niveau.id = :niveauId)
            group by g.id, g.libelle
            order by g.libelle asc
            """)
    List<EffectifGroupe> effectifsParGroupe(@Param("etablissementId") Long etablissementId,
                                            @Param("statut") StatutEleve statut,
                                            @Param("groupeId") Long groupeId,
                                            @Param("niveauId") Long niveauId);

    /**
     * Nombre d'élèves d'un statut donné, éventuellement restreint à un groupe ou
     * à un niveau. Un filtre de groupe ou de niveau exclut mécaniquement les
     * élèves sans classe.
     */
    @Query("""
            select count(e) from Eleve e
              left join e.groupe g
              left join g.niveau n
            where e.etablissementId = :etablissementId
              and e.statut = :statut
              and (:groupeId is null or g.id = :groupeId)
              and (:niveauId is null or n.id = :niveauId)
            """)
    long compterParStatut(@Param("etablissementId") Long etablissementId,
                          @Param("statut") StatutEleve statut,
                          @Param("groupeId") Long groupeId,
                          @Param("niveauId") Long niveauId);

    /**
     * Recherche paginée : le filtre d'établissement est obligatoire, les autres
     * critères sont optionnels (paramètre nul = critère ignoré). {@code recherche}
     * doit déjà être en minuscules et encadré de {@code %}.
     * <p>
     * Les jointures sont explicitement externes : un élève sans classe doit
     * rester visible dans une recherche non filtrée.
     */
    @Query("""
            select e from Eleve e
            left join e.groupe g
            left join g.niveau n
            where e.etablissementId = :etablissementId
              and (:groupeId is null or g.id = :groupeId)
              and (:niveauId is null or n.id = :niveauId)
              and (:statut is null or e.statut = :statut)
              and (:recherche is null
                   or lower(e.nom) like :recherche
                   or lower(e.prenom) like :recherche
                   or lower(e.codeMassar) like :recherche)
            """)
    Page<Eleve> rechercher(@Param("etablissementId") Long etablissementId,
                           @Param("groupeId") Long groupeId,
                           @Param("niveauId") Long niveauId,
                           @Param("statut") StatutEleve statut,
                           @Param("recherche") String recherche,
                           Pageable pageable);
}
