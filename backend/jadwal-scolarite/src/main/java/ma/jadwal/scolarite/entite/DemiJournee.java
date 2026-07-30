package ma.jadwal.scolarite.entite;

/**
 * Découpage de la journée scolaire pour la feuille d'appel.
 * {@link #JOURNEE} vaut deux demi-journées dans les statistiques.
 */
public enum DemiJournee {

    MATIN(1),
    APRES_MIDI(1),
    JOURNEE(2);

    private final int poids;

    DemiJournee(int poids) {
        this.poids = poids;
    }

    /**
     * Nombre de demi-journées représentées par cette valeur.
     */
    public int poids() {
        return poids;
    }

    /**
     * Deux saisies qui se chevauchent ne peuvent coexister pour un même élève
     * et une même date : la plus récente remplace l'autre. Une absence sur la
     * journée entière recouvre les deux demi-journées.
     */
    public boolean chevauche(DemiJournee autre) {
        return autre != null && (this == JOURNEE || autre == JOURNEE || this == autre);
    }
}
