package ma.jadwal.export.modele;

import java.util.List;

/**
 * Une ligne de la grille : soit une tranche horaire, soit un bandeau pleine largeur
 * (pause déjeuner, récréation…).
 *
 * @param horaire  libellé de la colonne de gauche, ex. « 09:00 - 09:30 » (null pour un bandeau)
 * @param bandeau  libellé centré du bandeau pleine largeur, ex. « DÉJEUNER — 2H » (null sinon)
 * @param cellules une entrée par jour, dans le même ordre que {@link GrillePlanning#jours()} ;
 *                 {@code null} signifie « case couverte par la fusion d'une séance au-dessus »
 *                 et n'est donc pas dessinée (ignoré pour un bandeau)
 */
public record LignePlanning(String horaire, String bandeau, List<CellulePlanning> cellules) {

    public static LignePlanning horaire(String horaire, List<CellulePlanning> cellules) {
        return new LignePlanning(horaire, null, cellules);
    }

    public static LignePlanning bandeau(String libelle) {
        return new LignePlanning(null, libelle, List.of());
    }

    public boolean estBandeau() {
        return bandeau != null;
    }
}
