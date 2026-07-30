package ma.jadwal.scolarite.depot;

/**
 * Projection d'agrégat : nombre d'élèves d'un statut donné rattachés à un
 * groupe. Sert de dénominateur au taux d'absentéisme par classe.
 *
 * <p>Le libellé du groupe n'est pas une donnée personnelle : il nomme la classe,
 * pas l'élève.
 */
public record EffectifGroupe(Long groupeId, String groupeLibelle, Long effectif) {

    public long effectifOuZero() {
        return effectif == null ? 0L : effectif;
    }
}
