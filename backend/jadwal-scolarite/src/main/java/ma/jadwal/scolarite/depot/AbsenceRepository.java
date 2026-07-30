package ma.jadwal.scolarite.depot;

import ma.jadwal.scolarite.entite.Absence;
import ma.jadwal.scolarite.entite.DemiJournee;
import ma.jadwal.scolarite.entite.TypeAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Toutes les méthodes filtrent par établissement : une absence ne doit jamais
 * être lue ni modifiée depuis un autre locataire.
 */
public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    Optional<Absence> findByIdAndEtablissementId(Long id, Long etablissementId);

    List<Absence> findByEtablissementIdAndEleveIdOrderByDateAbsenceDescIdDesc(Long etablissementId, Long eleveId);

    List<Absence> findByEtablissementIdAndEleveIdAndDateAbsenceBetweenOrderByDateAbsenceDescIdDesc(
            Long etablissementId, Long eleveId, LocalDate debut, LocalDate fin);

    List<Absence> findByEtablissementIdAndDateAbsence(Long etablissementId, LocalDate dateAbsence);

    List<Absence> findByEtablissementIdAndDateAbsenceBetween(Long etablissementId, LocalDate debut, LocalDate fin);

    /** Saisies de l'élève pour une date, hors contexte séance (feuille de demi-journée). */
    List<Absence> findByEtablissementIdAndEleveIdAndDateAbsenceAndSeanceIsNull(
            Long etablissementId, Long eleveId, LocalDate dateAbsence);

    /** Saisie de l'élève pour une séance précise. */
    Optional<Absence> findByEtablissementIdAndEleveIdAndDateAbsenceAndSeanceId(
            Long etablissementId, Long eleveId, LocalDate dateAbsence, Long seanceId);

    /** Feuille d'appel d'une demi-journée pour un lot d'élèves. */
    List<Absence> findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(
            Long etablissementId, LocalDate dateAbsence, Collection<Long> eleveIds);

    /** Feuille d'appel d'une séance pour un lot d'élèves. */
    List<Absence> findByEtablissementIdAndDateAbsenceAndSeanceIdAndEleveIdIn(
            Long etablissementId, LocalDate dateAbsence, Long seanceId, Collection<Long> eleveIds);

    long countByEtablissementIdAndDateAbsence(Long etablissementId, LocalDate dateAbsence);

    long countByEtablissementIdAndDateAbsenceBetweenAndType(
            Long etablissementId, LocalDate debut, LocalDate fin, TypeAbsence type);

    long countByEtablissementIdAndDateAbsenceBetweenAndTypeAndDemiJournee(
            Long etablissementId, LocalDate debut, LocalDate fin, TypeAbsence type, DemiJournee demiJournee);

    long countByEtablissementIdAndDateAbsenceBetweenAndTypeAndJustifiee(
            Long etablissementId, LocalDate debut, LocalDate fin, TypeAbsence type, boolean justifiee);

    long countByEtablissementIdAndEleveIdAndDateAbsenceBetweenAndType(
            Long etablissementId, Long eleveId, LocalDate debut, LocalDate fin, TypeAbsence type);

    /**
     * Recherche filtrée d'une liste d'absences. Le filtre d'établissement est
     * obligatoire, tous les autres critères sont optionnels (paramètre nul =
     * critère ignoré).
     *
     * <p>Les jointures sont des {@code left join} sur le groupe : un élève sans
     * classe reste visible tant qu'aucun filtre de groupe ni de niveau n'est
     * demandé. Elles sont aussi des {@code fetch} pour éviter une requête par
     * ligne à la sérialisation (l'élève et son groupe sont EAGER).
     */
    @Query("""
            select a from Absence a
              join fetch a.eleve e
              left join fetch e.groupe g
              left join fetch g.niveau n
            where a.etablissementId = :etablissementId
              and a.dateAbsence between :debut and :fin
              and (:eleveId is null or e.id = :eleveId)
              and (:groupeId is null or g.id = :groupeId)
              and (:niveauId is null or n.id = :niveauId)
              and (:type is null or a.type = :type)
              and (:justifiee is null or a.justifiee = :justifiee)
            order by a.dateAbsence desc, e.nom asc, e.prenom asc, a.id desc
            """)
    List<Absence> rechercher(@Param("etablissementId") Long etablissementId,
                             @Param("debut") LocalDate debut,
                             @Param("fin") LocalDate fin,
                             @Param("eleveId") Long eleveId,
                             @Param("groupeId") Long groupeId,
                             @Param("niveauId") Long niveauId,
                             @Param("type") TypeAbsence type,
                             @Param("justifiee") Boolean justifiee);

    /**
     * Décomptes d'absences de la période, à la maille la plus fine utile aux
     * statistiques : groupe × jour × nature × demi-journée × justification.
     * Les séries temporelles, les totaux et la répartition par classe se
     * recomposent en mémoire à partir de ce seul aller-retour.
     *
     * <p>Un filtre de groupe ou de niveau exclut mécaniquement les élèves sans
     * classe, dont le {@code groupeId} projeté est nul.
     */
    @Query("""
            select new ma.jadwal.scolarite.depot.AgregatAbsences(
                       g.id, a.dateAbsence, a.type, a.demiJournee, a.justifiee, count(a))
            from Absence a
              join a.eleve e
              left join e.groupe g
              left join g.niveau n
            where a.etablissementId = :etablissementId
              and a.dateAbsence between :debut and :fin
              and (:groupeId is null or g.id = :groupeId)
              and (:niveauId is null or n.id = :niveauId)
            group by g.id, a.dateAbsence, a.type, a.demiJournee, a.justifiee
            order by a.dateAbsence asc
            """)
    List<AgregatAbsences> agreger(@Param("etablissementId") Long etablissementId,
                                  @Param("debut") LocalDate debut,
                                  @Param("fin") LocalDate fin,
                                  @Param("groupeId") Long groupeId,
                                  @Param("niveauId") Long niveauId);

    /**
     * Élèves dont le nombre d'absences non justifiées atteint le seuil d'alerte
     * sur la période. Ne renvoie que des identifiants : l'appelant recharge les
     * élèves du même établissement pour les nommer.
     */
    @Query("""
            select new ma.jadwal.scolarite.depot.CompteurAbsencesEleve(a.eleve.id, count(a))
            from Absence a
            where a.etablissementId = :etablissementId
              and a.type = :type
              and a.justifiee = false
              and a.dateAbsence between :debut and :fin
            group by a.eleve.id
            having count(a) >= :seuil
            order by count(a) desc, a.eleve.id asc
            """)
    List<CompteurAbsencesEleve> compterNonJustifieesParEleve(@Param("etablissementId") Long etablissementId,
                                                            @Param("type") TypeAbsence type,
                                                            @Param("debut") LocalDate debut,
                                                            @Param("fin") LocalDate fin,
                                                            @Param("seuil") long seuil);
}
